package com.gymgym.app.ui

import androidx.activity.compose.BackHandler
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gymgym.app.audio.SoundEffects
import com.gymgym.app.audio.VoiceFeedback
import com.gymgym.app.camera.CameraController
import com.gymgym.app.camera.PoseAnalyzer

@Composable
fun CameraScreen(
    exercise: Exercise,
    viewModel: MainViewModel,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val repCount by viewModel.repCount.collectAsState()
    val latestPose by viewModel.latestPose.collectAsState()
    val countdownValue by viewModel.countdownValue.collectAsState()
    val isTracking by viewModel.isTracking.collectAsState()
    val settings by viewModel.soundSettings.collectAsState()

    val previewView = remember { PreviewView(context) }
    val cameraController = remember { CameraController(context) }
    val voice = remember { VoiceFeedback(context) }
    val sounds = remember { SoundEffects(context) }

    // System back should log + tear down the session, same as "Change exercise".
    BackHandler(onBack = onExit)

    DisposableEffect(lifecycleOwner) {
        val analyzer = PoseAnalyzer(onPose = viewModel::onPose)
        cameraController.start(lifecycleOwner, previewView, analyzer)
        onDispose {
            cameraController.stop()
            analyzer.close()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            voice.shutdown()
            sounds.release()
        }
    }

    LaunchedEffect(countdownValue) {
        if (!settings.soundsEnabled || !settings.countdownVoice) return@LaunchedEffect
        when (val value = countdownValue) {
            null -> {}
            0 -> voice.speak("Go!")
            else -> voice.speak(value.toString())
        }
    }
    LaunchedEffect(repCount) {
        val step = settings.repAnnouncement.step
        if (settings.soundsEnabled && step > 0 && repCount > 0 && repCount % step == 0) {
            voice.speak(repCount.toString())
        }
    }

    // Bell on losing tracking, chime on regaining it — but only after the
    // person has been seen once, so the screen doesn't ring while setting up.
    var hasTracked by remember { mutableStateOf(false) }
    LaunchedEffect(isTracking) {
        if (isTracking) {
            if (hasTracked && settings.soundsEnabled && settings.trackingRegainedChime) {
                sounds.playTrackingRegained()
            }
            hasTracked = true
        } else if (hasTracked && settings.soundsEnabled && settings.trackingLostBell) {
            sounds.playTrackingLost()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        PoseOverlay(pose = latestPose, isTracking = isTracking, modifier = Modifier.fillMaxSize())

        Text(
            text = repCount.toString(),
            fontSize = 72.sp,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
        )
        Text(
            text = exercise.displayName,
            fontSize = 20.sp,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        )

        if (countdownValue == null && !isTracking) {
            Text(
                text = "Move into frame",
                fontSize = 18.sp,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 140.dp)
                    .background(Color(0xCCB3261E), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
        ) {
            Button(onClick = { viewModel.resetSession() }) { Text("Reset") }
            Button(onClick = onExit) { Text("Change exercise") }
        }

        countdownValue?.let { CountdownOverlay(value = it) }
    }
}
