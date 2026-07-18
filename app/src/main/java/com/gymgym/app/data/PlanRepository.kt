package com.gymgym.app.data

import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Editable snapshots of the plan tree, before it has a database identity. */
data class DraftPlan(
    val name: String,
    val endDate: Long?,
    val cycles: List<DraftCycle>,
)

data class DraftCycle(
    val name: String,
    val workouts: List<DraftWorkout>,
)

data class DraftWorkout(
    val name: String,
    val weekday: Int?,
    val exercises: List<DraftWorkoutExercise>,
)

data class DraftWorkoutExercise(
    val exerciseRef: String,
    val targetReps: Int,
    val targetSets: Int,
    val targetSeconds: Int?,
)

class PlanRepository(private val dao: PlanDao) {

    val plans: Flow<List<PlanWithCycles>> = dao.plansWithCycles()

    val activePlan: Flow<PlanWithCycles?> = dao.activePlan()

    /** Insert (id == 0) or replace an existing plan and its whole cycle/workout tree. */
    @Transaction
    suspend fun savePlan(id: Long, draft: DraftPlan): Long {
        val planId: Long
        if (id == 0L) {
            planId = dao.insertPlan(
                PlanEntity(
                    name = draft.name,
                    endDate = draft.endDate,
                    isActive = false,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        } else {
            planId = id
            // Preserve active flag and creation time; replace the tree wholesale.
            val existing = dao.getPlanOnce(id)?.plan
            dao.updatePlan(
                PlanEntity(
                    id = id,
                    name = draft.name,
                    endDate = draft.endDate,
                    isActive = existing?.isActive ?: false,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                ),
            )
            dao.deleteCyclesFor(id) // cascades to workouts + exercises
        }

        draft.cycles.forEachIndexed { cIndex, cycle ->
            val cycleId = dao.insertCycle(
                CycleEntity(planId = planId, name = cycle.name, position = cIndex),
            )
            cycle.workouts.forEachIndexed { wIndex, workout ->
                val workoutId = dao.insertWorkout(
                    WorkoutEntity(
                        cycleId = cycleId,
                        name = workout.name,
                        position = wIndex,
                        weekday = workout.weekday,
                    ),
                )
                dao.insertExercises(
                    workout.exercises.mapIndexed { eIndex, e ->
                        WorkoutExerciseEntity(
                            workoutId = workoutId,
                            exerciseRef = e.exerciseRef,
                            targetReps = e.targetReps,
                            targetSets = e.targetSets,
                            targetSeconds = e.targetSeconds,
                            position = eIndex,
                        )
                    },
                )
            }
        }
        return planId
    }

    /** Make [id] the single active plan. */
    @Transaction
    suspend fun setActivePlan(id: Long) {
        dao.clearActive()
        dao.markActive(id)
    }

    suspend fun deletePlan(id: Long) = dao.deletePlan(id)

    suspend fun allOnce(): List<PlanWithCycles> = dao.getAllOnce()

    suspend fun deleteAll() = dao.deleteAllPlans()
}
