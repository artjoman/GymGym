package com.gymgym.app.counter

import com.gymgym.app.pose.PoseSnapshot

interface RepCounter {
    val exerciseName: String

    /**
     * Feed the latest pose + timestamp. Returns the completed rep's [RepQuality]
     * exactly when a rep finishes, or null otherwise.
     */
    fun process(pose: PoseSnapshot, nowMs: Long): RepQuality?

    fun reset()
}
