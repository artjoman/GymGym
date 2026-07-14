package com.gymgym.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gymgym.app.audio.SoundEffects
import com.gymgym.app.audio.VoiceCommandListener
import com.gymgym.app.camera.CameraController
import com.gymgym.app.camera.PoseAnalyzer
import com.gymgym.app.camera.VideoRecorder
import com.gymgym.app.recording.RecordingStore
import com.gymgym.app.settings.CameraFacing
import com.gymgym.app.ui.theme.BrandGreen

@Composable
fun CameraScreen(
    exercise: Exercise?,
    viewModel: MainViewModel,
    onExit: () -> Unit,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val repCount by viewModel.repCount.collectAsState()
    val latestPose by viewModel.latestPose.collectAsState()
    val countdownValue by viewModel.countdownValue.collectAsState()
    val isTracking by viewModel.isTracking.collectAsState()
    val settings by viewModel.soundSettings.collectAsState()
    val planProgress by viewModel.planProgress.collectAsState()
    val planComplete by viewModel.planComplete.collectAsState()
    val requestExit by viewModel.requestExit.collectAsState()
    val celebration by viewModel.celebration.collectAsState()
    val paused by viewModel.paused.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val autoDetecting by viewModel.autoDetecting.collectAsState()
    val autoLocked by viewModel.autoLocked.collectAsState()

    val previewView = remember { PreviewView(context) }
    val cameraController = remember { CameraController(context) }
    val sounds = remember { SoundEffects(context) }
    val videoRecorder = remember { VideoRecorder(context) }
    var recordingAvailable by remember { mutableStateOf(false) }
    val recordingState by videoRecorder.state.collectAsState()

    // --- Hands-free voice control (opt-in) ---
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasMicPermission = granted }

    LaunchedEffect(settings.voiceControl) {
        if (settings.voiceControl && !hasMicPermission) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val canListen = settings.voiceControl && hasMicPermission
    val voiceListener = remember(canListen) {
        if (canListen) {
            VoiceCommandListener(context) { command ->
                when (command) {
                    VoiceCommandListener.VoiceCommand.PAUSE -> viewModel.pause()
                    VoiceCommandListener.VoiceCommand.RESUME -> viewModel.resume()
                    VoiceCommandListener.VoiceCommand.NEXT -> viewModel.skipToNextExercise()
                    VoiceCommandListener.VoiceCommand.RESET -> viewModel.resetSession()
                }
            }
        } else {
            null
        }
    }
    DisposableEffect(voiceListener) {
        voiceListener?.takeIf { it.available }?.start()
        onDispose { voiceListener?.stop() }
    }
    // Mute recognition while our own TTS is speaking so it doesn't self-trigger.
    LaunchedEffect(voiceListener, isSpeaking) {
        val listener = voiceListener ?: return@LaunchedEffect
        if (isSpeaking) listener.mute() else listener.unmute()
    }

    // System back should log + tear down the session, same as the exit button.
    BackHandler(onBack = onExit)

    // When a plan finishes, the ViewModel raises this; navigate away and let it
    // clear state (logging was already done as each exercise completed).
    LaunchedEffect(requestExit) {
        if (requestExit) {
            viewModel.finishPlanRun()
            onFinished()
        }
    }

    val useFrontCamera = settings.cameraFacing == CameraFacing.FRONT
    // Re-bind when the chosen lens changes.
    DisposableEffect(lifecycleOwner, useFrontCamera) {
        val analyzer = PoseAnalyzer(onPose = viewModel::onPose)
        cameraController.start(
            lifecycleOwner, previewView, analyzer, useFrontCamera,
            videoCapture = videoRecorder.videoCapture,
            onRecordingAvailable = { recordingAvailable = it },
        )
        onDispose {
            videoRecorder.stop()
            cameraController.stop()
            analyzer.close()
        }
    }
    // TTS lives in the ViewModel (warmed at app launch); only the bell player is
    // screen-scoped, so only it is released here.
    DisposableEffect(Unit) {
        onDispose { sounds.release() }
    }

    LaunchedEffect(countdownValue) {
        if (!settings.soundsEnabled || !settings.countdownVoice) return@LaunchedEffect
        when (val value = countdownValue) {
            null -> {}
            0 -> viewModel.speak("Go!")
            else -> viewModel.speak(value.toString())
        }
    }
    LaunchedEffect(repCount) {
        val step = settings.repAnnouncement.step
        if (settings.soundsEnabled && step > 0 && repCount > 0 && repCount % step == 0) {
            viewModel.speak(repCount.toString())
        }
    }

    LaunchedEffect(celebration) {
        if (celebration != null && settings.soundsEnabled) sounds.playCelebration()
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
        PoseOverlay(
            pose = latestPose,
            isTracking = isTracking,
            mirror = useFrontCamera,
            modifier = Modifier.fillMaxSize(),
        )

        val progress = planProgress
        if (!autoDetecting) {
            Text(
                text = if (progress != null) "${repCount} / ${progress.targetReps}" else repCount.toString(),
                fontSize = 72.sp,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = exercise?.displayName ?: "Auto-detect",
                    fontSize = 20.sp,
                    color = Color.White,
                )
                if (autoLocked) {
                    Text(
                        text = "AUTO",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF06210F),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .background(BrandGreen, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            if (progress != null) {
                Text(
                    text = "${progress.planName} · exercise ${progress.exerciseIndex + 1}/" +
                        "${progress.exerciseCount} · set ${progress.setIndex + 1}/${progress.setCount}",
                    fontSize = 14.sp,
                    color = Color(0xFFCFF5DD),
                )
            }
        }

        if (autoDetecting) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Detecting exercise…",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
                Text(
                    text = "Get in frame and start your reps",
                    fontSize = 15.sp,
                    color = Color(0xFFCFD8DC),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        if (countdownValue == null && !isTracking && !planComplete && celebration == null && !paused) {
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
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (progress != null) {
                GymButton("Skip exercise", { viewModel.skipToNextExercise() }, style = GymButtonStyle.Secondary)
                GymButton("Pause", { viewModel.pause() }, style = GymButtonStyle.Secondary)
                GymButton("Stop plan", onExit, style = GymButtonStyle.Secondary)
            } else {
                GymButton("Reset", { viewModel.resetSession() }, style = GymButtonStyle.Secondary)
                GymButton("Pause", { viewModel.pause() }, style = GymButtonStyle.Secondary)
                GymButton("Change exercise", onExit, style = GymButtonStyle.Secondary)
            }
        }

        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (recordingAvailable) {
                if (recordingState.active) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(23.dp))
                            .background(Color(0xE0C0202A))
                            .clickable { videoRecorder.stop() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Icon(
                            Icons.Rounded.FiberManualRecord,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = formatClock2(recordingState.elapsedMs),
                            fontSize = 15.sp,
                            color = Color.White,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                } else {
                    CircleIconButton(
                        icon = Icons.Rounded.FiberManualRecord,
                        contentDescription = "Start recording",
                        tint = Color(0xFFFF5A5A),
                        onClick = { videoRecorder.start(RecordingStore.newFile(context)) },
                    )
                }
            }
            CircleIconButton(
                icon = Icons.Rounded.Cameraswitch,
                contentDescription = "Switch camera",
                onClick = {
                    viewModel.setCameraFacing(
                        if (useFrontCamera) CameraFacing.BACK else CameraFacing.FRONT,
                    )
                },
            )
            if (canListen) {
                CircleIconButton(
                    icon = if (isSpeaking) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                    contentDescription = if (isSpeaking) "Voice paused" else "Listening",
                    tint = if (isSpeaking) Color(0xFF9AA5AD) else Color.White,
                )
            }
        }

        countdownValue?.let { CountdownOverlay(value = it) }

        celebration?.let { ComboOverlay(word = it) }

        if (paused && !planComplete) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(text = "Paused", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    if (canListen) {
                        Text(text = "Say \"resume\" or tap", fontSize = 16.sp, color = Color(0xFFCFD8DC))
                    }
                    GymButton("Resume", { viewModel.resume() })
                }
            }
        }

        if (planComplete) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xAA000000)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Workout complete!",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

/** Elapsed recording time as M:SS. */
private fun formatClock2(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
