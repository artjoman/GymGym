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

    /**
     * Recent (timestamp, shoulder-Y) samples, newest last. The torso baseline is
     * the highest shoulder position (smallest y) still inside [BASELINE_WINDOW_MS].
     *
     * This replaces an all-time anchor that could only ratchet upward. That anchor
     * deadlocked the counter: one extrapolated frame from a partly-out-of-frame
     * body would re-anchor it far too high, pinning the torso cue at maximum depth
     * forever — and its correction was gated behind "flexion looks extended", which
     * the pinned cue guaranteed would never happen. A bounded window plus a
     * percentile (see [baselineTop]) means no single sample can define the
     * baseline, and anything that slips through ages out. See PushupFramingTest.
     */
    private val recentShoulders = ArrayDeque<Pair<Long, Float>>()

    override fun process(pose: PoseSnapshot, nowMs: Long): RepQuality? {
        val elbow = averageElbowAngle(pose)
        val torso = torsoFlexion(pose, nowMs)

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
        recentShoulders.clear()
    }

    /**
     * The best available elbow angle. With both arms visible, prefer the one the
     * detector is actually confident about rather than averaging: from a side view
     * the far arm is occluded and ML Kit infers it, so averaging drags a good
     * reading toward a guessed one. Falls back to the mean when there is nothing
     * to choose between them (synthetic poses carry no scores).
     */
    private fun averageElbowAngle(pose: PoseSnapshot): Float? {
        val left = armAngle(pose, Landmark.LEFT_SHOULDER, Landmark.LEFT_ELBOW, Landmark.LEFT_WRIST)
        val right = armAngle(pose, Landmark.RIGHT_SHOULDER, Landmark.RIGHT_ELBOW, Landmark.RIGHT_WRIST)
        if (left == null || right == null) return left ?: right

        val leftScore = pose.confidence(Landmark.LEFT_SHOULDER, Landmark.LEFT_ELBOW, Landmark.LEFT_WRIST)
        val rightScore = pose.confidence(Landmark.RIGHT_SHOULDER, Landmark.RIGHT_ELBOW, Landmark.RIGHT_WRIST)
        if (leftScore == null || rightScore == null) return (left + right) / 2f
        return when {
            leftScore - rightScore > ARM_SCORE_MARGIN -> left
            rightScore - leftScore > ARM_SCORE_MARGIN -> right
            else -> (left + right) / 2f
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
    private fun torsoFlexion(pose: PoseSnapshot, nowMs: Long): Float? {
        val ls = pose[Landmark.LEFT_SHOULDER]
        val rs = pose[Landmark.RIGHT_SHOULDER]
        val lh = pose[Landmark.LEFT_HIP]
        val rh = pose[Landmark.RIGHT_HIP]
        if (ls == null || rs == null || lh == null || rh == null) return null

        val shoulder = mid(ls, rs)
        val hip = mid(lh, rh)
        val torsoLen = hypot(shoulder.x - hip.x, shoulder.y - hip.y)
        if (torsoLen <= 1f) return null

        recentShoulders.addLast(nowMs to shoulder.y)
        while (recentShoulders.size > 1 && nowMs - recentShoulders.first().first > BASELINE_WINDOW_MS) {
            recentShoulders.removeFirst()
        }

        val drop = (shoulder.y - baselineTop()).coerceAtLeast(0f)
        val fraction = (drop / (FULL_DROP_FRACTION * torsoLen)).coerceIn(0f, 1f)
        return 180f - fraction * 100f // fraction 0 -> 180 (extended), 1 -> 80 (deep)
    }

    /**
     * The "top" of recent motion: a low percentile of the windowed shoulder heights
     * rather than the outright minimum.
     *
     * Using the minimum let a single extrapolated frame define the baseline. A
     * percentile cannot be moved by one bad sample among many, so the cue degrades
     * gracefully instead of jamming — and unlike a velocity filter it needs no
     * guess about how fast a real body may move between two frames.
     */
    private fun baselineTop(): Float {
        val sorted = recentShoulders.map { it.second }.sorted()
        return sorted[((sorted.size - 1) * BASELINE_PERCENTILE).toInt()]
    }

    private fun mid(a: Point2D, b: Point2D) = Point2D((a.x + b.x) / 2f, (a.y + b.y) / 2f)

    private companion object {
        const val SMOOTHING = 0.5f            // EMA weight on the newest reading
        const val FULL_DROP_FRACTION = 0.5f   // shoulder drop (× torso length) counted as full depth
        const val BASELINE_WINDOW_MS = 8_000L // how long a "top" sample anchors the torso cue
        const val BASELINE_PERCENTILE = 0.1f  // low percentile of the window = the "top"
        const val ARM_SCORE_MARGIN = 0.15f    // confidence gap needed to trust one arm over both
        const val GOOD_DEPTH_MAX = 95f        // fused flexion must reach ≤ this for good depth
        const val MIN_DURATION_MS = 800L      // faster than this reads as a bounced rep
        const val WOBBLE_MAX = 0.20f          // side view: hips sag/pike less horizontally
    }
}
