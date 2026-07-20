package com.gymgym.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.gymgym.app.R
import com.gymgym.app.data.BodyMeasurement
import com.gymgym.app.data.BodyMetric
import com.gymgym.app.profile.LengthUnit
import com.gymgym.app.profile.Profile
import com.gymgym.app.profile.ProfileRepository
import com.gymgym.app.profile.TrainingMode
import com.gymgym.app.profile.WeightUnit
import java.text.DateFormatSymbols
import java.util.Calendar

@Composable
fun ProfileScreen(
    profile: Profile,
    bodyMeasurements: List<BodyMeasurement>,
    onDisplayName: (String) -> Unit,
    onWeightUnit: (WeightUnit) -> Unit,
    onLengthUnit: (LengthUnit) -> Unit,
    onTrainingMode: (TrainingMode) -> Unit,
    onWorkoutDays: (Set<Int>) -> Unit,
    onWorkoutTimeoutSeconds: (Int) -> Unit,
    onSetTimeoutSeconds: (Int) -> Unit,
    onExerciseTimeoutSeconds: (Int) -> Unit,
    onLogMeasurement: (BodyMetric, Double, String) -> Unit,
    onExport: (Uri) -> Unit,
    onImport: (Uri) -> Unit,
    onBack: () -> Unit,
) {
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(onExport) }

    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) pendingImport = uri }

    // Two tabs, switchable by tap or horizontal swipe (as in Statistics).
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
            Text(
                stringResource(R.string.profile_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        val tabLabels = listOf(
            stringResource(R.string.profile_title),
            stringResource(R.string.recordings_title),
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
            modifier = Modifier.weight(1f).padding(top = 12.dp),
        ) { page ->
        if (page == 1) {
            RecordingsContent(modifier = Modifier.fillMaxSize())
            return@HorizontalPager
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

        var name by rememberSaveable { mutableStateOf(profile.displayName) }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; onDisplayName(it) },
            label = { Text(stringResource(R.string.profile_display_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        // --- Training mode ---
        HorizontalDivider()
        Text(stringResource(R.string.profile_training_mode), style = MaterialTheme.typography.titleMedium)
        // Each mode explains itself, but only the selected one shows its note.
        // Switching modes never touches the per-workout weekdays stored in plans,
        // so they're preserved and restored when Weekly Schedule is re-enabled.
        for (mode in TrainingMode.entries) {
            val selected = profile.trainingMode == mode
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().selectable(
                        selected = selected,
                        onClick = { onTrainingMode(mode) },
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = selected, onClick = { onTrainingMode(mode) })
                    Text(stringResource(mode.labelRes()))
                }
                if (selected) {
                    Text(
                        stringResource(
                            if (mode == TrainingMode.SMART_CYCLE) {
                                R.string.profile_mode_smart_desc
                            } else {
                                R.string.profile_mode_weekly_desc
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 48.dp, bottom = 4.dp),
                    )
                }
            }
        }

        // --- Recovery ---
        HorizontalDivider()
        Text(stringResource(R.string.profile_recovery), style = MaterialTheme.typography.titleMedium)
        WorkoutRecoveryRow(
            label = stringResource(R.string.profile_workout_timeout),
            seconds = profile.workoutTimeoutSeconds,
            onSetSeconds = onWorkoutTimeoutSeconds,
            presetsHours = listOf(
                stringResource(R.string.recovery_beginner) to 72,
                stringResource(R.string.recovery_intermediate) to 48,
                stringResource(R.string.recovery_advanced) to 24,
            ),
        )
        RecoveryRow(
            label = stringResource(R.string.profile_set_timeout),
            valueText = stringResource(R.string.profile_seconds, profile.setTimeoutSeconds),
            presets = listOf(
                stringResource(R.string.recovery_beginner) to 300,
                stringResource(R.string.recovery_intermediate) to 180,
                stringResource(R.string.recovery_advanced) to 120,
            ),
            current = profile.setTimeoutSeconds,
            step = 10,
            onSet = onSetTimeoutSeconds,
        )
        RecoveryRow(
            label = stringResource(R.string.profile_exercise_timeout),
            valueText = stringResource(R.string.profile_seconds, profile.exerciseTimeoutSeconds),
            presets = listOf(
                stringResource(R.string.recovery_beginner) to 300,
                stringResource(R.string.recovery_intermediate) to 180,
                stringResource(R.string.recovery_advanced) to 120,
            ),
            current = profile.exerciseTimeoutSeconds,
            step = 10,
            onSet = onExerciseTimeoutSeconds,
        )

        // --- Units ---
        HorizontalDivider()
        Text(stringResource(R.string.profile_weight_unit), style = MaterialTheme.typography.titleMedium)
        for (unit in WeightUnit.entries) {
            UnitRadio(unit == profile.weightUnit, stringResource(unit.labelRes())) { onWeightUnit(unit) }
        }
        Text(stringResource(R.string.profile_length_unit), style = MaterialTheme.typography.titleMedium)
        for (unit in LengthUnit.entries) {
            UnitRadio(unit == profile.lengthUnit, stringResource(unit.labelRes())) { onLengthUnit(unit) }
        }

        // --- Body parameters ---
        HorizontalDivider()
        Text(stringResource(R.string.profile_body_params), style = MaterialTheme.typography.titleMedium)
        val weightCode = if (profile.weightUnit == WeightUnit.KG) "kg" else "lb"
        val lengthCode = if (profile.lengthUnit == LengthUnit.CM) "cm" else "in"
        for (metric in BodyMetric.entries) {
            val unitCode = if (metric == BodyMetric.WEIGHT) weightCode else lengthCode
            val last = bodyMeasurements.firstOrNull { it.type == metric.name }
            BodyMetricRow(
                label = stringResource(metric.labelRes()),
                unitCode = unitCode,
                last = last,
                onSave = { value -> onLogMeasurement(metric, value, unitCode) },
            )
        }

        // --- Backup ---
        HorizontalDivider()
        Text(stringResource(R.string.profile_backup_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.profile_backup_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GymButton(
                text = stringResource(R.string.action_export),
                onClick = { exportLauncher.launch("gymgym-backup.json") },
                modifier = Modifier.weight(1f),
                style = GymButtonStyle.Secondary,
            )
            GymButton(
                text = stringResource(R.string.action_import),
                onClick = {
                    importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/*"))
                },
                modifier = Modifier.weight(1f),
                style = GymButtonStyle.Secondary,
            )
        }

        }
        }
    }

    val importUri = pendingImport
    if (importUri != null) {
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(stringResource(R.string.profile_import_title)) },
            text = { Text(stringResource(R.string.profile_import_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    onImport(importUri)
                    pendingImport = null
                }) { Text(stringResource(R.string.action_replace)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun UnitRadio(selected: Boolean, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeekdayPicker(selected: Set<Int>, onToggle: (Int) -> Unit) {
    val symbols = remember { DateFormatSymbols.getInstance().shortWeekdays }
    // Our encoding: 1=Mon..7=Sun. Map to Calendar day for the localized label.
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (day in 1..7) {
            val calDay = if (day == 7) Calendar.SUNDAY else day + 1
            FilterChip(
                selected = day in selected,
                onClick = { onToggle(day) },
                label = { Text(symbols[calDay]) },
            )
        }
    }
}

@Composable
private fun RecoveryRow(
    label: String,
    valueText: String,
    presets: List<Pair<String, Int>>,
    current: Int,
    step: Int,
    onSet: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { onSet(current - step) }) { Text("−") }
                Text(
                    valueText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                OutlinedButton(onClick = { onSet(current + step) }) { Text("+") }
            }
        }
        if (presets.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for ((name, value) in presets) {
                    FilterChip(
                        selected = current == value,
                        onClick = { onSet(value) },
                        label = { Text(name) },
                    )
                }
            }
        }
    }
}

/**
 * Between-workouts recovery: chosen in whole hours (8-hour step, 8–168h) but
 * stored in seconds. The value is normalized for display so a stray stored value
 * (e.g. an old 172690 s) still shows a clean hour figure, and ± move by 8h.
 */
@Composable
private fun WorkoutRecoveryRow(
    label: String,
    seconds: Int,
    onSetSeconds: (Int) -> Unit,
    presetsHours: List<Pair<String, Int>>,
) {
    val hours = ProfileRepository.workoutTimeoutHours(seconds)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = {
                    val next = (hours - ProfileRepository.WORKOUT_HOUR_STEP)
                        .coerceAtLeast(ProfileRepository.WORKOUT_MIN_HOURS)
                    onSetSeconds(next * 3600)
                }) { Text("−") }
                Text(
                    stringResource(R.string.profile_hours, hours),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                OutlinedButton(onClick = {
                    val next = (hours + ProfileRepository.WORKOUT_HOUR_STEP)
                        .coerceAtMost(ProfileRepository.WORKOUT_MAX_HOURS)
                    onSetSeconds(next * 3600)
                }) { Text("+") }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for ((name, presetHours) in presetsHours) {
                FilterChip(
                    selected = hours == presetHours,
                    onClick = { onSetSeconds(presetHours * 3600) },
                    label = { Text(name) },
                )
            }
        }
    }
}

@Composable
private fun BodyMetricRow(
    label: String,
    unitCode: String,
    last: BodyMeasurement?,
    onSave: (Double) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (last != null) {
                Text(
                    stringResource(R.string.profile_last_value, formatMeasure(last.value), last.unit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OutlinedTextField(
            value = input,
            onValueChange = { input = it.filter { c -> c.isDigit() || c == '.' } },
            singleLine = true,
            suffix = { Text(unitCode) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(120.dp),
        )
        TextButton(
            onClick = {
                input.toDoubleOrNull()?.let { onSave(it); input = "" }
            },
            enabled = input.toDoubleOrNull() != null,
        ) { Text(stringResource(R.string.action_save)) }
    }
}

private fun formatMeasure(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else "%.1f".format(v)
