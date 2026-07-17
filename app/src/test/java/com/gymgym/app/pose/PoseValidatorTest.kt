package com.gymgym.app.pose

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards against ML Kit's phantom skeletons being treated as a real person. */
class PoseValidatorTest {

    private val height = 1000

    private fun pose(landmarks: Map<Landmark, Point2D>) =
        PoseSnapshot(landmarks, imageWidth = 1000, imageHeight = height)

    // A full torso plus extra joints, at a believable size.
    private fun realPerson() = pose(
        mapOf(
            Landmark.LEFT_SHOULDER to Point2D(400f, 200f),
            Landmark.RIGHT_SHOULDER to Point2D(600f, 200f),
            Landmark.LEFT_HIP to Point2D(400f, 500f),
            Landmark.RIGHT_HIP to Point2D(600f, 500f),
            Landmark.LEFT_ELBOW to Point2D(380f, 350f),
            Landmark.RIGHT_ELBOW to Point2D(620f, 350f),
            Landmark.LEFT_KNEE to Point2D(400f, 700f),
            Landmark.RIGHT_KNEE to Point2D(600f, 700f),
        ),
    )

    @Test
    fun acceptsRealPerson() {
        assertTrue(realPerson().isPlausiblePerson())
    }

    @Test
    fun rejectsTooFewLandmarks() {
        assertFalse(
            pose(
                mapOf(
                    Landmark.LEFT_SHOULDER to Point2D(400f, 200f),
                    Landmark.RIGHT_SHOULDER to Point2D(600f, 200f),
                    Landmark.LEFT_HIP to Point2D(400f, 500f),
                    Landmark.RIGHT_HIP to Point2D(600f, 500f),
                ),
            ).isPlausiblePerson(),
        )
    }

    @Test
    fun rejectsMissingTorsoLandmark() {
        // Eight landmarks, but a core torso corner (a hip) is absent.
        val lm = realPerson().landmarks.toMutableMap()
        lm.remove(Landmark.RIGHT_HIP)
        lm[Landmark.LEFT_ANKLE] = Point2D(400f, 900f) // keep count at 8
        assertFalse(pose(lm).isPlausiblePerson())
    }

    @Test
    fun rejectsTinyTorso() {
        val lm = realPerson().landmarks.toMutableMap()
        // Collapse the torso to ~10px (< 10% of the 1000px image height).
        lm[Landmark.LEFT_HIP] = Point2D(400f, 210f)
        lm[Landmark.RIGHT_HIP] = Point2D(600f, 210f)
        assertFalse(pose(lm).isPlausiblePerson())
    }

    @Test
    fun rejectsZeroImageHeight() {
        assertFalse(
            PoseSnapshot(realPerson().landmarks, imageWidth = 1000, imageHeight = 0)
                .isPlausiblePerson(),
        )
    }
}
