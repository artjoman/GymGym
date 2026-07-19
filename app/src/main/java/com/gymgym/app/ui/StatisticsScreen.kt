package com.gymgym.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gymgym.app.R
import com.gymgym.app.data.BodyMeasurement
import com.gymgym.app.data.CompletedWorkoutWithExercises
import com.gymgym.app.data.WorkoutSession

/** Statistics + History unified under one screen with tabs. */
@Composable
fun StatisticsScreen(
    sessions: List<WorkoutSession>,
    completedWorkouts: List<CompletedWorkoutWithExercises>,
    bodyMeasurements: List<BodyMeasurement>,
    onOpenSession: (Long) -> Unit,
    onBack: () -> Unit,
    customNames: Map<String, String> = emptyMap(),
) {
    var tab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
            Text(
                stringResource(R.string.statistics_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        TabRow(selectedTabIndex = tab, modifier = Modifier.padding(top = 8.dp)) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.stats_title)) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.history_title)) })
        }

        val contentModifier = Modifier.weight(1f).padding(top = 12.dp)
        if (tab == 0) {
            StatsContent(sessions, bodyMeasurements, contentModifier)
        } else {
            HistoryContent(sessions, completedWorkouts, onOpenSession, contentModifier, customNames)
        }
    }
}
