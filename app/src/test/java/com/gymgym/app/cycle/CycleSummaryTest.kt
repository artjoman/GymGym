package com.gymgym.app.cycle

import com.gymgym.app.data.CompletedExercise
import com.gymgym.app.data.CompletedWorkout
import com.gymgym.app.data.CompletedWorkoutWithExercises
import com.gymgym.app.data.CycleEntity
import com.gymgym.app.data.CycleWithWorkouts
import com.gymgym.app.data.PlanEntity
import com.gymgym.app.data.PlanWithCycles
import com.gymgym.app.data.WorkoutEntity
import com.gymgym.app.data.WorkoutExerciseEntity
import com.gymgym.app.data.WorkoutWithExercises
import com.gymgym.app.profile.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CycleSummaryTest {

    // Plan 1 → Cycle 7 → three workouts. Each workout has a single rep-exercise.
    private fun workout(id: Long, name: String, pos: Int, targetReps: Int, targetSets: Int) =
        WorkoutWithExercises(
            WorkoutEntity(id, 7, name, pos, null),
            listOf(
                WorkoutExerciseEntity(
                    workoutId = id, exerciseRef = "squat",
                    targetReps = targetReps, targetSets = targetSets, targetSeconds = null, position = 0,
                ),
            ),
        )

    private val plan = PlanWithCycles(
        plan = PlanEntity(id = 1, name = "Custom Plan", endDate = null, isActive = true, createdAt = 0),
        cycles = listOf(
            CycleWithWorkouts(
                CycleEntity(7, 1, "Full Body Cycle", 0),
                listOf(
                    workout(10, "Workout 1", 0, 10, 3), // 30 planned
                    workout(11, "Workout 2", 1, 15, 3), // 45 planned
                    workout(12, "Workout 3", 2, 8, 3),  // 24 planned
                ),
            ),
        ),
    )

    private fun completed(id: Long, workoutId: Long, reps: Int, startedAt: Long) =
        CompletedWorkoutWithExercises(
            CompletedWorkout(id, 1, 7, workoutId, "W", startedAt, 0, 0),
            listOf(CompletedExercise(id, id, "squat", reps, reps, 0, 0, 0, 0)),
        )

    @Test
    fun lastCycleRollsUpRepsAndMarksSkipped() {
        // Workout 1: 30/30, Workout 2: 43/45, Workout 3: skipped (no row).
        val done = listOf(
            completed(1, 10, 30, 100),
            completed(2, 11, 43, 200),
        )
        // No active plan → the completed cycle is the "last" one (not excluded as current).
        val summary = CycleSummaries.cycleRecords(
            plans = listOf(plan),
            activePlan = null,
            progress = emptyMap(),
            completed = done,
            profile = Profile(),
        ).first()

        assertEquals("Full Body Cycle", summary.cycleName)
        assertEquals("Custom Plan", summary.planName)
        // Hierarchical: workouts are 100%, 96% (43/45) and 0% (skipped)
        // → cycle = (100 + 96 + 0) / 3 = 65.3% → 65%.
        assertEquals(65, summary.percent)

        val (w1, w2, w3) = summary.workouts
        assertEquals(CycleLineStatus.DONE, w1.status)
        assertEquals(100, w1.percent)
        assertEquals(CycleLineStatus.DONE, w2.status)
        assertEquals(96, w2.percent) // 43/45 = 95.6% → 96
        assertEquals(CycleLineStatus.SKIPPED, w3.status)
        assertEquals(0, w3.percent)
    }

    @Test
    fun homeSeparatesCurrentAndLastCycle() {
        val done = listOf(completed(1, 10, 30, 100))
        val home = CycleSummaries.compute(
            plans = listOf(plan),
            activePlan = plan,
            progress = mapOf(10L to CycleEngine.ProgressEntry("DONE", 100)),
            completed = done,
            profile = Profile(),
        )
        // The active plan's only cycle is the current one; there's no *other*
        // completed cycle, so Last cycle is empty.
        assertEquals(7L, home.currentCycle?.cycleId)
        assertNull(home.lastCycle)
        assertTrue(home.hasActivePlan)
    }

    @Test
    fun currentCycleDetailIsUpcomingWorkoutPlannedOnly() {
        val done = listOf(completed(1, 10, 30, 100))
        val home = CycleSummaries.compute(
            plans = listOf(plan),
            activePlan = plan,
            progress = mapOf(10L to CycleEngine.ProgressEntry("DONE", 100)),
            completed = done,
            profile = Profile(),
        )
        // Workout 10 done → the next workout is 11 ("Workout 2"), shown planned only.
        val detail = home.currentCycle?.detail
        assertEquals("Workout 2", detail?.workoutName)
        val ex = detail?.exercises?.first()
        assertEquals(15, ex?.targetReps)
        assertEquals(3, ex?.targetSets)
        assertNull(ex?.completedReps) // not executed yet
    }

    @Test
    fun finishedPassAppearsAsCompletedCycleEvenWhenCycleGoesActiveAgain() {
        // A full pass (all three workouts) then progress cleared — the same cycle is
        // active again for a fresh pass, but the finished pass must still show up.
        val done = listOf(
            completed(1, 10, 30, 100),
            completed(2, 11, 45, 200),
            completed(3, 12, 24, 300),
        )
        val home = CycleSummaries.compute(
            plans = listOf(plan),
            activePlan = plan,
            progress = emptyMap(), // pass rolled over
            completed = done,
            profile = Profile(),
        )
        assertEquals(7L, home.lastCycle?.cycleId)
        assertEquals(CycleStatus.COMPLETED, home.lastCycle?.status)
        // 99 done of 99 planned (30 + 45 + 24).
        assertEquals(100, home.lastCycle?.percent)
        // The fresh active pass shows nothing done yet.
        assertEquals(0, home.currentCycle?.percent)
    }

    @Test
    fun inProgressPassIsExcludedFromCompleted() {
        // Pass 1 complete, then Workout 1 started again (repeat = new pass in progress).
        val done = listOf(
            completed(1, 10, 30, 100),
            completed(2, 11, 45, 200),
            completed(3, 12, 24, 300),
            completed(4, 10, 30, 400), // repeat → pass 2
        )
        val records = CycleSummaries.cycleRecords(
            plans = listOf(plan),
            activePlan = plan,
            progress = mapOf(10L to CycleEngine.ProgressEntry("DONE", 100)),
            completed = done,
            profile = Profile(),
        )
        // Statistics → Cycles lists only completed passes (the active cycle lives on
        // the execution screen), and pass 2 is still in progress, so exactly one.
        assertEquals(1, records.size)
        assertEquals(CycleStatus.COMPLETED, records.first().status)
    }

    @Test
    fun completedCycleSurvivesPlanEditOrDeletion() {
        // Editing a plan replaces its cycle/workout rows with new ids (and deleting
        // one cascades them away), so the old cycle definition is gone. History must
        // still render from the snapshot recorded on each run.
        val done = listOf(
            snapshotted(1, 10, 30, 30, 100),
            snapshotted(2, 11, 45, 45, 200),
        )
        val records = CycleSummaries.cycleRecords(
            plans = emptyList(), // plan no longer exists
            activePlan = null,
            progress = emptyMap(),
            completed = done,
            profile = Profile(),
        )
        assertEquals(1, records.size)
        val cycle = records.first()
        assertEquals("Full Body Cycle", cycle.cycleName)
        assertEquals("Custom Plan", cycle.planName)
        assertEquals(100, cycle.percent)
        assertEquals(2, cycle.workouts.size)
    }

    /** A completed workout carrying the plan/cycle name snapshot. */
    private fun snapshotted(id: Long, workoutId: Long, reps: Int, planned: Int, startedAt: Long) =
        CompletedWorkoutWithExercises(
            CompletedWorkout(
                id = id, planId = 1, cycleId = 7, workoutId = workoutId, name = "W$workoutId",
                startedAt = startedAt, durationMs = 0, avgPercent = 0,
                planName = "Custom Plan", cycleName = "Full Body Cycle",
            ),
            listOf(
                CompletedExercise(
                    id = id, completedWorkoutId = id, exerciseRef = "squat",
                    reps = reps, goodReps = reps, targetReps = planned, targetSets = 1,
                    durationMs = 0, position = 0,
                ),
            ),
        )

    @Test
    fun noCompletedCyclesIsNull() {
        val home = CycleSummaries.compute(emptyList(), null, emptyMap(), emptyList(), Profile())
        assertNull(home.lastCycle)
        assertNull(home.currentCycle)
        assertEquals(false, home.hasActivePlan)
    }
}
