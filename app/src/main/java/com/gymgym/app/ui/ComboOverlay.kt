package com.gymgym.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

/**
 * Arcade-style combo callout: a chunky word that pops in with an overshoot and
 * a jaunty tilt, drawn in bright yellow with a black outline. [word] changing
 * restarts the animation.
 */
@Composable
fun ComboOverlay(word: String, modifier: Modifier = Modifier) {
    var t by remember(word) { mutableFloatStateOf(0f) }
    LaunchedEffect(word) {
        var start = 0L
        while (t < 1f) {
            withFrameNanos { now ->
                if (start == 0L) start = now
                t = ((now - start) / POP_DURATION_NANOS).coerceAtMost(1f)
            }
        }
    }

    // Overshoot: shoot past 1.0 then settle back.
    val scale = when {
        t < 0.6f -> 0.3f + (1.25f - 0.3f) * (t / 0.6f)
        else -> 1.25f - 0.25f * ((t - 0.6f) / 0.4f)
    }
    val tilt = -8f + 8f * sin(t * Math.PI.toFloat())

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = tilt
                alpha = (t * 3f).coerceAtMost(1f)
            },
            contentAlignment = Alignment.Center,
        ) {
            // Black outline: same text stamped at 8 offsets behind the fill.
            for (dx in -1..1) {
                for (dy in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    ComboText(word, Color.Black, Modifier.graphicsLayer {
                        translationX = dx * OUTLINE_PX
                        translationY = dy * OUTLINE_PX
                    })
                }
            }
            ComboText(word, Color(0xFFFFD400), Modifier)
        }
    }
}

@Composable
private fun ComboText(word: String, color: Color, modifier: Modifier) {
    Text(
        text = word,
        modifier = modifier,
        color = color,
        fontSize = 64.sp,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center,
    )
}

private const val POP_DURATION_NANOS = 320_000_000f
private const val OUTLINE_PX = 5f
