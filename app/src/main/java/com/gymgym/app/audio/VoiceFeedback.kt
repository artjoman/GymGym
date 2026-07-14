package com.gymgym.app.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

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

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { engine ->
                    engine.language = Locale.US
                    selectNaturalVoice(engine)
                    // A touch slower than default reads as calmer, less clipped.
                    engine.setSpeechRate(0.95f)
                    engine.setPitch(1.0f)
                }
                ready = true
            }
        }
    }

    /**
     * The engine's default voice is usually its lowest-quality (robotic) one.
     * Upgrade to the best available English voice, preferring on-device voices
     * so the countdown still works offline (gyms have poor signal) and ticks
     * aren't delayed by network latency.
     */
    private fun selectNaturalVoice(engine: TextToSpeech) {
        val voices = runCatching { engine.voices }.getOrNull() ?: return
        val english = voices.filter {
            it.locale.language == Locale.ENGLISH.language &&
                it.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true
        }
        if (english.isEmpty()) return

        val onDevice = english.filterNot { it.isNetworkConnectionRequired }
        val pool = onDevice.ifEmpty { english }
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
