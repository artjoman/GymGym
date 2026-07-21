package com.gymgym.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Progress of the *current cycle pass* of the active plan: which workouts have
 * been done or skipped. Cleared when the whole plan's workouts are processed and
 * a new pass begins. Distinct from [CompletedWorkout] (full history).
 */
@Entity(tableName = "workout_progress")
data class WorkoutProgress(
    @PrimaryKey val workoutId: Long,
    /** "DONE" or "SKIPPED". */
    val status: String,
    /** Completion percent (0 for skipped). */
    val percent: Int,
    val at: Long,
)

@Dao
interface WorkoutProgressDao {

    @Query("SELECT * FROM workout_progress")
    fun all(): Flow<List<WorkoutProgress>>

    @Query("SELECT * FROM workout_progress")
    suspend fun allOnce(): List<WorkoutProgress>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: WorkoutProgress)

    @Query("DELETE FROM workout_progress WHERE workoutId = :workoutId")
    suspend fun clearFor(workoutId: Long)

    @Query("DELETE FROM workout_progress")
    suspend fun clear()
}

class WorkoutProgressRepository(private val dao: WorkoutProgressDao) {

    val all: Flow<List<WorkoutProgress>> = dao.all()

    suspend fun allOnce(): List<WorkoutProgress> = dao.allOnce()

    suspend fun markDone(workoutId: Long, percent: Int) =
        dao.upsert(WorkoutProgress(workoutId, "DONE", percent, System.currentTimeMillis()))

    suspend fun markSkipped(workoutId: Long) =
        dao.upsert(WorkoutProgress(workoutId, "SKIPPED", 0, System.currentTimeMillis()))

    /** Un-process a workout (e.g. promoting a skipped one back to current). */
    suspend fun clearFor(workoutId: Long) = dao.clearFor(workoutId)

    suspend fun clear() = dao.clear()
}
