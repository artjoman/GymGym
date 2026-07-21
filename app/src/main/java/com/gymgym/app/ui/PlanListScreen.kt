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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymgym.app.R
import com.gymgym.app.data.PlanWithCycles
import com.gymgym.app.program.Program

@Composable
fun PlanListScreen(
    plans: List<PlanWithCycles>,
    onEdit: (Long) -> Unit,
    onNew: () -> Unit,
    onDelete: (Long) -> Unit,
    onSetActive: (Long) -> Unit,
    onUseProgram: (Program) -> Unit,
    onBack: () -> Unit,
) {
    // Two tabs, switchable by tap or horizontal swipe (as in Statistics).
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val scope = rememberCoroutineScope()
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

        val tabLabels = listOf(
            stringResource(R.string.plans_tab_my),
            stringResource(R.string.home_programs),
        )
        TabRow(selectedTabIndex = pagerState.currentPage, modifier = Modifier.padding(top = 8.dp)) {
            tabLabels.forEachIndexed { i, label ->
                Tab(
                    selected = pagerState.currentPage == i,
                    onClick = { scope.launch { pagerState.animateScrollToPage(i) } },
                    text = { Text(label) },
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 12.dp),
        ) { page ->
            if (page == 0) {
                Column(modifier = Modifier.fillMaxSize()) {
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
                                )
                            }
                        }
                    }
                }
            } else {
                ProgramsContent(
                    // Activating a program lands the user back on My Plans.
                    onUse = {
                        onUseProgram(it)
                        scope.launch { pagerState.animateScrollToPage(0) }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: PlanWithCycles,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetActive: () -> Unit,
) {
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
            // Workouts are started from Current mission, not from this list.
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
