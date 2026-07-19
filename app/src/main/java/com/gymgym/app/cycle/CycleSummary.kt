package com.gymgym.app.cycle

import com.gymgym.app.data.CompletedWorkoutRepository
import com.gymgym.app.data.CompletedWorkoutWithExercises
import com.gymgym.app.data.CycleWithWorkouts
import com.gymgym.app.data.PlanWithCycles
import com.gymgym.app.data.WorkoutWithExercises
import com.gymgym.app.profile.Profile
import com.gymgym.app.profile.TrainingMode

/** How a single workout fared within a cycle. */
enum class CycleLineStatus { DONE, SKIPPED, PENDING }

/** Whether a cycle is the in-progress one or a finished pass. */
enum class CycleStatus { ACTIVE, COMPLETED }

/** One workout's line within a cycle summary. */
data class CycleWorkoutLine(
    val workoutId: Long,
    val name: String,
    val percent: Int,
    val status: CycleLineStatus,
    /** Weekday (1=Mon..7=Sun) in Weekly Schedule mode, else null. */
    val weekday: Int?,
    /** This workout's exercises (results when executed, planned otherwise). */
    val exercises: List<CycleExerciseLine> = emptyList(),
)

/** One exercise within a featured workout, in the shared History display format. */
data class CycleExerciseLine(
    val exerciseRef: String,
    val targetReps: Int,
    val targetSets: Int,
    /** Reps done, or null when the workout hasn't been executed yet (planned only). */
    val completedReps: Int?,
)

/** The featured workout shown in detail under a cycle card. */
data class CycleWorkoutDetail(
    val workoutName: String,
    val exercises: List<CycleExerciseLine>,
)

/**
 * A cycle rolled up for display: the plan/cycle names, the overall completion %
 * (total completed reps ÷ total planned reps across its workouts), and each
 * workout in execution order.
 */
data class CycleSummary(
    val planId: Long?,
    val cycleId: Long,
    val planName: String,
    val cycleName: String,
    val percent: Int,
    val status: CycleStatus = CycleStatus.COMPLETED,
    /** First executed workout time in the cycle, if any. */
    val startedAt: Long? = null,
    /** Last executed workout end time (completed cycles only). */
    val completedAt: Long? = null,
    val workouts: List<CycleWorkoutLine>,
    /**
     * The featured workout's exercise breakdown: the upcoming workout (planned
     * only) for the current cycle, or the most recently executed workout (with
     * results) for a completed cycle. Null when there's nothing to detail.
     */
    val detail: CycleWorkoutDetail? = null,
)

/** The two cycle blocks shown above "Train smarter" on the home screen. */
data class HomeCycles(
    val hasActivePlan: Boolean,
    val lastCycle: CycleSummary?,
    val currentCycle: CycleSummary?,
)

/**
 * Pure reconstruction of cycle roll-ups from the stored plans, the completed
 * workouts (with their per-exercise reps), the current pass's progress, and the
 * profile. No I/O, so it is directly unit-testable.
 *
 * Planned reps come from the plan definition (targetReps × targetSets), so a
 * skipped/never-run workout still contributes its planned reps to the denominator
 * with zero completed. Completed reps come from the latest completed_workout row
 * for that workout within the cycle.
 */
object CycleSummaries {

    fun compute(
        plans: List<PlanWithCycles>,
        activePlan: PlanWithCycles?,
        progress: Map<Long, CycleEngine.ProgressEntry>,
        completed: List<CompletedWorkoutWithExercises>,
        profile: Profile,
    ): HomeCycles {
        val weekly = profile.trainingMode == TrainingMode.WEEKLY_SCHEDULE
        val current = currentCycleSummary(activePlan, progress, completed, weekly)
        val last = lastCycleSummary(plans, completed, weekly, excludeCycleId = current?.cycleId)
        return HomeCycles(hasActivePlan = activePlan != null, lastCycle = last, currentCycle = current)
    }

    /**
     * All cycle records for the Statistics → Cycles tab: the active cycle first
     * (latest), then every completed cycle by recency (excluding the active one).
     */
    fun cycleRecords(
        plans: List<PlanWithCycles>,
        activePlan: PlanWithCycles?,
        progress: Map<Long, CycleEngine.ProgressEntry>,
        completed: List<CompletedWorkoutWithExercises>,
        profile: Profile,
    ): List<CycleSummary> {
        val weekly = profile.trainingMode == TrainingMode.WEEKLY_SCHEDULE
        val active = currentCycleSummary(activePlan, progress, completed, weekly)
        val completedCycles = completed
            .filter { it.workout.cycleId != null && it.workout.cycleId != active?.cycleId }
            .sortedByDescending { it.workout.startedAt }
            .map { it.workout.cycleId!! }
            .distinct()
            .mapNotNull { cycleId ->
                val (plan, cyc) = findCycle(plans, cycleId) ?: return@mapNotNull null
                summarize(plan, cyc, completed.forCycle(cycleId), weekly, progress = null)
            }
        return listOfNotNull(active) + completedCycles
    }

    // --- internals ---

