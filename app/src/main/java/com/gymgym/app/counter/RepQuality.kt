package com.gymgym.app.counter

/** A way a rep falls short of good form. */
enum class RepFault { SHALLOW, TOO_FAST }

/** Verdict on a single completed rep; empty faults means a clean rep. */
data class RepQuality(val faults: Set<RepFault> = emptySet()) {
    val isGood: Boolean get() = faults.isEmpty()
}

/** Per-rep metrics captured over the down→up cycle by [RepStateMachine]. */
data class RepStats(val minAngle: Float, val maxAngle: Float, val durationMs: Long)

/**
 * Quality for a "closing" exercise, where a smaller angle means more depth
 * (squat, push-up, pull-up): shallow if it never bent past [goodDepthMax].
 */
fun closingQuality(stats: RepStats, goodDepthMax: Float, minDurationMs: Long): RepQuality =
    RepQuality(
        buildSet {
            if (stats.minAngle > goodDepthMax) add(RepFault.SHALLOW)
            if (stats.durationMs in 1 until minDurationMs) add(RepFault.TOO_FAST)
        },
    )

/**
 * Quality for an "extending" exercise, where a larger angle means fuller
 * lockout (overhead press): shallow if it never reached [goodExtendMin].
 */
fun extendingQuality(stats: RepStats, goodExtendMin: Float, minDurationMs: Long): RepQuality =
    RepQuality(
        buildSet {
            if (stats.maxAngle < goodExtendMin) add(RepFault.SHALLOW)
            if (stats.durationMs in 1 until minDurationMs) add(RepFault.TOO_FAST)
        },
    )
