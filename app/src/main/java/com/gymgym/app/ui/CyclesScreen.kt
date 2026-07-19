package com.gymgym.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gymgym.app.R
import com.gymgym.app.cycle.CycleSummary
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

/** History-style list of completed cycles (Statistics → Cycles tab). */
@Composable
fun CyclesContent(cycles: List<CycleSummary>, modifier: Modifier = Modifier) {
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
                items(cycles, key = { "${it.cycleId}" }) { cycle ->
                    CycleCard(summary = cycle)
                }
            }
        }
    }
}
