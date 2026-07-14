package com.gymgym.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymgym.app.data.WorkoutSession

@Composable
fun HistoryScreen(
    sessions: List<WorkoutSession>,
    onOpenSession: (Long) -> Unit,
    onBack: () -> Unit,
) {
    var filter by remember { mutableStateOf(WorkoutFilter()) }
    val filtered = sessions.applyFilter(filter)

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("History", style = MaterialTheme.typography.headlineSmall)

        Column(modifier = Modifier.padding(top = 12.dp)) {
            FilterBar(filter = filter, onFilter = { filter = it })
        }

        if (filtered.isEmpty()) {
            Text(
                if (sessions.isEmpty()) "No workouts yet. Finish a set to see it here."
                else "No sessions match this filter.",
                modifier = Modifier.padding(vertical = 24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.id }) { session ->
                    SessionRow(session, onClick = { onOpenSession(session.id) })
                }
            }
        }

        GymButton("Back", onBack, Modifier.padding(top = 16.dp), GymButtonStyle.Secondary)
    }
}

@Composable
private fun SessionRow(session: WorkoutSession, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    exerciseLabel(session.exerciseType),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "${formatDate(session.startedAt)} · ${formatDuration(session.durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("${session.repCount} reps", style = MaterialTheme.typography.titleMedium)
            Text(
                "  ›",
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
