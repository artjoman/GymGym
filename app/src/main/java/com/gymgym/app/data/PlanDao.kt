package com.gymgym.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {

    @Transaction
    @Query("SELECT * FROM plan ORDER BY name COLLATE NOCASE")
    fun plansWithExercises(): Flow<List<PlanWithExercises>>

    @Transaction
    @Query("SELECT * FROM plan ORDER BY name COLLATE NOCASE")
    suspend fun getAllOnce(): List<PlanWithExercises>

    @Query("DELETE FROM plan")
    suspend fun deleteAllPlans()

    @Insert
    suspend fun insertPlan(plan: PlanEntity): Long

    @Update
    suspend fun updatePlan(plan: PlanEntity)

    @Insert
    suspend fun insertExercises(items: List<PlanExerciseEntity>)

    @Query("DELETE FROM plan_exercise WHERE planId = :planId")
    suspend fun deleteExercisesFor(planId: Long)

    @Query("DELETE FROM plan WHERE id = :planId")
    suspend fun deletePlan(planId: Long)
}
