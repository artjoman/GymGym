package com.gymgym.app.ui

import androidx.lifecycle.ViewModel
import com.gymgym.app.counter.PullupCounter
import com.gymgym.app.counter.PushupCounter
import com.gymgym.app.counter.RepCounter
import com.gymgym.app.counter.SquatCounter
import com.gymgym.app.pose.PoseSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Exercise(val displayName: String, val framingTip: String) {
    SQUAT("Squat", "Prop the phone to the side, full body in frame"),
    PUSHUP("Pushup", "Prop the phone to the side, full body in frame"),
    PULLUP("Pullup", "Prop the phone facing you, bar and full body in frame"),
}

class MainViewModel : ViewModel() {
    private val _selectedExercise = MutableStateFlow<Exercise?>(null)
    val selectedExercise: StateFlow<Exercise?> = _selectedExercise.asStateFlow()

    private val _repCount = MutableStateFlow(0)
    val repCount: StateFlow<Int> = _repCount.asStateFlow()

    private val _latestPose = MutableStateFlow<PoseSnapshot?>(null)
    val latestPose: StateFlow<PoseSnapshot?> = _latestPose.asStateFlow()

    private var counter: RepCounter? = null

    fun selectExercise(exercise: Exercise) {
        _selectedExercise.value = exercise
        _repCount.value = 0
        counter = when (exercise) {
            Exercise.SQUAT -> SquatCounter()
            Exercise.PUSHUP -> PushupCounter()
            Exercise.PULLUP -> PullupCounter()
        }
    }

    fun onPose(pose: PoseSnapshot) {
        _latestPose.value = pose
        if (counter?.process(pose) == true) {
            _repCount.value = _repCount.value + 1
        }
    }

    fun resetSession() {
        _repCount.value = 0
        counter?.reset()
    }

    fun exitToSelection() {
        _selectedExercise.value = null
        _latestPose.value = null
        counter = null
    }
}
