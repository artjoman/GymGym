package com.gymgym.app.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner

/** Binds CameraX Preview + ImageAnalysis to a lifecycle. One instance per camera session. */
class CameraController(private val context: Context) {

    private var cameraProvider: ProcessCameraProvider? = null

    fun start(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        analyzer: ImageAnalysis.Analyzer,
        useFrontCamera: Boolean = false,
        videoCapture: VideoCapture<Recorder>? = null,
        onRecordingAvailable: (Boolean) -> Unit = {},
    ) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            cameraProvider = provider

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(ContextCompat.getMainExecutor(context), analyzer) }

            val selector = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            provider.unbindAll()

            // Try to bind preview + analysis + video together. Not every device
            // supports three concurrent use cases, so fall back to no recording.
            var recordingAvailable = false
            if (videoCapture != null) {
                val group = UseCaseGroup.Builder()
                    .addUseCase(preview)
                    .addUseCase(imageAnalysis)
                    .addUseCase(videoCapture)
                    .build()
                recordingAvailable = try {
                    provider.bindToLifecycle(lifecycleOwner, selector, group)
                    true
                } catch (_: Exception) {
                    false
                }
            }
            if (!recordingAvailable) {
                provider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalysis)
            }
            onRecordingAvailable(recordingAvailable)
        }, ContextCompat.getMainExecutor(context))
    }

    fun stop() {
        cameraProvider?.unbindAll()
        cameraProvider = null
    }
}
