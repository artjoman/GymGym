package com.gymgym.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One completed counting session. [exerciseType] stores the [Exercise] enum name. */
@Entity(tableName = "workout_session")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseType: String,
    val repCount: Int,
    val startedAt: Long,
    val durationMs: Long,
)

/** Aggregate row per exercise, produced by [WorkoutDao.stats]. */
data class ExerciseStat(
    val exerciseType: String,
    val sessionCount: Int,
    val totalReps: Int,
    val bestReps: Int,
)
