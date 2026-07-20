package com.gymgym.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymgym.app.R
import com.gymgym.app.cycle.CycleLineStatus
import com.gymgym.app.cycle.CycleStatus
import com.gymgym.app.cycle.CycleSummary
import com.gymgym.app.cycle.CycleWorkoutLine
import com.gymgym.app.exercise.ExerciseCatalog
import androidx.compose.ui.platform.LocalContext

/**
 * The shared exercise line used across History and the cycle cards:
 *   "<Name>: <TargetReps>×<Sets> sets • <Completed>/<Planned> reps"
 * With [completedReps] null it renders planned-only ("<Name>: 10×3 sets"); with
 * 0 completed it renders "<Name>: Skipped".
 */
@Composable
internal fun exerciseLineText(
    name: String,
    targetReps: Int,
    targetSets: Int,
    completedReps: Int?,
): String {
    val planned = targetReps * targetSets
    return when {
        completedReps == null ->
            if (planned > 0) stringResource(R.string.history_exercise_planned, name, targetReps, targetSets)
            else name
        planned <= 0 -> stringResource(R.string.history_exercise_legacy, name, completedReps)
        completedReps == 0 -> stringResource(R.string.history_exercise_skipped, name)
        else -> stringResource(R.string.history_exercise_line, name, targetReps, targetSets, completedReps, planned)
    }
}

/** Resolve an exercise ref to a display name (catalog string or custom name). */
@Composable
internal fun exerciseRefName(ref: String, customNames: Map<String, String>): String {
    val ctx = LocalContext.current
    return ExerciseCatalog.byId(ref)?.let { ctx.getString(it.nameRes) }
        ?: customNames[ref]
        ?: ref
}

/**
 * Statistics → Cycles tab: the active cycle (top) then completed cycles, each as
 * a full record — status, dates, overall %, and every workout in order with its
 * status, % and exercise breakdown.
 */
@Composable
fun CyclesContent(
    cycles: List<CycleSummary>,
    modifier: Modifier = Modifier,
    customNames: Map<String, String> = emptyMap(),
    expandLastCycle: Boolean = false,
) {
    // Records are collapsed by default; arriving from the Last cycle card expands
    // that record (the most recent completed one).
    val lastCompletedKey = remember(cycles) {
        cycles.firstOrNull { it.status == CycleStatus.COMPLETED }?.key()
    }
    val expanded = remember(cycles, expandLastCycle) {
        mutableStateMapOf<String, Boolean>().apply {
            if (expandLastCycle && lastCompletedKey != null) put(lastCompletedKey, true)
        }
    }
    Column(modifier = modifier.fillMaxWidth()) {
        if (cycles.isEmpty()) {
            Text(
                stringResource(R.string.home_no_cycles),
                modifier = Modifier.padding(vertical = 24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(cycles, key = { it.key() }) { cycle ->
                    val k = cycle.key()
                    CycleRecordCard(
                        summary = cycle,
                        customNames = customNames,
                        expanded = expanded[k] == true,
                        onToggle = { expanded[k] = expanded[k] != true },
                    )
                }
            }
        }
    }
}

/** Stable key: the same cycle can appear once per completed pass. */
private fun CycleSummary.key(): String = "$cycleId-$status-${startedAt ?: 0L}"

/**
 * One cycle record: header (name, %, plan, status/dates) and — when [expanded] —
 * every workout in order with its status, % and exercise breakdown.
 * [highlightWorkoutId] marks the workout the user is about to run.
 */
@Composable
internal fun CycleRecordCard(
    summary: CycleSummary,
    customNames: Map<String, String>,
    expanded: Boolean,
    onToggle: (() -> Unit)? = null,
    highlightWorkoutId: Long? = null,
) {
    val cardModifier = Modifier.fillMaxWidth()
        .let { if (onToggle != null) it.clickable(onClick = onToggle) else it }
    Card(modifier = cardModifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    "${summary.cycleName} (${summary.percent}%)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    summary.planName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(
                cycleStatusLine(summary),
                style = MaterialTheme.typography.labelMedium,
                color = if (summary.status == CycleStatus.ACTIVE) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(top = 2.dp),
            )
            if (!expanded) {
                // Collapsed: header only (tap to expand).
                return@Column
            }
            for (w in summary.workouts) {
                val highlighted = highlightWorkoutId != null && w.workoutId == highlightWorkoutId
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        w.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (highlighted) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (highlighted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        workoutStatusLabel(w),
                        style = MaterialTheme.typography.labelMedium,
                        color = when (w.status) {
                            CycleLineStatus.SKIPPED -> MaterialTheme.colorScheme.error
                            CycleLineStatus.DONE -> MaterialTheme.colorScheme.primary
                            CycleLineStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                for (ex in w.exercises) {
                    Text(
                        exerciseLineText(
                            name = exerciseRefName(ex.exerciseRef, customNames),
                            targetReps = ex.targetReps,
                            targetSets = ex.targetSets,
                            completedReps = ex.completedReps,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun cycleStatusLine(s: CycleSummary): String {
    val status = stringResource(
        if (s.status == CycleStatus.ACTIVE) R.string.cycle_status_active else R.string.cycle_status_completed,
    )
    val dates = when {
        s.startedAt != null && s.completedAt != null -> "${formatDate(s.startedAt)} – ${formatDate(s.completedAt)}"
        s.startedAt != null -> formatDate(s.startedAt)
        else -> null
    }
    return if (dates != null) "$status · $dates" else status
}

@Composable
private fun workoutStatusLabel(w: CycleWorkoutLine): String = when (w.status) {
    CycleLineStatus.DONE -> "${w.percent}%"
    CycleLineStatus.SKIPPED -> stringResource(R.string.cycle_skipped)
    CycleLineStatus.PENDING -> stringResource(R.string.cycle_status_pending)
}
