package com.gymgym.app.counter

import com.gymgym.app.pose.Landmark
import com.gymgym.app.pose.Point2D
import com.gymgym.app.pose.PoseSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression cover for the "counting dies when the person doesn't fit in frame"
 * reports. A partly-out-of-frame body makes ML Kit extrapolate the joints it
 * cannot see, and those guesses used to reach the torso baseline and wedge the
 * counter permanently. These drive that exact sequence.
 */
class PushupFramingTest {

    /** Same synthetic side-view pushup as PushupCounterTest, with a shoulder offset. */
    private fun frame(t: Float, shoulderShift: Float = 0f): PoseSnapshot {
        val shoulderY = 300f + t * 150f + shoulderShift
        val d = 8.7f + t * 110.3f
        return PoseSnapshot(
            mapOf(
                Landmark.LEFT_SHOULDER to Point2D(100f, shoulderY),
                Landmark.RIGHT_SHOULDER to Point2D(100f, shoulderY),
                Landmark.LEFT_HIP to Point2D(400f, 300f),
                Landmark.RIGHT_HIP to Point2D(400f, 300f),
                Landmark.LEFT_ELBOW to Point2D(100f - d, shoulderY + 100f),
                Landmark.LEFT_WRIST to Point2D(100f, shoulderY + 200f),
            ),
            imageWidth = 1080,
            imageHeight = 1920,
        )
    }

    /** Monotonic across calls — the baseline window is time-based, so it must not rewind. */
    private var now = 0L

    private fun run(counter: PushupCounter, frames: List<PoseSnapshot>): Int {
        var reps = 0
        for (f in frames) {
            if (counter.process(f, now) != null) reps++
            now += 60L
        }
        return reps
    }

    private fun glitch(counter: PushupCounter) {
        counter.process(frame(0f, shoulderShift = -400f), now)
        now += 60L
    }

    private fun cycles(n: Int, shift: Float = 0f): List<PoseSnapshot> =
        (0 until n).flatMap { List(6) { frame(1f, shift) } + List(6) { frame(0f, shift) } }

    /**
     * ONE bad frame — a shoulder misplaced far up the image, exactly what an
     * out-of-frame joint extrapolates to — must not cost more than that frame.
     * This used to wedge the counter for the rest of the set: the baseline
     * re-anchored to the outlier, pinning the torso cue at maximum depth, and the
     * correction was gated behind a flexion reading the pinned cue prevented.
     */
    @Test
    fun singleHighOutlierFrameDoesNotKillCounting() {
        val counter = PushupCounter()
        val before = run(counter, listOf(frame(0f), frame(0f)) + cycles(2))
        assertEquals("counter works before the glitch", 2, before)

        // A shoulder reading 400px (>1 torso) too high in a single frame.
        glitch(counter)

        assertEquals("all subsequent reps still count", 10, run(counter, cycles(10)))
    }

    /** The outlier must not shift the baseline at all, not merely be survivable. */
    @Test
    fun outlierDoesNotShiftTheBaseline() {
        val clean = PushupCounter()
        val glitched = PushupCounter()
        val warmup = listOf(frame(0f), frame(0f)) + cycles(1)
        run(clean, warmup)
        run(glitched, warmup)
        glitch(glitched)
        assertEquals(
            "a glitched frame yields the same count as a clean run",
            run(clean, cycles(6)),
            run(glitched, cycles(6)),
        )
    }

    /**
     * Recovery must not depend on an explicit reset — in the field nothing calls it
     * while the person stays plausibly in view, so a stuck counter stayed stuck for
     * the whole set.
     */
    @Test
    fun recoversWithoutAnExplicitReset() {
        val counter = PushupCounter()
        run(counter, listOf(frame(0f), frame(0f)) + cycles(1))
        glitch(counter)
        assertTrue("counting resumes on its own", run(counter, cycles(5)) > 0)
    }

    /**
     * With hips cropped out, torsoFlexion() returns null and the counter silently
     * falls back to the elbow cue alone — the very cue its own docs call unreliable
     * from a floor-level side view. Nothing tells the user the measurement degraded.
     */
    @Test
    fun croppedHipsSilentlyDropToElbowOnlyCue() {
        val counter = PushupCounter()
        val noHips = (0 until 4).flatMap { rep ->
            List(6) { frame(1f) } + List(6) { frame(0f) }
        }.map { snap ->
            PoseSnapshot(
                snap.landmarks.filterKeys { it != Landmark.LEFT_HIP && it != Landmark.RIGHT_HIP },
                snap.imageWidth,
                snap.imageHeight,
            )
        }
        // It still counts here only because the synthetic elbow bend is ideal.
        // On a real floor-level camera the elbow foreshortens and this is what fails.
        assertEquals(4, run(counter, noHips))
    }
}
