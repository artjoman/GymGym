package com.gymgym.app

import com.gymgym.app.R
import com.gymgym.app.settings.AppLocale

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gymgym.app.ui.AppBackground
import com.gymgym.app.ui.CameraScreen
import com.gymgym.app.data.PlanWithCycles
import com.gymgym.app.exercise.ExerciseRef
import com.gymgym.app.ui.ExerciseLibraryScreen
import com.gymgym.app.ui.ExerciseSelectScreen
import com.gymgym.app.ui.ExpertSupportScreen
import com.gymgym.app.ui.NextMissionScreen
import com.gymgym.app.notify.Reminders
import com.gymgym.app.ui.MainViewModel
import com.gymgym.app.ui.PlanEditScreen
import com.gymgym.app.ui.PlanListScreen
import com.gymgym.app.ui.ProfileScreen
import com.gymgym.app.ui.SessionDetailScreen
import com.gymgym.app.ui.SettingsScreen
import com.gymgym.app.ui.StatisticsScreen
import com.gymgym.app.ui.theme.GymGymTheme

private object Routes {
    const val HOME = "home"
    const val CAMERA = "camera"
    const val PLANS = "plans"
    const val PLAN_EDIT = "plan_edit"
    const val STATISTICS = "statistics"
    const val SESSION = "session"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val RECORDINGS = "recordings"
    const val LIBRARY = "library"
    const val PROGRAMS = "programs"
    const val MISSION = "mission"
    const val EXPERT = "expert"
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    /** Screen a tapped notification asked us to open; consumed once by AppRoot. */
    private var launchDestination by mutableStateOf<String?>(null)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.attach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        launchDestination = intent?.getStringExtra(Reminders.EXTRA_DESTINATION)
        setContent {
            val appSettings by viewModel.soundSettings.collectAsState()
            GymGymTheme(accent = appSettings.accentTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(
                        viewModel = viewModel,
                        launchDestination = launchDestination,
                        onDestinationHandled = { launchDestination = null },
                    )
                }
            }
        }
        // Consent + ad preload, deferred until the window/content exist so the
        // consent flow can't touch a not-yet-attached view (no-op on paid).
        window.decorView.post {
            (application as GymGymApp).container.adManager.warmUp(this)
        }
    }

    // A notification tapped while the app is already running arrives here.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchDestination = intent.getStringExtra(Reminders.EXTRA_DESTINATION)
    }
}

