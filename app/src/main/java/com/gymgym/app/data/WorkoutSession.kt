package com.gymgym.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** One completed counting session. [exerciseType] stores the [Exercise] enum name. */
@Entity(tableName = "workout_session")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseType: String,
    val repCount: Int,
    /** Reps completed with good form (≤ repCount). For timed holds, equals repCount. */
    @ColumnInfo(defaultValue = "0") val goodReps: Int = 0,
    val startedAt: Long,
    val durationMs: Long,
)

/** Aggregate row per exercise, produced by [WorkoutDao.stats]. */
data class ExerciseStat(
    val exerciseType: String,
    val sessionCount: Int,
    val totalReps: Int,
    val totalGoodReps: Int,
    val bestReps: Int,
    val avgReps: Double,
    val totalDurationMs: Long,
    val lastPerformedAt: Long,
    val firstPerformedAt: Long,
)
