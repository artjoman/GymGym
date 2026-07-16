package com.gymgym.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymgym.app.data.DraftExercise
import com.gymgym.app.data.PlanWithExercises

private data class DraftRow(val exercise: Exercise, val reps: Int, val sets: Int)

@Composable
fun PlanEditScreen(
    existing: PlanWithExercises?,
    onSave: (id: Long, name: String, exercises: List<DraftExercise>) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf(existing?.plan?.name ?: "") }
    val rows = remember {
        existing?.orderedExercises
            ?.mapNotNull { e ->
                Exercise.entries.find { it.name == e.exerciseType }
                    ?.let { DraftRow(it, e.targetReps, e.targetSets) }
            }
            .orEmpty()
            .toMutableStateList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (existing == null) "New plan" else "Edit plan",
            style = MaterialTheme.typography.headlineSmall,
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Plan name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        rows.forEachIndexed { index, row ->
            ExerciseRowCard(
                row = row,
                canMoveUp = index > 0,
                canMoveDown = index < rows.size - 1,
                onCycleExercise = {
                    // Plans are rep-based; timed (hold) exercises aren't cyclable here.
                    val plannable = Exercise.entries.filter { !it.timed }
                    val pos = plannable.indexOf(row.exercise).coerceAtLeast(0)
                    rows[index] = row.copy(exercise = plannable[(pos + 1) % plannable.size])
                },
                onReps = { rows[index] = row.copy(reps = it.coerceIn(1, 100)) },
                onSets = { rows[index] = row.copy(sets = it.coerceIn(1, 20)) },
                onMoveUp = { if (index > 0) rows.swap(index, index - 1) },
                onMoveDown = { if (index < rows.size - 1) rows.swap(index, index + 1) },
                onRemove = { rows.removeAt(index) },
            )
        }

        OutlinedButton(
            onClick = { rows.add(DraftRow(Exercise.SQUAT, reps = 10, sets = 3)) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Add exercise")
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GymButton(
                text = "Save",
                onClick = {
                    onSave(
                        existing?.plan?.id ?: 0L,
                        name.trim(),
                        rows.map { DraftExercise(it.exercise.name, it.reps, it.sets) },
                    )
                },
                enabled = name.isNotBlank() && rows.isNotEmpty(),
            )
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}

@Composable
private fun ExerciseRowCard(
    row: DraftRow,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onCycleExercise: () -> Unit,
    onReps: (Int) -> Unit,
    onSets: (Int) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = onCycleExercise) { Text(row.exercise.displayName) }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onMoveUp, enabled = canMoveUp) { Text("↑") }
                    TextButton(onClick = onMoveDown, enabled = canMoveDown) { Text("↓") }
                    TextButton(onClick = onRemove) { Text("✕") }
                }
            }
            Stepper(label = "Reps", value = row.reps, onChange = onReps)
            Stepper(label = "Sets", value = row.sets, onChange = onSets)
        }
    }
}

@Composable
private fun Stepper(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { onChange(value - 1) }) { Text("−") }
            Text(
                value.toString(),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            OutlinedButton(onClick = { onChange(value + 1) }) { Text("+") }
        }
    }
}

private fun <T> MutableList<T>.swap(a: Int, b: Int) {
    val tmp = this[a]
    this[a] = this[b]
    this[b] = tmp
}