@Composable
private fun AppRoot(
    viewModel: MainViewModel,
    launchDestination: String?,
    onDestinationHandled: () -> Unit,
) {
    val context = LocalContext.current
    val navController = rememberNavController()

    // A tapped notification can ask us to jump straight to a screen. Map the
    // requested destination to a route (unknown/invalid → stay on Home), then
    // consume it once so a Back press or recomposition doesn't re-trigger. This
    // runs whether the app was cold-started (onCreate) or resumed (onNewIntent).
    LaunchedEffect(launchDestination) {
        val dest = launchDestination ?: return@LaunchedEffect
        val route = when (dest) {
            Reminders.DEST_PROFILE -> Routes.PROFILE
            Reminders.DEST_MISSION -> Routes.MISSION
            Reminders.DEST_PLANS -> Routes.PLANS
            else -> null // unknown/invalid → fall back to Home (already the root)
        }
        if (route != null) {
            navController.navigate(route) {
                // Keep Home as the single root; don't stack duplicate destinations.
                popUpTo(Routes.HOME)
                launchSingleTop = true
            }
        }
        onDestinationHandled()
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var pendingStart by remember { mutableStateOf<(() -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
        if (granted) pendingStart?.invoke()
        pendingStart = null
    }

    fun requireCameraThen(action: () -> Unit) {
        if (hasCameraPermission) {
            action()
        } else {
            pendingStart = action
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Ads are gated here — at "open a workout", never once counting has started.
    val activity = context as Activity
    val adManager = remember { (activity.application as GymGymApp).container.adManager }

    // Notification permission (Android 13+) + reminder (re)scheduling.
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val dashboardForReminders by viewModel.dashboard.collectAsState()
    val reminderSettingsState by viewModel.reminderSettings.collectAsState()
    LaunchedEffect(dashboardForReminders.nextMission?.plannedDate, reminderSettingsState) {
        viewModel.rescheduleReminders()
    }

    // Library "Test"/"Rec": AI-counted moves run their counter, everything else
    // runs a one-set manual session — resolved inside the ViewModel from the ref.
    // fromLibrary=true so the run screen shows "Change exercise" and returns here.
    fun startTest(ref: String) = requireCameraThen {
        adManager.onWorkoutOpen(activity) {
            viewModel.startExerciseTest(ref)
            navController.navigate("${Routes.CAMERA}?fromLibrary=true")
        }
    }

    // Auto-detect is launched from the Exercise library, so it returns there too.
    fun startAuto() = requireCameraThen {
        adManager.onWorkoutOpen(activity) {
            viewModel.startAuto()
            navController.navigate("${Routes.CAMERA}?fromLibrary=true")
        }
    }

    fun startPlan(plan: PlanWithCycles) {
        // Run the first workout that has an AI-counted/timed exercise.
        val workout = plan.orderedCycles
            .flatMap { it.orderedWorkouts }
            .firstOrNull { w -> w.exercises.any { ExerciseRef.counter(it.exerciseRef) != null } }
            ?: return
        requireCameraThen {
            adManager.onWorkoutOpen(activity) {
                viewModel.startWorkout(workout, plan.plan.id, plan.plan.name)
                navController.navigate(Routes.CAMERA)
            }
        }
    }

    /** Start a specific workout (by id) from the active plan — used by Next Mission. */
    fun startWorkoutById(workoutId: Long) {
        val plan = viewModel.activePlan.value ?: return
        val workout = plan.orderedCycles
            .flatMap { it.orderedWorkouts }
            .firstOrNull { it.workout.id == workoutId } ?: return
        requireCameraThen {
            adManager.onWorkoutOpen(activity) {
                viewModel.startWorkout(workout, plan.plan.id, plan.plan.name)
                navController.navigate(Routes.CAMERA)
            }
        }
    }

    val settings by viewModel.soundSettings.collectAsState()
    AppBackground(
        style = settings.backgroundStyle,
        customPath = settings.customBackgroundPath,
    ) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(Routes.HOME) {
            val profile by viewModel.profile.collectAsState()
            val homeCycles by viewModel.homeCycles.collectAsState()
            ExerciseSelectScreen(
                greeting = profile.displayName,
                homeCycles = homeCycles,
                onOpenMission = { navController.navigate(Routes.MISSION) },
                // Expand the last cycle's record when arriving from that card.
                onOpenLastCycle = { navController.navigate("${Routes.STATISTICS}?tab=2&expandFirst=true") },
                onOpenLibrary = { navController.navigate(Routes.LIBRARY) },
                onOpenPlans = { navController.navigate(Routes.PLANS) },
                onOpenStatistics = { navController.navigate(Routes.STATISTICS) },
                onOpenProfile = { navController.navigate(Routes.PROFILE) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenExpert = { navController.navigate(Routes.EXPERT) },
            )
        }
        composable(
            "${Routes.CAMERA}?fromLibrary={fromLibrary}",
            arguments = listOf(
                navArgument("fromLibrary") { type = NavType.BoolType; defaultValue = false },
            ),
        ) { entry ->
            val fromLibrary = entry.arguments?.getBoolean("fromLibrary") ?: false
            val exercise by viewModel.selectedExercise.collectAsState()
            val autoDetecting by viewModel.autoDetecting.collectAsState()
            val manualActive by viewModel.manualActive.collectAsState()
            val planProgress by viewModel.planProgress.collectAsState()
            // A library test returns to the Exercise library; a workout run always
            // ends on Home (the central dashboard) — same as Skip.
            val exitRoute = if (fromLibrary) Routes.LIBRARY else Routes.HOME
            if (exercise != null || autoDetecting || manualActive != null || planProgress != null) {
                CameraScreen(
                    exercise = exercise,
                    viewModel = viewModel,
                    fromLibrary = fromLibrary,
                    onExit = {
                        viewModel.stopSession()
                        navController.popBackStack(exitRoute, inclusive = false)
                    },
                    onFinished = {
                        navController.popBackStack(exitRoute, inclusive = false)
                    },
                )
            }
        }
        composable(Routes.LIBRARY) {
            val customExercises by viewModel.customExercises.collectAsState()
            ExerciseLibraryScreen(
                customExercises = customExercises,
                onTest = ::startTest,
                onAutoDetect = ::startAuto,
                onAddCustom = viewModel::addCustomExercise,
                onDeleteCustom = viewModel::deleteCustomExercise,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.MISSION) {
            val dashboard by viewModel.dashboard.collectAsState()
            val homeCycles by viewModel.homeCycles.collectAsState()
            val customExercises by viewModel.customExercises.collectAsState()
            val customNames = customExercises.associate { ExerciseRef.forCustom(it.id) to it.name }
            NextMissionScreen(
                dashboard = dashboard,
                nextWorkout = homeCycles.currentCycle?.detail,
                customNames = customNames,
                onStart = { id ->
                    startWorkoutById(id)
                },
                onSwap = { id -> startWorkoutById(id) },
                onSkip = { id ->
                    viewModel.skipMission(id)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.PLANS) {
            val ctx = LocalContext.current
            val plans by viewModel.plans.collectAsState()
            PlanListScreen(
                plans = plans,
                onEdit = { id -> navController.navigate("${Routes.PLAN_EDIT}/$id") },
                onNew = { navController.navigate("${Routes.PLAN_EDIT}/0") },
                onDelete = viewModel::deletePlan,
                onSetActive = viewModel::setActivePlan,
                onStart = ::startPlan,
                onUseProgram = { program ->
                    // Activating a preset program creates/assigns it as the active plan.
                    viewModel.useProgram(program)
                    Toast.makeText(
                        ctx,
                        ctx.getString(R.string.programs_activated),
                        Toast.LENGTH_SHORT,
                    ).show()
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            "${Routes.PLAN_EDIT}/{planId}",
            arguments = listOf(navArgument("planId") { type = NavType.LongType }),
        ) { entry ->
            val planId = entry.arguments?.getLong("planId") ?: 0L
            val plans by viewModel.plans.collectAsState()
            val customExercises by viewModel.customExercises.collectAsState()
            val existing = plans.find { it.plan.id == planId }
            // For an edit, wait until the plan has loaded before seeding the form.
            if (planId == 0L || existing != null) {
                PlanEditScreen(
                    existing = existing,
                    customExercises = customExercises,
                    onSave = { id, draft ->
                        viewModel.savePlan(id, draft)
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() },
                )
            }
        }
        composable(
            "${Routes.STATISTICS}?tab={tab}&expandFirst={expandFirst}",
            arguments = listOf(
                navArgument("tab") { type = NavType.IntType; defaultValue = 0 },
                navArgument("expandFirst") { type = NavType.BoolType; defaultValue = false },
            ),
        ) { entry ->
            val initialTab = entry.arguments?.getInt("tab") ?: 0
            val expandFirst = entry.arguments?.getBoolean("expandFirst") ?: false
            val sessions by viewModel.history.collectAsState()
            val completedWorkouts by viewModel.completedWorkouts.collectAsState()
            val bodyMeasurements by viewModel.bodyMeasurements.collectAsState()
            val customExercises by viewModel.customExercises.collectAsState()
            val cycles by viewModel.cycleSummaries.collectAsState()
            val customNames = customExercises.associate { ExerciseRef.forCustom(it.id) to it.name }
            StatisticsScreen(
                sessions = sessions,
                completedWorkouts = completedWorkouts,
                bodyMeasurements = bodyMeasurements,
                onOpenSession = { id -> navController.navigate("${Routes.SESSION}/$id") },
                onBack = { navController.popBackStack() },
                customNames = customNames,
                cycles = cycles,
                initialTab = initialTab,
                expandLastCycle = expandFirst,
            )
        }
        composable(
            "${Routes.SESSION}/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
        ) { entry ->
            val sessionId = entry.arguments?.getLong("sessionId") ?: -1L
            val sessions by viewModel.history.collectAsState()
            SessionDetailScreen(
                session = sessions.find { it.id == sessionId },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.PROFILE) {
            val profile by viewModel.profile.collectAsState()
            val bodyMeasurements by viewModel.bodyMeasurements.collectAsState()
            val ctx = LocalContext.current
            ProfileScreen(
                profile = profile,
                bodyMeasurements = bodyMeasurements,
                onDisplayName = viewModel::setDisplayName,
                onWeightUnit = viewModel::setWeightUnit,
                onLengthUnit = viewModel::setLengthUnit,
                onTrainingMode = viewModel::setTrainingMode,
                onWorkoutDays = viewModel::setWorkoutDays,
                onWorkoutTimeoutSeconds = viewModel::setWorkoutTimeoutSeconds,
                onSetTimeoutSeconds = viewModel::setSetTimeoutSeconds,
                onExerciseTimeoutSeconds = viewModel::setExerciseTimeoutSeconds,
                onLogMeasurement = viewModel::logMeasurement,
                onExport = { uri ->
                    viewModel.exportBackup(uri) { ok ->
                        Toast.makeText(
                            ctx,
                            ctx.getString(if (ok) R.string.toast_backup_saved else R.string.toast_export_failed),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
                onImport = { uri ->
                    viewModel.importBackup(uri) { ok ->
                        Toast.makeText(
                            ctx,
                            ctx.getString(if (ok) R.string.toast_data_imported else R.string.toast_import_failed),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.EXPERT) {
            ExpertSupportScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            val soundSettings by viewModel.soundSettings.collectAsState()
            val reminders by viewModel.reminderSettings.collectAsState()
            SettingsScreen(
                settings = soundSettings,
                reminders = reminders,
                onUpcomingReminder = viewModel::setUpcomingReminder,
                onMissedReminder = viewModel::setMissedReminder,
                onCycleProgressReminder = viewModel::setCycleProgressReminder,
                onBodyReminder = viewModel::setBodyReminder,
                onSoundsEnabled = viewModel::setSoundsEnabled,
                onCountdownVoice = viewModel::setCountdownVoice,
                onRepAnnouncement = viewModel::setRepAnnouncement,
                onTrackingLostBell = viewModel::setTrackingLostBell,
                onTrackingRegainedChime = viewModel::setTrackingRegainedChime,
                onSetCelebration = viewModel::setSetCelebration,
                onVoiceControl = viewModel::setVoiceControl,
                onFormFeedback = viewModel::setFormFeedback,
                onStrictForm = viewModel::setStrictForm,
                onFormSensitivity = viewModel::setFormSensitivity,
                onAccentTheme = viewModel::setAccentTheme,
                onBackgroundStyle = viewModel::setBackgroundStyle,
                onCustomBackground = viewModel::setCustomBackground,
                onBack = { navController.popBackStack() },
            )
        }
    }
    }
}
