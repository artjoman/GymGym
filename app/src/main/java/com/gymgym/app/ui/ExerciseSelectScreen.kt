package com.gymgym.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gymgym.app.R
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExerciseSelectScreen(
    greeting: String?,
    dashboard: com.gymgym.app.cycle.DashboardState,
    onOpenMission: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenPrograms: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenRecordings: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenExpert: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
            // Wordmark
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) { append("GYM") }
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("GYM") }
                },
                style = MaterialTheme.typography.displayLarge,
                fontSize = 44.sp,
            )
            Text(
                text = if (greeting.isNullOrBlank()) {
                    stringResource(R.string.home_greeting_default)
                } else {
                    stringResource(R.string.home_greeting, greeting)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val mission = dashboard.nextMission
            if (dashboard.hasActivePlan && mission != null) {
                SectionLabel(stringResource(R.string.home_next_mission), top = 20.dp)
                androidx.compose.material3.Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenMission),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            dashboard.planName.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            mission.workout.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(10.dp))
                        CycleProgressBar(dashboard)
                    }
                }
            }

            dashboard.lastWorkout?.let { last ->
                SectionLabel(stringResource(R.string.home_last_workout), top = 20.dp)
                val context = androidx.compose.ui.platform.LocalContext.current
                androidx.compose.material3.Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenStatistics),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(
                                last.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${last.avgPercent}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(
                                R.string.history_meta,
                                formatDate(last.startedAt),
                                formatDuration(context, last.durationMs),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            SectionLabel(stringResource(R.string.home_train_smarter), top = 26.dp)
            GymButton(
                text = stringResource(R.string.home_exercise_library),
                onClick = onOpenLibrary,
                style = GymButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            GymButton(
                text = stringResource(R.string.home_programs),
                onClick = onOpenPrograms,
                style = GymButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            GymButton(
                text = stringResource(R.string.home_workout_plans),
                onClick = onOpenPlans,
                style = GymButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            GymButton(
                text = stringResource(R.string.home_recordings),
                onClick = onOpenRecordings,
                style = GymButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            GymButton(
                text = stringResource(R.string.statistics_title),
                onClick = onOpenStatistics,
                style = GymButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GymButton(stringResource(R.string.nav_profile), onOpenProfile, Modifier.weight(1f), GymButtonStyle.Secondary)
                GymButton(stringResource(R.string.nav_settings), onOpenSettings, Modifier.weight(1f), GymButtonStyle.Secondary)
            }
            Spacer(Modifier.height(12.dp))
            GymButton(
                text = stringResource(R.string.home_expert),
                onClick = onOpenExpert,
                style = GymButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(28.dp))
            CreatorFooter()
            Spacer(Modifier.height(16.dp))
        }
}

@Composable
private fun SectionLabel(text: String, top: androidx.compose.ui.unit.Dp) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
        letterSpacing = 2.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = top, bottom = 12.dp),
    )
}
