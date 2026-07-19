package com.gymgym.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gymgym.app.R
import com.gymgym.app.data.BodyMeasurement
import com.gymgym.app.data.BodyMetric
import com.gymgym.app.data.WorkoutSession

/** One point on a trend chart: an x label (date), the plotted value, and its display text. */
data class TrendPoint(val dateLabel: String, val value: Float, val display: String)

/** Scrollable Stats content (no title/back) — embedded in [StatisticsScreen]. */
@Composable
fun StatsContent(
    sessions: List<WorkoutSession>,
    bodyMeasurements: List<BodyMeasurement>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var filter by remember { mutableStateOf(WorkoutFilter()) }
    val filtered = sessions.applyFilter(filter)
    val stats = aggregateStats(filtered)
    val lineColor = MaterialTheme.colorScheme.primary

    val cutoff = filter.range.cutoff()
    val bodyByMetric: Map<BodyMetric, List<BodyMeasurement>> = BodyMetric.entries.associateWith { m ->
        bodyMeasurements.filter { it.type == m.name && it.loggedAt >= cutoff }.sortedBy { it.loggedAt }
    }.filterValues { it.isNotEmpty() }

    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FilterBar(filter = filter, onFilter = { filter = it })

        if (stats.isEmpty() && bodyByMetric.isEmpty()) {
            Text(stringResource(R.string.stats_empty), modifier = Modifier.padding(vertical = 24.dp))
        }

        for (stat in stats) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                exerciseLabel(context, stat.exerciseType),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (!isTimedExercise(stat.exerciseType) && stat.totalReps > 0) {
                                Text(
                                    stringResource(R.string.stats_good_form, stat.totalGoodReps * 100 / stat.totalReps),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Text(
                            stringResource(R.string.stats_last, formatDate(stat.lastPerformedAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    val timed = isTimedExercise(stat.exerciseType)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatCell(stringResource(R.string.stats_sessions), stat.sessionCount.toString(), Modifier.weight(1f))
                        if (timed) {
                            StatCell(stringResource(R.string.stats_best_hold), formatDurationLong(context, stat.bestReps * 1_000L), Modifier.weight(1f))
                            StatCell(stringResource(R.string.stats_avg_hold), formatDurationLong(context, (stat.avgReps * 1_000).toLong()), Modifier.weight(1f))
                        } else {
                            StatCell(stringResource(R.string.stats_total_reps), stat.totalReps.toString(), Modifier.weight(1f))
                            StatCell(stringResource(R.string.stats_best_set), stat.bestReps.toString(), Modifier.weight(1f))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (timed) {
                            StatCell(stringResource(R.string.stats_total_time), formatDurationLong(context, stat.totalDurationMs), Modifier.weight(1f), big = false)
                            StatCell(stringResource(R.string.stats_cell_last), formatDate(stat.lastPerformedAt), Modifier.weight(1f), big = false)
                        } else {
                            StatCell(stringResource(R.string.stats_avg_reps), String.format("%.1f", stat.avgReps), Modifier.weight(1f), big = false)
                            StatCell(stringResource(R.string.stats_total_time), formatDurationLong(context, stat.totalDurationMs), Modifier.weight(1f), big = false)
                        }
                        StatCell(stringResource(R.string.stats_since), formatDate(stat.firstPerformedAt), Modifier.weight(1f), big = false)
                    }
                }
            }
        }

        if (stats.isNotEmpty()) {
            val reps = filtered.take(15).reversed()
            if (reps.size >= 2) {
                TrendChart(
                    title = stringResource(R.string.stats_reps_trend),
                    points = reps.map { TrendPoint(formatDate(it.startedAt), it.repCount.toFloat(), it.repCount.toString()) },
                    yMin = 0f,
                    yMax = reps.maxOf { it.repCount }.coerceAtLeast(1).toFloat(),
                    formatY = { it.toInt().toString() },
                    lineColor = lineColor,
                )
            }
            val form = filtered.filter { !isTimedExercise(it.exerciseType) && it.repCount > 0 }.take(15).reversed()
            if (form.size >= 2) {
                TrendChart(
                    title = stringResource(R.string.stats_form_trend),
                    points = form.map {
                        val pct = it.goodReps * 100 / it.repCount
                        TrendPoint(formatDate(it.startedAt), pct.toFloat(), "$pct%")
                    },
                    yMin = 0f,
                    yMax = 100f,
                    formatY = { "${it.toInt()}%" },
                    lineColor = lineColor,
                )
            }
        }

        if (bodyByMetric.isNotEmpty()) {
            Text(
                stringResource(R.string.stats_body_trends),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            for ((metric, series) in bodyByMetric) {
                val latest = series.last()
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(metric.labelRes()),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text("${formatMeasure(latest.value)} ${latest.unit}", style = MaterialTheme.typography.titleMedium)
                        }
                        if (series.size >= 2) {
                            val min = series.minOf { it.value }.toFloat()
                            val max = series.maxOf { it.value }.toFloat()
                            TrendChart(
                                title = null,
                                points = series.map {
                                    TrendPoint(formatDate(it.loggedAt), it.value.toFloat(), "${formatMeasure(it.value)} ${it.unit}")
                                },
                                yMin = min,
                                yMax = if (max > min) max else min + 1f,
                                formatY = { formatMeasure(it.toDouble()) },
                                lineColor = lineColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Line chart with min/max value labels on the Y axis and first/last date on the X
 * axis. Tap to open a table of the raw values.
 */
@Composable
fun TrendChart(
    title: String?,
    points: List<TrendPoint>,
    yMin: Float,
    yMax: Float,
    formatY: (Float) -> String,
    lineColor: Color,
) {
    var showTable by remember { mutableStateOf(false) }
    val span = (yMax - yMin).takeIf { it > 0f } ?: 1f

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (title != null) Text(title, style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth().clickable { showTable = true }) {
            // Y axis labels (max on top, min on bottom).
            Column(
                modifier = Modifier.height(84.dp).width(44.dp).padding(end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                Text(formatY(yMax), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatY(yMin), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Canvas(modifier = Modifier.weight(1f).height(84.dp).padding(vertical = 4.dp)) {
                val stepX = if (points.size > 1) size.width / (points.size - 1) else 0f
                val path = Path()
                points.forEachIndexed { i, p ->
                    val norm = ((p.value - yMin) / span).coerceIn(0f, 1f)
                    val x = stepX * i
                    val y = size.height * (1f - norm)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f))
                points.forEachIndexed { i, p ->
                    val norm = ((p.value - yMin) / span).coerceIn(0f, 1f)
                    drawCircle(lineColor, radius = 6f, center = Offset(stepX * i, size.height * (1f - norm)))
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 44.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(points.first().dateLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(points.last().dateLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (showTable) {
        AlertDialog(
            onDismissRequest = { showTable = false },
            confirmButton = {
                TextButton(onClick = { showTable = false }) { Text(stringResource(R.string.action_done)) }
            },
            title = { Text(title ?: stringResource(R.string.stats_data_title)) },
            text = {
                Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    // Newest first.
                    for (p in points.reversed()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(p.dateLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(p.display, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
        )
    }
}

private fun formatMeasure(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else "%.1f".format(v)

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier, big: Boolean = true) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = if (big) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            maxLines = 1,
        )
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
