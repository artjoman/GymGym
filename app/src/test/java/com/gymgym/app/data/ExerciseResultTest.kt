package com.gymgym.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseResultTest {

    private fun result(reps: Int, targetReps: Int, targetSets: Int) =
        CompletedWorkoutRepository.ExerciseResult(
            exerciseRef = "x", reps = reps, goodReps = reps,
            targetReps = targetReps, targetSets = targetSets, durationMs = 0,
        )

    @Test
    fun partialCompletion() {
        // 15 of 10×2 = 20 planned → 75%.
        assertEquals(75, result(reps = 15, targetReps = 10, targetSets = 2).completionPercent())
    }

    @Test
    fun overshootCapsAtHundred() {
        assertEquals(100, result(reps = 30, targetReps = 10, targetSets = 2).completionPercent())
    }

    @Test
    fun exactlyComplete() {
        assertEquals(100, result(reps = 20, targetReps = 10, targetSets = 2).completionPercent())
    }

    @Test
    fun zeroTargetIsZero() {
        assertEquals(0, result(reps = 5, targetReps = 0, targetSets = 0).completionPercent())
    }
}
