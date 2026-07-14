package com.gymgym.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.gymgym.app.R

/**
 * Short non-speech cues via [SoundPool]. The same soft bell sample doubles as
 * both signals: normal pitch when tracking is lost, higher pitch when regained.
 */
class SoundEffects(context: Context) {

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val bellId = soundPool.load(context, R.raw.bell_soft, 1)
    private val comboId = soundPool.load(context, R.raw.combo, 1)

    fun playTrackingLost() {
        soundPool.play(bellId, 0.8f, 0.8f, 1, 0, 1f)
    }

    fun playTrackingRegained() {
        soundPool.play(bellId, 0.6f, 0.6f, 1, 0, 1.5f)
    }

    fun playCelebration() {
        soundPool.play(comboId, 1f, 1f, 1, 0, 1f)
    }

    fun release() = soundPool.release()
}
