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
    @Query("SELECT * FROM plan ORDER BY createdAt DESC")
    fun plansWithCycles(): Flow<List<PlanWithCycles>>

    @Transaction
    @Query("SELECT * FROM plan ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<PlanWithCycles>

    @Transaction
    @Query("SELECT * FROM plan WHERE id = :id")
    suspend fun getPlanOnce(id: Long): PlanWithCycles?

    @Transaction
    @Query("SELECT * FROM plan WHERE isActive = 1 LIMIT 1")
    fun activePlan(): Flow<PlanWithCycles?>

    @Insert
    suspend fun insertPlan(plan: PlanEntity): Long

    @Update
    suspend fun updatePlan(plan: PlanEntity)

    @Insert
    suspend fun insertCycle(cycle: CycleEntity): Long

    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Insert
    suspend fun insertExercises(items: List<WorkoutExerciseEntity>)

    @Query("DELETE FROM cycle WHERE planId = :planId")
    suspend fun deleteCyclesFor(planId: Long)

    @Query("DELETE FROM plan WHERE id = :planId")
    suspend fun deletePlan(planId: Long)

    @Query("DELETE FROM plan")
    suspend fun deleteAllPlans()

    @Query("UPDATE plan SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE plan SET isActive = 1 WHERE id = :planId")
    suspend fun markActive(planId: Long)
}
