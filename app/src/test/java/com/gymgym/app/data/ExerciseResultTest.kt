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

    // --- Hierarchical completion: exercise → workout → cycle ---

    @Test
    fun exercisePercentIsRepsOverPlanned() {
        assertEquals(90, CompletedWorkoutRepository.exercisePercent(completed = 27, planned = 30))
        assertEquals(100, CompletedWorkoutRepository.exercisePercent(completed = 30, planned = 30))
        assertEquals(0, CompletedWorkoutRepository.exercisePercent(completed = 0, planned = 30))
    }

    @Test
    fun exercisePercentNoPlanIsZero() {
        assertEquals(0, CompletedWorkoutRepository.exercisePercent(completed = 10, planned = 0))
    }

    @Test
    fun workoutPercentIsAverageOfExercises() {
        // Five exercises at 100/100/80/100/100 → 96%.
        assertEquals(96, CompletedWorkoutRepository.averagePercent(listOf(100, 100, 80, 100, 100)))
    }

    @Test
    fun cyclePercentIsAverageOfWorkoutsWithSkippedAsZero() {
        // (100 + 0 + 100 + 100 + 100 + 100 + 100) / 7 = 85.7% → 86%.
        assertEquals(86, CompletedWorkoutRepository.averagePercent(listOf(100, 0, 100, 100, 100, 100, 100)))
    }

    @Test
    fun averageOfNothingIsZero() {
        assertEquals(0, CompletedWorkoutRepository.averagePercent(emptyList()))
    }
}
