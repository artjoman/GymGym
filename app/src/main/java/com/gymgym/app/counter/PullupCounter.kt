package com.gymgym.app.counter

import com.gymgym.app.pose.Landmark
import com.gymgym.app.pose.PoseSnapshot
import com.gymgym.app.pose.angleBetween

/**
 * Counts pullups from the shoulder-elbow-wrist angle: dead hang (~180) up to a deep pull (<90).
 *
 * Elbow bend alone can't distinguish a real pull-up from an arm curl performed in front of the
 * body, so the contracted phase additionally requires the chin (nose) to have risen above wrist
 * height (smaller y = higher in the image) - a cheap proxy for "chin over the bar". When that
 * gate isn't met, the angle is clamped to a fully-extended reading so the rep can't start.
 */
class PullupCounter(private val tuning: FormTuning = FormTuning()) : RepCounter {
    override val exerciseName = "Pullup"

    private val stateMachine = RepStateMachine(downEnterThreshold = 90f, upEnterThreshold = 160f)

    override fun process(pose: PoseSnapshot, nowMs: Long): RepQuality? {
        val angle = armAngle(pose, Landmark.LEFT_SHOULDER, Landmark.LEFT_ELBOW, Landmark.LEFT_WRIST)
            ?: armAngle(pose, Landmark.RIGHT_SHOULDER, Landmark.RIGHT_ELBOW, Landmark.RIGHT_WRIST)
            ?: return null

        val nose = pose[Landmark.NOSE]
        val wrist = pose[Landmark.LEFT_WRIST] ?: pose[Landmark.RIGHT_WRIST]
        val chinOverBar = nose != null && wrist != null && nose.y <= wrist.y
        val gatedAngle = if (chinOverBar) angle else FULLY_EXTENDED_ANGLE

        val stats = stateMachine.process(gatedAngle, nowMs, normalizedLateral(pose)) ?: return null
        // A good pull-up drives the elbow well past the count threshold (chin high).
        return closingQuality(
            stats,
            GOOD_DEPTH_MAX + tuning.depthTolerance,
            MIN_DURATION_MS,
            WOBBLE_MAX * tuning.wobbleScale,
        )
    }

    override fun reset() = stateMachine.reset()

    private fun armAngle(pose: PoseSnapshot, shoulder: Landmark, elbow: Landmark, wrist: Landmark): Float? {
        val s = pose[shoulder] ?: return null
        val e = pose[elbow] ?: return null
        val w = pose[wrist] ?: return null
        return angleBetween(s, e, w)
    }

    private companion object {
        const val FULLY_EXTENDED_ANGLE = 180f
        const val GOOD_DEPTH_MAX = 75f   // counted at ≤90°; a strong pull reaches ≤75°
        const val MIN_DURATION_MS = 900L
        const val WOBBLE_MAX = 0.13f     // front view: swinging / kipping shows as sway
    }
}
