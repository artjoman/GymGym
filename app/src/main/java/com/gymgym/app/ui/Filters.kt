package com.gymgym.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gymgym.app.R
import com.gymgym.app.data.ExerciseStat
import com.gymgym.app.data.WorkoutSession

enum class DateRange(val label: String, val windowMs: Long) {
    ALL("All", Long.MAX_VALUE),
    DAY("24h", 24L * 3_600_000),
    WEEK("7d", 7L * 24 * 3_600_000),
    MONTH("30d", 30L * 24 * 3_600_000);

    /** Earliest timestamp included by this range (0 for ALL). */
    fun cutoff(): Long = if (this == ALL) 0L else System.currentTimeMillis() - windowMs
}

/** null exercise = all types. */
data class WorkoutFilter(
    val exercise: Exercise? = null,
    val range: DateRange = DateRange.ALL,
)

fun List<WorkoutSession>.applyFilter(criteria: WorkoutFilter): List<WorkoutSession> {
    val cutoff = if (criteria.range == DateRange.ALL) {
        Long.MIN_VALUE
    } else {
        System.currentTimeMillis() - criteria.range.windowMs
    }
    return filter { s ->
        (criteria.exercise == null || s.exerciseType == criteria.exercise.name) &&
            s.startedAt >= cutoff
    }
}

/** Per-exercise aggregates computed from a (filtered) session list. */
fun aggregateStats(sessions: List<WorkoutSession>): List<ExerciseStat> =
    sessions.groupBy { it.exerciseType }
        .map { (type, list) ->
            ExerciseStat(
                exerciseType = type,
                sessionCount = list.size,
                totalReps = list.sumOf { it.repCount },
                totalGoodReps = list.sumOf { it.goodReps },
                bestReps = list.maxOf { it.repCount },
                avgReps = list.map { it.repCount }.average(),
                totalDurationMs = list.sumOf { it.durationMs },
                lastPerformedAt = list.maxOf { it.startedAt },
                firstPerformedAt = list.minOf { it.startedAt },
            )
        }
        .sortedByDescending { it.lastPerformedAt }

@Composable
fun FilterBar(filter: WorkoutFilter, onFilter: (WorkoutFilter) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = filter.exercise == null,
                onClick = { onFilter(filter.copy(exercise = null)) },
                label = { Text(stringResource(R.string.range_all)) },
            )
            for (ex in Exercise.entries) {
                FilterChip(
                    selected = filter.exercise == ex,
                    onClick = { onFilter(filter.copy(exercise = ex)) },
                    label = { Text(stringResource(ex.labelRes())) },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (range in DateRange.entries) {
                FilterChip(
                    selected = filter.range == range,
                    onClick = { onFilter(filter.copy(range = range)) },
                    label = { Text(stringResource(range.labelRes())) },
                )
            }
        }
    }
}
