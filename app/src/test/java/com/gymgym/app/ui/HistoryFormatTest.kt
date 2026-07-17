package com.gymgym.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryFormatTest {

    @Test
    fun durationShort() {
        assertEquals("0s", formatDuration(0))
        assertEquals("45s", formatDuration(45_000))
        assertEquals("1m 30s", formatDuration(90_000))
    }

    @Test
    fun durationLongIncludesHours() {
        assertEquals("5s", formatDurationLong(5_000))
        assertEquals("2m 5s", formatDurationLong(125_000))
        assertEquals("1h 1m 1s", formatDurationLong(3_661_000))
    }

    @Test
    fun paceHandlesEmpty() {
        assertEquals("—", formatPace(0, 60_000))
        assertEquals("—", formatPace(10, 0))
        assertTrue(formatPace(20, 60_000).contains("reps/min"))
    }

    @Test
    fun exerciseLabelMapsKnownAndUnknown() {
        assertEquals("Squat", exerciseLabel("SQUAT"))
        assertEquals("Plank", exerciseLabel("PLANK"))
        assertEquals("MYSTERY", exerciseLabel("MYSTERY")) // unknown passes through
    }

    @Test
    fun timedExerciseDetection() {
        assertTrue(isTimedExercise("PLANK"))
        assertTrue(!isTimedExercise("SQUAT"))
        assertTrue(!isTimedExercise("NOPE"))
    }
}
