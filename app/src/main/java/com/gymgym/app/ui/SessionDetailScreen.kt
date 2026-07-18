package com.gymgym.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gymgym.app.R
import com.gymgym.app.data.WorkoutSession

@Composable
fun SessionDetailScreen(session: WorkoutSession?, onBack: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.session_title), style = MaterialTheme.typography.headlineSmall)

        if (session == null) {
            Text(stringResource(R.string.session_unavailable))
            GymButton(stringResource(R.string.action_back), onBack, style = GymButtonStyle.Secondary)
            return@Column
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    exerciseLabel(context, session.exerciseType).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                val timed = isTimedExercise(session.exerciseType)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        if (timed) formatDurationLong(context, session.durationMs) else session.repCount.toString(),
                        style = MaterialTheme.typography.displayLarge,
                    )
                    if (!timed) {
                        Text(
                            stringResource(R.string.session_reps_suffix),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                DetailRow(stringResource(R.string.session_started), formatDateTime(session.startedAt))
                DetailRow(stringResource(R.string.session_ended), formatDateTime(session.startedAt + session.durationMs))
                DetailRow(stringResource(R.string.session_duration), formatDurationLong(context, session.durationMs))
                if (!timed) {
                    DetailRow(stringResource(R.string.session_pace), formatPace(context, session.repCount, session.durationMs))
                    val pct = if (session.repCount > 0) session.goodReps * 100 / session.repCount else 0
                    DetailRow(
                        stringResource(R.string.session_good_form),
                        stringResource(R.string.session_good_form_value, session.goodReps, session.repCount, pct),
                    )
                }
            }
        }

        GymButton(stringResource(R.string.action_back), onBack, style = GymButtonStyle.Secondary)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
        )
    }
}
