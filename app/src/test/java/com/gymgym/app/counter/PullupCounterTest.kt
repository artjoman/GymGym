package com.gymgym.app.counter

import com.gymgym.app.pose.Landmark
import com.gymgym.app.pose.Point2D
import com.gymgym.app.pose.PoseSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pull-up counting hinges on the chin-over-bar gate: elbow bend alone can't tell
 * a real pull-up from an arm curl, so a bend only "counts" when the nose is at
 * or above wrist (bar) height.
 *
 * Arm is vertical (shoulder below, wrist overhead at the bar); the elbow is
 * pushed sideways by [elbowDx] to bend it (0 = straight ≈ 180°, 130 ≈ 75°).
 */
class PullupCounterTest {

    private fun pose(elbowDx: Float, noseY: Float) = PoseSnapshot(
        mapOf(
            Landmark.LEFT_SHOULDER to Point2D(100f, 400f),
            Landmark.LEFT_ELBOW to Point2D(100f + elbowDx, 300f),
            Landmark.LEFT_WRIST to Point2D(100f, 200f), // the bar, y = 200
            Landmark.NOSE to Point2D(100f, noseY),
        ),
        imageWidth = 1000,
        imageHeight = 1000,
    )

    private val hang get() = pose(elbowDx = 0f, noseY = 250f)      // arms extended
    private fun pull(noseY: Float) = pose(elbowDx = 130f, noseY = noseY) // deep bend

    private fun countReps(frames: List<PoseSnapshot>): Int {
        val counter = PullupCounter()
        var reps = 0
        var t = 0L
        for (f in frames) {
            if (counter.process(f, t) != null) reps++
            t += 120
        }
        return reps
    }

    @Test
    fun countsPullWhenChinClearsBar() {
        // nose (190) above the bar (200) during the pull -> the bend counts.
        val frames = listOf(hang) + List(5) { pull(noseY = 190f) } + List(2) { hang }
        assertEquals(1, countReps(frames))
    }

    @Test
    fun rejectsBendWhenChinBelowBar() {
        // Same deep elbow bend, but nose (230) stays below the bar -> gated out.
        val frames = listOf(hang) + List(5) { pull(noseY = 230f) } + List(2) { hang }
        assertEquals(0, countReps(frames))
    }
}
