package com.gymgym.app.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.gymgym.app.R

/**
 * Full-screen gym-photo background with a subtle tilt parallax (the image
 * drifts as the phone is tilted) under a dark gradient scrim that keeps text
 * legible. Screen content is drawn on top.
 */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val maxShift = with(LocalDensity.current) { 22.dp.toPx() }
    val tilt = rememberTiltOffset(maxShift)

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF07090C))) {
        Image(
            painter = painterResource(R.drawable.gym_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // 1.18x overscan gives room to shift without exposing edges.
                    scaleX = 1.18f
                    scaleY = 1.18f
                    translationX = tilt.value.x
                    translationY = tilt.value.y
                },
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.0f to Color(0xCC07090C),
                    0.42f to Color(0x9907090C),
                    1.0f to Color(0xF207090C),
                ),
            ),
        )
        content()
    }
}

/** Accelerometer-driven, smoothed, baseline-relative offset for the parallax. */
@Composable
private fun rememberTiltOffset(maxPx: Float): State<Offset> {
    val context = LocalContext.current
    val offset = remember { mutableStateOf(Offset.Zero) }
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            var baseX: Float? = null
            var baseY: Float? = null
            override fun onSensorChanged(e: SensorEvent) {
                val x = e.values[0]
                val y = e.values[1]
                if (baseX == null) { baseX = x; baseY = y }
                // Deviation from how the user is holding the phone, gently scaled.
                val tx = ((x - baseX!!) / 5f).coerceIn(-1f, 1f)
                val ty = ((y - baseY!!) / 5f).coerceIn(-1f, 1f)
                val cur = offset.value
                val targetX = -tx * maxPx
                val targetY = ty * maxPx * 0.6f
                // Low-pass filter so movement is smooth, not jittery.
                offset.value = Offset(
                    cur.x + (targetX - cur.x) * 0.12f,
                    cur.y + (targetY - cur.y) * 0.12f,
                )
            }
            override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}
        }
        if (sensor != null) {
            sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sm?.unregisterListener(listener) }
    }
    return offset
}
