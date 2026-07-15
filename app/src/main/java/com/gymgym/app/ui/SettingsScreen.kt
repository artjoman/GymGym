package com.gymgym.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gymgym.app.settings.RepAnnouncementMode
import com.gymgym.app.settings.SoundSettings

@Composable
fun SettingsScreen(
    settings: SoundSettings,
    onSoundsEnabled: (Boolean) -> Unit,
    onCountdownVoice: (Boolean) -> Unit,
    onRepAnnouncement: (RepAnnouncementMode) -> Unit,
    onTrackingLostBell: (Boolean) -> Unit,
    onTrackingRegainedChime: (Boolean) -> Unit,
    onSetCelebration: (Boolean) -> Unit,
    onVoiceControl: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Sound settings", style = MaterialTheme.typography.headlineSmall)

        SwitchRow(
            label = "All sounds",
            checked = settings.soundsEnabled,
            enabled = true,
            onChange = onSoundsEnabled,
        )
        HorizontalDivider()

        SwitchRow(
            label = "Countdown voice (3…2…1…GO)",
            checked = settings.countdownVoice,
            enabled = settings.soundsEnabled,
            onChange = onCountdownVoice,
        )
        SwitchRow(
            label = "Out-of-frame bell",
            checked = settings.trackingLostBell,
            enabled = settings.soundsEnabled,
            onChange = onTrackingLostBell,
        )
        SwitchRow(
            label = "Back-in-frame chime",
            checked = settings.trackingRegainedChime,
            enabled = settings.soundsEnabled,
            onChange = onTrackingRegainedChime,
        )
        SwitchRow(
            label = "Set-complete combo callout",
            checked = settings.setCelebration,
            enabled = true,
            onChange = onSetCelebration,
        )
        HorizontalDivider()

        Text("Spoken rep count", style = MaterialTheme.typography.titleMedium)
        for (mode in RepAnnouncementMode.entries) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = settings.repAnnouncement == mode,
                        enabled = settings.soundsEnabled,
                        onClick = { onRepAnnouncement(mode) },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = settings.repAnnouncement == mode,
                    enabled = settings.soundsEnabled,
                    onClick = { onRepAnnouncement(mode) },
                )
                Text(mode.label)
            }
        }
        HorizontalDivider()

        Text("Hands-free control", style = MaterialTheme.typography.titleMedium)
        SwitchRow(
            label = "Voice control (beta)",
            checked = settings.voiceControl,
            enabled = true,
            onChange = onVoiceControl,
        )
        Text(
            "Say \"next\", \"pause\", \"resume\", or \"reset\" during a workout. " +
                "Uses on-device recognition and the microphone; needs mic permission.",
            style = MaterialTheme.typography.bodySmall,
        )
        HorizontalDivider()

        Text("About", style = MaterialTheme.typography.titleMedium)
        val context = LocalContext.current
        TextButton(
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)),
                    )
                }
            },
            contentPadding = PaddingValues(0.dp),
        ) {
            Text("Privacy policy")
        }

        GymButton("Done", onBack, Modifier.padding(top = 16.dp))
    }
}

private const val PRIVACY_POLICY_URL = "https://projectorum.com/gymgym-privacy-policy"

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, enabled = enabled, onCheckedChange = onChange)
    }
}
