package com.gymgym.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gymgym.app.R
import com.gymgym.app.data.BodyMeasurement
import com.gymgym.app.data.BodyMetric
import com.gymgym.app.data.WorkoutSession

@Composable
fun StatsScreen(
    sessions: List<WorkoutSession>,
    bodyMeasurements: List<BodyMeasurement>,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var filter by remember { mutableStateOf(WorkoutFilter()) }
    val filtered = sessions.applyFilter(filter)
    val stats = aggregateStats(filtered)
    val lineColor = MaterialTheme.colorScheme.primary

    // Body measurements grouped by metric, oldest -> newest, within the date range.
    val cutoff = filter.range.cutoff()
    val bodyByMetric: Map<BodyMetric, List<BodyMeasurement>> = BodyMetric.entries.associateWith { m ->
        bodyMeasurements.filter { it.type == m.name && it.loggedAt >= cutoff }.sortedBy { it.loggedAt }
    }.filterValues { it.isNotEmpty() }
    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineSmall)

        FilterBar(filter = filter, onFilter = { filter = it })

        if (stats.isEmpty() && bodyByMetric.isEmpty()) {
            Text(
                stringResource(R.string.stats_empty),
                modifier = Modifier.padding(vertical = 24.dp),
            )
        }
        if (stats.isNotEmpty()) {
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
                                        stringResource(
                                            R.string.stats_good_form,
                                            stat.totalGoodReps * 100 / stat.totalReps,
                                        ),
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

            // Reps across recent (filtered) sessions, oldest -> newest.
            val trend = filtered.take(15).map { it.repCount }.reversed()
            if (trend.size >= 2) {
                Text(stringResource(R.string.stats_reps_trend), style = MaterialTheme.typography.titleMedium)
                Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                    val maxVal = (trend.max()).coerceAtLeast(1)
                    val stepX = if (trend.size > 1) size.width / (trend.size - 1) else 0f
                    val path = Path()
                    trend.forEachIndexed { i, reps ->
                        val x = stepX * i
                        val y = size.height * (1f - reps.toFloat() / maxVal)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f),
                    )
                    trend.forEachIndexed { i, reps ->
                        val x = stepX * i
                        val y = size.height * (1f - reps.toFloat() / maxVal)
                        drawCircle(color = lineColor, radius = 6f, center = Offset(x, y))
                    }
                }
            }

            // Form quality (good-rep %) over recent rep sessions, on a fixed 0–100 scale.
            val formTrend = filtered
                .filter { !isTimedExercise(it.exerciseType) && it.repCount > 0 }
                .take(15)
                .map { it.goodReps * 100 / it.repCount }
                .reversed()
            if (formTrend.size >= 2) {
                Text(stringResource(R.string.stats_form_trend), style = MaterialTheme.typography.titleMedium)
                Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                    val stepX = size.width / (formTrend.size - 1)
                    val path = Path()
                    formTrend.forEachIndexed { i, pct ->
                        val x = stepX * i
                        val y = size.height * (1f - pct / 100f)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f),
                    )
                    formTrend.forEachIndexed { i, pct ->
                        val x = stepX * i
                        val y = size.height * (1f - pct / 100f)
                        drawCircle(color = lineColor, radius = 6f, center = Offset(x, y))
                    }
                }
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
                            Text(
                                "${formatMeasure(latest.value)} ${latest.unit}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        if (series.size >= 2) {
                            BodyTrendChart(series.map { it.value.toFloat() }, lineColor)
                        }
                    }
                }
            }
        }

        GymButton(stringResource(R.string.action_back), onBack, Modifier.padding(top = 8.dp), GymButtonStyle.Secondary)
    }
}

@Composable
private fun BodyTrendChart(values: List<Float>, lineColor: androidx.compose.ui.graphics.Color) {
    val min = values.min()
    val max = values.max()
    val span = (max - min).takeIf { it > 0f } ?: 1f
    Canvas(modifier = Modifier.fillMaxWidth().height(80.dp).padding(top = 12.dp)) {
        val stepX = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = stepX * i
            // Leave 10% headroom top/bottom so the line isn't clipped to the edges.
            val norm = (v - min) / span
            val y = size.height * (0.9f - norm * 0.8f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f),
        )
        values.forEachIndexed { i, v ->
            val x = stepX * i
            val norm = (v - min) / span
            val y = size.height * (0.9f - norm * 0.8f)
            drawCircle(color = lineColor, radius = 6f, center = Offset(x, y))
        }
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
