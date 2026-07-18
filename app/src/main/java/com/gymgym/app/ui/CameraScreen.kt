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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.res.stringResource
import com.gymgym.app.R
import com.gymgym.app.camera.PoseAnalyzer
import com.gymgym.app.counter.RepFault
import com.gymgym.app.camera.VideoRecorder
import com.gymgym.app.recording.RecordingStore
import com.gymgym.app.settings.CameraFacing

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
    val elapsedMs by viewModel.elapsedMs.collectAsState()
    val timerRunning by viewModel.timerRunning.collectAsState()
    val goodReps by viewModel.goodReps.collectAsState()
    val repFeedback by viewModel.repFeedback.collectAsState()
    val restRemaining by viewModel.restRemaining.collectAsState()
    val timed = exercise?.timed == true

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
    // Live snapshots so the remembered voice callback reads current state.
    val frontFacing = rememberUpdatedState(settings.cameraFacing == CameraFacing.FRONT)
    val recordingActive = rememberUpdatedState(recordingState.active)
    val canRecord = rememberUpdatedState(recordingAvailable)
    val voiceListener = remember(canListen) {
        if (canListen) {
            VoiceCommandListener(context) { command ->
                when (command) {
                    VoiceCommandListener.VoiceCommand.PAUSE -> viewModel.pause()
                    VoiceCommandListener.VoiceCommand.RESUME -> viewModel.resume()
                    VoiceCommandListener.VoiceCommand.NEXT -> viewModel.skipToNextExercise()
                    VoiceCommandListener.VoiceCommand.RESET -> viewModel.resetSession()
                    VoiceCommandListener.VoiceCommand.START_TIMER -> viewModel.startTimer()
                    VoiceCommandListener.VoiceCommand.STOP_TIMER -> viewModel.stopTimer()
                    VoiceCommandListener.VoiceCommand.START_RECORDING ->
                        if (canRecord.value && !recordingActive.value) {
                            videoRecorder.start(RecordingStore.newFile(context))
                        }
                    VoiceCommandListener.VoiceCommand.STOP_RECORDING ->
                        if (recordingActive.value) videoRecorder.stop()
                    VoiceCommandListener.VoiceCommand.SWITCH_CAMERA ->
                        viewModel.setCameraFacing(
                            if (frontFacing.value) CameraFacing.BACK else CameraFacing.FRONT,
                        )
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
            0 -> viewModel.speak(context.getString(R.string.speak_go))
            else -> viewModel.speak(value.toString())
        }
    }
    LaunchedEffect(repCount) {
        val step = settings.repAnnouncement.step
        // Timed exercises drive repCount as elapsed seconds — don't announce those.
        if (!timed && settings.soundsEnabled && step > 0 && repCount > 0 && repCount % step == 0) {
            viewModel.speak(repCount.toString())
        }
    }

    LaunchedEffect(celebration) {
        if (celebration != null && settings.soundsEnabled) sounds.playCelebration()
    }

    // A dull tone when a rep is flagged for poor form (voice cue is spoken by the VM).
    LaunchedEffect(repFeedback) {
        val fb = repFeedback ?: return@LaunchedEffect
        if (!fb.quality.isGood && settings.formFeedback && settings.soundsEnabled) sounds.playBadForm()
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
        if (timed) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (progress != null) {
                        "${formatClock2(elapsedMs)} / ${formatClock2(progress.targetReps * 1000L)}"
                    } else {
                        formatClock2(elapsedMs)
                    },
                    fontSize = if (progress != null) 52.sp else 72.sp,
                    color = if (timerRunning) MaterialTheme.colorScheme.primary else Color.White,
                )
                // Standalone plank is voice/button controlled; a plan hold auto-runs.
                if (progress == null && canListen) {
                    Text(
                        text = if (timerRunning) stringResource(R.string.camera_say_stop) else stringResource(R.string.camera_say_start),
                        fontSize = 14.sp,
                        color = Color(0xFFCFD8DC),
                    )
                }
            }
        } else if (!autoDetecting) {
            val badFault = repFeedback?.takeIf { !it.quality.isGood }?.quality
            val cue = when {
                badFault == null -> null
                RepFault.SHALLOW in badFault.faults -> stringResource(R.string.form_cue_shallow_caps)
                RepFault.WOBBLY in badFault.faults -> stringResource(R.string.form_cue_wobbly_caps)
                RepFault.TOO_FAST in badFault.faults -> stringResource(R.string.form_cue_fast_caps)
                else -> null
            }
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (progress != null) "${repCount} / ${progress.targetReps}" else repCount.toString(),
                    fontSize = 72.sp,
                    color = if (cue != null) Color(0xFFFFB020) else Color.White,
                )
                when {
                    cue != null -> Text(
                        text = cue,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB020),
                    )
                    settings.formFeedback && repCount > 0 -> Text(
                        text = stringResource(R.string.camera_good_count, goodReps),
                        fontSize = 15.sp,
                        color = Color(0xFFCFD8DC),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = exercise?.let { stringResource(it.labelRes()) } ?: stringResource(R.string.camera_auto_detect),
                    fontSize = 20.sp,
                    color = Color.White,
                )
                if (autoLocked) {
                    Text(
                        text = stringResource(R.string.camera_auto_badge),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF06210F),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            if (progress != null) {
                Text(
                    text = stringResource(
                        R.string.camera_plan_progress,
                        progress.planName,
                        progress.exerciseIndex + 1,
                        progress.exerciseCount,
                        progress.setIndex + 1,
                        progress.setCount,
                    ),
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
                    text = stringResource(R.string.camera_detecting),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
                Text(
                    text = stringResource(R.string.camera_detecting_hint),
                    fontSize = 15.sp,
                    color = Color(0xFFCFD8DC),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        if (countdownValue == null && !isTracking && !planComplete && celebration == null && !paused && !timed) {
            Text(
                text = stringResource(R.string.camera_move_into_frame),
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
            if (timed && progress != null) {
                // Plan hold runs automatically to its target; just skip or stop.
                GymButton(stringResource(R.string.camera_skip_exercise), { viewModel.skipToNextExercise() }, style = GymButtonStyle.Secondary)
                GymButton(stringResource(R.string.camera_stop_plan), onExit, style = GymButtonStyle.Secondary)
            } else if (timed) {
                GymButton(
                    if (timerRunning) stringResource(R.string.camera_stop_timer) else stringResource(R.string.camera_start_timer),
                    { if (timerRunning) viewModel.stopTimer() else viewModel.startTimer() },
                )
                GymButton(stringResource(R.string.camera_change_exercise), onExit, style = GymButtonStyle.Secondary)
            } else if (progress != null) {
                GymButton(stringResource(R.string.camera_skip_exercise), { viewModel.skipToNextExercise() }, style = GymButtonStyle.Secondary)
                GymButton(stringResource(R.string.camera_pause), { viewModel.pause() }, style = GymButtonStyle.Secondary)
                GymButton(stringResource(R.string.camera_stop_plan), onExit, style = GymButtonStyle.Secondary)
            } else {
                GymButton(stringResource(R.string.camera_reset), { viewModel.resetSession() }, style = GymButtonStyle.Secondary)
                GymButton(stringResource(R.string.camera_pause), { viewModel.pause() }, style = GymButtonStyle.Secondary)
                GymButton(stringResource(R.string.camera_change_exercise), onExit, style = GymButtonStyle.Secondary)
            }
        }

        Column(
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp),
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
                        contentDescription = stringResource(R.string.cd_start_recording),
                        tint = Color(0xFFFF5A5A),
                        onClick = { videoRecorder.start(RecordingStore.newFile(context)) },
                    )
                }
            }
            CircleIconButton(
                icon = Icons.Rounded.Cameraswitch,
                contentDescription = stringResource(R.string.cd_switch_camera),
                onClick = {
                    viewModel.setCameraFacing(
                        if (useFrontCamera) CameraFacing.BACK else CameraFacing.FRONT,
                    )
                },
            )
            if (canListen) {
                CircleIconButton(
                    icon = if (isSpeaking) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                    contentDescription = if (isSpeaking) stringResource(R.string.cd_voice_paused) else stringResource(R.string.cd_listening),
                    tint = if (isSpeaking) Color(0xFF9AA5AD) else Color.White,
                )
            }
        }

        countdownValue?.let { CountdownOverlay(value = it) }

        celebration?.let { ComboOverlay(word = it) }

        restRemaining?.let { secs ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.camera_rest),
                        fontSize = 26.sp,
                        color = Color(0xFFCFD8DC),
                    )
                    Text(
                        text = "$secs",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }

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
                    Text(text = stringResource(R.string.camera_paused), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    if (canListen) {
                        Text(text = stringResource(R.string.camera_say_resume), fontSize = 16.sp, color = Color(0xFFCFD8DC))
                    }
                    GymButton(stringResource(R.string.action_resume), { viewModel.resume() })
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
                    text = stringResource(R.string.camera_workout_complete),
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
