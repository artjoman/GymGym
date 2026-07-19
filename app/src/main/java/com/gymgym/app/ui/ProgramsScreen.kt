package com.gymgym.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymgym.app.R
import com.gymgym.app.exercise.ExerciseCatalog
import com.gymgym.app.program.Program
import com.gymgym.app.program.Programs

/** Preset programs list (+ detail drill-down), embedded as a tab in Workout Plans. */
@Composable
fun ProgramsContent(
    onUse: (Program) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf<Program?>(null) }

    val current = selected
    if (current != null) {
        ProgramDetail(
            program = current,
            onUse = { onUse(current) },
            onBack = { selected = null },
            modifier = modifier,
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            for (program in Programs.ALL) {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { selected = program },
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(program.nameRes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(program.descRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgramDetail(
    program: Program,
    onUse: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
        Text(stringResource(program.nameRes), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(program.descRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        for (cycle in program.cycles) {
            for (workout in cycle.workouts) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            workout.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        for (e in workout.exercises) {
                            val label = ExerciseCatalog.byId(e.ref)
                                ?.let { context.getString(it.nameRes) } ?: e.ref
                            val detail = if (e.seconds != null) {
                                context.getString(R.string.plan_summary_seconds, label, e.seconds, e.sets)
                            } else {
                                context.getString(R.string.plan_summary_reps, label, e.reps, e.sets)
                            }
                            Text(
                                detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }
        }

        Text(
            stringResource(R.string.programs_active_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        GymButton(
            text = stringResource(R.string.programs_use),
            onClick = onUse,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}
