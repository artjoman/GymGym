package com.gymgym.app.counter

import com.gymgym.app.pose.Landmark
import com.gymgym.app.pose.PoseSnapshot
import com.gymgym.app.pose.angleBetween

/** Counts squats from the hip-knee-ankle angle: standing tall (~180) down to squat depth (<100). */
class SquatCounter : RepCounter {
    override val exerciseName = "Squat"

    private val stateMachine = RepStateMachine(downEnterThreshold = 100f, upEnterThreshold = 160f)

    override fun process(pose: PoseSnapshot): Boolean {
        val angle = legAngle(pose, Landmark.LEFT_HIP, Landmark.LEFT_KNEE, Landmark.LEFT_ANKLE)
            ?: legAngle(pose, Landmark.RIGHT_HIP, Landmark.RIGHT_KNEE, Landmark.RIGHT_ANKLE)
            ?: return false
        return stateMachine.process(angle)
    }

    override fun reset() = stateMachine.reset()

    private fun legAngle(pose: PoseSnapshot, hip: Landmark, knee: Landmark, ankle: Landmark): Float? {
        val h = pose[hip] ?: return null
        val k = pose[knee] ?: return null
        val a = pose[ankle] ?: return null
        return angleBetween(h, k, a)
    }
}
