package com.gymgym.app.counter

import com.gymgym.app.pose.PoseSnapshot

interface RepCounter {
    val exerciseName: String

    /** Feed the latest pose. Returns true exactly when a rep just completed. */
    fun process(pose: PoseSnapshot): Boolean

    fun reset()
}
