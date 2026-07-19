package com.gymgym.app.profile

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.profileDataStore by preferencesDataStore(name = "profile")

enum class WeightUnit(val label: String) { KG("Kilograms (kg)"), LB("Pounds (lb)") }

enum class LengthUnit(val label: String) { CM("Centimeters (cm)"), IN("Inches (in)") }

/** How the app schedules the next workout. */
enum class TrainingMode { SMART_CYCLE, WEEKLY_SCHEDULE }

data class Profile(
    val displayName: String = "",
    val weightUnit: WeightUnit = WeightUnit.KG,
    val lengthUnit: LengthUnit = LengthUnit.CM,
    val trainingMode: TrainingMode = TrainingMode.SMART_CYCLE,
    /** Weekdays (1=Mon..7=Sun) the user trains in Weekly Schedule mode. */
    val workoutDays: Set<Int> = setOf(2, 3, 4, 5, 6, 7),
    /** Recovery: seconds between workouts (Smart Cycle next-date offset). Default 48h. */
    val workoutTimeoutSeconds: Int = 172_800,
    /** Recovery: rest seconds between sets. */
    val setTimeoutSeconds: Int = 180,
    /** Recovery: rest seconds between exercises. */
    val exerciseTimeoutSeconds: Int = 180,
)

class ProfileRepository(context: Context) {

    private val dataStore = context.applicationContext.profileDataStore

    val profile: Flow<Profile> = dataStore.data.map { prefs ->
        Profile(
            displayName = prefs[DISPLAY_NAME] ?: "",
            weightUnit = prefs[WEIGHT_UNIT]
                ?.let { stored -> WeightUnit.entries.find { it.name == stored } }
                ?: WeightUnit.KG,
            lengthUnit = prefs[LENGTH_UNIT]
                ?.let { stored -> LengthUnit.entries.find { it.name == stored } }
                ?: LengthUnit.CM,
            trainingMode = prefs[TRAINING_MODE]
                ?.let { stored -> TrainingMode.entries.find { it.name == stored } }
                ?: TrainingMode.SMART_CYCLE,
            workoutDays = prefs[WORKOUT_DAYS]
                ?.split(",")?.mapNotNull { it.toIntOrNull() }?.toSet()
                ?: setOf(2, 3, 4, 5, 6, 7),
            workoutTimeoutSeconds = prefs[WORKOUT_TIMEOUT_S] ?: 172_800,
            setTimeoutSeconds = prefs[SET_TIMEOUT_S] ?: 180,
            exerciseTimeoutSeconds = prefs[EXERCISE_TIMEOUT_S] ?: 180,
        )
    }

    suspend fun setDisplayName(value: String) = dataStore.edit { it[DISPLAY_NAME] = value }

    suspend fun setWeightUnit(unit: WeightUnit) = dataStore.edit { it[WEIGHT_UNIT] = unit.name }

    suspend fun setLengthUnit(unit: LengthUnit) = dataStore.edit { it[LENGTH_UNIT] = unit.name }

    suspend fun setTrainingMode(mode: TrainingMode) = dataStore.edit { it[TRAINING_MODE] = mode.name }

    suspend fun setWorkoutDays(days: Set<Int>) =
        dataStore.edit { it[WORKOUT_DAYS] = days.sorted().joinToString(",") }

    suspend fun setWorkoutTimeoutSeconds(seconds: Int) =
        dataStore.edit { it[WORKOUT_TIMEOUT_S] = normalizeWorkoutTimeoutSeconds(seconds) }

    suspend fun setSetTimeoutSeconds(seconds: Int) =
        dataStore.edit { it[SET_TIMEOUT_S] = seconds }

    suspend fun setExerciseTimeoutSeconds(seconds: Int) =
        dataStore.edit { it[EXERCISE_TIMEOUT_S] = seconds }

    companion object {
        // Between-workouts recovery is chosen in whole hours, in 8-hour steps.
        const val WORKOUT_HOUR_STEP = 8
        const val WORKOUT_MIN_HOURS = 8
        const val WORKOUT_MAX_HOURS = 168

        /** The stored between-workouts value (seconds) as a valid 8-hour step in [8h, 168h]. */
        fun workoutTimeoutHours(seconds: Int): Int {
            val hours = Math.round(seconds / 3600.0).toInt()
            val stepped = Math.round(hours / WORKOUT_HOUR_STEP.toDouble()).toInt() * WORKOUT_HOUR_STEP
            return stepped.coerceIn(WORKOUT_MIN_HOURS, WORKOUT_MAX_HOURS)
        }

        /** Normalize any between-workouts value (seconds) onto a valid 8-hour step. */
        fun normalizeWorkoutTimeoutSeconds(seconds: Int): Int = workoutTimeoutHours(seconds) * 3600

        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val LENGTH_UNIT = stringPreferencesKey("length_unit")
        val TRAINING_MODE = stringPreferencesKey("training_mode")
        val WORKOUT_DAYS = stringPreferencesKey("workout_days")
        val WORKOUT_TIMEOUT_S = intPreferencesKey("workout_timeout_s")
        val SET_TIMEOUT_S = intPreferencesKey("set_timeout_s")
        val EXERCISE_TIMEOUT_S = intPreferencesKey("exercise_timeout_s")
    }
}
