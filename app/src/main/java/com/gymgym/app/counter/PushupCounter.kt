package com.gymgym.app.counter

import com.gymgym.app.pose.Landmark
import com.gymgym.app.pose.Point2D
import com.gymgym.app.pose.PoseSnapshot
import com.gymgym.app.pose.angleBetween
import kotlin.math.hypot

/**
 * Counts pushups from a fused "arm flexion" signal (180 = arms extended / top,
 * ~80 = deep bottom) driven by two complementary cues:
 *
 *  1. **Elbow angle**, averaged across both arms when visible. More robust than a
 *     single arm, which drops out whenever one wrist self-occludes.
 *  2. **Torso drop** — how far the shoulders have descended from their extended
 *     ("top") position, normalized by torso length. From a floor-level side view
 *     the elbow foreshortens and rarely reads a true sub-90° bend, so a genuine
 *     deep rep can look shallow to the elbow cue alone. The torso cue catches it,
 *     and also carries the count on its own when both wrists are occluded.
 *
 * The two are fused with `min` (either cue reaching "deep" makes the rep deep),
 * smoothed to kill per-frame jitter, then fed to the shared [RepStateMachine]
 * whose hysteresis + minimum-frames-in-down guard against double counts.
 */
class PushupCounter(private val tuning: FormTuning = FormTuning()) : RepCounter {
    override val exerciseName = "Pushup"

    private val stateMachine = RepStateMachine(downEnterThreshold = 105f, upEnterThreshold = 150f)

    private var smoothedFlexion = Float.NaN
    // Shoulder Y at the extended/top position, in image pixels (y grows downward,
    // so the top of a pushup is the *smallest* y). Anchors the torso-drop cue.
    private var topShoulderY = Float.NaN

    override fun process(pose: PoseSnapshot, nowMs: Long): RepQuality? {
        val elbow = averageElbowAngle(pose)
        val torso = torsoFlexion(pose)

        val fused = when {
            elbow != null && torso != null -> minOf(elbow, torso)
            elbow != null -> elbow
            torso != null -> torso
            else -> return null // neither arms nor torso readable this frame
        }

        smoothedFlexion = if (smoothedFlexion.isNaN()) fused else SMOOTHING * fused + (1 - SMOOTHING) * smoothedFlexion
        val stats = stateMachine.process(smoothedFlexion, nowMs, normalizedLateral(pose)) ?: return null
        // A good push-up bends the (fused) flexion clearly past the count threshold.
        return closingQuality(
            stats,
            GOOD_DEPTH_MAX + tuning.depthTolerance,
            MIN_DURATION_MS,
            WOBBLE_MAX * tuning.wobbleScale,
        )
    }

    override fun reset() {
        stateMachine.reset()
        smoothedFlexion = Float.NaN
        topShoulderY = Float.NaN
    }

    /** Mean of the two shoulder-elbow-wrist angles, or the single visible one. */
    private fun averageElbowAngle(pose: PoseSnapshot): Float? {
        val left = armAngle(pose, Landmark.LEFT_SHOULDER, Landmark.LEFT_ELBOW, Landmark.LEFT_WRIST)
        val right = armAngle(pose, Landmark.RIGHT_SHOULDER, Landmark.RIGHT_ELBOW, Landmark.RIGHT_WRIST)
        return when {
            left != null && right != null -> (left + right) / 2f
            else -> left ?: right
        }
    }

    private fun armAngle(pose: PoseSnapshot, shoulder: Landmark, elbow: Landmark, wrist: Landmark): Float? {
        val s = pose[shoulder] ?: return null
        val e = pose[elbow] ?: return null
        val w = pose[wrist] ?: return null
        return angleBetween(s, e, w)
    }

    /**
     * Elbow-angle-equivalent value derived from how far the shoulders have dropped
     * below their extended position. Returns null until a torso baseline exists.
     */
    private fun torsoFlexion(pose: PoseSnapshot): Float? {
        val ls = pose[Landmark.LEFT_SHOULDER]
        val rs = pose[Landmark.RIGHT_SHOULDER]
        val lh = pose[Landmark.LEFT_HIP]
        val rh = pose[Landmark.RIGHT_HIP]
        if (ls == null || rs == null || lh == null || rh == null) return null

        val shoulder = mid(ls, rs)
        val hip = mid(lh, rh)
        val torsoLen = hypot(shoulder.x - hip.x, shoulder.y - hip.y)
        if (torsoLen <= 1f) return null

        if (topShoulderY.isNaN()) topShoulderY = shoulder.y
        updateTop(shoulder.y)

        val drop = (shoulder.y - topShoulderY).coerceAtLeast(0f)
        val fraction = (drop / (FULL_DROP_FRACTION * torsoLen)).coerceIn(0f, 1f)
        return 180f - fraction * 100f // fraction 0 -> 180 (extended), 1 -> 80 (deep)
    }

    /**
     * Keep the "top" anchored to extended positions: snap up instantly whenever the
     * shoulders rise higher, and ease down slowly only while extended so the
     * baseline follows a drifting camera without chasing the descent of a rep.
     */
    private fun updateTop(shoulderY: Float) {
        if (shoulderY < topShoulderY) {
            topShoulderY = shoulderY
        } else if (!smoothedFlexion.isNaN() && smoothedFlexion >= TOP_ANCHOR_FLEXION) {
            topShoulderY += (shoulderY - topShoulderY) * TOP_DRIFT
        }
    }

    private fun mid(a: Point2D, b: Point2D) = Point2D((a.x + b.x) / 2f, (a.y + b.y) / 2f)

    private companion object {
        const val SMOOTHING = 0.5f            // EMA weight on the newest reading
        const val FULL_DROP_FRACTION = 0.5f   // shoulder drop (× torso length) counted as full depth
        const val TOP_ANCHOR_FLEXION = 150f   // only re-anchor the top when arms are ~extended
        const val TOP_DRIFT = 0.05f           // slow follow of the top baseline
        const val GOOD_DEPTH_MAX = 95f        // fused flexion must reach ≤ this for good depth
        const val MIN_DURATION_MS = 800L      // faster than this reads as a bounced rep
        const val WOBBLE_MAX = 0.20f          // side view: hips sag/pike less horizontally
    }
}
