package com.gymgym.app.counter

/**
 * Generic UP/DOWN rep counter driven by a single angle signal.
 *
 * Uses separate enter-thresholds per phase (hysteresis) so noise near the
 * midpoint doesn't cause double counts, and requires a minimum number of
 * consecutive frames in DOWN before a rep can complete, to reject partial dips.
 *
 * [downEnterThreshold] must be < [upEnterThreshold]: the angle closes (e.g. a
 * bent knee/elbow) to enter DOWN, then opens back up to complete the rep. On
 * completion it returns [RepStats] describing the rep's depth and tempo so the
 * caller can judge form.
 */
class RepStateMachine(
    private val downEnterThreshold: Float,
    private val upEnterThreshold: Float,
    private val minFramesInDown: Int = 3,
) {
    private enum class Phase { UNKNOWN, UP, DOWN }

    private var phase = Phase.UNKNOWN
    private var framesInDown = 0

    // Metrics accumulated across the current rep's down→up window.
    private var inRep = false
    private var minAngle = Float.MAX_VALUE
    private var maxAngle = -Float.MAX_VALUE
    private var repStartMs = 0L
    private var latMin = Float.MAX_VALUE
    private var latMax = -Float.MAX_VALUE
    private var latSeen = false

    /**
     * Feed the latest angle + timestamp, and optionally a normalized lateral
     * position (for sway). Returns [RepStats] exactly when a rep completes.
     */
    fun process(angle: Float, nowMs: Long, lateral: Float? = null): RepStats? {
        return when (phase) {
            Phase.UNKNOWN, Phase.UP -> {
                if (angle <= downEnterThreshold) {
                    if (!inRep) {
                        inRep = true
                        repStartMs = nowMs
                        minAngle = angle
                        maxAngle = angle
                        latMin = Float.MAX_VALUE
                        latMax = -Float.MAX_VALUE
                        latSeen = false
                    }
                    observe(angle, lateral)
                    framesInDown++
                    if (framesInDown >= minFramesInDown) phase = Phase.DOWN
                } else {
                    framesInDown = 0
                    // A dip that never reached DOWN is abandoned; discard its metrics.
                    if (phase != Phase.DOWN) inRep = false
                }
                null
            }
            Phase.DOWN -> {
                observe(angle, lateral)
                if (angle >= upEnterThreshold) {
                    val sway = if (latSeen) latMax - latMin else 0f
                    val stats = RepStats(minAngle, maxAngle, (nowMs - repStartMs).coerceAtLeast(0), sway)
                    phase = Phase.UP
                    framesInDown = 0
                    inRep = false
                    stats
                } else {
                    null
                }
            }
        }
    }

    private fun observe(angle: Float, lateral: Float?) {
        if (angle < minAngle) minAngle = angle
        if (angle > maxAngle) maxAngle = angle
        if (lateral != null) {
            latSeen = true
            if (lateral < latMin) latMin = lateral
            if (lateral > latMax) latMax = lateral
        }
    }

    fun reset() {
        phase = Phase.UNKNOWN
        framesInDown = 0
        inRep = false
    }
}
