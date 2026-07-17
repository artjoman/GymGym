package com.gymgym.app.counter

import com.gymgym.app.pose.Landmark
import com.gymgym.app.pose.PoseSnapshot
import com.gymgym.app.pose.angleBetween

/**
 * Counts overhead dumbbell presses from the shoulder-elbow-wrist angle: racked
 * at the shoulders (elbow bent, <100) pressed to full extension overhead (>165).
 */
class DumbbellPressCounter(private val tuning: FormTuning = FormTuning()) : RepCounter {
    override val exerciseName = "Dumbbell Press"

    private val stateMachine = RepStateMachine(downEnterThreshold = 100f, upEnterThreshold = 165f)

    override fun process(pose: PoseSnapshot, nowMs: Long): RepQuality? {
        val angle = armAngle(pose, Landmark.LEFT_SHOULDER, Landmark.LEFT_ELBOW, Landmark.LEFT_WRIST)
            ?: armAngle(pose, Landmark.RIGHT_SHOULDER, Landmark.RIGHT_ELBOW, Landmark.RIGHT_WRIST)
            ?: return null
        val stats = stateMachine.process(angle, nowMs, normalizedLateral(pose)) ?: return null
        // A good press locks out fully overhead (counted at ≥165°, good at ≥168°).
        return extendingQuality(
            stats,
            GOOD_EXTEND_MIN - tuning.depthTolerance,
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
        const val GOOD_EXTEND_MIN = 168f
        const val MIN_DURATION_MS = 800L
        const val WOBBLE_MAX = 0.15f // front view: leaning / using the legs
    }
}
