package com.gymgym.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymgym.app.data.ExerciseStat
import com.gymgym.app.data.WorkoutSession

@Composable
fun StatsScreen(
    stats: List<ExerciseStat>,
    recentSessions: List<WorkoutSession>,
    onBack: () -> Unit,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Stats", style = MaterialTheme.typography.headlineSmall)

        if (stats.isEmpty()) {
            Text(
                "No stats yet. Finish a set to start tracking.",
                modifier = Modifier.padding(vertical = 24.dp),
            )
        } else {
            for (stat in stats) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            exerciseLabel(stat.exerciseType),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            StatCell("Sessions", stat.sessionCount.toString())
                            StatCell("Total reps", stat.totalReps.toString())
                            StatCell("Best set", stat.bestReps.toString())
                        }
                    }
                }
            }

            // Reps across recent sessions, oldest -> newest.
            val trend = recentSessions.take(15).map { it.repCount }.reversed()
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
        }

        GymButton("Back", onBack, Modifier.padding(top = 8.dp), GymButtonStyle.Secondary)
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
