package com.gymgym.app.ui

import com.gymgym.app.data.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Test

class FiltersTest {

    private fun session(type: String, reps: Int, good: Int, startedAt: Long) =
        WorkoutSession(exerciseType = type, repCount = reps, goodReps = good, startedAt = startedAt, durationMs = 30_000)

    @Test
    fun aggregateSumsRepsAndGoodReps() {
        val stats = aggregateStats(
            listOf(
                session("SQUAT", 10, 6, 1_000),
                session("SQUAT", 8, 8, 2_000),
                session("PUSHUP", 12, 9, 3_000),
            ),
        )
        val squat = stats.first { it.exerciseType == "SQUAT" }
        assertEquals(2, squat.sessionCount)
        assertEquals(18, squat.totalReps)
        assertEquals(14, squat.totalGoodReps)
        assertEquals(10, squat.bestReps)
        assertEquals(9.0, squat.avgReps, 0.001)
        assertEquals(2, stats.size) // one row per exercise type
    }

    @Test
    fun filterByExerciseType() {
        val all = listOf(
            session("SQUAT", 10, 10, 1_000),
            session("PUSHUP", 10, 10, 2_000),
        )
        val squats = all.applyFilter(WorkoutFilter(exercise = Exercise.SQUAT))
        assertEquals(1, squats.size)
        assertEquals("SQUAT", squats.single().exerciseType)
    }

    @Test
    fun filterByDateRangeDropsOldSessions() {
        val now = System.currentTimeMillis()
        val all = listOf(
            session("SQUAT", 10, 10, now - 2 * 3_600_000),      // 2h ago
            session("SQUAT", 10, 10, now - 40L * 24 * 3_600_000), // 40 days ago
        )
        assertEquals(2, all.applyFilter(WorkoutFilter(range = DateRange.ALL)).size)
        assertEquals(1, all.applyFilter(WorkoutFilter(range = DateRange.DAY)).size)
        assertEquals(1, all.applyFilter(WorkoutFilter(range = DateRange.MONTH)).size)
    }
}
