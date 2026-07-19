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
    homeCycles: com.gymgym.app.cycle.HomeCycles,
    customNames: Map<String, String>,
    onOpenMission: () -> Unit,
    onOpenLastCycle: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenPlans: () -> Unit,
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

            // LAST CYCLE — most recent completed cycle (tap → Statistics → Cycles).
            SectionLabel(stringResource(R.string.home_last_cycle), top = 20.dp)
            val lastCycle = homeCycles.lastCycle
            if (lastCycle != null) {
                CycleCard(
                    summary = lastCycle,
                    onClick = onOpenLastCycle,
                    customNames = customNames,
                    onStatistics = onOpenLastCycle,
                )
            } else {
                EmptyCycleCard(stringResource(R.string.home_no_cycles))
            }

            // CURRENT MISSION — the in-progress cycle (tap → Next Mission).
            SectionLabel(stringResource(R.string.home_current_mission), top = 20.dp)
            val currentCycle = homeCycles.currentCycle
            if (currentCycle != null) {
                CycleCard(
                    summary = currentCycle,
                    onClick = onOpenMission,
                    customNames = customNames,
                    onStatistics = onOpenLastCycle,
                )
            } else {
                EmptyCycleCard(stringResource(R.string.home_no_plan))
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
                text = stringResource(R.string.home_workout_plans),
                onClick = onOpenPlans,
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

/**
 * A cycle block: cycle name + overall % as the title, the plan name small in the
 * upper-right, and each workout in execution order below. Used for both LAST
 * CYCLE and CURRENT MISSION so they share layout and information hierarchy.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun CycleCard(
    summary: com.gymgym.app.cycle.CycleSummary,
    onClick: (() -> Unit)? = null,
    customNames: Map<String, String> = emptyMap(),
    onStatistics: (() -> Unit)? = null,
) {
    val cardModifier = Modifier.fillMaxWidth()
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
    androidx.compose.material3.Card(modifier = cardModifier) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.Top) {
                Text(
                    "${summary.cycleName} (${summary.percent}%)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    summary.planName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            if (summary.workouts.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for (w in summary.workouts) {
                        Text(
                            cycleWorkoutText(w),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (w.status == com.gymgym.app.cycle.CycleLineStatus.SKIPPED) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
            // Featured workout's exercises (planned for Current mission, results for
            // Last cycle) in the same format as History / Workout Details.
            summary.detail?.let { detail ->
                Spacer(Modifier.height(10.dp))
                Text(
                    detail.workoutName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                for (ex in detail.exercises) {
                    Text(
                        exerciseLineText(
                            name = exerciseRefName(ex.exerciseRef, customNames),
                            targetReps = ex.targetReps,
                            targetSets = ex.targetSets,
                            completedReps = ex.completedReps,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            if (onStatistics != null) {
                androidx.compose.material3.TextButton(
                    onClick = onStatistics,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(stringResource(R.string.statistics_title))
                }
            }
        }
    }
}

@Composable
internal fun cycleWorkoutText(w: com.gymgym.app.cycle.CycleWorkoutLine): String {
    val base = when (w.status) {
        com.gymgym.app.cycle.CycleLineStatus.DONE -> "${w.name} — ${w.percent}%"
        com.gymgym.app.cycle.CycleLineStatus.SKIPPED ->
            "${w.name} — ${stringResource(R.string.cycle_skipped)}"
        com.gymgym.app.cycle.CycleLineStatus.PENDING -> w.name
    }
    return w.weekday?.let { "$base · ${weekdayShort(it)}" } ?: base
}

@Composable
private fun EmptyCycleCard(message: String) {
    androidx.compose.material3.Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
