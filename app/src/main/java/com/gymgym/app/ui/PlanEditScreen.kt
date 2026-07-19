package com.gymgym.app.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymgym.app.R
import com.gymgym.app.data.CustomExercise
import com.gymgym.app.data.DraftCycle
import com.gymgym.app.data.DraftPlan
import com.gymgym.app.data.DraftWorkout
import com.gymgym.app.data.DraftWorkoutExercise
import com.gymgym.app.data.PlanWithCycles
import com.gymgym.app.exercise.ExerciseCatalog
import com.gymgym.app.exercise.ExerciseRef
import java.text.DateFormat
import java.util.Date

// --- Editable in-memory state (Compose-observable) ---

private class ExRow(
    val ref: String,
    val label: String,
    val timed: Boolean,
    reps: Int,
    sets: Int,
    seconds: Int,
) {
    var reps by mutableStateOf(reps)
    var sets by mutableStateOf(sets)
    var seconds by mutableStateOf(seconds)
}

private class WoRow(name: String, val exercises: SnapshotStateList<ExRow>) {
    var name by mutableStateOf(name)
}

private class CyRow(name: String, val workouts: SnapshotStateList<WoRow>) {
    var name by mutableStateOf(name)
}

@Composable
fun PlanEditScreen(
    existing: PlanWithCycles?,
    customExercises: List<CustomExercise>,
    onSave: (id: Long, draft: DraftPlan) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var planName by remember { mutableStateOf(existing?.plan?.name ?: "") }
    var endDate by remember { mutableStateOf(existing?.plan?.endDate) }
    val cycles = remember {
        existing?.orderedCycles.orEmpty().map { cw ->
            CyRow(
                cw.cycle.name,
                cw.orderedWorkouts.map { ww ->
                    WoRow(
                        ww.workout.name,
                        ww.orderedExercises.map { we ->
                            val timed = ExerciseRef.counter(we.exerciseRef)?.timed == true
                            ExRow(
                                we.exerciseRef,
                                refLabel(context, we.exerciseRef, customExercises),
                                timed,
                                we.targetReps.coerceAtLeast(1),
                                we.targetSets.coerceAtLeast(1),
                                we.targetSeconds ?: 30,
                            )
                        }.toMutableStateList(),
                    )
                }.toMutableStateList(),
            )
        }.toMutableStateList()
    }

    var openCycle by remember { mutableStateOf<CyRow?>(null) }
    var openWorkout by remember { mutableStateOf<WoRow?>(null) }

    val saveEnabled = planName.isNotBlank() && cycles.isNotEmpty()
    val saveAll: () -> Unit = {
        onSave(existing?.plan?.id ?: 0L, buildDraft(planName.trim(), endDate, cycles))
    }

    when {
        openWorkout != null -> WorkoutEditor(
            workout = openWorkout!!,
            customExercises = customExercises,
            onSaveAll = saveAll,
            saveEnabled = saveEnabled,
            onBack = { openWorkout = null },
        )
        openCycle != null -> CycleEditor(
            cycle = openCycle!!,
            onOpenWorkout = { openWorkout = it },
            onSaveAll = saveAll,
            saveEnabled = saveEnabled,
            onBack = { openCycle = null },
        )
        else -> PlanEditor(
            isNew = existing == null,
            name = planName,
            onName = { planName = it },
            endDate = endDate,
            onEndDate = { endDate = it },
            cycles = cycles,
            onOpenCycle = { openCycle = it },
            onSave = {
                onSave(existing?.plan?.id ?: 0L, buildDraft(planName.trim(), endDate, cycles))
            },
            onCancel = onCancel,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanEditor(
    isNew: Boolean,
    name: String,
    onName: (String) -> Unit,
    endDate: Long?,
    onEndDate: (Long?) -> Unit,
    cycles: SnapshotStateList<CyRow>,
    onOpenCycle: (CyRow) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (isNew) stringResource(R.string.plans_new) else stringResource(R.string.plan_edit_edit),
            style = MaterialTheme.typography.headlineSmall,
        )

        OutlinedTextField(
            value = name,
            onValueChange = onName,
            label = { Text(stringResource(R.string.plan_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(stringResource(R.string.plan_end_date), style = MaterialTheme.typography.titleMedium)
                Text(
                    endDate?.let { formatDay(it) } ?: stringResource(R.string.plan_end_date_none),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (endDate != null) {
                    TextButton(onClick = { onEndDate(null) }) { Text(stringResource(R.string.action_clear)) }
                }
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Text(stringResource(R.string.plan_pick_date))
                }
            }
        }

        HorizontalDivider()
        Text(stringResource(R.string.plan_cycles_label), style = MaterialTheme.typography.titleMedium)

        if (cycles.isEmpty()) {
            Text(
                stringResource(R.string.plan_no_cycles),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        cycles.forEachIndexed { index, cycle ->
            NodeCard(
                title = cycle.name.ifBlank { stringResource(R.string.plan_untitled_cycle) },
                subtitle = context.resources.getQuantityString(
                    R.plurals.plan_workouts, cycle.workouts.size, cycle.workouts.size,
                ),
                onOpen = { onOpenCycle(cycle) },
                onRemove = { cycles.removeAt(index) },
            )
        }

        OutlinedButton(
            onClick = {
                val newCycle = CyRow(
                    context.getString(R.string.plan_default_cycle_name, cycles.size + 1),
                    mutableListOf<WoRow>().toMutableStateList(),
                )
                cycles.add(newCycle)
                onOpenCycle(newCycle)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.plan_new_cycle))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GymButton(
                text = stringResource(R.string.action_save),
                onClick = onSave,
                enabled = name.isNotBlank() && cycles.isNotEmpty(),
            )
            TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = endDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onEndDate(state.selectedDateMillis)
                    showDatePicker = false
                }) { Text(stringResource(R.string.action_done)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun CycleEditor(
    cycle: CyRow,
    onOpenWorkout: (WoRow) -> Unit,
    onSaveAll: () -> Unit,
    saveEnabled: Boolean,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EditorHeader(stringResource(R.string.plan_edit_cycle), onBack)

        OutlinedTextField(
            value = cycle.name,
            onValueChange = { cycle.name = it },
            label = { Text(stringResource(R.string.plan_cycle_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(stringResource(R.string.plan_workouts_label), style = MaterialTheme.typography.titleMedium)
        if (cycle.workouts.isEmpty()) {
            Text(
                stringResource(R.string.plan_no_workouts),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        cycle.workouts.forEachIndexed { index, workout ->
            NodeCard(
                title = workout.name.ifBlank { stringResource(R.string.plan_untitled_workout) },
                subtitle = context.resources.getQuantityString(
                    R.plurals.plan_exercises,
                    workout.exercises.size, workout.exercises.size,
                ),
                onOpen = { onOpenWorkout(workout) },
                onRemove = { cycle.workouts.removeAt(index) },
            )
        }

        OutlinedButton(
            onClick = {
                val newWorkout = WoRow(
                    context.getString(R.string.plan_default_workout_name, cycle.workouts.size + 1),
                    mutableListOf<ExRow>().toMutableStateList(),
                )
                cycle.workouts.add(newWorkout)
                onOpenWorkout(newWorkout)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.plan_add_workout))
        }
        GymButton(
            text = stringResource(R.string.plan_save),
            onClick = onSaveAll,
            enabled = saveEnabled,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}

@Composable
private fun WorkoutEditor(
    workout: WoRow,
    customExercises: List<CustomExercise>,
    onSaveAll: () -> Unit,
    saveEnabled: Boolean,
    onBack: () -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EditorHeader(stringResource(R.string.plan_edit_workout), onBack)

        OutlinedTextField(
            value = workout.name,
            onValueChange = { workout.name = it },
            label = { Text(stringResource(R.string.plan_workout_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (workout.exercises.isEmpty()) {
            Text(
                stringResource(R.string.plan_no_exercises),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        workout.exercises.forEachIndexed { index, ex ->
            ExerciseRowCard(
                row = ex,
                canMoveUp = index > 0,
                canMoveDown = index < workout.exercises.size - 1,
                onReps = { ex.reps = it.coerceIn(1, 100) },
                onSeconds = { ex.seconds = it.coerceIn(5, 600) },
                onSets = { ex.sets = it.coerceIn(1, 20) },
                onMoveUp = { if (index > 0) workout.exercises.swap(index, index - 1) },
                onMoveDown = { if (index < workout.exercises.size - 1) workout.exercises.swap(index, index + 1) },
                onRemove = { workout.exercises.removeAt(index) },
            )
        }

        OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.plan_add_exercise))
        }
        GymButton(
            text = stringResource(R.string.plan_save),
            onClick = onSaveAll,
            enabled = saveEnabled,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }

    if (showPicker) {
        ExercisePickerDialog(
            customExercises = customExercises,
            onPick = { ref, label, timed ->
                showPicker = false
                workout.exercises.add(
                    ExRow(ref, label, timed, reps = 10, sets = 3, seconds = 30),
                )
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun EditorHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun NodeCard(
    title: String,
    subtitle: String,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onRemove) { Text("✕") }
        }
    }
}

@Composable
private fun ExerciseRowCard(
    row: ExRow,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onReps: (Int) -> Unit,
    onSeconds: (Int) -> Unit,
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
                Text(
                    row.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onMoveUp, enabled = canMoveUp) { Text("↑") }
                    TextButton(onClick = onMoveDown, enabled = canMoveDown) { Text("↓") }
                    TextButton(onClick = onRemove) { Text("✕") }
                }
            }
            if (row.timed) {
                Stepper(stringResource(R.string.plan_seconds), row.seconds, onSeconds, step = 5)
            } else {
                Stepper(stringResource(R.string.plan_reps), row.reps, onReps)
            }
            Stepper(stringResource(R.string.plan_sets), row.sets, onSets)
        }
    }
}

@Composable
private fun ExercisePickerDialog(
    customExercises: List<CustomExercise>,
    onPick: (ref: String, label: String, timed: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
        title = { Text(stringResource(R.string.plan_add_exercise)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
            ) {
                for ((category, exercises) in ExerciseCatalog.byCategory()) {
                    if (exercises.isEmpty()) continue
                    Text(
                        stringResource(category.labelRes).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                    exercises.forEach { ex ->
                        val label = stringResource(ex.nameRes)
                        PickerRow(label) { onPick(ex.id, label, ex.isTimed) }
                    }
                }
                if (customExercises.isNotEmpty()) {
                    Text(
                        stringResource(R.string.library_custom_section).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                    customExercises.forEach { c ->
                        PickerRow(c.name) { onPick(ExerciseRef.forCustom(c.id), c.name, false) }
                    }
                }
            }
        },
    )
}

@Composable
private fun PickerRow(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
    )
}

@Composable
private fun Stepper(label: String, value: Int, onChange: (Int) -> Unit, step: Int = 1) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { onChange(value - step) }) { Text("−") }
            Text(
                value.toString(),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            OutlinedButton(onClick = { onChange(value + step) }) { Text("+") }
        }
    }
}

private fun buildDraft(name: String, endDate: Long?, cycles: List<CyRow>): DraftPlan =
    DraftPlan(
        name = name,
        endDate = endDate,
        cycles = cycles.map { cy ->
            DraftCycle(
                name = cy.name.trim(),
                workouts = cy.workouts.map { wo ->
                    DraftWorkout(
                        name = wo.name.trim(),
                        weekday = null,
                        exercises = wo.exercises.map { ex ->
                            DraftWorkoutExercise(
                                exerciseRef = ex.ref,
                                targetReps = ex.reps,
                                targetSets = ex.sets,
                                targetSeconds = if (ex.timed) ex.seconds else null,
                            )
                        },
                    )
                },
            )
        },
    )

private fun refLabel(context: Context, ref: String, custom: List<CustomExercise>): String {
    ExerciseRef.customId(ref)?.let { id -> return custom.find { it.id == id }?.name ?: ref }
    return ExerciseCatalog.byId(ref)?.let { context.getString(it.nameRes) } ?: ref
}

private fun formatDay(millis: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))

private fun <T> SnapshotStateList<T>.swap(a: Int, b: Int) {
    val tmp = this[a]
    this[a] = this[b]
    this[b] = tmp
}
