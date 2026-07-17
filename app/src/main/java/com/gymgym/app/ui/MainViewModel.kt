package com.gymgym.app.ui

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gymgym.app.GymGymApp
import com.gymgym.app.backup.BackupData
import com.gymgym.app.backup.BackupPlan
import com.gymgym.app.backup.BackupPlanExercise
import com.gymgym.app.backup.BackupProfile
import com.gymgym.app.backup.BackupSession
import com.gymgym.app.backup.BackupSettings
import com.gymgym.app.audio.VoiceFeedback
import com.gymgym.app.counter.DumbbellPressCounter
import com.gymgym.app.counter.PullupCounter
import com.gymgym.app.counter.PushupCounter
import com.gymgym.app.counter.FormTuning
import com.gymgym.app.counter.RepCounter
import com.gymgym.app.counter.RepFault
import com.gymgym.app.counter.RepQuality
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
import com.gymgym.app.settings.AccentTheme
import com.gymgym.app.settings.BackgroundStyle
import com.gymgym.app.settings.CameraFacing
import com.gymgym.app.settings.FormSensitivity
import com.gymgym.app.settings.RepAnnouncementMode
import com.gymgym.app.settings.SettingsRepository
import com.gymgym.app.settings.SoundSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class Exercise(val displayName: String, val framingTip: String, val timed: Boolean = false) {
    SQUAT("Squat", "Prop the phone to the side, full body in frame"),
    PUSHUP("Pushup", "Prop the phone to the side, full body in frame"),
    PULLUP("Pullup", "Prop the phone facing you, bar and full body in frame"),
    DUMBBELL_PRESS("Dumbbell Press", "Prop the phone facing you, arms and torso in frame"),
    // Hold-for-time exercise: a stopwatch the user starts/stops (voice or button)
    // rather than a rep counter.
    PLANK("Plank", "Prop the phone to the side, full body in frame", timed = true),
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

/** Transient feedback for a just-completed rep: its form quality and whether it was counted. */
data class RepFeedback(val quality: RepQuality, val counted: Boolean)

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

    /** Reps in the current set/exercise completed with good form. */
    private val _goodReps = MutableStateFlow(0)
    val goodReps: StateFlow<Int> = _goodReps.asStateFlow()

    /** Transient feedback about the just-completed rep; null when nothing to show. */
    private val _repFeedback = MutableStateFlow<RepFeedback?>(null)
    val repFeedback: StateFlow<RepFeedback?> = _repFeedback.asStateFlow()

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

    // --- Timed (hold) exercises, e.g. plank ---
    /** Elapsed hold time in ms for the current timed exercise. */
    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs.asStateFlow()

    /** Whether the hold stopwatch is currently running. */
    private val _timerRunning = MutableStateFlow(false)
    val timerRunning: StateFlow<Boolean> = _timerRunning.asStateFlow()

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
    private var feedbackJob: Job? = null
    private var timerJob: Job? = null
    private var timerBaseMs = 0L
    private var timedLogged = false
    // >0 for a plan's timed set (auto-completes at this hold); 0 for a manual hold.
    private var timedTargetMs = 0L
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
    private var exerciseGoodAccum = 0

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
        _goodReps.value = 0
        _repFeedback.value = null
        sessionStartedAt = System.currentTimeMillis()
        counter = counterFor(exercise) // null for timed exercises
        _autoDetecting.value = false
        _autoLocked.value = false
        if (exercise.timed) prepareTimedExercise() else startCountdown()
    }

    // --- Timed (hold) exercises: plank etc. Controlled by start/stop, not reps. ---

    /** Reset the stopwatch; it waits for a start command rather than a countdown. */
    private fun prepareTimedExercise() {
        timerJob?.cancel()
        _timerRunning.value = false
        _elapsedMs.value = 0L
        timedLogged = false
        timedTargetMs = 0L // standalone hold: manual, no auto-complete
        _countdownValue.value = null
    }

    /** Start (or restart) a manual hold stopwatch (standalone plank). */
    fun startTimer() {
        val exercise = _selectedExercise.value ?: return
        if (!exercise.timed || _timerRunning.value) return
        // A start after a completed hold begins a fresh attempt.
        if (timedLogged || _elapsedMs.value == 0L) {
            _elapsedMs.value = 0L
            timedLogged = false
            sessionStartedAt = System.currentTimeMillis()
        }
        timedTargetMs = 0L
        _timerRunning.value = true
        timerBaseMs = SystemClock.elapsedRealtime() - _elapsedMs.value
        if (soundSettings.value.soundsEnabled) speak("Timer started")
        launchTimerJob()
    }

    /** Auto-start a plan's timed set after its countdown (target already set). */
    private fun beginTimedHold() {
        if (timedTargetMs <= 0) return
        _elapsedMs.value = 0L
        _repCount.value = 0
        timedLogged = false
        _timerRunning.value = true
        timerBaseMs = SystemClock.elapsedRealtime()
        launchTimerJob()
    }

    private fun launchTimerJob() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timerRunning.value) {
                val e = SystemClock.elapsedRealtime() - timerBaseMs
                // A plan's timed set auto-completes once the target hold is reached.
                if (timedTargetMs > 0 && e >= timedTargetMs) {
                    _elapsedMs.value = timedTargetMs
                    _repCount.value = (timedTargetMs / 1_000).toInt()
                    _timerRunning.value = false
                    onSetComplete()
                    break
                }
                _elapsedMs.value = e
                // Held seconds double as the "rep" count so plan accounting (accumulate,
                // skip, log) works unchanged for timed sets.
                if (timedTargetMs > 0) _repCount.value = (e / 1_000).toInt()
                delay(TIMER_TICK_MS)
            }
        }
    }

    /** Stop the hold stopwatch and log the held time. */
    fun stopTimer() {
        val exercise = _selectedExercise.value ?: return
        if (!exercise.timed || !_timerRunning.value) return
        _timerRunning.value = false
        timerJob?.cancel()
        val held = SystemClock.elapsedRealtime() - timerBaseMs
        _elapsedMs.value = held
        logTimedSession(exercise, held, sessionStartedAt)
        timedLogged = true
        if (soundSettings.value.soundsEnabled) speak("Time, ${spokenDuration(held)}")
    }

    /** Persist a hold: seconds go in repCount so existing best/total/avg aggregates
     *  work, the true hold time in durationMs. */
    private fun logTimedSession(exercise: Exercise, heldMs: Long, startedAt: Long) {
        if (heldMs < 1_000) return // ignore sub-second taps
        val seconds = (heldMs / 1_000).toInt()
        viewModelScope.launch {
            workoutRepository.log(
                WorkoutSession(
                    exerciseType = exercise.name,
                    repCount = seconds,
                    goodReps = seconds, // a hold has no per-rep form; count it fully "good"
                    startedAt = startedAt,
                    durationMs = heldMs,
                ),
            )
        }
    }

    private fun spokenDuration(ms: Long): String {
        val total = (ms / 1_000).toInt()
        val m = total / 60
        val s = total % 60
        fun plural(n: Int) = if (n == 1) "" else "s"
        return when {
            m > 0 && s > 0 -> "$m minute${plural(m)} $s second${plural(s)}"
            m > 0 -> "$m minute${plural(m)}"
            else -> "$s second${plural(s)}"
        }
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
        _goodReps.value = 0
        _repFeedback.value = null
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
        _goodReps.value = 0
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
        exerciseGoodAccum += _goodReps.value
        finishCurrentExercise()
    }

    private fun beginCurrentStep(isNewExercise: Boolean) {
        val step = currentStep() ?: return
        if (isNewExercise) {
            exerciseRepsAccum = 0
            exerciseGoodAccum = 0
            sessionStartedAt = System.currentTimeMillis()
        }
        _selectedExercise.value = step.exercise
        _repCount.value = 0
        _goodReps.value = 0
        _repFeedback.value = null
        counter = counterFor(step.exercise)
        // For a timed step the target is a hold duration (seconds stored in targetReps).
        timedTargetMs = if (step.exercise.timed) step.targetReps.toLong() * 1_000 else 0L
        _elapsedMs.value = 0L
        _timerRunning.value = false
        updatePlanProgress()
        startCountdown()
    }

    private fun onSetComplete() {
        val step = currentStep() ?: return
        exerciseRepsAccum += _repCount.value
        exerciseGoodAccum += _goodReps.value
        val moreSets = setIndex < step.targetSets - 1
        val moreExercises = stepIndex < planSteps.size - 1

        // Final set of the final exercise → straight to the completion screen.
        if (!moreSets && !moreExercises) {
            logSession(step.exercise, exerciseRepsAccum, exerciseGoodAccum, sessionStartedAt)
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
            logSession(step.exercise, exerciseRepsAccum, exerciseGoodAccum, sessionStartedAt)
            stepIndex++
            setIndex = 0
            beginCurrentStep(isNewExercise = true)
        }
    }

    private fun finishCurrentExercise() {
        val step = currentStep() ?: return
        logSession(step.exercise, exerciseRepsAccum, exerciseGoodAccum, sessionStartedAt)
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
        // Timed exercises are governed by start/stop, not the pause overlay.
        if (_selectedExercise.value?.timed == true) return
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
        counter?.process(pose, SystemClock.elapsedRealtime())?.let { onRepCompleted(it) }
    }

    private fun onRepCompleted(quality: RepQuality) {
        val s = soundSettings.value
        val strict = s.formFeedback && s.strictForm
        // In strict mode a bad rep is rejected (doesn't count toward the set).
        val counts = quality.isGood || !strict
        if (counts) {
            _repCount.value += 1
            if (quality.isGood) _goodReps.value += 1
        }
        if (s.formFeedback) showRepFeedback(quality, counts)
        if (!counts) return
        val step = currentStep() ?: return // single ad-hoc exercise
        if (_repCount.value >= step.targetReps) onSetComplete()
    }

    private fun showRepFeedback(quality: RepQuality, counted: Boolean) {
        _repFeedback.value = RepFeedback(quality, counted)
        feedbackJob?.cancel()
        feedbackJob = viewModelScope.launch {
            delay(REP_FEEDBACK_MS)
            _repFeedback.value = null
        }
        if (!quality.isGood && soundSettings.value.soundsEnabled) speak(cueFor(quality))
    }

    private fun cueFor(quality: RepQuality): String = when {
        RepFault.SHALLOW in quality.faults -> "Go deeper"
        RepFault.WOBBLY in quality.faults -> "Steady"
        RepFault.TOO_FAST in quality.faults -> "Slow down"
        else -> ""
    }

    fun resetSession() {
        _repCount.value = 0
        _goodReps.value = 0
        _repFeedback.value = null
        counter?.reset()
        startCountdown()
    }

    /** Manual exit from the run screen (button or system back), for both modes. */
    fun stopSession() {
        val exercise = _selectedExercise.value
        // Freeze any running hold first so the reads below are stable.
        if (exercise?.timed == true) {
            _timerRunning.value = false
            timerJob?.cancel()
        }
        if (exercise?.timed == true && !isPlanRun) {
            // Standalone hold: bank the exact held time.
            if (!timedLogged) logTimedSession(exercise, _elapsedMs.value, sessionStartedAt)
            clearSession()
            return
        }
        if (isPlanRun) {
            if (!_planComplete.value) {
                val step = currentStep()
                if (step != null) {
                    logSession(
                        step.exercise,
                        exerciseRepsAccum + _repCount.value,
                        exerciseGoodAccum + _goodReps.value,
                        sessionStartedAt,
                    )
                }
            }
        } else {
            logSession(_selectedExercise.value, _repCount.value, _goodReps.value, sessionStartedAt)
        }
        clearSession()
    }

    private fun clearSession() {
        countdownJob?.cancel()
        transitionJob?.cancel()
        feedbackJob?.cancel()
        timerJob?.cancel()
        _timerRunning.value = false
        _elapsedMs.value = 0L
        timedLogged = false
        timedTargetMs = 0L
        _goodReps.value = 0
        _repFeedback.value = null
        exerciseGoodAccum = 0
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
    private fun logSession(exercise: Exercise?, reps: Int, goodReps: Int, startedAt: Long) {
        if (exercise == null || reps < 1) return
        viewModelScope.launch {
            workoutRepository.log(
                WorkoutSession(
                    exerciseType = exercise.name,
                    repCount = reps,
                    goodReps = goodReps.coerceIn(0, reps),
                    startedAt = startedAt,
                    durationMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(0),
                ),
            )
        }
    }

    private fun counterFor(exercise: Exercise): RepCounter? {
        val tuning = tuningFor(soundSettings.value.formSensitivity)
        return when (exercise) {
            Exercise.SQUAT -> SquatCounter(tuning)
            Exercise.PUSHUP -> PushupCounter(tuning)
            Exercise.PULLUP -> PullupCounter(tuning)
            Exercise.DUMBBELL_PRESS -> DumbbellPressCounter(tuning)
            Exercise.PLANK -> null // timed exercise: no rep counter
        }
    }

    private fun tuningFor(sensitivity: FormSensitivity): FormTuning = when (sensitivity) {
        FormSensitivity.LENIENT -> FormTuning(depthTolerance = 8f, wobbleScale = 1.4f)
        FormSensitivity.STANDARD -> FormTuning()
        FormSensitivity.STRICT -> FormTuning(depthTolerance = -6f, wobbleScale = 0.75f)
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

    fun setFormFeedback(value: Boolean) =
        viewModelScope.launch { settingsRepository.setFormFeedback(value) }

    fun setStrictForm(value: Boolean) =
        viewModelScope.launch { settingsRepository.setStrictForm(value) }

    fun setFormSensitivity(value: FormSensitivity) =
        viewModelScope.launch { settingsRepository.setFormSensitivity(value) }

    fun setAccentTheme(theme: AccentTheme) =
        viewModelScope.launch { settingsRepository.setAccentTheme(theme) }

    fun setBackgroundStyle(style: BackgroundStyle) =
        viewModelScope.launch { settingsRepository.setBackgroundStyle(style) }

    /** Copy a picked image into app storage and switch to it as the background. */
    fun setCustomBackground(uri: Uri) = viewModelScope.launch {
        val path = withContext(Dispatchers.IO) {
            runCatching {
                val app = getApplication<Application>()
                // Fresh filename each time so the background recomposes (path changes).
                app.filesDir.listFiles { f -> f.name.startsWith("custom_bg_") }
                    ?.forEach { it.delete() }
                val dest = java.io.File(app.filesDir, "custom_bg_${System.currentTimeMillis()}.jpg")
                app.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { input.copyTo(it) }
                } ?: error("no input stream")
                dest.absolutePath
            }.getOrNull()
        }
        if (path != null) settingsRepository.setCustomBackground(path)
    }

    fun setDisplayName(value: String) =
        viewModelScope.launch { profileRepository.setDisplayName(value) }

    fun setWeightUnit(unit: WeightUnit) =
        viewModelScope.launch { profileRepository.setWeightUnit(unit) }

    // --- Backup: export / import (history, plans, settings, profile; no videos) ---

    private val backupJson = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun exportBackup(uri: Uri, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        val ok = runCatching {
            val data = BackupData(
                exportedAt = System.currentTimeMillis(),
                sessions = workoutRepository.allOnce().map {
                    BackupSession(it.exerciseType, it.repCount, it.goodReps, it.startedAt, it.durationMs)
                },
                plans = planRepository.allOnce().map { pw ->
                    BackupPlan(
                        name = pw.plan.name,
                        exercises = pw.orderedExercises.map {
                            BackupPlanExercise(it.exerciseType, it.targetReps, it.targetSets, it.position)
                        },
                    )
                },
                settings = soundSettings.value.let { s ->
                    BackupSettings(
                        s.soundsEnabled, s.countdownVoice, s.repAnnouncement.name,
                        s.trackingLostBell, s.trackingRegainedChime, s.setCelebration,
                        s.voiceControl, s.cameraFacing.name,
                        s.accentTheme.name, s.backgroundStyle.name,
                        s.formFeedback, s.strictForm, s.formSensitivity.name,
                    )
                },
                profile = profile.value.let { p -> BackupProfile(p.displayName, p.weightUnit.name) },
            )
            val text = backupJson.encodeToString(data)
            withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
                    it.write(text.toByteArray())
                } ?: error("no output stream")
            }
        }.isSuccess
        onResult(ok)
    }

    fun importBackup(uri: Uri, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        val ok = runCatching {
            val text = withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() } ?: error("no input stream")
            }
            val data = backupJson.decodeFromString<BackupData>(text)

            workoutRepository.replaceAll(
                data.sessions.map {
                    WorkoutSession(
                        exerciseType = it.exerciseType,
                        repCount = it.repCount,
                        goodReps = it.goodReps.coerceIn(0, it.repCount),
                        startedAt = it.startedAt,
                        durationMs = it.durationMs,
                    )
                },
            )
            planRepository.deleteAll()
            data.plans.forEach { p ->
                planRepository.savePlan(
                    id = 0L,
                    name = p.name,
                    exercises = p.exercises.sortedBy { it.position }.map {
                        DraftExercise(it.exerciseType, it.targetReps, it.targetSets)
                    },
                )
            }
            with(data.settings) {
                settingsRepository.setSoundsEnabled(soundsEnabled)
                settingsRepository.setCountdownVoice(countdownVoice)
                settingsRepository.setRepAnnouncement(
                    RepAnnouncementMode.entries.find { it.name == repAnnouncement }
                        ?: RepAnnouncementMode.EVERY_REP,
                )
                settingsRepository.setTrackingLostBell(trackingLostBell)
                settingsRepository.setTrackingRegainedChime(trackingRegainedChime)
                settingsRepository.setSetCelebration(setCelebration)
                settingsRepository.setVoiceControl(voiceControl)
                settingsRepository.setCameraFacing(
                    CameraFacing.entries.find { it.name == cameraFacing } ?: CameraFacing.BACK,
                )
                settingsRepository.setAccentTheme(
                    AccentTheme.entries.find { it.name == accentTheme } ?: AccentTheme.EMERALD,
                )
                // CUSTOM can't restore (the image file isn't in the backup) → fall back.
                val bg = BackgroundStyle.entries.find { it.name == backgroundStyle }
                    ?: BackgroundStyle.GYM_EMERALD
                settingsRepository.setBackgroundStyle(
                    if (bg == BackgroundStyle.CUSTOM) BackgroundStyle.GYM_EMERALD else bg,
                )
                settingsRepository.setFormFeedback(formFeedback)
                settingsRepository.setStrictForm(strictForm)
                settingsRepository.setFormSensitivity(
                    FormSensitivity.entries.find { it.name == formSensitivity }
                        ?: FormSensitivity.STANDARD,
                )
            }
            profileRepository.setDisplayName(data.profile.displayName)
            profileRepository.setWeightUnit(
                WeightUnit.entries.find { it.name == data.profile.weightUnit } ?: WeightUnit.KG,
            )
        }.isSuccess
        onResult(ok)
    }

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
            // A plan's timed set starts holding as soon as the countdown clears.
            if (_selectedExercise.value?.timed == true) beginTimedHold()
        }
    }

    private companion object {
        const val COUNTDOWN_SECONDS = 3
        const val GO_DISPLAY_MS = 700L
        const val TIMER_TICK_MS = 100L
        const val REP_FEEDBACK_MS = 1_400L
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
