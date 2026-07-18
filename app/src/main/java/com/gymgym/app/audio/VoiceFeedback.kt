package com.gymgym.app.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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

    /**
     * Whether we may use higher-quality network TTS voices. True only when the
     * build holds the INTERNET permission — the free flavor does; the paid
     * flavor stays fully offline, so it never reaches for a network voice.
     */
    private val networkAllowed: Boolean =
        context.checkSelfPermission(Manifest.permission.INTERNET) ==
            PackageManager.PERMISSION_GRANTED

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
     * Upgrade to the best available voice for [locale].
     *
     * We prefer on-device voices so the countdown works offline (gyms have poor
     * signal) without network latency, and keep them whenever a decent one
     * exists. But many non-English languages ship only a poor offline voice
     * while a much better one is available over the network — in that case, if
     * the build is allowed network access, fall back to the network voice for
     * natural-sounding cues. English is unaffected: its on-device voices are
     * good, so it stays offline.
     */
    private fun selectNaturalVoice(engine: TextToSpeech, locale: Locale) {
        val voices = runCatching { engine.voices }.getOrNull() ?: return
        val matching = voices.filter {
            it.locale.language == locale.language &&
                it.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true
        }
        if (matching.isEmpty()) return

        val bestOnDevice = matching.filterNot { it.isNetworkConnectionRequired }
            .maxByOrNull(Voice::getQuality)

        val chosen = if (bestOnDevice != null && bestOnDevice.quality >= Voice.QUALITY_NORMAL) {
            // A good enough offline voice — keep it offline.
            bestOnDevice
        } else if (networkAllowed) {
            // Poor/absent offline voice: use the best network voice if it wins.
            val bestNetwork = matching.filter { it.isNetworkConnectionRequired }
                .maxByOrNull(Voice::getQuality)
            listOfNotNull(bestNetwork, bestOnDevice).maxByOrNull(Voice::getQuality)
        } else {
            // Offline-only build: best available on-device voice, or any match.
            bestOnDevice ?: matching.maxByOrNull(Voice::getQuality)
        }
        chosen?.let { engine.voice = it }
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
