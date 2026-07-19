package com.gymgym.app.cycle

import com.gymgym.app.cycle.CycleEngine.ProgressEntry
import com.gymgym.app.data.CompletedWorkout
import com.gymgym.app.data.CycleEntity
import com.gymgym.app.data.CycleWithWorkouts
import com.gymgym.app.data.PlanEntity
import com.gymgym.app.data.PlanWithCycles
import com.gymgym.app.data.WorkoutEntity
import com.gymgym.app.data.WorkoutWithExercises
import com.gymgym.app.profile.Profile
import com.gymgym.app.profile.TrainingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CycleEngineTest {

    // Plan: cycle1(w1,w2), cycle2(w3,w4). Workout ids 1..4.
    private fun plan(): PlanWithCycles {
        fun w(id: Long, cycleId: Long, pos: Int) =
            WorkoutWithExercises(WorkoutEntity(id, cycleId, "W$id", pos, null), emptyList())
        return PlanWithCycles(
            plan = PlanEntity(id = 1, name = "P", endDate = null, isActive = true, createdAt = 0),
            cycles = listOf(
                CycleWithWorkouts(CycleEntity(1, 1, "C1", 0), listOf(w(1, 1, 0), w(2, 1, 1))),
                CycleWithWorkouts(CycleEntity(2, 1, "C2", 0), listOf(w(3, 2, 0), w(4, 2, 1))),
            ),
        )
    }

    private val profile = Profile()

    @Test
    fun firstMissionIsFirstWorkout() {
        val s = CycleEngine.compute(plan(), emptyMap(), null, profile, now = 0)
        assertTrue(s.hasActivePlan)
        assertEquals(1L, s.nextMission?.workout?.id)
        assertEquals(BlockStatus.NEXT, s.blocks.first().status)
        assertEquals(BlockStatus.PENDING, s.blocks[1].status)
        assertEquals(0, s.cyclePercent)
    }

    @Test
    fun doneAdvancesToNext() {
        val progress = mapOf(1L to ProgressEntry("DONE", 90))
        val s = CycleEngine.compute(plan(), progress, null, profile, now = 0)
        assertEquals(2L, s.nextMission?.workout?.id)
        assertEquals(BlockStatus.DONE, s.blocks[0].status)
        assertEquals(90, s.blocks[0].percent)
        assertEquals(BlockStatus.NEXT, s.blocks[1].status)
        assertEquals(50, s.cyclePercent)
    }

    @Test
    fun skippedCountsAsProcessed() {
        val progress = mapOf(1L to ProgressEntry("SKIPPED", 0), 2L to ProgressEntry("DONE", 80))
        val s = CycleEngine.compute(plan(), progress, null, profile, now = 0)
        // Cycle 1 fully processed → next mission is cycle 2's first workout.
        assertEquals(3L, s.nextMission?.workout?.id)
        assertEquals(2L, s.currentCycle?.id)
    }

    @Test
    fun allProcessedResetsToNewPass() {
        val progress = (1L..4L).associateWith { ProgressEntry("DONE", 100) }
        val s = CycleEngine.compute(plan(), progress, null, profile, now = 0)
        // Pass complete → back to the very first workout with a clean cycle.
        assertEquals(1L, s.nextMission?.workout?.id)
        assertEquals(BlockStatus.NEXT, s.blocks.first().status)
        assertEquals(0, s.cyclePercent)
    }

    @Test
    fun smartDateIsLastEndPlusRecovery() {
        val last = CompletedWorkout(1, 1, 1, 1, "W1", startedAt = 1_000, durationMs = 2_000, avgPercent = 90)
        val p = profile.copy(trainingMode = TrainingMode.SMART_CYCLE, workoutTimeoutSeconds = 172_800)
        val s = CycleEngine.compute(plan(), mapOf(1L to ProgressEntry("DONE", 90)), last, p, now = 5_000)
        assertEquals(1_000 + 2_000 + 172_800L * 1_000, s.nextMission?.plannedDate)
    }

    @Test
    fun noActivePlanIsEmpty() {
        val s = CycleEngine.compute(null, emptyMap(), null, profile, now = 0)
        assertFalse(s.hasActivePlan)
        assertNull(s.nextMission)
    }

    @Test
    fun weeklyScheduleOrdersByWeekday() {
        // Cycle with w10 (Wed=3, pos 0) and w11 (Mon=1, pos 1).
        fun w(id: Long, weekday: Int?, pos: Int) =
            WorkoutWithExercises(WorkoutEntity(id, 5, "W$id", pos, weekday), emptyList())
        val plan = PlanWithCycles(
            plan = PlanEntity(id = 2, name = "P", endDate = null, isActive = true, createdAt = 0),
            cycles = listOf(CycleWithWorkouts(CycleEntity(5, 2, "C", 0), listOf(w(10, 3, 0), w(11, 1, 1)))),
        )
        val weekly = Profile(trainingMode = TrainingMode.WEEKLY_SCHEDULE)
        val s = CycleEngine.compute(plan, emptyMap(), null, weekly, now = 0)
        // Monday (w11) comes before Wednesday (w10) despite its later position.
        assertEquals(11L, s.nextMission?.workout?.id)
        assertEquals(1, s.nextMission?.weekday)
        assertEquals(11L, s.blocks.first().workout.id)
    }
}
