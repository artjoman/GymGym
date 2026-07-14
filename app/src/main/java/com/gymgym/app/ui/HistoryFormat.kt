package com.gymgym.app.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

/** Turns a stored [Exercise] enum name back into its display label, tolerating unknowns. */
fun exerciseLabel(exerciseType: String): String =
    Exercise.entries.find { it.name == exerciseType }?.displayName ?: exerciseType

fun formatDate(epochMillis: Long): String = dateFormat.format(Date(epochMillis))

fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}
