package com.gymgym.app.cycle

import com.gymgym.app.data.CompletedWorkout
import com.gymgym.app.data.CycleEntity
import com.gymgym.app.data.PlanWithCycles
import com.gymgym.app.data.WorkoutEntity
import com.gymgym.app.profile.Profile
import com.gymgym.app.profile.TrainingMode
import java.util.Calendar

/** Status of a workout within the current cycle pass. */
enum class BlockStatus { DONE, SKIPPED, NEXT, PENDING }

data class WorkoutBlock(
    val workout: WorkoutEntity,
    val status: BlockStatus,
    val percent: Int,
)

data class NextMission(
    val cycle: CycleEntity,
    val workout: WorkoutEntity,
    /** Planned start (epoch ms): last workout end + recovery, or the weekly slot. */
    val plannedDate: Long?,
)

data class DashboardState(
    val hasActivePlan: Boolean,
    val planName: String?,
    val currentCycle: CycleEntity?,
    val blocks: List<WorkoutBlock>,
    /** Fraction (0..100) of the current cycle's workouts processed this pass. */
    val cyclePercent: Int,
    val lastWorkout: CompletedWorkout?,
    val nextMission: NextMission?,
)

/**
 * Pure computation of the home dashboard from the active plan, the current pass's
 * per-workout progress, the last completed workout, and the profile. No I/O — so
 * it is directly unit-testable.
 */
object CycleEngine {

    fun compute(
        plan: PlanWithCycles?,
        progress: Map<Long, ProgressEntry>,
        lastWorkout: CompletedWorkout?,
        profile: Profile,
        now: Long,
    ): DashboardState {
        if (plan == null) {
            return DashboardState(false, null, null, emptyList(), 0, lastWorkout, null)
        }

        // Flattened workout sequence in plan order (cycles then workouts).
        val sequence: List<Pair<CycleEntity, WorkoutEntity>> = plan.orderedCycles.flatMap { cw ->
            cw.orderedWorkouts.map { cw.cycle to it.workout }
        }
        if (sequence.isEmpty()) {
            return DashboardState(true, plan.plan.name, null, emptyList(), 0, lastWorkout, null)
        }

        // If every workout is processed, the pass is complete → start a fresh pass.
        val allProcessed = sequence.all { it.second.id in progress }
        val effectiveProgress = if (allProcessed) emptyMap() else progress

        val nextPair = sequence.firstOrNull { it.second.id !in effectiveProgress } ?: sequence.first()
        val currentCycleEntity = nextPair.first
        val currentWorkouts = plan.orderedCycles
            .first { it.cycle.id == currentCycleEntity.id }
            .orderedWorkouts.map { it.workout }

        val blocks = currentWorkouts.map { w ->
            val entry = effectiveProgress[w.id]
            val status = when {
                entry?.status == "DONE" -> BlockStatus.DONE
                entry?.status == "SKIPPED" -> BlockStatus.SKIPPED
                w.id == nextPair.second.id -> BlockStatus.NEXT
                else -> BlockStatus.PENDING
            }
            WorkoutBlock(w, status, entry?.percent ?: 0)
        }

        val processedInCycle = blocks.count { it.status == BlockStatus.DONE || it.status == BlockStatus.SKIPPED }
        val cyclePercent = if (blocks.isEmpty()) 0 else processedInCycle * 100 / blocks.size

        val plannedDate = computePlannedDate(profile, lastWorkout, now)

        return DashboardState(
            hasActivePlan = true,
            planName = plan.plan.name,
            currentCycle = currentCycleEntity,
            blocks = blocks,
            cyclePercent = cyclePercent,
            lastWorkout = lastWorkout,
            nextMission = NextMission(currentCycleEntity, nextPair.second, plannedDate),
        )
    }

    private fun computePlannedDate(profile: Profile, lastWorkout: CompletedWorkout?, now: Long): Long? =
        when (profile.trainingMode) {
            TrainingMode.SMART_CYCLE -> {
                val base = lastWorkout?.let { it.startedAt + it.durationMs } ?: return now
                base + profile.workoutTimeoutHours * 3_600_000L
            }
            TrainingMode.WEEKLY_SCHEDULE -> nextWeekdaySlot(profile.workoutDays, now)
        }

    /** Next day (from [now]) whose weekday (1=Mon..7=Sun) is in [days], at 08:00 local. */
    private fun nextWeekdaySlot(days: Set<Int>, now: Long): Long? {
        if (days.isEmpty()) return null
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        for (offset in 0..7) {
            val probe = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            // Calendar.MONDAY=2..SUNDAY=1 → map to our 1=Mon..7=Sun.
            val calDay = probe.get(Calendar.DAY_OF_WEEK)
            val ourDay = if (calDay == Calendar.SUNDAY) 7 else calDay - 1
            if (ourDay in days && (offset > 0 || probe.timeInMillis >= now)) {
                return probe.timeInMillis
            }
        }
        return null
    }

    /** Progress of one workout this pass. */
    data class ProgressEntry(val status: String, val percent: Int)
}
