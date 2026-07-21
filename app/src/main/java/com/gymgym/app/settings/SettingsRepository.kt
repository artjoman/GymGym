package com.gymgym.app.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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

enum class CameraFacing { BACK, FRONT }

/** How strict the form check is; adjusts the good-form depth/wobble thresholds. */
enum class FormSensitivity(val label: String) {
    LENIENT("Lenient"),
    STANDARD("Standard"),
    STRICT("Strict"),
}

data class SoundSettings(
    val soundsEnabled: Boolean = true,
    val countdownVoice: Boolean = true,
    val repAnnouncement: RepAnnouncementMode = RepAnnouncementMode.EVERY_REP,
    val trackingLostBell: Boolean = true,
    val trackingRegainedChime: Boolean = true,
    val setCelebration: Boolean = true,
    val voiceControl: Boolean = false,
    /** Mute the mic during a workout: stop listening for voice commands. Persists. */
    val micMuted: Boolean = false,
    /** Show the arcade overlay when an achievement is earned. */
    val achievementCelebration: Boolean = true,
    val cameraFacing: CameraFacing = CameraFacing.BACK,
    val accentTheme: AccentTheme = AccentTheme.AMBER,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.GYM_AMBER,
    val customBackgroundPath: String? = null,
    /** Show live form feedback (shallow / too-fast cues) during a workout. */
    val formFeedback: Boolean = true,
    /** Strict counting: only good-form reps count toward the set. */
    val strictForm: Boolean = false,
    /** How strict the form check is. */
    val formSensitivity: FormSensitivity = FormSensitivity.STANDARD,
)

