package com.gymgym.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.gymgym.app.R
import kotlinx.coroutines.delay

private val SQUAT_DOWN = listOf(
    R.drawable.demo_squat_01,
    R.drawable.demo_squat_02,
    R.drawable.demo_squat_03,
    R.drawable.demo_squat_04,
    R.drawable.demo_squat_05,
    R.drawable.demo_squat_06,
)

// Play the descent, then the same frames reversed for the way up (ping-pong).
// This guarantees the return trip faces the identical direction, and gives a
// full down-and-up rep in 10 steps.
private val SQUAT_FRAMES = SQUAT_DOWN + SQUAT_DOWN.subList(1, SQUAT_DOWN.size - 1).reversed()

/**
 * Compact arcade-panel card that loops Reppo's feet-anchored squat demo. The
 * frames share one canvas with the feet planted, so the loop no longer bobs.
 */
@Composable
fun ExerciseDemoBanner(modifier: Modifier = Modifier) {
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(FRAME_MS)
            frame = (frame + 1) % SQUAT_FRAMES.size
        }
    }
    Box(
        modifier = modifier
            .width(150.dp)
            .height(185.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(SQUAT_FRAMES[frame]),
            contentDescription = "Squat demo animation",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

// 10 frames × 380ms ≈ 3.8s for a full squat down-and-up, slow enough to follow.
private const val FRAME_MS = 380L
