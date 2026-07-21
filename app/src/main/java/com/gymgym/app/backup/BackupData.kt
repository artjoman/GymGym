package com.gymgym.app.backup

import kotlinx.serialization.Serializable

/**
 * Portable snapshot of a user's data for transferring between devices.
 * Intentionally excludes video recordings (they live as separate files).
 */
@Serializable
data class BackupData(
    val version: Int = 5,
    val exportedAt: Long,
    val sessions: List<BackupSession> = emptyList(),
    val plans: List<BackupPlan> = emptyList(),
    val customExercises: List<BackupCustomExercise> = emptyList(),
    val bodyMeasurements: List<BackupBodyMeasurement> = emptyList(),
    /** v5: workout history, which drives Statistics and every achievement. */
    val completedWorkouts: List<BackupCompletedWorkout> = emptyList(),
    /** v5: earned dates only — progress is always recomputed from history. */
    val achievements: List<BackupAchievement> = emptyList(),
    val settings: BackupSettings = BackupSettings(),
    val profile: BackupProfile = BackupProfile(),
)

/**
 * A finished run. Plan/cycle are stored by *name* only: ids are re-generated on
 * import (and change whenever a plan is edited), so names are all that survives.
 */
@Serializable
data class BackupCompletedWorkout(
    val name: String,
    val planName: String = "",
    val cycleName: String = "",
    val startedAt: Long,
    val durationMs: Long,
    val avgPercent: Int,
    val exercises: List<BackupCompletedExercise> = emptyList(),
)

@Serializable
data class BackupCompletedExercise(
    val exerciseRef: String,
    val reps: Int,
    val goodReps: Int = 0,
    val targetReps: Int = 0,
    val targetSets: Int = 0,
    val durationMs: Long = 0,
    val position: Int = 0,
)

@Serializable
data class BackupAchievement(
    val id: String,
    val unlockedAt: Long,
)

@Serializable
data class BackupBodyMeasurement(
    val type: String,
    val value: Double,
    val unit: String,
    val loggedAt: Long,
)

@Serializable
data class BackupCustomExercise(
    val name: String,
    val createdAt: Long,
)

@Serializable
data class BackupSession(
    val exerciseType: String,
    val repCount: Int,
    val goodReps: Int = 0,
    val startedAt: Long,
    val durationMs: Long,
)

@Serializable
data class BackupPlan(
    val name: String,
    val endDate: Long? = null,
    val isActive: Boolean = false,
    val cycles: List<BackupCycle> = emptyList(),
)

@Serializable
data class BackupCycle(
    val name: String,
    val workouts: List<BackupWorkout> = emptyList(),
)

@Serializable
data class BackupWorkout(
    val name: String,
    val weekday: Int? = null,
    val exercises: List<BackupWorkoutExercise> = emptyList(),
)

@Serializable
data class BackupWorkoutExercise(
    val exerciseRef: String,
    val targetReps: Int,
    val targetSets: Int,
    val targetSeconds: Int? = null,
    val position: Int,
)

@Serializable
data class BackupSettings(
    val soundsEnabled: Boolean = true,
    val countdownVoice: Boolean = true,
    val repAnnouncement: String = "EVERY_REP",
    val trackingLostBell: Boolean = true,
    val trackingRegainedChime: Boolean = true,
    val setCelebration: Boolean = true,
    val voiceControl: Boolean = false,
    val cameraFacing: String = "BACK",
    val accentTheme: String = "AMBER",
    val backgroundStyle: String = "GYM_AMBER",
    val formFeedback: Boolean = true,
    val strictForm: Boolean = false,
    val formSensitivity: String = "STANDARD",
)

@Serializable
data class BackupProfile(
    val displayName: String = "",
    val weightUnit: String = "KG",
    val lengthUnit: String = "CM",
    val trainingMode: String = "SMART_CYCLE",
    val workoutDays: String = "2,3,4,5,6,7",
    val workoutTimeoutSeconds: Int = 172_800,
    val setTimeoutSeconds: Int = 180,
    val exerciseTimeoutSeconds: Int = 180,
)
