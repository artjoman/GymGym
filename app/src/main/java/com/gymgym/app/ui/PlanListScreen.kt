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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymgym.app.R
import com.gymgym.app.data.PlanWithExercises

@Composable
fun PlanListScreen(
    plans: List<PlanWithExercises>,
    onRun: (PlanWithExercises) -> Unit,
    onEdit: (Long) -> Unit,
    onNew: () -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp)) {
        Text(stringResource(R.string.plans_title), style = MaterialTheme.typography.headlineSmall)

        GymButton(
            text = stringResource(R.string.plans_new),
            onClick = onNew,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
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
                        onRun = { onRun(plan) },
                        onEdit = { onEdit(plan.plan.id) },
                        onDelete = { onDelete(plan.plan.id) },
                    )
                }
            }
        }

        TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.action_back))
        }
    }
}

@Composable
private fun PlanCard(
    plan: PlanWithExercises,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                plan.plan.name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                planSummary(LocalContext.current, plan),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onRun) { Text(stringResource(R.string.action_start)) }
                OutlinedButton(onClick = onEdit) { Text(stringResource(R.string.action_edit)) }
                TextButton(onClick = onDelete) { Text(stringResource(R.string.action_delete)) }
            }
        }
    }
}

private fun planSummary(context: Context, plan: PlanWithExercises): String {
    val parts = plan.orderedExercises.map { e ->
        val label = exerciseLabel(context, e.exerciseType)
        if (isTimedExercise(e.exerciseType)) {
            context.getString(R.string.plan_summary_seconds, label, e.targetReps, e.targetSets)
        } else {
            context.getString(R.string.plan_summary_reps, label, e.targetReps, e.targetSets)
        }
    }
    return if (parts.isEmpty()) context.getString(R.string.plans_no_exercises) else parts.joinToString(" · ")
}
