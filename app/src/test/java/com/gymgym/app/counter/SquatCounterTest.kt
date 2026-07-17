package com.gymgym.app.counter

import com.gymgym.app.pose.Landmark
import com.gymgym.app.pose.Point2D
import com.gymgym.app.pose.PoseSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

/**
 * Drives [SquatCounter] with a synthetic leg whose hip-knee-ankle angle equals a
 * target: hip straight above the knee, ankle placed so the interior knee angle is
 * exactly `theta` (see derivation: angle == theta).
 */
class SquatCounterTest {

    private fun kneePose(theta: Float): PoseSnapshot {
        val kx = 200f
        val ky = 500f
        val l = 200f
        val a = Math.toRadians((180f - theta).toDouble())
        val ankle = Point2D(kx + l * sin(a).toFloat(), ky + l * cos(a).toFloat())
        return PoseSnapshot(
            mapOf(
                Landmark.LEFT_HIP to Point2D(kx, ky - l),
                Landmark.LEFT_KNEE to Point2D(kx, ky),
                Landmark.LEFT_ANKLE to ankle,
            ),
            imageWidth = 1080,
            imageHeight = 1920,
        )
    }

    /** Stand → hold at [bottom]° → stand, at [frameMs] per frame; returns the rep's quality. */
    private fun runRep(counter: SquatCounter, bottom: Float, frameMs: Long): RepQuality? {
        val thetas = listOf(180f) + List(6) { bottom } + List(3) { 180f }
        var now = 0L
        var result: RepQuality? = null
        for (th in thetas) {
            counter.process(kneePose(th), now)?.let { result = it }
            now += frameMs
        }
        return result
    }

    @Test
    fun deepControlledRepIsGood() {
        val q = runRep(SquatCounter(), bottom = 80f, frameMs = 150L)
        assertEquals(RepQuality(), q)
        assertTrue(q!!.isGood)
    }

    @Test
    fun shallowRepFlagged() {
        // 98° counts (≤100) but isn't deep enough (>95) → SHALLOW, still controlled tempo.
        val q = runRep(SquatCounter(), bottom = 98f, frameMs = 150L)
        assertEquals(setOf(RepFault.SHALLOW), q!!.faults)
    }

    @Test
    fun bouncedRepFlaggedTooFast() {
        val q = runRep(SquatCounter(), bottom = 80f, frameMs = 40L)
        assertEquals(setOf(RepFault.TOO_FAST), q!!.faults)
    }

    @Test
    fun partialDipDoesNotCount() {
        // Never bends past the count threshold → no rep completes.
        var now = 0L
        val counter = SquatCounter()
        var result: RepQuality? = null
        for (th in listOf(180f, 130f, 180f, 130f, 180f)) {
            counter.process(kneePose(th), now)?.let { result = it }
            now += 150L
        }
        assertNull(result)
    }
}
