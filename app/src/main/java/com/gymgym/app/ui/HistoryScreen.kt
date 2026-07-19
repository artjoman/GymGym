package com.gymgym.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymgym.app.R
import com.gymgym.app.data.CompletedWorkoutRepository
import com.gymgym.app.data.CompletedWorkoutWithExercises
import com.gymgym.app.data.WorkoutSession
import com.gymgym.app.exercise.ExerciseCatalog

/** Scrollable History content (no title/back) — embedded in [StatisticsScreen]. */
@Composable
fun HistoryContent(
    sessions: List<WorkoutSession>,
    completedWorkouts: List<CompletedWorkoutWithExercises>,
    onOpenSession: (Long) -> Unit,
    modifier: Modifier = Modifier,
    customNames: Map<String, String> = emptyMap(),
) {
    var filter by remember { mutableStateOf(WorkoutFilter()) }
    val filtered = sessions.applyFilter(filter)
    val cutoff = filter.range.cutoff()
    val filteredWorkouts = completedWorkouts.filter { it.workout.startedAt >= cutoff }
    val expanded = remember { mutableStateMapOf<Long, Boolean>() }

    Column(modifier = modifier.fillMaxWidth()) {
        FilterBar(filter = filter, onFilter = { filter = it })

        if (filtered.isEmpty() && filteredWorkouts.isEmpty()) {
            Text(
                if (sessions.isEmpty() && completedWorkouts.isEmpty()) {
                    stringResource(R.string.history_empty)
                } else {
                    stringResource(R.string.history_empty_filtered)
                },
                modifier = Modifier.padding(vertical = 24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (filteredWorkouts.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.history_workouts)) }
                    items(filteredWorkouts, key = { "w${it.workout.id}" }) { cw ->
                        CompletedWorkoutRow(
                            item = cw,
                            expanded = expanded[cw.workout.id] == true,
                            onToggle = { expanded[cw.workout.id] = expanded[cw.workout.id] != true },
                            customNames = customNames,
                        )
                    }
                }
                if (filtered.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.history_exercises)) }
                    items(filtered, key = { "s${it.id}" }) { session ->
                        SessionRow(session, onClick = { onOpenSession(session.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun CompletedWorkoutRow(
    item: CompletedWorkoutWithExercises,
    expanded: Boolean,
    onToggle: () -> Unit,
    customNames: Map<String, String> = emptyMap(),
) {
    val context = LocalContext.current
    val w = item.workout
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(w.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(
                            R.string.history_meta,
                            formatDate(w.startedAt),
                            formatDuration(context, w.durationMs),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Workout % = total completed reps ÷ total planned reps across all
                // exercises, computed live so it always matches the rows below (and
                // is correct for rows saved before the formula changed). Legacy rows
                // without stored targets fall back to the value recorded at the time.
                val totalPlanned = item.orderedExercises.sumOf { it.targetReps * it.targetSets }
                val totalCompleted = item.orderedExercises.sumOf { it.reps }
                val pct = if (totalPlanned > 0) {
                    CompletedWorkoutRepository.workoutPercent(totalCompleted, totalPlanned)
                } else {
                    w.avgPercent
                }
                Text(
                    "$pct%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (expanded) {
                for (e in item.orderedExercises) {
                    // Same representation as the home cycle cards / Workout Details.
                    val line = exerciseLineText(
                        name = exerciseRefName(e.exerciseRef, customNames),
                        targetReps = e.targetReps,
                        targetSets = e.targetSets,
                        completedReps = e.reps,
                    )
                    Text(
                        line,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: WorkoutSession, onClick: () -> Unit) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    exerciseLabel(context, session.exerciseType),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(
                        R.string.history_meta,
                        formatDate(session.startedAt),
                        formatDuration(context, session.durationMs),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                if (isTimedExercise(session.exerciseType)) formatDurationLong(context, session.durationMs)
                else pluralStringResource(R.plurals.reps, session.repCount, session.repCount),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "  ›",
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
