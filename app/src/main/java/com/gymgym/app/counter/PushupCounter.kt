package com.gymgym.app.counter

import com.gymgym.app.pose.Landmark
import com.gymgym.app.pose.PoseSnapshot
import com.gymgym.app.pose.angleBetween

/** Counts pushups from the shoulder-elbow-wrist angle: arms extended (~180) down to a deep bend (<90). */
class PushupCounter : RepCounter {
    override val exerciseName = "Pushup"

    private val stateMachine = RepStateMachine(downEnterThreshold = 90f, upEnterThreshold = 160f)

    override fun process(pose: PoseSnapshot): Boolean {
        val angle = armAngle(pose, Landmark.LEFT_SHOULDER, Landmark.LEFT_ELBOW, Landmark.LEFT_WRIST)
            ?: armAngle(pose, Landmark.RIGHT_SHOULDER, Landmark.RIGHT_ELBOW, Landmark.RIGHT_WRIST)
            ?: return false
        return stateMachine.process(angle)
    }

    override fun reset() = stateMachine.reset()

    private fun armAngle(pose: PoseSnapshot, shoulder: Landmark, elbow: Landmark, wrist: Landmark): Float? {
        val s = pose[shoulder] ?: return null
        val e = pose[elbow] ?: return null
        val w = pose[wrist] ?: return null
        return angleBetween(s, e, w)
    }
}
