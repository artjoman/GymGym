package com.gymgym.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.gymgym.app.R
import com.gymgym.app.recording.RecordingStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val recordingDateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

@Composable
fun RecordingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var files by remember { mutableStateOf(RecordingStore.list(context)) }

    Column(modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp)) {
        Text(stringResource(R.string.recordings_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.recordings_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        if (files.isEmpty()) {
            Text(
                stringResource(R.string.recordings_empty),
                modifier = Modifier.padding(vertical = 24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(files, key = { it.absolutePath }) { file ->
                    RecordingRow(
                        file = file,
                        onShare = { RecordingStore.share(context, file) },
                        onPlay = { RecordingStore.play(context, file) },
                        onDelete = {
                            RecordingStore.delete(file)
                            files = RecordingStore.list(context)
                        },
                    )
                }
            }
        }

        GymButton(stringResource(R.string.action_back), onBack, Modifier.padding(top = 16.dp), GymButtonStyle.Secondary)
    }
}

@Composable
private fun RecordingRow(
    file: File,
    onShare: () -> Unit,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = stringResource(R.string.confirm_delete_message),
            onConfirm = onDelete,
            onDismiss = { confirmDelete = false },
        )
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                recordingDateFormat.format(Date(file.lastModified())),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "%.1f MB".format(file.length() / 1_048_576.0),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GymButton(stringResource(R.string.action_share), onShare)
                TextButton(onClick = onPlay) { Text(stringResource(R.string.action_play)) }
                TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.action_delete)) }
            }
        }
    }
}
