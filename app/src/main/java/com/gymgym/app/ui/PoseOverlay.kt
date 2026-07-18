package com.gymgym.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.gymgym.app.pose.Landmark
import com.gymgym.app.pose.PoseSnapshot
import com.gymgym.app.ui.theme.LocalBrand

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

/**
 * Draws a soft neon skeleton over the camera preview in the app's accent color:
 * a wide translucent glow under a brighter round-capped limb, with joints as a
 * glowing dot with a white core. A rounded red frame shows while tracking is lost.
 */
@Composable
fun PoseOverlay(
    pose: PoseSnapshot?,
    isTracking: Boolean,
    mirror: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val accent = LocalBrand.current.accent
    val limb = lerp(accent, Color.White, 0.12f)   // brightened filament
    val glow = accent.copy(alpha = 0.22f)
    val jointGlow = accent.copy(alpha = 0.30f)
    val jointCore = Color.White.copy(alpha = 0.92f)

    Canvas(modifier = modifier.fillMaxSize()) {
        if (!isTracking) {
            drawRoundRect(
                color = Color(0xFFFF5A5A),
                style = Stroke(width = 10f),
                cornerRadius = CornerRadius(36f, 36f),
            )
        }
        if (pose == null || pose.imageWidth == 0 || pose.imageHeight == 0) return@Canvas

        val scaleX = size.width / pose.imageWidth
        val scaleY = size.height / pose.imageHeight

        // Front camera preview is mirrored, so mirror the overlay to match.
        fun offsetOf(landmark: Landmark): Offset? {
            val point = pose[landmark] ?: return null
            val x = point.x * scaleX
            return Offset(if (mirror) size.width - x else x, point.y * scaleY)
        }

        val edges = SKELETON_EDGES.mapNotNull { (from, to) ->
            val s = offsetOf(from) ?: return@mapNotNull null
            val e = offsetOf(to) ?: return@mapNotNull null
            s to e
        }

        // Glow pass first, so it never paints over an adjacent solid limb.
        for ((s, e) in edges) {
            drawLine(glow, s, e, strokeWidth = 16f, cap = StrokeCap.Round)
        }
        for ((s, e) in edges) {
            drawLine(limb, s, e, strokeWidth = 6f, cap = StrokeCap.Round)
        }

        for (landmark in pose.landmarks.keys) {
            val c = offsetOf(landmark) ?: continue
            drawCircle(jointGlow, radius = 12f, center = c)
            drawCircle(accent, radius = 6f, center = c)
            drawCircle(jointCore, radius = 2.6f, center = c)
        }
    }
}
