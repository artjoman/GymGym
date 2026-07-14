package com.gymgym.app.pose

import kotlin.math.hypot

private val CORE_LANDMARKS = listOf(
    Landmark.LEFT_SHOULDER,
    Landmark.RIGHT_SHOULDER,
    Landmark.LEFT_HIP,
    Landmark.RIGHT_HIP,
)

private const val MIN_LANDMARKS = 8

/** Torso (mid-shoulder to mid-hip) must span at least this fraction of the image height. */
private const val MIN_TORSO_FRACTION = 0.1f

/**
 * Guards against ML Kit's phantom poses: the detector always tries to fit a
 * skeleton and will happily hallucinate one onto furniture or wall clutter.
 * Those fits tend to be sparse, missing the torso, or tiny — so require the
 * full torso plus most of the body at a believable size before treating the
 * pose as a real person.
 */
fun PoseSnapshot.isPlausiblePerson(): Boolean {
    if (imageHeight == 0) return false
    if (landmarks.size < MIN_LANDMARKS) return false
    val corners = CORE_LANDMARKS.map { this[it] ?: return false }
    val (leftShoulder, rightShoulder, leftHip, rightHip) = corners
    val torsoLength = hypot(
        (leftShoulder.x + rightShoulder.x) / 2 - (leftHip.x + rightHip.x) / 2,
        (leftShoulder.y + rightShoulder.y) / 2 - (leftHip.y + rightHip.y) / 2,
    )
    return torsoLength >= imageHeight * MIN_TORSO_FRACTION
}
