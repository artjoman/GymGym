package com.gymgym.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.gymgym.app.pose.Landmark
import com.gymgym.app.pose.PoseSnapshot

private val SKELETON_EDGES = listOf(
    Landmark.LEFT_SHOULDER to Landmark.RIGHT_SHOULDER,
    Landmark.LEFT_SHOULDER to Landmark.LEFT_ELBOW,
    Landmark.LEFT_ELBOW to Landmark.LEFT_WRIST,
    Landmark.RIGHT_SHOULDER to Landmark.RIGHT_ELBOW,
    Landmark.RIGHT_ELBOW to Landmark.RIGHT_WRIST,
    Landmark.LEFT_SHOULDER to Landmark.LEFT_HIP,
    Landmark.RIGHT_SHOULDER to Landmark.RIGHT_HIP,
    Landmark.LEFT_HIP to Landmark.RIGHT_HIP,
    Landmark.LEFT_HIP to Landmark.LEFT_KNEE,
    Landmark.LEFT_KNEE to Landmark.LEFT_ANKLE,
    Landmark.RIGHT_HIP to Landmark.RIGHT_KNEE,
    Landmark.RIGHT_KNEE to Landmark.RIGHT_ANKLE,
)

/** Draws a simple skeleton over the camera preview, scaled from image space to the canvas. */
@Composable
fun PoseOverlay(pose: PoseSnapshot?, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (pose == null || pose.imageWidth == 0 || pose.imageHeight == 0) return@Canvas

        val scaleX = size.width / pose.imageWidth
        val scaleY = size.height / pose.imageHeight

        fun offsetOf(landmark: Landmark): Offset? {
            val point = pose[landmark] ?: return null
            return Offset(point.x * scaleX, point.y * scaleY)
        }

        for ((from, to) in SKELETON_EDGES) {
            val start = offsetOf(from) ?: continue
            val end = offsetOf(to) ?: continue
            drawLine(color = Color(0xFF7FE0A0), start = start, end = end, strokeWidth = 6f)
        }
        for (landmark in pose.landmarks.keys) {
            val center = offsetOf(landmark) ?: continue
            drawCircle(color = Color(0xFF1B6B3A), radius = 10f, center = center)
        }
    }
}
