package com.gymgym.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.gymgym.app.R
import com.gymgym.app.data.CustomExercise
import com.gymgym.app.exercise.CatalogExercise
import com.gymgym.app.exercise.ExerciseCatalog
import com.gymgym.app.exercise.ExerciseRef

@Composable
fun ExerciseLibraryScreen(
    customExercises: List<CustomExercise>,
    onTest: (String) -> Unit,
    onAutoDetect: () -> Unit,
    onAddCustom: (String) -> Unit,
    onDeleteCustom: (Long) -> Unit,
    onBack: () -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.library_title), style = MaterialTheme.typography.headlineSmall)

        GymButton(
            text = stringResource(R.string.home_auto_detect),
            onClick = onAutoDetect,
            style = GymButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )

        for ((category, exercises) in ExerciseCatalog.byCategory()) {
            if (exercises.isEmpty()) continue
            LibrarySectionLabel(stringResource(category.labelRes))
            for (exercise in exercises) {
                CatalogRow(exercise = exercise, onTest = { onTest(exercise.id) })
            }
        }

        LibrarySectionLabel(stringResource(R.string.library_custom_section))
        if (customExercises.isEmpty()) {
            Text(
                stringResource(R.string.library_custom_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            for (custom in customExercises) {
                CustomRow(
                    name = custom.name,
                    onTest = { onTest(ExerciseRef.forCustom(custom.id)) },
                    onDelete = { onDeleteCustom(custom.id) },
                )
            }
        }
        GymButton(
            text = stringResource(R.string.library_add_custom),
            onClick = { showAdd = true },
            style = GymButtonStyle.Secondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )

        GymButton(
            text = stringResource(R.string.action_done),
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )
    }

    if (showAdd) {
        AddCustomDialog(
            onAdd = { name ->
                showAdd = false
                onAddCustom(name)
            },
            onDismiss = { showAdd = false },
        )
    }
}

/**
 * The row's action affordance: a play triangle + "Test" for AI-counted moves, or a
 * red record dot + "Rec" for manual ones — same size, weight and label styling.
 */
@Composable
private fun TestAction(isAiCounted: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Both actions share the accent tint and icon size so Test and Rec line up.
        Icon(
            imageVector = if (isAiCounted) Icons.Rounded.PlayArrow else Icons.Rounded.FiberManualRecord,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Text(
            stringResource(if (isAiCounted) R.string.library_test else R.string.library_manual_test),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun LibrarySectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
        letterSpacing = 2.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun CatalogRow(exercise: CatalogExercise, onTest: () -> Unit) {
    val ctx = LocalContext.current
    val muscles = exercise.muscles.joinToString(", ") { ctx.getString(it.labelRes) }
    // Every move is testable now: AI-counted moves run their live counter, the
    // rest run a manual session. AI moves are badged so the capability is clear.
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTest),
        colors = CardDefaults.cardColors(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(exercise.nameRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    muscles,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (exercise.isAiCounted) {
                    Text(
                        stringResource(R.string.library_ai_count),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            TestAction(isAiCounted = exercise.isAiCounted)
        }
    }
}

@Composable
private fun CustomRow(name: String, onTest: () -> Unit, onDelete: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = name,
            onConfirm = onDelete,
            onDismiss = { confirmDelete = false },
        )
    }
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onTest)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(R.string.library_manual_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TestAction(isAiCounted = false)
            IconButton(onClick = { confirmDelete = true }) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AddCustomDialog(onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.library_add_custom)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                label = { Text(stringResource(R.string.library_custom_hint)) },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(name) },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(R.string.action_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
