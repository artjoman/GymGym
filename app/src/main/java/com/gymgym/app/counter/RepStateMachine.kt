package com.gymgym.app.counter

/**
 * Generic UP/DOWN rep counter driven by a single angle signal.
 *
 * Uses separate enter-thresholds per phase (hysteresis) so noise near the
 * midpoint doesn't cause double counts, and requires a minimum number of
 * consecutive frames in DOWN before a rep can complete, to reject partial dips.
 *
 * [downEnterThreshold] must be < [upEnterThreshold]: the angle closes (e.g. a
 * bent knee/elbow) to enter DOWN, then opens back up to complete the rep.
 */
class RepStateMachine(
    private val downEnterThreshold: Float,
    private val upEnterThreshold: Float,
    private val minFramesInDown: Int = 3,
) {
    private enum class Phase { UNKNOWN, UP, DOWN }

    private var phase = Phase.UNKNOWN
    private var framesInDown = 0

    /** Feed the latest angle reading. Returns true exactly when a rep completes. */
    fun process(angle: Float): Boolean {
        return when (phase) {
            Phase.UNKNOWN, Phase.UP -> {
                if (angle <= downEnterThreshold) {
                    framesInDown++
                    if (framesInDown >= minFramesInDown) {
                        phase = Phase.DOWN
                    }
                } else {
                    framesInDown = 0
                }
                false
            }
            Phase.DOWN -> {
                if (angle >= upEnterThreshold) {
                    phase = Phase.UP
                    framesInDown = 0
                    true
                } else {
                    false
                }
            }
        }
    }

    fun reset() {
        phase = Phase.UNKNOWN
        framesInDown = 0
    }
}
