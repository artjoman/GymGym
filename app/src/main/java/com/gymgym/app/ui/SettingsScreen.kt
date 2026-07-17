package com.gymgym.app.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymgym.app.R
import com.gymgym.app.settings.AccentTheme
import com.gymgym.app.settings.BackgroundStyle
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
    onFormFeedback: (Boolean) -> Unit,
    onStrictForm: (Boolean) -> Unit,
    onAccentTheme: (AccentTheme) -> Unit,
    onBackgroundStyle: (BackgroundStyle) -> Unit,
    onCustomBackground: (Uri) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Appearance", style = MaterialTheme.typography.headlineSmall)

        Text("Color scheme", style = MaterialTheme.typography.titleMedium)
        AccentRow(selected = settings.accentTheme, onSelect = onAccentTheme)

        Text(
            "Background",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        BackgroundRow(
            selected = settings.backgroundStyle,
            customPath = settings.customBackgroundPath,
            onSelect = onBackgroundStyle,
            onPick = onCustomBackground,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

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

        Text("Form check", style = MaterialTheme.typography.titleMedium)
        SwitchRow(
            label = "Form feedback",
            checked = settings.formFeedback,
            enabled = true,
            onChange = onFormFeedback,
        )
        SwitchRow(
            label = "Strict counting (only good reps)",
            checked = settings.strictForm,
            enabled = settings.formFeedback,
            onChange = onStrictForm,
        )
        Text(
            "Flags shallow or bounced reps with a cue. Strict counting makes a bad " +
                "rep not count toward the set.",
            style = MaterialTheme.typography.bodySmall,
        )
        HorizontalDivider()

        Text("Hands-free control", style = MaterialTheme.typography.titleMedium)
        SwitchRow(
            label = "Voice control (beta)",
            checked = settings.voiceControl,
            enabled = true,
            onChange = onVoiceControl,
        )
        Text(
            "Say \"next\", \"pause\", \"resume\", or \"reset\" during a workout; " +
                "\"start\"/\"stop\" for a plank timer; \"start/stop recording\"; or " +
                "\"switch camera\". " +
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

        CreatorFooter(Modifier.padding(top = 16.dp))
    }
}

private const val PRIVACY_POLICY_URL = "https://projectorum.com/gymgym-privacy-policy"

// --- Appearance pickers ---

@Composable
private fun AccentRow(selected: AccentTheme, onSelect: (AccentTheme) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        for (theme in AccentTheme.entries) {
            val isSel = theme == selected
            val color = Color(theme.accent)
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSel) 3.dp else 1.dp,
                        color = if (isSel) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            Color.White.copy(alpha = 0.25f)
                        },
                        shape = CircleShape,
                    )
                    .clickable { onSelect(theme) },
                contentAlignment = Alignment.Center,
            ) {
                if (isSel) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = theme.label,
                        tint = if (color.luminance() > 0.45f) Color(0xFF08130C) else Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

private fun previewRes(style: BackgroundStyle): Int? = when (style) {
    BackgroundStyle.GYM_EMERALD -> R.drawable.gym_bg
    BackgroundStyle.GYM_AZURE -> R.drawable.gym_bg_azure
    BackgroundStyle.GYM_VIOLET -> R.drawable.gym_bg_violet
    BackgroundStyle.GYM_AMBER -> R.drawable.gym_bg_amber
    else -> null
}

@Composable
private fun BackgroundRow(
    selected: BackgroundStyle,
    customPath: String?,
    onSelect: (BackgroundStyle) -> Unit,
    onPick: (Uri) -> Unit,
) {
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(onPick) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        for (style in BackgroundStyle.entries) {
            when (style) {
                BackgroundStyle.CUSTOM -> BgTile(
                    label = "Upload",
                    selected = selected == BackgroundStyle.CUSTOM,
                    customPath = customPath,
                    isUpload = true,
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                )
                else -> BgTile(
                    label = style.label,
                    selected = selected == style,
                    previewRes = previewRes(style),
                    onClick = { onSelect(style) },
                )
            }
        }
    }
}

@Composable
private fun BgTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    previewRes: Int? = null,
    customPath: String? = null,
    isUpload: Boolean = false,
) {
    val shape = RoundedCornerShape(12.dp)
    val ring = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.18f)
    val thumb = if (isUpload) rememberThumb(customPath) else null
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(104.dp)
                .clip(shape)
                .background(Color(0xFF10151B))
                .border(if (selected) 3.dp else 1.dp, ring, shape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            when {
                previewRes != null -> Image(
                    painter = painterResource(previewRes),
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                thumb != null -> Image(
                    bitmap = thumb,
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                isUpload -> Icon(
                    Icons.Rounded.Add,
                    contentDescription = "Upload image",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
                // NONE: plain dark tile, nothing to draw.
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp).width(72.dp),
        )
    }
}

/** Small decoded thumbnail of the user's custom background, or null. */
@Composable
private fun rememberThumb(path: String?): ImageBitmap? = remember(path) {
    if (path.isNullOrBlank()) return@remember null
    runCatching {
        val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
        BitmapFactory.decodeFile(path, opts)?.asImageBitmap()
    }.getOrNull()
}

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
