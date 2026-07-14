package com.gymgym.app.camera

import android.content.Context
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class RecordingState(val active: Boolean = false, val elapsedMs: Long = 0)

/**
 * Wraps CameraX [VideoCapture] for medium-quality (720p, falling back lower),
 * video-only recording to a file. Auto-stops at [MAX_DURATION_MS] (10 minutes).
 * Expose [videoCapture] so the camera controller can bind it alongside preview
 * and analysis.
 */
class VideoRecorder(context: Context) {

    private val appContext = context.applicationContext

    private val recorder = Recorder.Builder()
        .setQualitySelector(
            QualitySelector.fromOrderedList(
                listOf(Quality.HD, Quality.SD),
                FallbackStrategy.lowerQualityOrHigherThan(Quality.SD),
            ),
        )
        .build()

    val videoCapture: VideoCapture<Recorder> = VideoCapture.withOutput(recorder)

    private val _state = MutableStateFlow(RecordingState())
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    private var recording: Recording? = null

    fun start(file: File) {
        if (recording != null) return
        val options = FileOutputOptions.Builder(file).build()
        recording = videoCapture.output
            .prepareRecording(appContext, options) // no withAudioEnabled -> video only
            .start(ContextCompat.getMainExecutor(appContext)) { event ->
                when (event) {
                    is VideoRecordEvent.Start ->
                        _state.value = RecordingState(active = true, elapsedMs = 0)
                    is VideoRecordEvent.Status -> {
                        val ms = event.recordingStats.recordedDurationNanos / 1_000_000
                        _state.value = RecordingState(active = true, elapsedMs = ms)
                        if (ms >= MAX_DURATION_MS) stop()
                    }
                    is VideoRecordEvent.Finalize -> {
                        _state.value = RecordingState(active = false, elapsedMs = 0)
                        recording = null
                    }
                }
            }
    }

    fun stop() {
        recording?.stop()
        recording = null
    }

    val isRecording: Boolean get() = recording != null

    companion object {
        const val MAX_DURATION_MS = 10 * 60 * 1000L
    }
}
