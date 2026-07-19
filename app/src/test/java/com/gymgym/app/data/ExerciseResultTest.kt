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

    // --- Workout-level completion: total completed reps ÷ total planned reps ---

    @Test
    fun workoutPercentRoundsToNearest() {
        // Squat 27/30, Push-Up 30/30, Pull-Up 18/20 → 75/80 = 93.75% → 94%.
        assertEquals(94, CompletedWorkoutRepository.workoutPercent(totalCompleted = 75, totalPlanned = 80))
    }

    @Test
    fun workoutPercentAllComplete() {
        assertEquals(100, CompletedWorkoutRepository.workoutPercent(totalCompleted = 99, totalPlanned = 99))
    }

    @Test
    fun workoutPercentSkippedExerciseCountsPlanned() {
        // Squat 30/30, Push-Up skipped 0/45, Pull-Up 24/24 → 54/99 = 54.5% → 55%.
        assertEquals(55, CompletedWorkoutRepository.workoutPercent(totalCompleted = 54, totalPlanned = 99))
    }

    @Test
    fun workoutPercentNoPlanIsZero() {
        assertEquals(0, CompletedWorkoutRepository.workoutPercent(totalCompleted = 10, totalPlanned = 0))
    }
}
