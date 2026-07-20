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
        val last = completedPasses(plans, current?.cycleId, progress, completed, weekly).firstOrNull()
        return HomeCycles(hasActivePlan = activePlan != null, lastCycle = last, currentCycle = current)
    }

    /**
     * Completed cycle records for the Statistics → Cycles tab, most recent first.
     * The active cycle is deliberately excluded — it's shown (expanded) on the
     * workout execution screen instead.
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
        return completedPasses(plans, active?.cycleId, progress, completed, weekly)
    }

    /**
     * Every finished cycle *pass*, most recent first.
     *
     * A pass is not persisted (the per-workout progress is cleared when a pass
     * completes and the same cycle becomes active again), so passes are
     * reconstructed from the completed_workout rows: within one cycle, a repeated
     * workout starts a new pass. The final pass of the currently-active cycle is
     * still in progress when that cycle has any progress recorded, so it's
     * excluded; every earlier pass is a completed cycle. Workouts of the cycle
     * with no row in a pass were skipped (0%).
     */
    private fun completedPasses(
        plans: List<PlanWithCycles>,
        activeCycleId: Long?,
        progress: Map<Long, CycleEngine.ProgressEntry>,
        completed: List<CompletedWorkoutWithExercises>,
        weekly: Boolean,
    ): List<CycleSummary> {
        val byCycle = completed.filter { it.workout.cycleId != null }.groupBy { it.workout.cycleId!! }
        val out = mutableListOf<CycleSummary>()
        for ((cycleId, rows) in byCycle) {
            val (plan, cyc) = findCycle(plans, cycleId) ?: continue
            val passes = splitPasses(rows)
            // The last pass of the active cycle is the one still being worked on.
            val lastIsInProgress = cycleId == activeCycleId &&
                cyc.workouts.any { it.workout.id in progress }
            val finished = if (lastIsInProgress) passes.dropLast(1) else passes
            finished.forEach { pass ->
                out += summarize(plan, cyc, pass, weekly, progress = null, status = CycleStatus.COMPLETED)
            }
        }
        return out.sortedByDescending { it.completedAt ?: it.startedAt ?: 0L }
    }

    /** Split one cycle's completed workouts into passes; a repeated workout starts a new pass. */
    private fun splitPasses(
        rows: List<CompletedWorkoutWithExercises>,
    ): List<List<CompletedWorkoutWithExercises>> {
        val passes = mutableListOf<List<CompletedWorkoutWithExercises>>()
        var current = mutableListOf<CompletedWorkoutWithExercises>()
        val seen = mutableSetOf<Long>()
        for (row in rows.sortedBy { it.workout.startedAt }) {
            val id = row.workout.workoutId
            if (id != null && id in seen) {
                passes += current
                current = mutableListOf()
                seen.clear()
            }
            current += row
            if (id != null) seen += id
        }
        if (current.isNotEmpty()) passes += current
        return passes
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
        // Only this pass's rows count — earlier passes of the same cycle must not
        // make already-reset workouts look done again.
        val passRows = if (effectiveProgress.isEmpty()) {
            emptyList()
        } else {
            splitPasses(completed.forCycle(cyc.cycle.id)).lastOrNull().orEmpty()
        }
        val summary = summarize(
            activePlan, cyc, passRows, weekly, effectiveProgress,
            status = CycleStatus.ACTIVE,
        )
        // Featured detail = the upcoming workout, planned only (not executed yet).
        return summary.copy(detail = workoutDetail(nextPair.second, completedRow = null))
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
        val lines = orderedWorkouts(cyc, weekly).map { w ->
            val row = completedForCycle
                .filter { it.workout.workoutId == w.workout.id }
                .maxByOrNull { it.workout.startedAt }
            val lineStatus = when {
                progress != null -> when (progress[w.workout.id]?.status) {
                    "DONE" -> CycleLineStatus.DONE
                    "SKIPPED" -> CycleLineStatus.SKIPPED
                    else -> if (row != null) CycleLineStatus.DONE else CycleLineStatus.PENDING
                }
                row != null -> CycleLineStatus.DONE
                else -> CycleLineStatus.SKIPPED
            }
            val exercises = workoutDetail(w, if (lineStatus == CycleLineStatus.PENDING) null else row).exercises
            // Workout % = average of its exercises'; a skipped workout counts as 0.
            val workoutPercent = if (lineStatus == CycleLineStatus.SKIPPED) {
                0
            } else {
                CompletedWorkoutRepository.averagePercent(
                    exercises.map {
                        CompletedWorkoutRepository.exercisePercent(
                            it.completedReps ?: 0,
                            it.targetReps * it.targetSets,
                        )
                    },
                )
            }
            CycleWorkoutLine(
                workoutId = w.workout.id,
                name = w.workout.name,
                percent = workoutPercent,
                status = lineStatus,
                weekday = if (weekly) w.workout.weekday else null,
                exercises = exercises,
            )
        }
        val rows = completedForCycle
        return CycleSummary(
            planId = plan.plan.id,
            cycleId = cyc.cycle.id,
            planName = plan.plan.name,
            cycleName = cyc.cycle.name,
            // Cycle % = average of its workouts' percentages.
            percent = CompletedWorkoutRepository.averagePercent(lines.map { it.percent }),
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
