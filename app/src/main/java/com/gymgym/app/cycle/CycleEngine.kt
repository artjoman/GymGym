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
    /** Assigned weekday (1=Mon..7=Sun) in Weekly Schedule mode, else null. */
    val weekday: Int? = null,
)

data class NextMission(
    val cycle: CycleEntity,
    val workout: WorkoutEntity,
    /** Planned start (epoch ms): last workout end + recovery, or the weekly slot. */
    val plannedDate: Long?,
    val weekday: Int? = null,
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

        val weekly = profile.trainingMode == TrainingMode.WEEKLY_SCHEDULE

        // Workout order: plan order normally; by (weekday, position) in Weekly mode.
        fun cycleWorkouts(cw: com.gymgym.app.data.CycleWithWorkouts): List<WorkoutEntity> =
            if (weekly) {
                cw.orderedWorkouts.map { it.workout }
                    .sortedWith(compareBy({ it.weekday ?: 99 }, { it.position }))
            } else {
                cw.orderedWorkouts.map { it.workout }
            }

        val sequence: List<Pair<CycleEntity, WorkoutEntity>> = plan.orderedCycles.flatMap { cw ->
            cycleWorkouts(cw).map { cw.cycle to it }
        }
        if (sequence.isEmpty()) {
            return DashboardState(true, plan.plan.name, null, emptyList(), 0, lastWorkout, null)
        }

        // If every workout is processed, the pass is complete → start a fresh pass.
        val allProcessed = sequence.all { it.second.id in progress }
        val effectiveProgress = if (allProcessed) emptyMap() else progress

        val nextPair = sequence.firstOrNull { it.second.id !in effectiveProgress } ?: sequence.first()
        val currentCycleEntity = nextPair.first
        val currentWorkouts = cycleWorkouts(
            plan.orderedCycles.first { it.cycle.id == currentCycleEntity.id },
        )

        val blocks = currentWorkouts.map { w ->
            val entry = effectiveProgress[w.id]
            val status = when {
                entry?.status == "DONE" -> BlockStatus.DONE
                entry?.status == "SKIPPED" -> BlockStatus.SKIPPED
                w.id == nextPair.second.id -> BlockStatus.NEXT
                else -> BlockStatus.PENDING
            }
            WorkoutBlock(w, status, entry?.percent ?: 0, if (weekly) w.weekday else null)
        }

        val processedInCycle = blocks.count { it.status == BlockStatus.DONE || it.status == BlockStatus.SKIPPED }
        val cyclePercent = if (blocks.isEmpty()) 0 else processedInCycle * 100 / blocks.size

        val nextWorkout = nextPair.second
        val plannedDate = computePlannedDate(profile, nextWorkout.weekday, lastWorkout, now)

        return DashboardState(
            hasActivePlan = true,
            planName = plan.plan.name,
            currentCycle = currentCycleEntity,
            blocks = blocks,
            cyclePercent = cyclePercent,
            lastWorkout = lastWorkout,
            nextMission = NextMission(
                currentCycleEntity, nextWorkout, plannedDate,
                if (weekly) nextWorkout.weekday else null,
            ),
        )
    }

    private fun computePlannedDate(
        profile: Profile,
        nextWeekday: Int?,
        lastWorkout: CompletedWorkout?,
        now: Long,
    ): Long? = when (profile.trainingMode) {
        TrainingMode.SMART_CYCLE -> {
            val base = lastWorkout?.let { it.startedAt + it.durationMs } ?: return now
            base + profile.workoutTimeoutSeconds * 1_000L
        }
        TrainingMode.WEEKLY_SCHEDULE ->
            // The next mission's own weekday if assigned, else the profile's training days.
            nextWeekdaySlot(nextWeekday?.let { setOf(it) } ?: profile.workoutDays, now)
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
