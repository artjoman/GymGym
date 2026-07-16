package com.gymgym.app.counter

import com.gymgym.app.pose.Landmark
import com.gymgym.app.pose.Point2D
import com.gymgym.app.pose.PoseSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Drives [PushupCounter] with synthetic pushup motion. A "descent" parameter
 * t in [0,1] (0 = top/arms extended, 1 = deep bottom) is turned into a plausible
 * side-view pose: the elbow bends and the shoulders drop as t grows.
 */
class PushupCounterTest {

    // Elbow-x offset that yields ~170° (t=0) up to ~80° (t=1) at the elbow vertex,
    // with shoulder at (100, sY) and wrist straight below it at (100, sY+200).
    private fun frame(t: Float, includeArms: Boolean = true): PoseSnapshot {
        val shoulderY = 300f + t * 150f
        val d = 8.7f + t * 110.3f
        val lm = mutableMapOf<Landmark, Point2D>(
            Landmark.LEFT_SHOULDER to Point2D(100f, shoulderY),
            Landmark.RIGHT_SHOULDER to Point2D(100f, shoulderY),
            Landmark.LEFT_HIP to Point2D(400f, 300f),
            Landmark.RIGHT_HIP to Point2D(400f, 300f),
        )
        if (includeArms) {
            lm[Landmark.LEFT_ELBOW] = Point2D(100f - d, shoulderY + 100f)
            lm[Landmark.LEFT_WRIST] = Point2D(100f, shoulderY + 200f)
        }
        return PoseSnapshot(lm, imageWidth = 1080, imageHeight = 1920)
    }

    private fun countReps(counter: PushupCounter, ts: List<Float>, includeArms: Boolean = true): Int {
        var reps = 0
        for (t in ts) if (counter.process(frame(t, includeArms))) reps++
        return reps
    }

    /** One full descend/ascend cycle, sampled densely enough for the hysteresis. */
    private fun reps(n: Int, prime: List<Float> = listOf(0f, 0f)): List<Float> =
        prime + (0 until n).flatMap { List(6) { 1f } + List(6) { 0f } }

    @Test
    fun countsCleanPushups() {
        assertEquals(5, countReps(PushupCounter(), reps(5)))
    }

    @Test
    fun countsFromTorsoDropWhenWristsOccluded() {
        // No elbow/wrist landmarks -> the torso-drop fallback must carry the count.
        assertEquals(4, countReps(PushupCounter(), reps(4), includeArms = false))
    }

    @Test
    fun ignoresShallowBobbing() {
        // Never descends past a shallow dip -> no rep should complete.
        val bob = List(40) { if (it % 2 == 0) 0f else 0.4f }
        assertEquals(0, countReps(PushupCounter(), bob))
    }

    @Test
    fun resetClearsCount() {
        val counter = PushupCounter()
        assertEquals(3, countReps(counter, reps(3)))
        counter.reset()
        assertEquals(2, countReps(counter, reps(2)))
    }
}
