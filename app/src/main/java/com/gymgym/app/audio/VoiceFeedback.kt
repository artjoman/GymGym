package com.gymgym.app.audio

import android.content.Context
import android.speech.tts.TextToSpeech
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
                tts?.language = Locale.US
                ready = true
            }
        }
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
