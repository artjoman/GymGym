package com.gymgym.app.counter

import com.gymgym.app.pose.Landmark
import com.gymgym.app.pose.Point2D
import com.gymgym.app.pose.PoseSnapshot
import kotlin.math.hypot

/** A way a rep falls short of good form. */
enum class RepFault { SHALLOW, TOO_FAST, WOBBLY }

/** Verdict on a single completed rep; empty faults means a clean rep. */
data class RepQuality(val faults: Set<RepFault> = emptySet()) {
    val isGood: Boolean get() = faults.isEmpty()
}

/**
 * Per-rep metrics captured over the down→up cycle by [RepStateMachine].
 * [lateralRange] is the side-to-side sway of the hips (normalized by torso length).
 */
data class RepStats(
    val minAngle: Float,
    val maxAngle: Float,
    val durationMs: Long,
    val lateralRange: Float = 0f,
)

/** Per-exercise form-strictness tuning derived from the user's sensitivity setting. */
data class FormTuning(val depthTolerance: Float = 0f, val wobbleScale: Float = 1f)

/**
 * Quality for a "closing" exercise, where a smaller angle means more depth
 * (squat, push-up, pull-up): shallow if it never bent past [goodDepthMax].
 */
fun closingQuality(
    stats: RepStats,
    goodDepthMax: Float,
    minDurationMs: Long,
    wobbleMax: Float,
): RepQuality =
    RepQuality(
        buildSet {
            if (stats.minAngle > goodDepthMax) add(RepFault.SHALLOW)
            if (stats.durationMs in 1 until minDurationMs) add(RepFault.TOO_FAST)
            if (stats.lateralRange > wobbleMax) add(RepFault.WOBBLY)
        },
    )

/**
 * Quality for an "extending" exercise, where a larger angle means fuller
 * lockout (overhead press): shallow if it never reached [goodExtendMin].
 */
fun extendingQuality(
    stats: RepStats,
    goodExtendMin: Float,
    minDurationMs: Long,
    wobbleMax: Float,
): RepQuality =
    RepQuality(
        buildSet {
            if (stats.maxAngle < goodExtendMin) add(RepFault.SHALLOW)
            if (stats.durationMs in 1 until minDurationMs) add(RepFault.TOO_FAST)
            if (stats.lateralRange > wobbleMax) add(RepFault.WOBBLY)
        },
    )

/**
 * Horizontal hip position normalized by torso length; the range of this over a
 * rep measures side-to-side sway (swinging pull-ups, leaning press, wobbly
 * squat). Null when shoulders/hips aren't both visible.
 */
fun normalizedLateral(pose: PoseSnapshot): Float? {
    val ls = pose[Landmark.LEFT_SHOULDER] ?: return null
    val rs = pose[Landmark.RIGHT_SHOULDER] ?: return null
    val lh = pose[Landmark.LEFT_HIP] ?: return null
    val rh = pose[Landmark.RIGHT_HIP] ?: return null
    val shoulder = Point2D((ls.x + rs.x) / 2f, (ls.y + rs.y) / 2f)
    val hip = Point2D((lh.x + rh.x) / 2f, (lh.y + rh.y) / 2f)
    val torso = hypot(shoulder.x - hip.x, shoulder.y - hip.y)
    if (torso <= 1f) return null
    return hip.x / torso
}
