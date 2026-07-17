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
import androidx.compose.ui.unit.dp
import com.gymgym.app.data.WorkoutSession

@Composable
fun StatsScreen(
    sessions: List<WorkoutSession>,
    onBack: () -> Unit,
) {
    var filter by remember { mutableStateOf(WorkoutFilter()) }
    val filtered = sessions.applyFilter(filter)
    val stats = aggregateStats(filtered)
    val lineColor = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Stats", style = MaterialTheme.typography.headlineSmall)

        FilterBar(filter = filter, onFilter = { filter = it })

        if (stats.isEmpty()) {
            Text(
                "No stats yet. Finish a set to start tracking.",
                modifier = Modifier.padding(vertical = 24.dp),
            )
        } else {
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
                                    exerciseLabel(stat.exerciseType),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                if (!isTimedExercise(stat.exerciseType) && stat.totalReps > 0) {
                                    Text(
                                        "${stat.totalGoodReps * 100 / stat.totalReps}% good form",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Text(
                                "Last: ${formatDate(stat.lastPerformedAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        val timed = isTimedExercise(stat.exerciseType)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            StatCell("Sessions", stat.sessionCount.toString(), Modifier.weight(1f))
                            if (timed) {
                                StatCell("Best hold", formatDurationLong(stat.bestReps * 1_000L), Modifier.weight(1f))
                                StatCell("Avg hold", formatDurationLong((stat.avgReps * 1_000).toLong()), Modifier.weight(1f))
                            } else {
                                StatCell("Total reps", stat.totalReps.toString(), Modifier.weight(1f))
                                StatCell("Best set", stat.bestReps.toString(), Modifier.weight(1f))
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (timed) {
                                StatCell("Total time", formatDurationLong(stat.totalDurationMs), Modifier.weight(1f), big = false)
                                StatCell("Last", formatDate(stat.lastPerformedAt), Modifier.weight(1f), big = false)
                            } else {
                                StatCell("Avg reps", String.format("%.1f", stat.avgReps), Modifier.weight(1f), big = false)
                                StatCell("Total time", formatDurationLong(stat.totalDurationMs), Modifier.weight(1f), big = false)
                            }
                            StatCell("Since", formatDate(stat.firstPerformedAt), Modifier.weight(1f), big = false)
                        }
                    }
                }
            }

            // Reps across recent (filtered) sessions, oldest -> newest.
            val trend = filtered.take(15).map { it.repCount }.reversed()
            if (trend.size >= 2) {
                Text("Recent reps trend", style = MaterialTheme.typography.titleMedium)
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
                Text("Form quality trend", style = MaterialTheme.typography.titleMedium)
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

        GymButton("Back", onBack, Modifier.padding(top = 8.dp), GymButtonStyle.Secondary)
    }
}

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
