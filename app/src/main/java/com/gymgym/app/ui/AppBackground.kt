package com.gymgym.app.ui

import android.content.Context
import android.graphics.BitmapFactory
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.gymgym.app.R
import com.gymgym.app.settings.BackgroundStyle
import com.gymgym.app.ui.theme.LocalBrand

/**
 * Full-screen home background: a chosen gym photo (or the user's own image) with
 * a subtle tilt parallax under a dark scrim that keeps text legible, or a plain
 * dark ground with a faint accent glow. Screen content is drawn on top.
 */
@Composable
fun AppBackground(
    style: BackgroundStyle,
    customPath: String?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val presetRes = when (style) {
        BackgroundStyle.GYM_EMERALD -> R.drawable.gym_bg
        BackgroundStyle.GYM_AZURE -> R.drawable.gym_bg_azure
        BackgroundStyle.GYM_VIOLET -> R.drawable.gym_bg_violet
        BackgroundStyle.GYM_AMBER -> R.drawable.gym_bg_amber
        else -> null
    }
    val custom = rememberCustomBitmap(style, customPath)
    val hasImage = presetRes != null || custom != null

    val maxShift = with(LocalDensity.current) { 22.dp.toPx() }
    val tilt = rememberTiltOffset(maxShift)
    val accent = LocalBrand.current.accent

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF07090C))) {
        val imageModifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                // 1.18x overscan gives room to shift without exposing edges.
                scaleX = 1.18f
                scaleY = 1.18f
                translationX = tilt.value.x
                translationY = tilt.value.y
            }
        when {
            custom != null -> Image(
                bitmap = custom,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = imageModifier,
            )
            presetRes != null -> Image(
                painter = painterResource(presetRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = imageModifier,
            )
        }

        if (hasImage) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0.0f to Color(0xCC07090C),
                        0.42f to Color(0x9907090C),
                        1.0f to Color(0xF207090C),
                    ),
                ),
            )
        } else {
            // No image: a faint accent glow from the top keeps it from feeling flat.
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0.0f to accent.copy(alpha = 0.12f),
                        0.5f to Color.Transparent,
                    ),
                ),
            )
        }
        content()
    }
}

/** Decode the user's picked image (downsampled) into an [ImageBitmap], or null. */
@Composable
private fun rememberCustomBitmap(style: BackgroundStyle, path: String?): ImageBitmap? =
    remember(style, path) {
        if (style != BackgroundStyle.CUSTOM || path.isNullOrBlank()) return@remember null
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            var sample = 1
            val largest = maxOf(bounds.outWidth, bounds.outHeight)
            while (largest / sample > 1440) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(path, opts)?.asImageBitmap()
        }.getOrNull()
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
