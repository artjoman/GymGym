package com.gymgym.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Insert
    suspend fun insert(session: WorkoutSession)

    @Query("SELECT * FROM workout_session ORDER BY startedAt DESC")
    fun allSessions(): Flow<List<WorkoutSession>>

    @Query(
        """
        SELECT exerciseType AS exerciseType,
               COUNT(*) AS sessionCount,
               SUM(repCount) AS totalReps,
               MAX(repCount) AS bestReps
        FROM workout_session
        GROUP BY exerciseType
        """,
    )
    fun stats(): Flow<List<ExerciseStat>>
}
