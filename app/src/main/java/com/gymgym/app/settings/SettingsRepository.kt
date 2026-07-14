package com.gymgym.app.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** How often the rep count is spoken aloud. */
enum class RepAnnouncementMode(val label: String, val step: Int) {
    OFF("Off", 0),
    EVERY_REP("Every rep", 1),
    EVERY_5("Every 5 reps", 5),
    EVERY_10("Every 10 reps", 10),
}

data class SoundSettings(
    val soundsEnabled: Boolean = true,
    val countdownVoice: Boolean = true,
    val repAnnouncement: RepAnnouncementMode = RepAnnouncementMode.EVERY_REP,
    val trackingLostBell: Boolean = true,
    val trackingRegainedChime: Boolean = true,
    val setCelebration: Boolean = true,
    val voiceControl: Boolean = false,
)

class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.dataStore

    val settings: Flow<SoundSettings> = dataStore.data.map { prefs ->
        SoundSettings(
            soundsEnabled = prefs[SOUNDS_ENABLED] ?: true,
            countdownVoice = prefs[COUNTDOWN_VOICE] ?: true,
            repAnnouncement = prefs[REP_ANNOUNCEMENT]
                ?.let { stored -> RepAnnouncementMode.entries.find { it.name == stored } }
                ?: RepAnnouncementMode.EVERY_REP,
            trackingLostBell = prefs[TRACKING_LOST_BELL] ?: true,
            trackingRegainedChime = prefs[TRACKING_REGAINED_CHIME] ?: true,
            setCelebration = prefs[SET_CELEBRATION] ?: true,
            voiceControl = prefs[VOICE_CONTROL] ?: false,
        )
    }

    suspend fun setSoundsEnabled(value: Boolean) = dataStore.edit { it[SOUNDS_ENABLED] = value }

    suspend fun setCountdownVoice(value: Boolean) = dataStore.edit { it[COUNTDOWN_VOICE] = value }

    suspend fun setRepAnnouncement(mode: RepAnnouncementMode) =
        dataStore.edit { it[REP_ANNOUNCEMENT] = mode.name }

    suspend fun setTrackingLostBell(value: Boolean) =
        dataStore.edit { it[TRACKING_LOST_BELL] = value }

    suspend fun setTrackingRegainedChime(value: Boolean) =
        dataStore.edit { it[TRACKING_REGAINED_CHIME] = value }

    suspend fun setSetCelebration(value: Boolean) =
        dataStore.edit { it[SET_CELEBRATION] = value }

    suspend fun setVoiceControl(value: Boolean) =
        dataStore.edit { it[VOICE_CONTROL] = value }

    private companion object {
        val SOUNDS_ENABLED = booleanPreferencesKey("sounds_enabled")
        val COUNTDOWN_VOICE = booleanPreferencesKey("countdown_voice")
        val REP_ANNOUNCEMENT = stringPreferencesKey("rep_announcement")
        val TRACKING_LOST_BELL = booleanPreferencesKey("tracking_lost_bell")
        val TRACKING_REGAINED_CHIME = booleanPreferencesKey("tracking_regained_chime")
        val SET_CELEBRATION = booleanPreferencesKey("set_celebration")
        val VOICE_CONTROL = booleanPreferencesKey("voice_control")
    }
}
