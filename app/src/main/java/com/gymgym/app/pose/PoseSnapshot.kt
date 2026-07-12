package com.gymgym.app.pose

data class Point2D(val x: Float, val y: Float)

/**
 * Simplified view of an ML Kit Pose: just the landmarks the counters need, by name,
 * plus the (rotation-adjusted) source image dimensions so an overlay can scale points.
 */
data class PoseSnapshot(
    val landmarks: Map<Landmark, Point2D>,
    val imageWidth: Int,
    val imageHeight: Int,
) {
    operator fun get(landmark: Landmark): Point2D? = landmarks[landmark]
}

enum class Landmark {
    NOSE,
    LEFT_SHOULDER, RIGHT_SHOULDER,
    LEFT_ELBOW, RIGHT_ELBOW,
    LEFT_WRIST, RIGHT_WRIST,
    LEFT_HIP, RIGHT_HIP,
    LEFT_KNEE, RIGHT_KNEE,
    LEFT_ANKLE, RIGHT_ANKLE,
}
