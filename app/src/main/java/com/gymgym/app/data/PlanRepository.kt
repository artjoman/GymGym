package com.gymgym.app.data

import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

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

    suspend fun activePlanOnce(): PlanWithCycles? = dao.getActiveOnce()

    /** Snapshot of all plans — used to decide whether a new plan is the user's first. */
    suspend fun plansOnce(): List<PlanWithCycles> = plans.first()

    /**
     * Move [workoutId] to the front of its cycle's *unfinished* workouts.
     *
     * Slots (position, and the weekday attached to that position) stay put;
     * already-executed workouts keep their slot. The unfinished workouts are
     * redistributed over the remaining slots with the selected one first and the
     * others in their existing relative order, so nothing is added, removed or
     * duplicated — each simply inherits the weekday of its new slot.
     */
    @Transaction
    suspend fun makeWorkoutNext(workoutId: Long, doneWorkoutIds: Set<Long>, weekly: Boolean) {
        val plan = dao.getActiveOnce() ?: return
        val cycle = plan.cycles.firstOrNull { c -> c.workouts.any { it.workout.id == workoutId } } ?: return
        val ordered = executionOrder(cycle.workouts.map { it.workout }, weekly)
        val byId = ordered.associateBy { it.id }
        makeNextAssignment(ordered, doneWorkoutIds, workoutId, weekly).forEach { updated ->
            val original = byId[updated.id] ?: return@forEach
            if (original.position != updated.position || original.weekday != updated.weekday) {
                dao.updateWorkout(updated)
            }
        }
    }

    suspend fun deletePlan(id: Long) = dao.deletePlan(id)

    suspend fun allOnce(): List<PlanWithCycles> = dao.getAllOnce()

    suspend fun deleteAll() = dao.deleteAllPlans()
}

/** How the engine reads a cycle: by weekday then position in Weekly Schedule. */
internal fun executionOrder(workouts: List<WorkoutEntity>, weekly: Boolean): List<WorkoutEntity> =
    if (weekly) {
        workouts.sortedWith(compareBy({ it.weekday ?: 99 }, { it.position }))
    } else {
        workouts.sortedBy { it.position }
    }

/**
 * Slot reassignment behind "Make next" (pure, so the rules are testable).
 *
 * Slots — a position and, in Weekly Schedule, the weekday attached to it — stay
 * fixed, and executed workouts keep their slot. The unfinished workouts are laid
 * back over the remaining slots with [selectedId] first and the others in their
 * existing relative order, each inheriting its new slot's weekday. Nothing is
 * added, removed or duplicated.
 *
 * @param ordered the cycle's workouts in execution order.
 * @return every unfinished workout with its new position/weekday.
 */
internal fun makeNextAssignment(
    ordered: List<WorkoutEntity>,
    doneIds: Set<Long>,
    selectedId: Long,
    weekly: Boolean,
): List<WorkoutEntity> {
    if (ordered.none { it.id == selectedId }) return emptyList()
    val unfinishedSlots = ordered.indices.filter { ordered[it].id !in doneIds }
    val unfinished = unfinishedSlots.map { ordered[it] }
    val selected = unfinished.firstOrNull { it.id == selectedId } ?: return emptyList()
    val reordered = listOf(selected) + unfinished.filter { it.id != selected.id }
    return reordered.mapIndexed { i, workout ->
        val slot = ordered[unfinishedSlots[i]]
        workout.copy(
            position = slot.position,
            weekday = if (weekly) slot.weekday else workout.weekday,
        )
    }
}
