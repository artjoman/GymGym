package com.gymgym.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.gymgym.app.R
import com.gymgym.app.cycle.BlockStatus
import com.gymgym.app.cycle.DashboardState
import java.text.DateFormat
import java.util.Date

@Composable
fun NextMissionScreen(
    dashboard: DashboardState,
    onStart: (Long) -> Unit,
    onSwap: (Long) -> Unit,
    onSkip: (Long) -> Unit,
    onBack: () -> Unit,
    /** The upcoming workout's exercises, shown expanded before Start. */
    nextWorkout: com.gymgym.app.cycle.CycleWorkoutDetail? = null,
    customNames: Map<String, String> = emptyMap(),
) {
    var showSwap by remember { mutableStateOf(false) }
    val mission = dashboard.nextMission

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
        Text(stringResource(R.string.mission_title), style = MaterialTheme.typography.headlineSmall)

        if (mission == null) {
            Text(
                stringResource(R.string.mission_no_plan),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        Text(
            dashboard.currentCycle?.name.orEmpty(),
            style = MaterialTheme.typography.titleMedium,
        )
        CycleProgressBar(dashboard)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    mission.workout.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                mission.plannedDate?.let {
                    Text(
                        stringResource(R.string.mission_planned, formatMissionTime(it)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                // Expanded: what you're about to do, in the shared exercise format.
                nextWorkout?.exercises?.forEach { ex ->
                    Text(
                        exerciseLineText(
                            name = exerciseRefName(ex.exerciseRef, customNames),
                            targetReps = ex.targetReps,
                            targetSets = ex.targetSets,
                            completedReps = ex.completedReps,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        GymButton(
            text = stringResource(R.string.action_start),
            onClick = { onStart(mission.workout.id) },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            GymButton(
                text = stringResource(R.string.mission_swap),
                onClick = { showSwap = true },
                style = GymButtonStyle.Secondary,
                modifier = Modifier.weight(1f),
            )
            GymButton(
                text = stringResource(R.string.mission_skip),
                onClick = { onSkip(mission.workout.id) },
                style = GymButtonStyle.Secondary,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (showSwap) {
        AlertDialog(
            onDismissRequest = { showSwap = false },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSwap = false }) { Text(stringResource(R.string.action_cancel)) }
            },
            title = { Text(stringResource(R.string.mission_swap_pick)) },
            text = {
                // Every workout in the cycle — completed ones can be repeated; the
                // current one is shown but disabled (can't swap with itself).
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    dashboard.blocks.forEach { block ->
                        val label = when (block.status) {
                            BlockStatus.NEXT ->
                                "${block.workout.name} — ${stringResource(R.string.mission_swap_current)}"
                            BlockStatus.DONE ->
                                "${block.workout.name} — ${stringResource(R.string.mission_swap_completed, block.percent)}"
                            BlockStatus.SKIPPED ->
                                "${block.workout.name} — ${stringResource(R.string.cycle_skipped)}"
                            BlockStatus.PENDING -> block.workout.name
                        }
                        TextButton(
                            enabled = block.status != BlockStatus.NEXT,
                            onClick = {
                                showSwap = false
                                onSwap(block.workout.id)
                            },
                        ) { Text(label) }
                    }
                }
            },
        )
    }
}

@Composable
fun CycleProgressBar(dashboard: DashboardState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            dashboard.blocks.forEach { block ->
                val color = when (block.status) {
                    BlockStatus.DONE -> MaterialTheme.colorScheme.primary
                    BlockStatus.SKIPPED -> MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    BlockStatus.NEXT -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    BlockStatus.PENDING -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color),
                    contentAlignment = Alignment.Center,
                ) {
                    if (block.status == BlockStatus.DONE) {
                        Text(
                            "${block.percent}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            dashboard.blocks.forEach { block ->
                val wd = block.weekday?.let { " · ${weekdayShort(it)}" }.orEmpty()
                Text(
                    block.workout.name + wd,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
            }
        }
    }
}

private fun formatMissionTime(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))

/** Localized short weekday name for our 1=Mon..7=Sun encoding. */
internal fun weekdayShort(day: Int): String {
    val symbols = java.text.DateFormatSymbols.getInstance().shortWeekdays
    val calDay = if (day == 7) java.util.Calendar.SUNDAY else day + 1
    return symbols.getOrNull(calDay).orEmpty()
}
