package com.gymgym.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymgym.app.profile.Profile
import androidx.compose.ui.res.stringResource
import com.gymgym.app.R
import com.gymgym.app.profile.WeightUnit

@Composable
fun ProfileScreen(
    profile: Profile,
    onDisplayName: (String) -> Unit,
    onWeightUnit: (WeightUnit) -> Unit,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.headlineSmall)

        // Local edit state so keystrokes are immediate; persistence to DataStore
        // happens in the background. Binding the field straight to the async
        // DataStore value bounced the cursor to the start after every keystroke.
        var name by rememberSaveable { mutableStateOf(profile.displayName) }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; onDisplayName(it) },
            label = { Text(stringResource(R.string.profile_display_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(stringResource(R.string.profile_weight_unit), style = MaterialTheme.typography.titleMedium)
        for (unit in WeightUnit.entries) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = profile.weightUnit == unit,
                        onClick = { onWeightUnit(unit) },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = profile.weightUnit == unit,
                    onClick = { onWeightUnit(unit) },
                )
                Text(stringResource(unit.labelRes()))
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

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
                    importLauncher.launch(
                        arrayOf("application/json", "application/octet-stream", "text/*"),
                    )
                },
                modifier = Modifier.weight(1f),
                style = GymButtonStyle.Secondary,
            )
        }

        GymButton(stringResource(R.string.action_done), onBack, Modifier.padding(top = 16.dp))
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