/** Motivation & Control reminder preferences. */
data class ReminderSettings(
    val upcomingEnabled: Boolean = false,
    val upcomingHours: Int = 2,
    val missedEnabled: Boolean = false,
    val missedHours: Int = 4,
    val cycleProgressEnabled: Boolean = false,
    val bodyReminderEnabled: Boolean = false,
    val bodyReminderDays: Int = 7,
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
            micMuted = prefs[MIC_MUTED] ?: false,
            achievementCelebration = prefs[ACHIEVEMENT_CELEBRATION] ?: true,
            cameraFacing = prefs[CAMERA_FACING]
                ?.let { stored -> CameraFacing.entries.find { it.name == stored } }
                ?: CameraFacing.BACK,
            accentTheme = prefs[ACCENT_THEME]
                ?.let { stored -> AccentTheme.entries.find { it.name == stored } }
                ?: AccentTheme.AMBER,
            backgroundStyle = prefs[BACKGROUND_STYLE]
                ?.let { stored -> BackgroundStyle.entries.find { it.name == stored } }
                ?: BackgroundStyle.GYM_AMBER,
            customBackgroundPath = prefs[CUSTOM_BG_PATH],
            formFeedback = prefs[FORM_FEEDBACK] ?: true,
            strictForm = prefs[STRICT_FORM] ?: false,
            formSensitivity = prefs[FORM_SENSITIVITY]
                ?.let { stored -> FormSensitivity.entries.find { it.name == stored } }
                ?: FormSensitivity.STANDARD,
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

    suspend fun setMicMuted(value: Boolean) =
        dataStore.edit { it[MIC_MUTED] = value }

    suspend fun setAchievementCelebration(value: Boolean) =
        dataStore.edit { it[ACHIEVEMENT_CELEBRATION] = value }

    suspend fun setCameraFacing(facing: CameraFacing) =
        dataStore.edit { it[CAMERA_FACING] = facing.name }

    suspend fun setAccentTheme(theme: AccentTheme) =
        dataStore.edit { it[ACCENT_THEME] = theme.name }

    suspend fun setBackgroundStyle(style: BackgroundStyle) =
        dataStore.edit { it[BACKGROUND_STYLE] = style.name }

    /** Persist the custom background file path and switch to the CUSTOM style. */
    suspend fun setCustomBackground(path: String) = dataStore.edit {
        it[CUSTOM_BG_PATH] = path
        it[BACKGROUND_STYLE] = BackgroundStyle.CUSTOM.name
    }

    suspend fun setFormFeedback(value: Boolean) = dataStore.edit { it[FORM_FEEDBACK] = value }

    suspend fun setStrictForm(value: Boolean) = dataStore.edit { it[STRICT_FORM] = value }

    suspend fun setFormSensitivity(value: FormSensitivity) =
        dataStore.edit { it[FORM_SENSITIVITY] = value.name }

    // --- Reminders (Motivation & Control) ---

    val reminders: Flow<ReminderSettings> = dataStore.data.map { prefs ->
        ReminderSettings(
            upcomingEnabled = prefs[REM_UPCOMING] ?: false,
            upcomingHours = prefs[REM_UPCOMING_H] ?: 2,
            missedEnabled = prefs[REM_MISSED] ?: false,
            missedHours = prefs[REM_MISSED_H] ?: 4,
            cycleProgressEnabled = prefs[REM_CYCLE] ?: false,
            bodyReminderEnabled = prefs[REM_BODY] ?: false,
            bodyReminderDays = prefs[REM_BODY_D] ?: 7,
        )
    }

    suspend fun setUpcomingReminder(enabled: Boolean, hours: Int) = dataStore.edit {
        it[REM_UPCOMING] = enabled
        it[REM_UPCOMING_H] = hours
    }

    suspend fun setMissedReminder(enabled: Boolean, hours: Int) = dataStore.edit {
        it[REM_MISSED] = enabled
        it[REM_MISSED_H] = hours
    }

    suspend fun setCycleProgressReminder(enabled: Boolean) = dataStore.edit { it[REM_CYCLE] = enabled }

    suspend fun setBodyReminder(enabled: Boolean, days: Int) = dataStore.edit {
        it[REM_BODY] = enabled
        it[REM_BODY_D] = days
    }

    private companion object {
        val SOUNDS_ENABLED = booleanPreferencesKey("sounds_enabled")
        val COUNTDOWN_VOICE = booleanPreferencesKey("countdown_voice")
        val REP_ANNOUNCEMENT = stringPreferencesKey("rep_announcement")
        val TRACKING_LOST_BELL = booleanPreferencesKey("tracking_lost_bell")
        val TRACKING_REGAINED_CHIME = booleanPreferencesKey("tracking_regained_chime")
        val SET_CELEBRATION = booleanPreferencesKey("set_celebration")
        val VOICE_CONTROL = booleanPreferencesKey("voice_control")
        val MIC_MUTED = booleanPreferencesKey("mic_muted")
        val ACHIEVEMENT_CELEBRATION = booleanPreferencesKey("achievement_celebration")
        val CAMERA_FACING = stringPreferencesKey("camera_facing")
        val ACCENT_THEME = stringPreferencesKey("accent_theme")
        val BACKGROUND_STYLE = stringPreferencesKey("background_style")
        val CUSTOM_BG_PATH = stringPreferencesKey("custom_bg_path")
        val FORM_FEEDBACK = booleanPreferencesKey("form_feedback")
        val STRICT_FORM = booleanPreferencesKey("strict_form")
        val FORM_SENSITIVITY = stringPreferencesKey("form_sensitivity")
        val REM_UPCOMING = booleanPreferencesKey("rem_upcoming")
        val REM_UPCOMING_H = intPreferencesKey("rem_upcoming_h")
        val REM_MISSED = booleanPreferencesKey("rem_missed")
        val REM_MISSED_H = intPreferencesKey("rem_missed_h")
        val REM_CYCLE = booleanPreferencesKey("rem_cycle")
        val REM_BODY = booleanPreferencesKey("rem_body")
        val REM_BODY_D = intPreferencesKey("rem_body_d")
    }
}
