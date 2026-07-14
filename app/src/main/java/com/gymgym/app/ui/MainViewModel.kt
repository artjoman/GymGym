package com.gymgym.app.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gymgym.app.GymGymApp
import com.gymgym.app.counter.PullupCounter
import com.gymgym.app.counter.PushupCounter
import com.gymgym.app.counter.RepCounter
import com.gymgym.app.counter.SquatCounter
import com.gymgym.app.data.ExerciseStat
import com.gymgym.app.data.WorkoutSession
import com.gymgym.app.pose.PoseSnapshot
import com.gymgym.app.pose.isPlausiblePerson
import com.gymgym.app.profile.Profile
import com.gymgym.app.profile.ProfileRepository
import com.gymgym.app.profile.WeightUnit
import com.gymgym.app.settings.RepAnnouncementMode
import com.gymgym.app.settings.SettingsRepository
import com.gymgym.app.settings.SoundSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Exercise(val displayName: String, val framingTip: String) {
    SQUAT("Squat", "Prop the phone to the side, full body in frame"),
    PUSHUP("Pushup", "Prop the phone to the side, full body in frame"),
    PULLUP("Pullup", "Prop the phone facing you, bar and full body in frame"),
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val profileRepository = ProfileRepository(application)
    private val workoutRepository =
        (application as GymGymApp).container.workoutRepository

    val soundSettings: StateFlow<SoundSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, SoundSettings())

    val history: StateFlow<List<WorkoutSession>> = workoutRepository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stats: StateFlow<List<ExerciseStat>> = workoutRepository.stats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val profile: StateFlow<Profile> = profileRepository.profile
        .stateIn(viewModelScope, SharingStarted.Eagerly, Profile())

    private val _selectedExercise = MutableStateFlow<Exercise?>(null)
    val selectedExercise: StateFlow<Exercise?> = _selectedExercise.asStateFlow()

    private val _repCount = MutableStateFlow(0)
    val repCount: StateFlow<Int> = _repCount.asStateFlow()

    private val _latestPose = MutableStateFlow<PoseSnapshot?>(null)
    val latestPose: StateFlow<PoseSnapshot?> = _latestPose.asStateFlow()

    /** Seconds remaining in the pre-set countdown; 0 means "GO", null means not counting down. */
    private val _countdownValue = MutableStateFlow<Int?>(null)
    val countdownValue: StateFlow<Int?> = _countdownValue.asStateFlow()

    /**
     * Whether the camera currently sees enough of the body to count reps.
     * ML Kit keeps delivering (near-empty) poses when nobody is in frame, so
     * "lost" means no recent snapshot with enough confident landmarks.
     */
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private var counter: RepCounter? = null
    private var countdownJob: Job? = null
    private var lastTrackedAtMs = 0L
    private var consecutivePlausibleFrames = 0
    private var sessionStartedAt = 0L

    init {
        viewModelScope.launch {
            while (true) {
                delay(TRACKING_CHECK_INTERVAL_MS)
                if (_selectedExercise.value == null) continue
                val stale = SystemClock.elapsedRealtime() - lastTrackedAtMs > TRACKING_TIMEOUT_MS
                if (stale && _isTracking.value) {
                    _isTracking.value = false
                    // Drop any half-finished rep so a phantom or stale pose
                    // can't complete it once tracking resumes.
                    counter?.reset()
                }
            }
        }
    }

    fun selectExercise(exercise: Exercise) {
        _selectedExercise.value = exercise
        _repCount.value = 0
        sessionStartedAt = System.currentTimeMillis()
        counter = when (exercise) {
            Exercise.SQUAT -> SquatCounter()
            Exercise.PUSHUP -> PushupCounter()
            Exercise.PULLUP -> PullupCounter()
        }
        startCountdown()
    }

    fun onPose(pose: PoseSnapshot) {
        _latestPose.value = pose
        if (pose.isPlausiblePerson()) {
            consecutivePlausibleFrames++
            lastTrackedAtMs = SystemClock.elapsedRealtime()
            if (consecutivePlausibleFrames >= STABLE_FRAMES_TO_TRACK) {
                _isTracking.value = true
            }
        } else {
            consecutivePlausibleFrames = 0
            return
        }
        if (_countdownValue.value != null) return
        if (!_isTracking.value) return
        if (counter?.process(pose) == true) {
            _repCount.value = _repCount.value + 1
        }
    }

    fun resetSession() {
        _repCount.value = 0
        counter?.reset()
        startCountdown()
    }

    fun exitToSelection() {
        logSessionIfCounted()
        countdownJob?.cancel()
        _countdownValue.value = null
        _selectedExercise.value = null
        _latestPose.value = null
        _isTracking.value = false
        lastTrackedAtMs = 0L
        consecutivePlausibleFrames = 0
        counter = null
    }

    /** Persist the finished session, but only if the user actually did reps. */
    private fun logSessionIfCounted() {
        val exercise = _selectedExercise.value ?: return
        val reps = _repCount.value
        if (reps < 1) return
        val startedAt = sessionStartedAt
        viewModelScope.launch {
            workoutRepository.log(
                WorkoutSession(
                    exerciseType = exercise.name,
                    repCount = reps,
                    startedAt = startedAt,
                    durationMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(0),
                ),
            )
        }
    }

    fun setSoundsEnabled(value: Boolean) =
        viewModelScope.launch { settingsRepository.setSoundsEnabled(value) }

    fun setCountdownVoice(value: Boolean) =
        viewModelScope.launch { settingsRepository.setCountdownVoice(value) }

    fun setRepAnnouncement(mode: RepAnnouncementMode) =
        viewModelScope.launch { settingsRepository.setRepAnnouncement(mode) }

    fun setTrackingLostBell(value: Boolean) =
        viewModelScope.launch { settingsRepository.setTrackingLostBell(value) }

    fun setTrackingRegainedChime(value: Boolean) =
        viewModelScope.launch { settingsRepository.setTrackingRegainedChime(value) }

    fun setDisplayName(value: String) =
        viewModelScope.launch { profileRepository.setDisplayName(value) }

    fun setWeightUnit(unit: WeightUnit) =
        viewModelScope.launch { profileRepository.setWeightUnit(unit) }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (second in COUNTDOWN_SECONDS downTo 1) {
                _countdownValue.value = second
                delay(1_000)
            }
            _countdownValue.value = 0
            delay(GO_DISPLAY_MS)
            _countdownValue.value = null
            counter?.reset()
        }
    }

    private companion object {
        const val COUNTDOWN_SECONDS = 3
        const val GO_DISPLAY_MS = 700L
        const val TRACKING_CHECK_INTERVAL_MS = 250L
        const val TRACKING_TIMEOUT_MS = 800L
        const val STABLE_FRAMES_TO_TRACK = 5
    }
}
