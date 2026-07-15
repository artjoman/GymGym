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
        Text("Recordings", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Share a workout with your trainer for feedback.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        if (files.isEmpty()) {
            Text(
                "No recordings yet. Tap ● REC on the workout screen to record.",
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

        GymButton("Back", onBack, Modifier.padding(top = 16.dp), GymButtonStyle.Secondary)
    }
}

@Composable
private fun RecordingRow(
    file: File,
    onShare: () -> Unit,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
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
                GymButton("Share", onShare)
                TextButton(onClick = onPlay) { Text("Play") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}
