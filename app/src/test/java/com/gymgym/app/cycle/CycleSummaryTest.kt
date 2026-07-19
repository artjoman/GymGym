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
        val summary = CycleSummaries.allCycles(listOf(plan), done, Profile()).first()

        assertEquals("Full Body Cycle", summary.cycleName)
        assertEquals("Custom Plan", summary.planName)
        // Total completed 73 / total planned 99 = 73.7% → 74%.
        assertEquals(74, summary.percent)

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
    fun noCompletedCyclesIsNull() {
        val home = CycleSummaries.compute(emptyList(), null, emptyMap(), emptyList(), Profile())
        assertNull(home.lastCycle)
        assertNull(home.currentCycle)
        assertEquals(false, home.hasActivePlan)
    }
}
