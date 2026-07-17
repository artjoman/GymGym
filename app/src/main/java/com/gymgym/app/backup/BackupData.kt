package com.gymgym.app.backup

import kotlinx.serialization.Serializable

/**
 * Portable snapshot of a user's data for transferring between devices.
 * Intentionally excludes video recordings (they live as separate files).
 */
@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: Long,
    val sessions: List<BackupSession> = emptyList(),
    val plans: List<BackupPlan> = emptyList(),
    val settings: BackupSettings = BackupSettings(),
    val profile: BackupProfile = BackupProfile(),
)

@Serializable
data class BackupSession(
    val exerciseType: String,
    val repCount: Int,
    val startedAt: Long,
    val durationMs: Long,
)

@Serializable
data class BackupPlan(
    val name: String,
    val exercises: List<BackupPlanExercise> = emptyList(),
)

@Serializable
data class BackupPlanExercise(
    val exerciseType: String,
    val targetReps: Int,
    val targetSets: Int,
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
    val accentTheme: String = "EMERALD",
    val backgroundStyle: String = "GYM_EMERALD",
)

@Serializable
data class BackupProfile(
    val displayName: String = "",
    val weightUnit: String = "KG",
)
