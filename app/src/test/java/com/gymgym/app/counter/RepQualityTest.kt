package com.gymgym.app.counter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure-function checks for the fault logic, including the wobble (lateral sway) fault. */
class RepQualityTest {

    private fun stats(min: Float = 80f, max: Float = 180f, dur: Long = 1500L, sway: Float = 0f) =
        RepStats(minAngle = min, maxAngle = max, durationMs = dur, lateralRange = sway)

    @Test
    fun cleanClosingRep() {
        val q = closingQuality(stats(), goodDepthMax = 95f, minDurationMs = 800L, wobbleMax = 0.18f)
        assertTrue(q.isGood)
    }

    @Test
    fun shallowFlagged() {
        val q = closingQuality(stats(min = 100f), 95f, 800L, 0.18f)
        assertEquals(setOf(RepFault.SHALLOW), q.faults)
    }

    @Test
    fun tooFastFlagged() {
        val q = closingQuality(stats(dur = 400L), 95f, 800L, 0.18f)
        assertEquals(setOf(RepFault.TOO_FAST), q.faults)
    }

    @Test
    fun wobblyFlagged() {
        val q = closingQuality(stats(sway = 0.25f), 95f, 800L, 0.18f)
        assertEquals(setOf(RepFault.WOBBLY), q.faults)
    }

    @Test
    fun multipleFaults() {
        val q = closingQuality(stats(min = 110f, dur = 300L, sway = 0.30f), 95f, 800L, 0.18f)
        assertEquals(setOf(RepFault.SHALLOW, RepFault.TOO_FAST, RepFault.WOBBLY), q.faults)
    }

    @Test
    fun pressLockoutAndWobble() {
        assertTrue(extendingQuality(stats(max = 170f), 168f, 800L, 0.15f).isGood)
        assertEquals(
            setOf(RepFault.SHALLOW),
            extendingQuality(stats(max = 166f), 168f, 800L, 0.15f).faults,
        )
        assertEquals(
            setOf(RepFault.WOBBLY),
            extendingQuality(stats(max = 170f, sway = 0.20f), 168f, 800L, 0.15f).faults,
        )
    }
}
