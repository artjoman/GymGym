package com.gymgym.app.counter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Direct tests of the hysteresis, min-frames-in-down guard, and captured metrics. */
class RepStateMachineTest {

    private fun sm() = RepStateMachine(downEnterThreshold = 100f, upEnterThreshold = 160f)

    @Test
    fun completesOnDownThenUpAndCapturesMetrics() {
        val m = sm()
        assertNull(m.process(170f, 0))    // up (outside a rep)
        assertNull(m.process(90f, 100))   // down frame 1
        assertNull(m.process(80f, 200))   // down frame 2
        assertNull(m.process(85f, 300))   // down frame 3 -> now DOWN
        val stats = m.process(165f, 400)  // opens back up -> rep completes
        assertNotNull(stats)
        assertEquals(80f, stats!!.minAngle, 0.01f)   // deepest in the window
        assertEquals(165f, stats.maxAngle, 0.01f)    // completion frame is the peak
        assertEquals(300L, stats.durationMs)         // 400 - 100 (first down frame)
    }

    @Test
    fun partialDipDoesNotComplete() {
        val m = sm()
        assertNull(m.process(170f, 0))
        assertNull(m.process(90f, 100))  // only 2 frames in down (< minFramesInDown)
        assertNull(m.process(95f, 200))
        assertNull(m.process(170f, 300)) // back up before reaching DOWN -> no rep
    }

    @Test
    fun tracksLateralSwayRange() {
        val m = sm()
        m.process(170f, 0, 1.0f) // outside rep -> ignored
        m.process(90f, 100, 2.0f)
        m.process(80f, 200, 2.5f)
        m.process(85f, 300, 1.8f)
        val stats = m.process(165f, 400, 2.2f)
        assertEquals(0.7f, stats!!.lateralRange, 0.001f) // 2.5 - 1.8
    }

    @Test
    fun lateralRangeIsZeroWhenNotSampled() {
        val m = sm()
        m.process(170f, 0)
        m.process(90f, 100); m.process(80f, 200); m.process(85f, 300)
        val stats = m.process(165f, 400)
        assertEquals(0f, stats!!.lateralRange, 0.0f)
    }

    @Test
    fun resetClearsInFlightRep() {
        val m = sm()
        m.process(90f, 100); m.process(80f, 200); m.process(85f, 300) // in DOWN
        m.reset()
        // A lone up reading after reset must not complete a phantom rep.
        assertNull(m.process(165f, 400))
    }

    @Test
    fun countsTwoConsecutiveReps() {
        val m = sm()
        var reps = 0
        val cycle = listOf(170f, 90f, 80f, 85f, 165f) // one rep
        var t = 0L
        repeat(2) {
            for (a in cycle) {
                if (m.process(a, t) != null) reps++
                t += 100
            }
        }
        assertEquals(2, reps)
    }
}
