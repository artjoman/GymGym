package com.gymgym.app.ui

import android.app.Activity
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
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.res.stringResource
import com.gymgym.app.R
import com.gymgym.app.BuildConfig
import com.gymgym.app.settings.AccentTheme
import com.gymgym.app.settings.AppLocale
import com.gymgym.app.settings.BackgroundStyle
import com.gymgym.app.settings.FormSensitivity
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
    onFormSensitivity: (FormSensitivity) -> Unit,
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
        LanguageRow()
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(stringResource(R.string.settings_appearance), style = MaterialTheme.typography.headlineSmall)

        Text(stringResource(R.string.settings_color_scheme), style = MaterialTheme.typography.titleMedium)
        AccentRow(selected = settings.accentTheme, onSelect = onAccentTheme)

        Text(
            stringResource(R.string.settings_background),
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

        Text(stringResource(R.string.settings_sound), style = MaterialTheme.typography.headlineSmall)

        SwitchRow(
            label = stringResource(R.string.settings_all_sounds),
            checked = settings.soundsEnabled,
            enabled = true,
            onChange = onSoundsEnabled,
        )
        HorizontalDivider()

        SwitchRow(
            label = stringResource(R.string.settings_countdown_voice),
            checked = settings.countdownVoice,
            enabled = settings.soundsEnabled,
            onChange = onCountdownVoice,
        )
        SwitchRow(
            label = stringResource(R.string.settings_out_of_frame_bell),
            checked = settings.trackingLostBell,
            enabled = settings.soundsEnabled,
            onChange = onTrackingLostBell,
        )
        SwitchRow(
            label = stringResource(R.string.settings_back_in_frame_chime),
            checked = settings.trackingRegainedChime,
            enabled = settings.soundsEnabled,
            onChange = onTrackingRegainedChime,
        )
        SwitchRow(
            label = stringResource(R.string.settings_combo_callout),
            checked = settings.setCelebration,
            enabled = true,
            onChange = onSetCelebration,
        )
        HorizontalDivider()

        Text(stringResource(R.string.settings_spoken_rep_count), style = MaterialTheme.typography.titleMedium)
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
                Text(stringResource(mode.labelRes()))
            }
        }
        HorizontalDivider()

        Text(stringResource(R.string.settings_form_check), style = MaterialTheme.typography.titleMedium)
        SwitchRow(
            label = stringResource(R.string.settings_form_feedback),
            checked = settings.formFeedback,
            enabled = true,
            onChange = onFormFeedback,
        )
        SwitchRow(
            label = stringResource(R.string.settings_strict_counting),
            checked = settings.strictForm,
            enabled = settings.formFeedback,
            onChange = onStrictForm,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.settings_sensitivity), modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (level in FormSensitivity.entries) {
                    FilterChip(
                        selected = settings.formSensitivity == level,
                        enabled = settings.formFeedback,
                        onClick = { onFormSensitivity(level) },
                        label = { Text(stringResource(level.labelRes())) },
                    )
                }
            }
        }
        Text(
            stringResource(R.string.settings_form_desc),
            style = MaterialTheme.typography.bodySmall,
        )
        HorizontalDivider()

        Text(stringResource(R.string.settings_hands_free), style = MaterialTheme.typography.titleMedium)
        SwitchRow(
            label = stringResource(R.string.settings_voice_control),
            checked = settings.voiceControl,
            enabled = true,
            onChange = onVoiceControl,
        )
        Text(
            stringResource(R.string.settings_voice_desc),
            style = MaterialTheme.typography.bodySmall,
        )
        HorizontalDivider()

        Text(stringResource(R.string.settings_about), style = MaterialTheme.typography.titleMedium)
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
            Text(stringResource(R.string.settings_privacy_policy))
        }

        GymButton(stringResource(R.string.action_done), onBack, Modifier.padding(top = 16.dp))

        Text(
            stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
        CreatorFooter(Modifier.padding(top = 2.dp))
    }
}

private const val PRIVACY_POLICY_URL = "https://projectorum.com/gymgym-privacy-policy"

// --- Language ---

@Composable
private fun LanguageRow() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    // Re-read on each composition so the label reflects the current choice
    // (on API 33+ the framework recreates the activity when it changes).
    val currentTag = AppLocale.currentTag(context)
    val currentLabel = if (currentTag.isEmpty()) {
        stringResource(R.string.settings_language_system)
    } else {
        AppLocale.displayName(currentTag)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                currentLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
    }

    if (showDialog) {
        LanguageDialog(
            current = currentTag,
            onDismiss = { showDialog = false },
            onSelect = { tag ->
                showDialog = false
                if (tag != currentTag) {
                    AppLocale.setTag(context, tag)
                    // Pre-33 the framework won't recreate for us.
                    if (AppLocale.needsManualRestart()) {
                        (context as? Activity)?.recreate()
                    }
                }
            },
        )
    }
}

@Composable
private fun LanguageDialog(
    current: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    // "" (system default) first, then the shipped languages in their autonyms.
    val options = remember { listOf("") + AppLocale.SUPPORTED }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
        title = { Text(stringResource(R.string.settings_language)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                for (tag in options) {
                    val label = if (tag.isEmpty()) {
                        stringResource(R.string.settings_language_system)
                    } else {
                        AppLocale.displayName(tag)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = tag == current, onClick = { onSelect(tag) })
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = tag == current, onClick = { onSelect(tag) })
                        Text(label)
                    }
                }
            }
        },
    )
}

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
                        contentDescription = stringResource(theme.labelRes()),
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
                    label = stringResource(R.string.bg_upload),
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
                    label = stringResource(style.labelRes()),
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
