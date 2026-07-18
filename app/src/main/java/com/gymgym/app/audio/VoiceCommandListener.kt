package com.gymgym.app.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Continuous hands-free command listener built on [SpeechRecognizer].
 *
 * [SpeechRecognizer] is one-shot, so we restart it after every result/error to
 * keep listening. Recognition is forced on-device (EXTRA_PREFER_OFFLINE) to
 * honour the app's no-network privacy stance and cut latency, and pinned to the
 * app's current language so localized command words are transcribed correctly.
 * While TTS is speaking the caller should [mute] us, otherwise the recognizer
 * hears the app's own voice.
 */
class VoiceCommandListener(
    private val context: Context,
    private val onCommand: (VoiceCommand) -> Unit,
) {
    enum class VoiceCommand {
        PAUSE, RESUME, NEXT, RESET,
        START_TIMER, STOP_TIMER,
        START_RECORDING, STOP_RECORDING,
        SWITCH_CAMERA,
    }

    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var active = false
    private var muted = false

    /** App language tag (e.g. "es", "zh-CN") the recognizer transcribes in. */
    private val languageTag: String =
        context.resources.configuration.locales.get(0).toLanguageTag()

    /** English command words plus this language's synonyms (English always works). */
    private val vocabulary: CommandVocabulary = CommandVocabulary.forLanguageTag(languageTag)

    val available: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context)

    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
    }

    fun start() {
        if (!available || active) return
        active = true
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(listener)
        }
        listenAgain()
    }

    fun stop() {
        active = false
        handler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        recognizer = null
    }

    /** Pause recognition (e.g. while TTS speaks) without tearing down the engine. */
    fun mute() {
        if (muted) return
        muted = true
        recognizer?.cancel()
    }

    fun unmute() {
        if (!muted) return
        muted = false
        listenAgain()
    }

    private fun listenAgain() {
        if (!active || muted) return
        try {
            recognizer?.startListening(intent)
        } catch (_: Exception) {
            // Recognizer momentarily busy; try again shortly.
            restartSoon()
        }
    }

    private fun restartSoon() {
        if (!active || muted) return
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ listenAgain() }, RESTART_DELAY_MS)
    }

    private val listener = object : RecognitionListener {
        override fun onResults(results: Bundle?) {
            parse(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty())
            restartSoon()
        }

        override fun onError(error: Int) = restartSoon()

        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun parse(candidates: List<String>) {
        commandFor(candidates.joinToString(" ").lowercase(), vocabulary)?.let(onCommand)
    }

    companion object {
        private const val RESTART_DELAY_MS = 300L

        /**
         * Map recognized (lowercased) speech to a command. Order matters: multi-word
         * intents ("start recording", "switch camera") and "restart" are matched
         * before the bare "start"/"stop" timer rules. Visible for testing.
         */
        internal fun commandFor(
            text: String,
            vocab: CommandVocabulary = CommandVocabulary.ENGLISH,
        ): VoiceCommand? = when {
            containsAny(text, vocab.record) ->
                if (containsAny(text, vocab.stopModifiers)) {
                    VoiceCommand.STOP_RECORDING
                } else {
                    VoiceCommand.START_RECORDING
                }
            containsAny(text, vocab.switchCamera) -> VoiceCommand.SWITCH_CAMERA
            containsAny(text, vocab.next) -> VoiceCommand.NEXT
            containsAny(text, vocab.reset) -> VoiceCommand.RESET
            containsAny(text, vocab.resume) -> VoiceCommand.RESUME
            containsAny(text, vocab.start) -> VoiceCommand.START_TIMER
            containsAny(text, vocab.stop) -> VoiceCommand.STOP_TIMER
            containsAny(text, vocab.pause) -> VoiceCommand.PAUSE
            else -> null
        }

        private fun containsAny(text: String, words: List<String>) =
            words.any { it.isNotEmpty() && text.contains(it) }
    }
}
