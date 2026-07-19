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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gymgym.app.R

@Composable
fun ExpertSupportScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.expert_title), style = MaterialTheme.typography.headlineSmall)

        LevelCard(
            titleRes = R.string.expert_l1,
            features = listOf(
                R.string.expert_l1_f1, R.string.expert_l1_f2,
                R.string.expert_l1_f3, R.string.expert_l1_f4,
            ),
        )
        LevelCard(
            titleRes = R.string.expert_l2,
            features = listOf(
                R.string.expert_l2_f1, R.string.expert_l2_f2,
                R.string.expert_l2_f3, R.string.expert_l2_f4,
            ),
        )
        LevelCard(
            titleRes = R.string.expert_l3,
            features = listOf(
                R.string.expert_l3_f1, R.string.expert_l3_f2,
                R.string.expert_l3_f3, R.string.expert_l3_f4,
            ),
        )

        TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.action_back))
        }
    }
}

@Composable
private fun LevelCard(titleRes: Int, features: List<Int>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(stringResource(R.string.expert_coming_soon)) },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
            for (f in features) {
                Text(
                    "•  ${stringResource(f)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
