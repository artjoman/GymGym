package com.gymgym.app.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Thin wrapper around the platform [TextToSpeech] service.
 *
 * Engine init is asynchronous; speak() calls made before the engine is ready
 * are dropped rather than queued so the countdown UI never waits on it.
 * Uses QUEUE_FLUSH so a fast rep pace announces the latest count instead of
 * falling behind a backlog.
 */
class VoiceFeedback(context: Context) {

    private var ready = false
    private var tts: TextToSpeech? = null

    /**
     * The app's current UI language. Spoken cues are fetched already-localized
     * from resources, so the engine must speak in this language to pronounce
     * them correctly (falling back to English if its voice data is missing).
     */
    private val appLocale: Locale = context.resources.configuration.locales.get(0)

    /** True while an utterance is being spoken; used to mute voice recognition. */
    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { engine ->
                    val spoken = selectLanguage(engine)
                    selectNaturalVoice(engine, spoken)
                    // A touch slower than default reads as calmer, less clipped.
                    engine.setSpeechRate(0.95f)
                    engine.setPitch(1.0f)
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) { _speaking.value = true }
                        override fun onDone(utteranceId: String?) { _speaking.value = false }
                        @Deprecated("deprecated in API 21")
                        override fun onError(utteranceId: String?) { _speaking.value = false }
                    })
                }
                ready = true
            }
        }
    }

    /**
     * Set the engine to the app language, falling back to English when that
     * language's voice data isn't installed/supported so cues still speak
     * (mispronounced) rather than going silent. Returns the language actually set.
     */
    private fun selectLanguage(engine: TextToSpeech): Locale {
        val result = runCatching { engine.setLanguage(appLocale) }.getOrDefault(
            TextToSpeech.LANG_NOT_SUPPORTED,
        )
        return if (result == TextToSpeech.LANG_MISSING_DATA ||
            result == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            engine.language = Locale.US
            Locale.US
        } else {
            appLocale
        }
    }

    /**
     * The engine's default voice is usually its lowest-quality (robotic) one.
     * Upgrade to the best available voice for [locale], preferring on-device
     * voices so the countdown still works offline (gyms have poor signal) and
     * ticks aren't delayed by network latency.
     */
    private fun selectNaturalVoice(engine: TextToSpeech, locale: Locale) {
        val voices = runCatching { engine.voices }.getOrNull() ?: return
        val matching = voices.filter {
            it.locale.language == locale.language &&
                it.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true
        }
        if (matching.isEmpty()) return

        val onDevice = matching.filterNot { it.isNetworkConnectionRequired }
        val pool = onDevice.ifEmpty { matching }
        pool.maxByOrNull(Voice::getQuality)?.let { engine.voice = it }
    }

    fun speak(text: String) {
        if (!ready) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
    }

    fun shutdown() {
        ready = false
        tts?.shutdown()
        tts = null
    }
}
