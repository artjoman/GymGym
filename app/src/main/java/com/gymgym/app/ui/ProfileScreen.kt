package com.gymgym.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymgym.app.profile.Profile
import com.gymgym.app.profile.WeightUnit

@Composable
fun ProfileScreen(
    profile: Profile,
    onDisplayName: (String) -> Unit,
    onWeightUnit: (WeightUnit) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = profile.displayName,
            onValueChange = onDisplayName,
            label = { Text("Display name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Weight unit", style = MaterialTheme.typography.titleMedium)
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
                Text(unit.label)
            }
        }

        Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) { Text("Done") }
    }
}
