package com.gymgym.app.ui

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
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

    val previewView = remember { PreviewView(context) }
    val cameraController = remember { CameraController(context) }

    DisposableEffect(lifecycleOwner) {
        val analyzer = PoseAnalyzer(onPose = viewModel::onPose)
        cameraController.start(lifecycleOwner, previewView, analyzer)
        onDispose {
            cameraController.stop()
            analyzer.close()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        PoseOverlay(pose = latestPose, modifier = Modifier.fillMaxSize())

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

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
        ) {
            Button(onClick = { viewModel.resetSession() }) { Text("Reset") }
            Button(onClick = onExit) { Text("Change exercise") }
        }
    }
}
