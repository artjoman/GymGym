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
        for ((mlkitLandmark, ourLandmark) in landmarkMap) {
            val lm = getPoseLandmark(mlkitLandmark) ?: continue
            if (lm.inFrameLikelihood < MIN_LIKELIHOOD) continue
            points[ourLandmark] = Point2D(lm.position.x, lm.position.y)
        }
        return PoseSnapshot(points, imageWidth, imageHeight)
    }

    fun close() = detector.close()

    private companion object {
        const val MIN_LIKELIHOOD = 0.5f
    }
}
