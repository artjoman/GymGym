package com.gymgym.app.ui

import android.content.Context
import com.gymgym.app.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
private val dateTimeFormat = SimpleDateFormat("EEE, MMM d yyyy · HH:mm", Locale.getDefault())
private val clockFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

/** Turns a stored [Exercise] enum name back into its localized label, tolerating unknowns. */
fun exerciseLabel(context: Context, exerciseType: String): String =
    Exercise.entries.find { it.name == exerciseType }?.let { context.getString(it.labelRes()) }
        ?: exerciseType

/** Whether the stored exercise type is a hold-for-time exercise (plank, etc.),
 *  so screens format it as a duration instead of reps. */
fun isTimedExercise(exerciseType: String): Boolean =
    Exercise.entries.find { it.name == exerciseType }?.timed == true

fun formatDate(epochMillis: Long): String = dateFormat.format(Date(epochMillis))

fun formatDateTime(epochMillis: Long): String = dateTimeFormat.format(Date(epochMillis))

fun formatClock(epochMillis: Long): String = clockFormat.format(Date(epochMillis))

fun formatDuration(context: Context, durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = (totalSeconds / 60).toInt()
    val seconds = (totalSeconds % 60).toInt()
    return if (minutes > 0) {
        context.getString(R.string.duration_ms, minutes, seconds)
    } else {
        context.getString(R.string.duration_s, seconds)
    }
}

/** Longer duration form for detail/summary rows, includes hours when present. */
fun formatDurationLong(context: Context, durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val h = (totalSeconds / 3600).toInt()
    val m = ((totalSeconds % 3600) / 60).toInt()
    val s = (totalSeconds % 60).toInt()
    return when {
        h > 0 -> context.getString(R.string.duration_hms, h, m, s)
        m > 0 -> context.getString(R.string.duration_ms, m, s)
        else -> context.getString(R.string.duration_s, s)
    }
}

/** Reps per minute, or an em dash when it can't be computed. */
fun formatPace(context: Context, reps: Int, durationMs: Long): String {
    if (durationMs <= 0 || reps <= 0) return context.getString(R.string.pace_none)
    val perMin = reps / (durationMs / 60_000.0)
    return context.getString(R.string.pace, String.format(Locale.getDefault(), "%.1f", perMin))
}
