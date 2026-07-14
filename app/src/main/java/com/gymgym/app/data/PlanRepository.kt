package com.gymgym.app.data

import kotlinx.coroutines.flow.Flow

/** Editable snapshot of one exercise row, before it has a database identity. */
data class DraftExercise(
    val exerciseType: String,
    val targetReps: Int,
    val targetSets: Int,
)

class PlanRepository(private val dao: PlanDao) {

    val plans: Flow<List<PlanWithExercises>> = dao.plansWithExercises()

    /** Insert (id == 0) or replace an existing plan and its exercise rows. */
    suspend fun savePlan(id: Long, name: String, exercises: List<DraftExercise>) {
        val planId = if (id == 0L) {
            dao.insertPlan(PlanEntity(name = name))
        } else {
            dao.updatePlan(PlanEntity(id = id, name = name))
            dao.deleteExercisesFor(id)
            id
        }
        dao.insertExercises(
            exercises.mapIndexed { index, e ->
                PlanExerciseEntity(
                    planId = planId,
                    exerciseType = e.exerciseType,
                    targetReps = e.targetReps,
                    targetSets = e.targetSets,
                    position = index,
                )
            },
        )
    }

    suspend fun deletePlan(id: Long) = dao.deletePlan(id)

    suspend fun allOnce(): List<PlanWithExercises> = dao.getAllOnce()

    suspend fun deleteAll() = dao.deleteAllPlans()
}
