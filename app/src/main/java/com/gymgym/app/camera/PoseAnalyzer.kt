package com.gymgym.app.camera

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import com.gymgym.app.pose.Landmark
import com.gymgym.app.pose.PoseSnapshot
import com.gymgym.app.pose.Point2D

/**
 * Wraps the ML Kit streaming pose detector as a CameraX [ImageAnalysis.Analyzer].
 * Only ever processes one frame at a time; ML Kit's async detector drops the
 * ImageProxy itself once analysis completes (success or failure).
 */
class PoseAnalyzer(
    private val onPose: (PoseSnapshot) -> Unit,
) : ImageAnalysis.Analyzer {

    private val detector = PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build(),
    )

    private val landmarkMap = mapOf(
        PoseLandmark.NOSE to Landmark.NOSE,
        PoseLandmark.LEFT_SHOULDER to Landmark.LEFT_SHOULDER,
        PoseLandmark.RIGHT_SHOULDER to Landmark.RIGHT_SHOULDER,
        PoseLandmark.LEFT_ELBOW to Landmark.LEFT_ELBOW,
        PoseLandmark.RIGHT_ELBOW to Landmark.RIGHT_ELBOW,
        PoseLandmark.LEFT_WRIST to Landmark.LEFT_WRIST,
        PoseLandmark.RIGHT_WRIST to Landmark.RIGHT_WRIST,
        PoseLandmark.LEFT_HIP to Landmark.LEFT_HIP,
        PoseLandmark.RIGHT_HIP to Landmark.RIGHT_HIP,
        PoseLandmark.LEFT_KNEE to Landmark.LEFT_KNEE,
        PoseLandmark.RIGHT_KNEE to Landmark.RIGHT_KNEE,
        PoseLandmark.LEFT_ANKLE to Landmark.LEFT_ANKLE,
        PoseLandmark.RIGHT_ANKLE to Landmark.RIGHT_ANKLE,
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val rotation = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)
        val (width, height) = if (rotation == 90 || rotation == 270) {
            mediaImage.height to mediaImage.width
        } else {
            mediaImage.width to mediaImage.height
        }
        detector.process(inputImage)
            .addOnSuccessListener { pose -> onPose(pose.toSnapshot(width, height)) }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun Pose.toSnapshot(imageWidth: Int, imageHeight: Int): PoseSnapshot {
        val points = mutableMapOf<Landmark, Point2D>()
        val scores = mutableMapOf<Landmark, Float>()
        val marginX = imageWidth * OUT_OF_FRAME_MARGIN
        val marginY = imageHeight * OUT_OF_FRAME_MARGIN
        for ((mlkitLandmark, ourLandmark) in landmarkMap) {
            val lm = getPoseLandmark(mlkitLandmark) ?: continue
            if (lm.inFrameLikelihood < MIN_LIKELIHOOD) continue
            val p = lm.position
            // ML Kit emits all 33 landmarks on every frame, *extrapolating* the ones
            // it cannot actually see — including coordinates well outside the image.
            // Those arrive looking like real joints and poison any angle computed
            // from them, which is exactly why a partly-out-of-frame body counts so
            // badly. A joint outside the frame is not a measurement; drop it.
            if (p.x < -marginX || p.y < -marginY ||
                p.x > imageWidth + marginX || p.y > imageHeight + marginY
            ) {
                continue
            }
            points[ourLandmark] = Point2D(p.x, p.y)
            scores[ourLandmark] = lm.inFrameLikelihood
        }
        return PoseSnapshot(points, imageWidth, imageHeight, scores)
    }

    fun close() = detector.close()

    private companion object {
        // Deliberately left at 0.5: raising it makes landmarks drop out more often,
        // and a missing hip silently degrades the counters to a worse cue. The
        // bounds check below is the targeted fix for bad framing.
        const val MIN_LIKELIHOOD = 0.5f

        /** Slack around the image rect, so a joint pressed against the edge survives. */
        const val OUT_OF_FRAME_MARGIN = 0.02f
    }
}
