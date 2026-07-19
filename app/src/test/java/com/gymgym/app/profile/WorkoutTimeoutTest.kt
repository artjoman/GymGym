package com.gymgym.app.profile

import org.junit.Assert.assertEquals
import org.junit.Test

/** Between-workouts recovery: stored in seconds, chosen in 8-hour steps [8h, 168h]. */
class WorkoutTimeoutTest {

    @Test
    fun straySecondsNormalizeToNearestEightHourStep() {
        // The reported 172690 s (~47.97 h) should read as a clean 48 h.
        assertEquals(48, ProfileRepository.workoutTimeoutHours(172_690))
        assertEquals(48 * 3600, ProfileRepository.normalizeWorkoutTimeoutSeconds(172_690))
    }

    @Test
    fun exactPresetsAreUnchanged() {
        assertEquals(72, ProfileRepository.workoutTimeoutHours(72 * 3600))
        assertEquals(48, ProfileRepository.workoutTimeoutHours(48 * 3600))
        assertEquals(24, ProfileRepository.workoutTimeoutHours(24 * 3600))
    }

    @Test
    fun clampsToRange() {
        assertEquals(8, ProfileRepository.workoutTimeoutHours(0))          // below min
        assertEquals(8, ProfileRepository.workoutTimeoutHours(3600))       // 1h → min 8h
        assertEquals(168, ProfileRepository.workoutTimeoutHours(999 * 3600)) // above max
    }

    @Test
    fun roundsToEightHourStep() {
        assertEquals(56, ProfileRepository.workoutTimeoutHours(53 * 3600)) // 53h → 56h
        assertEquals(48, ProfileRepository.workoutTimeoutHours(51 * 3600)) // 51h → 48h
    }
}
