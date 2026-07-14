package com.gymgym.app.ui

import com.gymgym.app.pose.Landmark
import com.gymgym.app.pose.Point2D
import com.gymgym.app.pose.PoseSnapshot
import com.gymgym.app.pose.angleBetween
import kotlin.math.abs

/**
 * Heuristic auto-detector for which exercise is being performed. It accumulates
 * simple pose features over a window of frames and only returns a [guess] once
 * it has seen enough evidence — that window is the "buffer" before the caller
 * locks the exercise in, so a set doesn't flip type mid-rep.
 *
 * Discriminators (image y grows downward):
 *  - Torso horizontal  → Pushup.
 *  - Deep knee bend    → Squat (legs do the work).
 *  - Arms overhead:
 *      both racked (near shoulders) and overhead across the window → Dumbbell Press;
 *      overhead the whole time (never racked)                      → Pullup.
 *
 * These thresholds are first-cut and will need real-world tuning.
 */
class ExerciseClassifier {
    private var valid = 0
    private var pushupFrames = 0
    private var overheadFrames = 0
    private var rackFrames = 0
    private var kneeBentFrames = 0
    private var minKneeAngle = 180f

    fun reset() {
        valid = 0
        pushupFrames = 0
        overheadFrames = 0
        rackFrames = 0
        kneeBentFrames = 0
        minKneeAngle = 180f
    }

    fun observe(pose: PoseSnapshot) {
        val ls = pose[Landmark.LEFT_SHOULDER]
        val rs = pose[Landmark.RIGHT_SHOULDER]
        val lh = pose[Landmark.LEFT_HIP]
        val rh = pose[Landmark.RIGHT_HIP]
        if (ls == null || rs == null || lh == null || rh == null) return
        val h = if (pose.imageHeight > 0) pose.imageHeight.toFloat() else return
        valid++

        val shoulder = mid(ls, rs)
        val hip = mid(lh, rh)
        val dx = abs(shoulder.x - hip.x)
        val dy = abs(shoulder.y - hip.y)

        // Horizontal torso → pushup; skip arm/leg checks.
        if (dx > dy * 1.2f) {
            pushupFrames++
            return
        }

        // Legs: deepest knee bend seen.
        val knee = kneeAngle(pose)
        if (knee != null) {
            if (knee < minKneeAngle) minKneeAngle = knee
            if (knee < 140f) kneeBentFrames++
        }

        // Arms: where are the wrists relative to head / shoulders?
        val wristY = averageY(pose[Landmark.LEFT_WRIST], pose[Landmark.RIGHT_WRIST])
        if (wristY != null) {
            val headY = pose[Landmark.NOSE]?.y ?: (shoulder.y - 0.12f * h)
            when {
                wristY < headY -> overheadFrames++
                wristY < shoulder.y + 0.06f * h -> rackFrames++
            }
        }
    }

    fun guess(): Exercise? {
        if (valid < MIN_FRAMES) return null
        if (pushupFrames > valid * 0.5f) return Exercise.PUSHUP
        if (minKneeAngle < 115f && kneeBentFrames > valid * 0.25f) return Exercise.SQUAT
        if (overheadFrames > valid * 0.15f) {
            return if (rackFrames > valid * 0.12f) Exercise.DUMBBELL_PRESS else Exercise.PULLUP
        }
        if (kneeBentFrames > valid * 0.2f) return Exercise.SQUAT
        return null
    }

    private fun kneeAngle(pose: PoseSnapshot): Float? {
        legAngle(pose, Landmark.LEFT_HIP, Landmark.LEFT_KNEE, Landmark.LEFT_ANKLE)?.let { return it }
        return legAngle(pose, Landmark.RIGHT_HIP, Landmark.RIGHT_KNEE, Landmark.RIGHT_ANKLE)
    }

    private fun legAngle(pose: PoseSnapshot, hip: Landmark, knee: Landmark, ankle: Landmark): Float? {
        val hp = pose[hip] ?: return null
        val kp = pose[knee] ?: return null
        val ap = pose[ankle] ?: return null
        return angleBetween(hp, kp, ap)
    }

    private fun averageY(a: Point2D?, b: Point2D?): Float? = when {
        a != null && b != null -> (a.y + b.y) / 2f
        a != null -> a.y
        b != null -> b.y
        else -> null
    }

    private fun mid(a: Point2D, b: Point2D) = Point2D((a.x + b.x) / 2f, (a.y + b.y) / 2f)

    private companion object {
        const val MIN_FRAMES = 28 // ~2s of tracking before committing
    }
}
