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
    /**
     * Per-landmark detector confidence, where the source provides it. Lets a
     * counter prefer the limb it can actually see over one ML Kit inferred —
     * in a side view the far arm is occluded and guessed, and averaging it in
     * drags the angle toward nonsense. Empty for synthetic/test poses.
     */
    val scores: Map<Landmark, Float> = emptyMap(),
) {
    operator fun get(landmark: Landmark): Point2D? = landmarks[landmark]

    /** Mean confidence over [marks], or null if any is missing or unscored. */
    fun confidence(vararg marks: Landmark): Float? {
        var sum = 0f
        for (m in marks) sum += scores[m] ?: return null
        return sum / marks.size
    }
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
