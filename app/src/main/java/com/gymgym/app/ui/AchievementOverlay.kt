package com.gymgym.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymgym.app.R
import com.gymgym.app.achievement.AchievementDef
import kotlin.math.sin

private const val POP_NANOS = 320_000_000f
private const val OUTLINE_PX = 3f

/**
 * Unlock celebration, in the same arcade treatment as [ComboOverlay] (yellow fill,
 * stamped black outline, overshoot pop) so a badge and a combo callout read as
 * one system. Hosted app-wide rather than inside a screen, because achievements
 * can land outside a workout — logging a body measurement, for instance.
 */
@Composable
fun AchievementOverlay(
    achievement: AchievementDef,
    modifier: Modifier = Modifier,
) {
    var t by remember(achievement.id) { mutableFloatStateOf(0f) }
    LaunchedEffect(achievement.id) {
        var start = 0L
        while (t < 1f) {
            withFrameNanos { now ->
                if (start == 0L) start = now
                t = ((now - start) / POP_NANOS).coerceAtMost(1f)
            }
        }
    }
    val scale = when {
        t < 0.6f -> 0.3f + (1.25f - 0.3f) * (t / 0.6f)
        else -> 1.25f - 0.25f * ((t - 0.6f) / 0.4f)
    }
    val tilt = -8f + 8f * sin(t * Math.PI.toFloat())

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xB3000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(32.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = tilt
                    alpha = (t * 3f).coerceAtMost(1f)
                },
        ) {
            AchievementBadge(earned = true, size = 120.dp)
            OutlinedArcadeText(stringResource(R.string.achievement_unlocked), 30.sp)
            OutlinedArcadeText(stringResource(achievement.nameRes), 22.sp)
            Text(
                stringResource(achievement.descRes),
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Arcade lettering: the same text stamped at 8 offsets behind a yellow fill. */
@Composable
private fun OutlinedArcadeText(text: String, size: androidx.compose.ui.unit.TextUnit) {
    Box(contentAlignment = Alignment.Center) {
        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx == 0 && dy == 0) continue
                ArcadeText(text, size, Color.Black, Modifier.graphicsLayer {
                    translationX = dx * OUTLINE_PX
                    translationY = dy * OUTLINE_PX
                })
            }
        }
        // Matches ComboOverlay's arcade yellow so callouts and badges agree.
        ArcadeText(text, size, Color(0xFFFFD400), Modifier)
    }
}

@Composable
private fun ArcadeText(
    text: String,
    size: androidx.compose.ui.unit.TextUnit,
    color: Color,
    modifier: Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineMedium,
    )
}
