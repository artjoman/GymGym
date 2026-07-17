package com.gymgym.app.counter

import com.gymgym.app.pose.Landmark
import com.gymgym.app.pose.Point2D
import com.gymgym.app.pose.PoseSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Overhead press is an "extending" exercise: racked (elbow bent) → locked out
 * overhead. Quality keys on the lockout — a good rep reaches ≥168°.
 *
 * Vertical arm (shoulder below, wrist overhead); the elbow is pushed sideways by
 * [elbowDx] to bend it (109 ≈ 85° racked, 4 ≈ 175° locked out, 12 ≈ 166° short).
 */
class DumbbellPressCounterTest {

    private fun pose(elbowDx: Float) = PoseSnapshot(
        mapOf(
            Landmark.LEFT_SHOULDER to Point2D(100f, 300f),
            Landmark.LEFT_ELBOW to Point2D(100f + elbowDx, 200f),
            Landmark.LEFT_WRIST to Point2D(100f, 100f),
        ),
        imageWidth = 1000,
        imageHeight = 1000,
    )

    private val racked get() = pose(elbowDx = 109f)

    /** Rack → lock out at [lockoutDx]; returns the completed rep's quality. */
    private fun runRep(lockoutDx: Float): RepQuality? {
        val counter = DumbbellPressCounter()
        val frames = List(6) { racked } + List(2) { pose(elbowDx = lockoutDx) }
        var t = 0L
        var result: RepQuality? = null
        for (f in frames) {
            counter.process(f, t)?.let { result = it }
            t += 150
        }
        return result
    }

    @Test
    fun fullLockoutIsGood() {
        val q = runRep(lockoutDx = 4f) // ~175°
        assertEquals(RepQuality(), q)
        assertTrue(q!!.isGood)
    }

    @Test
    fun shortLockoutFlaggedShallow() {
        val q = runRep(lockoutDx = 12f) // ~166°, counts (≥165) but not full lockout (<168)
        assertEquals(setOf(RepFault.SHALLOW), q!!.faults)
    }
}
