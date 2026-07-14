package com.gymgym.app.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gymgym.app.GymGymApp
import com.gymgym.app.audio.VoiceFeedback
import com.gymgym.app.counter.DumbbellPressCounter
import com.gymgym.app.counter.PullupCounter
import com.gymgym.app.counter.PushupCounter
import com.gymgym.app.counter.RepCounter
import com.gymgym.app.counter.SquatCounter
import com.gymgym.app.data.DraftExercise
import com.gymgym.app.data.ExerciseStat
import com.gymgym.app.data.PlanWithExercises
import com.gymgym.app.data.WorkoutSession
import com.gymgym.app.pose.PoseSnapshot
import com.gymgym.app.pose.isPlausiblePerson
import com.gymgym.app.profile.Profile
import com.gymgym.app.profile.ProfileRepository
import com.gymgym.app.profile.WeightUnit
import com.gymgym.app.settings.CameraFacing
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
    DUMBBELL_PRESS("Dumbbell Press", "Prop the phone facing you, arms and torso in frame"),
}

/** Live progress while running a multi-exercise plan; null during an ad-hoc single exercise. */
data class PlanProgress(
    val planName: String,
    val exerciseLabel: String,
    val exerciseIndex: Int,
    val exerciseCount: Int,
    val setIndex: Int,
    val setCount: Int,
    val targetReps: Int,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val profileRepository = ProfileRepository(application)
    private val workoutRepository =
        (application as GymGymApp).container.workoutRepository
    private val planRepository =
        (application as GymGymApp).container.planRepository

    // Created at ViewModel construction (app launch) so the TTS engine is warm
    // and ready by the time the user reaches a countdown.
    private val voiceFeedback = VoiceFeedback(application)

    val soundSettings: StateFlow<SoundSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, SoundSettings())

    val history: StateFlow<List<WorkoutSession>> = workoutRepository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stats: StateFlow<List<ExerciseStat>> = workoutRepository.stats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val profile: StateFlow<Profile> = profileRepository.profile
        .stateIn(viewModelScope, SharingStarted.Eagerly, Profile())

    val plans: StateFlow<List<PlanWithExercises>> = planRepository.plans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    private val _planProgress = MutableStateFlow<PlanProgress?>(null)
    val planProgress: StateFlow<PlanProgress?> = _planProgress.asStateFlow()

    private val _planComplete = MutableStateFlow(false)
    val planComplete: StateFlow<Boolean> = _planComplete.asStateFlow()

    /** Arcade callout word shown briefly when a set completes; null otherwise. */
    private val _celebration = MutableStateFlow<String?>(null)
    val celebration: StateFlow<String?> = _celebration.asStateFlow()

    private val _paused = MutableStateFlow(false)
    val paused: StateFlow<Boolean> = _paused.asStateFlow()

    /** True while TTS is speaking; the run screen mutes voice recognition then. */
    val isSpeaking: StateFlow<Boolean> = voiceFeedback.speaking

    /** One-shot signal for the run screen to pop back once a plan finishes. */
    private val _requestExit = MutableStateFlow(false)
    val requestExit: StateFlow<Boolean> = _requestExit.asStateFlow()

    /** True while auto-mode is still figuring out which exercise is being done. */
    private val _autoDetecting = MutableStateFlow(false)
    val autoDetecting: StateFlow<Boolean> = _autoDetecting.asStateFlow()

    /** True once auto-mode has locked an exercise in (shows an AUTO badge). */
    private val _autoLocked = MutableStateFlow(false)
    val autoLocked: StateFlow<Boolean> = _autoLocked.asStateFlow()

    private val classifier = ExerciseClassifier()

    private var counter: RepCounter? = null
    private var countdownJob: Job? = null
    private var transitionJob: Job? = null
    private var lastTrackedAtMs = 0L
    private var consecutivePlausibleFrames = 0
    private var sessionStartedAt = 0L

    // Plan-run engine state.
    private data class PlanStep(val exercise: Exercise, val targetReps: Int, val targetSets: Int)

    private var planName = ""
    private var planSteps: List<PlanStep> = emptyList()
    private var stepIndex = 0
    private var setIndex = 0
    private var exerciseRepsAccum = 0

    private val isPlanRun get() = planSteps.isNotEmpty()
    private fun currentStep(): PlanStep? = planSteps.getOrNull(stepIndex)

    init {
        viewModelScope.launch {
            while (true) {
                delay(TRACKING_CHECK_INTERVAL_MS)
                if (_selectedExercise.value == null && !_autoDetecting.value) continue
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

    // --- Single ad-hoc exercise ---

    fun selectExercise(exercise: Exercise) {
        planSteps = emptyList()
        _planProgress.value = null
        _planComplete.value = false
        _requestExit.value = false
        _selectedExercise.value = exercise
        _repCount.value = 0
        sessionStartedAt = System.currentTimeMillis()
        counter = counterFor(exercise)
        _autoDetecting.value = false
        _autoLocked.value = false
        startCountdown()
    }

    // --- Auto-detect mode ---

    /** Start a session with no chosen exercise; the classifier picks and locks one. */
    fun startAuto() {
        planSteps = emptyList()
        _planProgress.value = null
        _planComplete.value = false
        _requestExit.value = false
        _selectedExercise.value = null
        _repCount.value = 0
        counter = null
        classifier.reset()
        _autoLocked.value = false
        _autoDetecting.value = true
        _countdownValue.value = null
        _isTracking.value = false
        lastTrackedAtMs = 0L
        consecutivePlausibleFrames = 0
    }

    /** Commit to a detected exercise and start counting immediately (no countdown). */
    private fun lockAutoExercise(exercise: Exercise) {
        _autoDetecting.value = false
        _autoLocked.value = true
        _selectedExercise.value = exercise
        _repCount.value = 0
        counter = counterFor(exercise)
        sessionStartedAt = System.currentTimeMillis()
        _countdownValue.value = null
        if (soundSettings.value.soundsEnabled) speak("${exercise.displayName} detected")
    }

    // --- Plan run ---

    fun startPlan(plan: PlanWithExercises) {
        val steps = plan.orderedExercises.mapNotNull { pe ->
            val ex = Exercise.entries.find { it.name == pe.exerciseType } ?: return@mapNotNull null
            PlanStep(ex, pe.targetReps.coerceAtLeast(1), pe.targetSets.coerceAtLeast(1))
        }
        if (steps.isEmpty()) return
        planName = plan.plan.name
        planSteps = steps
        stepIndex = 0
        setIndex = 0
        _planComplete.value = false
        _requestExit.value = false
        beginCurrentStep(isNewExercise = true)
    }

    /** Manual "skip" — bank the current exercise's reps and jump to the next exercise. */
    fun skipToNextExercise() {
        if (!isPlanRun || _planComplete.value || _celebration.value != null) return
        exerciseRepsAccum += _repCount.value
        finishCurrentExercise()
    }

    private fun beginCurrentStep(isNewExercise: Boolean) {
        val step = currentStep() ?: return
        if (isNewExercise) {
            exerciseRepsAccum = 0
            sessionStartedAt = System.currentTimeMillis()
        }
        _selectedExercise.value = step.exercise
        _repCount.value = 0
        counter = counterFor(step.exercise)
        updatePlanProgress()
        startCountdown()
    }

    private fun onSetComplete() {
        val step = currentStep() ?: return
        exerciseRepsAccum += _repCount.value
        val moreSets = setIndex < step.targetSets - 1
        val moreExercises = stepIndex < planSteps.size - 1

        // Final set of the final exercise → straight to the completion screen.
        if (!moreSets && !moreExercises) {
            logSession(step.exercise, exerciseRepsAccum, sessionStartedAt)
            onPlanComplete()
            return
        }

        if (!soundSettings.value.setCelebration) {
            advanceAfterSet(moreSets, step)
            return
        }

        // Arcade combo callout, then advance to the next set/exercise.
        countdownJob?.cancel()
        _countdownValue.value = null
        val word = CELEBRATION_WORDS.random()
        _celebration.value = word
        if (soundSettings.value.soundsEnabled) speak(word)
        transitionJob?.cancel()
        transitionJob = viewModelScope.launch {
            delay(CELEBRATION_DISPLAY_MS)
            _celebration.value = null
            advanceAfterSet(moreSets, step)
        }
    }

    private fun advanceAfterSet(moreSets: Boolean, step: PlanStep) {
        if (moreSets) {
            setIndex++
            beginCurrentStep(isNewExercise = false)
        } else {
            logSession(step.exercise, exerciseRepsAccum, sessionStartedAt)
            stepIndex++
            setIndex = 0
            beginCurrentStep(isNewExercise = true)
        }
    }

    private fun finishCurrentExercise() {
        val step = currentStep() ?: return
        logSession(step.exercise, exerciseRepsAccum, sessionStartedAt)
        if (stepIndex < planSteps.size - 1) {
            stepIndex++
            setIndex = 0
            beginCurrentStep(isNewExercise = true)
        } else {
            onPlanComplete()
        }
    }

    private fun onPlanComplete() {
        _planComplete.value = true
        countdownJob?.cancel()
        _countdownValue.value = null
        if (soundSettings.value.soundsEnabled) speak("Workout complete!")
        viewModelScope.launch {
            delay(PLAN_COMPLETE_DISPLAY_MS)
            _requestExit.value = true
        }
    }

    private fun updatePlanProgress() {
        val step = currentStep()
        _planProgress.value = step?.let {
            PlanProgress(
                planName = planName,
                exerciseLabel = it.exercise.displayName,
                exerciseIndex = stepIndex,
                exerciseCount = planSteps.size,
                setIndex = setIndex,
                setCount = it.targetSets,
                targetReps = it.targetReps,
            )
        }
    }

    /** Called by the run screen once it has observed [requestExit] and navigated away. */
    fun finishPlanRun() {
        clearSession()
        _requestExit.value = false
        _planComplete.value = false
    }

    // --- Shared session lifecycle ---

    /** Pause the active session — stops counting and the countdown until [resume]. */
    fun pause() {
        if (_paused.value || _selectedExercise.value == null || _planComplete.value) return
        _paused.value = true
        countdownJob?.cancel()
        _countdownValue.value = null
        counter?.reset()
    }

    fun resume() {
        if (!_paused.value) return
        _paused.value = false
        startCountdown()
    }

    fun onPose(pose: PoseSnapshot) {
        _latestPose.value = pose
        if (_planComplete.value || _celebration.value != null || _paused.value) return
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
        if (_autoDetecting.value) {
            if (_isTracking.value) {
                classifier.observe(pose)
                classifier.guess()?.let { lockAutoExercise(it) }
            }
            return
        }
        if (_countdownValue.value != null) return
        if (!_isTracking.value) return
        if (counter?.process(pose) == true) onRepCompleted()
    }

    private fun onRepCompleted() {
        _repCount.value = _repCount.value + 1
        val step = currentStep() ?: return // single ad-hoc exercise
        if (_repCount.value >= step.targetReps) onSetComplete()
    }

    fun resetSession() {
        _repCount.value = 0
        counter?.reset()
        startCountdown()
    }

    /** Manual exit from the run screen (button or system back), for both modes. */
    fun stopSession() {
        if (isPlanRun) {
            if (!_planComplete.value) {
                val step = currentStep()
                if (step != null) logSession(step.exercise, exerciseRepsAccum + _repCount.value, sessionStartedAt)
            }
        } else {
            logSession(_selectedExercise.value, _repCount.value, sessionStartedAt)
        }
        clearSession()
    }

    private fun clearSession() {
        countdownJob?.cancel()
        transitionJob?.cancel()
        _celebration.value = null
        _paused.value = false
        _countdownValue.value = null
        _selectedExercise.value = null
        _latestPose.value = null
        _isTracking.value = false
        lastTrackedAtMs = 0L
        consecutivePlausibleFrames = 0
        counter = null
        planSteps = emptyList()
        stepIndex = 0
        setIndex = 0
        exerciseRepsAccum = 0
        planName = ""
        _planProgress.value = null
        _autoDetecting.value = false
        _autoLocked.value = false
        classifier.reset()
    }

    /** Persist a finished exercise, but only if reps were actually counted. */
    private fun logSession(exercise: Exercise?, reps: Int, startedAt: Long) {
        if (exercise == null || reps < 1) return
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

    private fun counterFor(exercise: Exercise): RepCounter = when (exercise) {
        Exercise.SQUAT -> SquatCounter()
        Exercise.PUSHUP -> PushupCounter()
        Exercise.PULLUP -> PullupCounter()
        Exercise.DUMBBELL_PRESS -> DumbbellPressCounter()
    }

    // --- Plan CRUD ---

    fun savePlan(id: Long, name: String, exercises: List<DraftExercise>) =
        viewModelScope.launch { planRepository.savePlan(id, name, exercises) }

    fun deletePlan(id: Long) =
        viewModelScope.launch { planRepository.deletePlan(id) }

    // --- Settings & profile ---

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

    fun setSetCelebration(value: Boolean) =
        viewModelScope.launch { settingsRepository.setSetCelebration(value) }

    fun setVoiceControl(value: Boolean) =
        viewModelScope.launch { settingsRepository.setVoiceControl(value) }

    fun setCameraFacing(facing: CameraFacing) =
        viewModelScope.launch { settingsRepository.setCameraFacing(facing) }

    fun setDisplayName(value: String) =
        viewModelScope.launch { profileRepository.setDisplayName(value) }

    fun setWeightUnit(unit: WeightUnit) =
        viewModelScope.launch { profileRepository.setWeightUnit(unit) }

    /** Speak via the pre-warmed TTS engine. No-op until the engine is ready. */
    fun speak(text: String) = voiceFeedback.speak(text)

    override fun onCleared() {
        voiceFeedback.shutdown()
        super.onCleared()
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        _countdownValue.value = COUNTDOWN_SECONDS // set synchronously to gate stray reps
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
        const val PLAN_COMPLETE_DISPLAY_MS = 1_800L
        const val CELEBRATION_DISPLAY_MS = 1_300L
        const val TRACKING_CHECK_INTERVAL_MS = 250L
        const val TRACKING_TIMEOUT_MS = 800L
        const val STABLE_FRAMES_TO_TRACK = 5

        val CELEBRATION_WORDS = listOf(
            "SUPER!", "GREAT!", "AWESOME!", "COMBO!", "NICE!", "PERFECT!", "BOOM!",
        )
    }
}
