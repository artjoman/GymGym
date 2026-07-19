package com.gymgym.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymgym.app.R
import com.gymgym.app.data.PlanWithCycles
import com.gymgym.app.exercise.ExerciseRef
import com.gymgym.app.program.Program

@Composable
fun PlanListScreen(
    plans: List<PlanWithCycles>,
    onEdit: (Long) -> Unit,
    onNew: () -> Unit,
    onDelete: (Long) -> Unit,
    onSetActive: (Long) -> Unit,
    onStart: (PlanWithCycles) -> Unit,
    onUseProgram: (Program) -> Unit,
    onBack: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
            Text(
                stringResource(R.string.home_workout_plans),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        TabRow(selectedTabIndex = tab, modifier = Modifier.padding(top = 8.dp)) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.plans_tab_my)) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.home_programs)) })
        }

        val contentModifier = Modifier.weight(1f).padding(top = 12.dp)
        if (tab == 0) {
            Column(modifier = contentModifier) {
                GymButton(
                    text = stringResource(R.string.plans_new),
                    onClick = onNew,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (plans.isEmpty()) {
                    Text(
                        stringResource(R.string.plans_empty),
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(plans, key = { it.plan.id }) { plan ->
                            PlanCard(
                                plan = plan,
                                onEdit = { onEdit(plan.plan.id) },
                                onDelete = { onDelete(plan.plan.id) },
                                onSetActive = { onSetActive(plan.plan.id) },
                                onStart = { onStart(plan) },
                            )
                        }
                    }
                }
            }
        } else {
            ProgramsContent(onUse = onUseProgram, modifier = contentModifier)
        }
    }
}

@Composable
private fun PlanCard(
    plan: PlanWithCycles,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetActive: () -> Unit,
    onStart: () -> Unit,
) {
    val runnable = plan.cycles.any { c ->
        c.workouts.any { w -> w.exercises.any { ExerciseRef.counter(it.exerciseRef) != null } }
    }
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = plan.plan.name,
            onConfirm = onDelete,
            onDismiss = { confirmDelete = false },
        )
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    plan.plan.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (plan.plan.isActive) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(stringResource(R.string.plans_active)) },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledLabelColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
            Text(
                planSummary(
                    cycles = plan.cycles.size,
                    workouts = plan.workoutCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (runnable) {
                GymButton(
                    text = stringResource(R.string.action_start),
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onEdit) { Text(stringResource(R.string.action_edit)) }
                if (!plan.plan.isActive) {
                    OutlinedButton(onClick = onSetActive) { Text(stringResource(R.string.plans_set_active)) }
                }
                TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.action_delete)) }
            }
        }
    }
}

@Composable
private fun planSummary(cycles: Int, workouts: Int): String {
    val cyclesText = pluralStringResource(R.plurals.plan_cycles, cycles, cycles)
    val workoutsText = pluralStringResource(R.plurals.plan_workouts, workouts, workouts)
    return "$cyclesText · $workoutsText"
}
