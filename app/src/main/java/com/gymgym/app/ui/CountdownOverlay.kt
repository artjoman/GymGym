package com.gymgym.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.sp

/**
 * Full-screen countdown overlay; [value] of 0 renders as "GO!".
 *
 * The pop-in is animated by hand with withFrameNanos so we don't need the
 * compose animation artifact for a single effect.
 */
@Composable
fun CountdownOverlay(value: Int, modifier: Modifier = Modifier) {
    var progress by remember(value) { mutableFloatStateOf(0f) }
    LaunchedEffect(value) {
        var startNanos = 0L
        while (progress < 1f) {
            withFrameNanos { now ->
                if (startNanos == 0L) startNanos = now
                progress = ((now - startNanos) / POP_DURATION_NANOS).coerceAtMost(1f)
            }
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x66000000)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (value == 0) "GO!" else value.toString(),
            fontSize = 120.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.graphicsLayer {
                val scale = 1.6f - 0.6f * progress
                scaleX = scale
                scaleY = scale
                alpha = 0.3f + 0.7f * progress
            },
        )
    }
}

private const val POP_DURATION_NANOS = 350_000_000f