    private fun currentCycleSummary(
        activePlan: PlanWithCycles?,
        progress: Map<Long, CycleEngine.ProgressEntry>,
        completed: List<CompletedWorkoutWithExercises>,
        weekly: Boolean,
    ): CycleSummary? {
        if (activePlan == null || activePlan.cycles.isEmpty()) return null
        val sequence = activePlan.orderedCycles.flatMap { cw -> orderedWorkouts(cw, weekly).map { cw to it } }
        if (sequence.isEmpty()) return null
        val allProcessed = sequence.all { it.second.workout.id in progress }
        val effectiveProgress = if (allProcessed) emptyMap() else progress
        val nextPair = sequence.firstOrNull { it.second.workout.id !in effectiveProgress } ?: sequence.first()
        val cyc = nextPair.first
        val summary = summarize(
            activePlan, cyc, completed.forCycle(cyc.cycle.id), weekly, effectiveProgress,
            status = CycleStatus.ACTIVE,
        )
        // Featured detail = the upcoming workout, planned only (not executed yet).
        return summary.copy(detail = workoutDetail(nextPair.second, completedRow = null))
    }

    private fun lastCycleSummary(
        plans: List<PlanWithCycles>,
        completed: List<CompletedWorkoutWithExercises>,
        weekly: Boolean,
        excludeCycleId: Long?,
    ): CycleSummary? {
        val latest = completed
            .filter { it.workout.cycleId != null && it.workout.cycleId != excludeCycleId }
            .maxByOrNull { it.workout.startedAt } ?: return null
        val cycleId = latest.workout.cycleId ?: return null
        val (plan, cyc) = findCycle(plans, cycleId) ?: return null
        val summary = summarize(plan, cyc, completed.forCycle(cycleId), weekly, progress = null)
        // Featured detail = the most recently executed workout, with its results.
        val featured = cyc.orderedWorkouts.firstOrNull { it.workout.id == latest.workout.workoutId }
        val detail = featured?.let { workoutDetail(it, completedRow = latest) }
        return summary.copy(detail = detail)
    }

    /** Build the featured workout's exercise breakdown (planned only when [completedRow] is null). */
    private fun workoutDetail(
        w: WorkoutWithExercises,
        completedRow: CompletedWorkoutWithExercises?,
    ): CycleWorkoutDetail {
        val completedExercises = completedRow?.orderedExercises
        val lines = w.orderedExercises.mapIndexed { i, pe ->
            CycleExerciseLine(
                exerciseRef = pe.exerciseRef,
                targetReps = pe.targetReps,
                targetSets = pe.targetSets,
                completedReps = completedExercises?.getOrNull(i)?.reps,
            )
        }
        return CycleWorkoutDetail(workoutName = w.workout.name, exercises = lines)
    }

    /**
     * Roll up one cycle. [progress] is supplied for the in-progress current cycle
     * (so a not-yet-run workout reads PENDING, not SKIPPED); for a finished cycle
     * it is null and a workout with no completed row is treated as SKIPPED.
     */
    private fun summarize(
        plan: PlanWithCycles,
        cyc: CycleWithWorkouts,
        completedForCycle: List<CompletedWorkoutWithExercises>,
        weekly: Boolean,
        progress: Map<Long, CycleEngine.ProgressEntry>?,
        status: CycleStatus = CycleStatus.COMPLETED,
    ): CycleSummary {
        var totalCompleted = 0
        var totalPlanned = 0
        val lines = orderedWorkouts(cyc, weekly).map { w ->
            val planned = plannedReps(w)
            val row = completedForCycle
                .filter { it.workout.workoutId == w.workout.id }
                .maxByOrNull { it.workout.startedAt }
            val done = row?.let { r -> r.orderedExercises.sumOf { it.reps } } ?: 0
            totalCompleted += done
            totalPlanned += planned
            val lineStatus = when {
                progress != null -> when (progress[w.workout.id]?.status) {
                    "DONE" -> CycleLineStatus.DONE
                    "SKIPPED" -> CycleLineStatus.SKIPPED
                    else -> if (row != null) CycleLineStatus.DONE else CycleLineStatus.PENDING
                }
                row != null -> CycleLineStatus.DONE
                else -> CycleLineStatus.SKIPPED
            }
            CycleWorkoutLine(
                workoutId = w.workout.id,
                name = w.workout.name,
                percent = CompletedWorkoutRepository.workoutPercent(done, planned),
                status = lineStatus,
                weekday = if (weekly) w.workout.weekday else null,
                exercises = workoutDetail(w, if (lineStatus == CycleLineStatus.PENDING) null else row).exercises,
            )
        }
        val rows = completedForCycle
        return CycleSummary(
            planId = plan.plan.id,
            cycleId = cyc.cycle.id,
            planName = plan.plan.name,
            cycleName = cyc.cycle.name,
            percent = CompletedWorkoutRepository.workoutPercent(totalCompleted, totalPlanned),
            status = status,
            startedAt = rows.minOfOrNull { it.workout.startedAt },
            completedAt = if (status == CycleStatus.COMPLETED) {
                rows.maxOfOrNull { it.workout.startedAt + it.workout.durationMs }
            } else {
                null
            },
            workouts = lines,
        )
    }

    private fun plannedReps(w: WorkoutWithExercises): Int =
        w.orderedExercises.sumOf { it.targetReps * it.targetSets }

    private fun orderedWorkouts(cw: CycleWithWorkouts, weekly: Boolean): List<WorkoutWithExercises> =
        if (weekly) {
            cw.orderedWorkouts.sortedWith(compareBy({ it.workout.weekday ?: 99 }, { it.workout.position }))
        } else {
            cw.orderedWorkouts
        }

    private fun findCycle(plans: List<PlanWithCycles>, cycleId: Long): Pair<PlanWithCycles, CycleWithWorkouts>? {
        for (plan in plans) {
            val cyc = plan.cycles.firstOrNull { it.cycle.id == cycleId }
            if (cyc != null) return plan to cyc
        }
        return null
    }

    private fun List<CompletedWorkoutWithExercises>.forCycle(cycleId: Long) =
        filter { it.workout.cycleId == cycleId }
}
