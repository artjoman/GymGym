package com.gymgym.app.counter

import com.gymgym.app.pose.Landmark
import com.gymgym.app.pose.PoseSnapshot
import com.gymgym.app.pose.angleBetween

/** Counts squats from the hip-knee-ankle angle: standing tall (~180) down to squat depth (<100). */
class SquatCounter(private val tuning: FormTuning = FormTuning()) : RepCounter {
    override val exerciseName = "Squat"

    private val stateMachine = RepStateMachine(downEnterThreshold = 100f, upEnterThreshold = 160f)

    override fun process(pose: PoseSnapshot, nowMs: Long): RepQuality? {
        val angle = legAngle(pose, Landmark.LEFT_HIP, Landmark.LEFT_KNEE, Landmark.LEFT_ANKLE)
            ?: legAngle(pose, Landmark.RIGHT_HIP, Landmark.RIGHT_KNEE, Landmark.RIGHT_ANKLE)
            ?: return null
        val stats = stateMachine.process(angle, nowMs, normalizedLateral(pose)) ?: return null
        // A counted rep dips below 100°; a *good* squat reaches ~parallel (≤90°).
        return closingQuality(
            stats,
            GOOD_DEPTH_MAX + tuning.depthTolerance,
            MIN_DURATION_MS,
            WOBBLE_MAX * tuning.wobbleScale,
        )
    }

    override fun reset() = stateMachine.reset()

    private fun legAngle(pose: PoseSnapshot, hip: Landmark, knee: Landmark, ankle: Landmark): Float? {
        val h = pose[hip] ?: return null
        val k = pose[knee] ?: return null
        val a = pose[ankle] ?: return null
        return angleBetween(h, k, a)
    }

    private companion object {
        const val GOOD_DEPTH_MAX = 95f
        const val MIN_DURATION_MS = 800L
        const val WOBBLE_MAX = 0.18f // side view: forward/back hip drift
    }
}
