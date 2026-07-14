package com.gymgym.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymgym.app.data.WorkoutSession

@Composable
fun HistoryScreen(
    sessions: List<WorkoutSession>,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("History", style = MaterialTheme.typography.headlineSmall)

        if (sessions.isEmpty()) {
            Text(
                "No workouts yet. Finish a set to see it here.",
                modifier = Modifier.padding(vertical = 24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sessions, key = { it.id }) { session -> SessionRow(session) }
            }
        }

        GymButton("Back", onBack, Modifier.padding(top = 16.dp), GymButtonStyle.Secondary)
    }
}

@Composable
private fun SessionRow(session: WorkoutSession) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    exerciseLabel(session.exerciseType),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "${formatDate(session.startedAt)} · ${formatDuration(session.durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text("${session.repCount} reps", style = MaterialTheme.typography.titleMedium)
        }
    }
}
