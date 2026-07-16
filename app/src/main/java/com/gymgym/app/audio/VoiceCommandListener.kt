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
 * honour the app's no-network privacy stance and cut latency. While TTS is
 * speaking the caller should [mute] us, otherwise the recognizer hears the
 * app's own voice.
 */
class VoiceCommandListener(
    private val context: Context,
    private val onCommand: (VoiceCommand) -> Unit,
) {
    enum class VoiceCommand { PAUSE, RESUME, NEXT, RESET, START, STOP }

    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var active = false
    private var muted = false

    val available: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context)

    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
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
        val text = candidates.joinToString(" ").lowercase()
        val command = when {
            containsAny(text, "next", "skip") -> VoiceCommand.NEXT
            // "restart"/"again" must be checked before "start" so they don't match START.
            containsAny(text, "reset", "restart", "again") -> VoiceCommand.RESET
            containsAny(text, "resume", "continue", "unpause") -> VoiceCommand.RESUME
            containsAny(text, "start", "begin") -> VoiceCommand.START
            containsAny(text, "stop", "finish", "done", "end") -> VoiceCommand.STOP
            containsAny(text, "pause", "wait", "hold") -> VoiceCommand.PAUSE
            else -> null
        }
        command?.let(onCommand)
    }

    private fun containsAny(text: String, vararg words: String) = words.any { text.contains(it) }

    private companion object {
        const val RESTART_DELAY_MS = 300L
    }
}
