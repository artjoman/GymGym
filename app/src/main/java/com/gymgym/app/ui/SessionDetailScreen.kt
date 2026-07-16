package com.gymgym.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gymgym.app.data.WorkoutSession
import com.gymgym.app.ui.theme.BrandGreen

@Composable
fun SessionDetailScreen(session: WorkoutSession?, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Session detail", style = MaterialTheme.typography.headlineSmall)

        if (session == null) {
            Text("This session is no longer available.")
            GymButton("Back", onBack, style = GymButtonStyle.Secondary)
            return@Column
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    exerciseLabel(session.exerciseType).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = BrandGreen,
                )
                val timed = isTimedExercise(session.exerciseType)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        if (timed) formatDurationLong(session.durationMs) else session.repCount.toString(),
                        style = MaterialTheme.typography.displayLarge,
                    )
                    if (!timed) {
                        Text(
                            " reps",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                DetailRow("Started", formatDateTime(session.startedAt))
                DetailRow("Ended", formatDateTime(session.startedAt + session.durationMs))
                DetailRow("Duration", formatDurationLong(session.durationMs))
                if (!timed) DetailRow("Pace", formatPace(session.repCount, session.durationMs))
            }
        }

        GymButton("Back", onBack, style = GymButtonStyle.Secondary)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
        )
    }
}
