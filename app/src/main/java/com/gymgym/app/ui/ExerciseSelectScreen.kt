package com.gymgym.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ExerciseSelectScreen(
    greeting: String?,
    onExerciseSelected: (Exercise) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (greeting.isNullOrBlank()) "GymGym" else "Hi, $greeting",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text("Pick an exercise", modifier = Modifier.padding(top = 8.dp))
        for (exercise in Exercise.entries) {
            Button(
                onClick = { onExerciseSelected(exercise) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(exercise.displayName)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onOpenHistory, modifier = Modifier.weight(1f)) {
                Text("History")
            }
            OutlinedButton(onClick = onOpenStats, modifier = Modifier.weight(1f)) {
                Text("Stats")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onOpenProfile, modifier = Modifier.weight(1f)) {
                Text("Profile")
            }
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.weight(1f)) {
                Text("Settings")
            }
        }
    }
}
