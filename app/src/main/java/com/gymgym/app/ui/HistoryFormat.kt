package com.gymgym.app.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
private val dateTimeFormat = SimpleDateFormat("EEE, MMM d yyyy · HH:mm", Locale.getDefault())
private val clockFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

/** Turns a stored [Exercise] enum name back into its display label, tolerating unknowns. */
fun exerciseLabel(exerciseType: String): String =
    Exercise.entries.find { it.name == exerciseType }?.displayName ?: exerciseType

/** Whether the stored exercise type is a hold-for-time exercise (plank, etc.),
 *  so screens format it as a duration instead of reps. */
fun isTimedExercise(exerciseType: String): Boolean =
    Exercise.entries.find { it.name == exerciseType }?.timed == true

fun formatDate(epochMillis: Long): String = dateFormat.format(Date(epochMillis))

fun formatDateTime(epochMillis: Long): String = dateTimeFormat.format(Date(epochMillis))

fun formatClock(epochMillis: Long): String = clockFormat.format(Date(epochMillis))

fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}

/** Longer duration form for detail/summary rows, includes hours when present. */
fun formatDurationLong(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return when {
        h > 0 -> "${h}h ${m}m ${s}s"
        m > 0 -> "${m}m ${s}s"
        else -> "${s}s"
    }
}

/** Reps per minute, or an em dash when it can't be computed. */
fun formatPace(reps: Int, durationMs: Long): String {
    if (durationMs <= 0 || reps <= 0) return "—"
    val perMin = reps / (durationMs / 60_000.0)
    return String.format(Locale.getDefault(), "%.1f reps/min", perMin)
}
