package com.gymgym.app.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Runs under Robolectric so the helpers can resolve real (default/English)
 * string resources — the formatting and label logic localizes via Context.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HistoryFormatTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun durationShort() {
        assertEquals("0s", formatDuration(context, 0))
        assertEquals("45s", formatDuration(context, 45_000))
        assertEquals("1m 30s", formatDuration(context, 90_000))
    }

    @Test
    fun durationLongIncludesHours() {
        assertEquals("5s", formatDurationLong(context, 5_000))
        assertEquals("2m 5s", formatDurationLong(context, 125_000))
        assertEquals("1h 1m 1s", formatDurationLong(context, 3_661_000))
    }

    @Test
    fun paceHandlesEmpty() {
        assertEquals("—", formatPace(context, 0, 60_000))
        assertEquals("—", formatPace(context, 10, 0))
        assertTrue(formatPace(context, 20, 60_000).contains("reps/min"))
    }

    @Test
    fun exerciseLabelMapsKnownAndUnknown() {
        assertEquals("Squat", exerciseLabel(context, "SQUAT"))
        assertEquals("Plank", exerciseLabel(context, "PLANK"))
        assertEquals("MYSTERY", exerciseLabel(context, "MYSTERY")) // unknown passes through
    }

    @Test
    fun timedExerciseDetection() {
        assertTrue(isTimedExercise("PLANK"))
        assertTrue(!isTimedExercise("SQUAT"))
        assertTrue(!isTimedExercise("NOPE"))
    }
}
