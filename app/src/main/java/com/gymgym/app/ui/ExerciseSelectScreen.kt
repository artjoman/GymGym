package com.gymgym.app.ui

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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymgym.app.settings.BackgroundStyle

@Composable
fun ExerciseSelectScreen(
    greeting: String?,
    backgroundStyle: BackgroundStyle,
    customBackgroundPath: String?,
    onExerciseSelected: (Exercise) -> Unit,
    onAutoDetect: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenRecordings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    AppBackground(style = backgroundStyle, customPath = customBackgroundPath) {
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
                text = if (greeting.isNullOrBlank()) "Ready to train?" else "Let's go, $greeting",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionLabel("Choose your exercise", top = 20.dp)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (exercise in Exercise.entries) {
                    GymButton(
                        text = exercise.displayName,
                        onClick = { onExerciseSelected(exercise) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                GymButton(
                    text = "✨ Auto-detect",
                    onClick = onAutoDetect,
                    style = GymButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionLabel("Train smarter", top = 26.dp)
            GymButton(
                text = "Workout plans",
                onClick = onOpenPlans,
                style = GymButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            GymButton(
                text = "📹 Recordings",
                onClick = onOpenRecordings,
                style = GymButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GymButton("History", onOpenHistory, Modifier.weight(1f), GymButtonStyle.Secondary)
                GymButton("Stats", onOpenStats, Modifier.weight(1f), GymButtonStyle.Secondary)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GymButton("Profile", onOpenProfile, Modifier.weight(1f), GymButtonStyle.Secondary)
                GymButton("Settings", onOpenSettings, Modifier.weight(1f), GymButtonStyle.Secondary)
            }
            Spacer(Modifier.height(28.dp))
            CreatorFooter()
            Spacer(Modifier.height(16.dp))
        }
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
