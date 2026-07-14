package com.gymgym.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Insert
    suspend fun insert(session: WorkoutSession)

    @Insert
    suspend fun insertAll(sessions: List<WorkoutSession>)

    @Query("SELECT * FROM workout_session ORDER BY startedAt DESC")
    suspend fun getAllOnce(): List<WorkoutSession>

    @Query("DELETE FROM workout_session")
    suspend fun deleteAll()

    @Query("SELECT * FROM workout_session ORDER BY startedAt DESC")
    fun allSessions(): Flow<List<WorkoutSession>>

    @Query(
        """
        SELECT exerciseType AS exerciseType,
               COUNT(*) AS sessionCount,
               SUM(repCount) AS totalReps,
               MAX(repCount) AS bestReps,
               AVG(repCount) AS avgReps,
               SUM(durationMs) AS totalDurationMs,
               MAX(startedAt) AS lastPerformedAt,
               MIN(startedAt) AS firstPerformedAt
        FROM workout_session
        GROUP BY exerciseType
        ORDER BY lastPerformedAt DESC
        """,
    )
    fun stats(): Flow<List<ExerciseStat>>
}
