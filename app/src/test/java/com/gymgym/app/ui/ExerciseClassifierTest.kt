package com.gymgym.app.ui

import com.gymgym.app.pose.Landmark
import com.gymgym.app.pose.Point2D
import com.gymgym.app.pose.PoseSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExerciseClassifierTest {

    private fun pose(landmarks: Map<Landmark, Point2D>) =
        PoseSnapshot(landmarks, imageWidth = 1000, imageHeight = 1000)

    // Horizontal torso (shoulders far from hips in x, level in y) reads as a push-up.
    private fun pushupPose() = pose(
        mapOf(
            Landmark.LEFT_SHOULDER to Point2D(100f, 500f),
            Landmark.RIGHT_SHOULDER to Point2D(100f, 500f),
            Landmark.LEFT_HIP to Point2D(400f, 500f),
            Landmark.RIGHT_HIP to Point2D(400f, 500f),
        ),
    )

    // Vertical torso + a deeply bent knee (~100°) reads as a squat.
    private fun squatPose() = pose(
        mapOf(
            Landmark.LEFT_SHOULDER to Point2D(200f, 100f),
            Landmark.RIGHT_SHOULDER to Point2D(200f, 100f),
            Landmark.LEFT_HIP to Point2D(200f, 400f),
            Landmark.RIGHT_HIP to Point2D(200f, 400f),
            Landmark.LEFT_KNEE to Point2D(200f, 600f),
            Landmark.LEFT_ANKLE to Point2D(397f, 635f),
        ),
    )

    @Test
    fun detectsPushupAfterEnoughFrames() {
        val c = ExerciseClassifier()
        repeat(30) { c.observe(pushupPose()) }
        assertEquals(Exercise.PUSHUP, c.guess())
    }

    @Test
    fun detectsSquat() {
        val c = ExerciseClassifier()
        repeat(30) { c.observe(squatPose()) }
        assertEquals(Exercise.SQUAT, c.guess())
    }

    @Test
    fun returnsNullBeforeEnoughEvidence() {
        val c = ExerciseClassifier()
        repeat(10) { c.observe(squatPose()) }
        assertNull(c.guess())
    }

    @Test
    fun resetClearsEvidence() {
        val c = ExerciseClassifier()
        repeat(30) { c.observe(pushupPose()) }
        c.reset()
        assertNull(c.guess())
    }
}
