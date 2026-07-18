package com.gymgym.app

import com.gymgym.app.R

import android.Manifest
import android.app.Activity
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
import com.gymgym.app.data.PlanWithExercises
import com.gymgym.app.ui.AppBackground
import com.gymgym.app.ui.CameraScreen
import com.gymgym.app.ui.Exercise
import com.gymgym.app.ui.ExerciseSelectScreen
import com.gymgym.app.ui.HistoryScreen
import com.gymgym.app.ui.MainViewModel
import com.gymgym.app.ui.PlanEditScreen
import com.gymgym.app.ui.PlanListScreen
import com.gymgym.app.ui.ProfileScreen
import com.gymgym.app.ui.RecordingsScreen
import com.gymgym.app.ui.SessionDetailScreen
import com.gymgym.app.ui.SettingsScreen
import com.gymgym.app.ui.StatsScreen
import com.gymgym.app.ui.theme.GymGymTheme

private object Routes {
    const val HOME = "home"
    const val CAMERA = "camera"
    const val PLANS = "plans"
    const val PLAN_EDIT = "plan_edit"
    const val HISTORY = "history"
    const val SESSION = "session"
    const val STATS = "stats"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val RECORDINGS = "recordings"
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val appSettings by viewModel.soundSettings.collectAsState()
            GymGymTheme(accent = appSettings.accentTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(viewModel)
                }
            }
        }
        // Consent + ad preload, deferred until the window/content exist so the
        // consent flow can't touch a not-yet-attached view (no-op on paid).
        window.decorView.post {
            (application as GymGymApp).container.adManager.warmUp(this)
        }
    }
}

@Composable
private fun AppRoot(viewModel: MainViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()

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

    fun startExercise(exercise: Exercise) = requireCameraThen {
        adManager.onWorkoutOpen(activity) {
            viewModel.selectExercise(exercise)
            navController.navigate(Routes.CAMERA)
        }
    }

    fun startPlan(plan: PlanWithExercises) = requireCameraThen {
        adManager.onWorkoutOpen(activity) {
            viewModel.startPlan(plan)
            navController.navigate(Routes.CAMERA)
        }
    }

    fun startAuto() = requireCameraThen {
        adManager.onWorkoutOpen(activity) {
            viewModel.startAuto()
            navController.navigate(Routes.CAMERA)
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
            ExerciseSelectScreen(
                greeting = profile.displayName,
                onExerciseSelected = ::startExercise,
                onAutoDetect = ::startAuto,
                onOpenPlans = { navController.navigate(Routes.PLANS) },
                onOpenRecordings = { navController.navigate(Routes.RECORDINGS) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenStats = { navController.navigate(Routes.STATS) },
                onOpenProfile = { navController.navigate(Routes.PROFILE) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.CAMERA) {
            val exercise by viewModel.selectedExercise.collectAsState()
            val autoDetecting by viewModel.autoDetecting.collectAsState()
            if (exercise != null || autoDetecting) {
                CameraScreen(
                    exercise = exercise,
                    viewModel = viewModel,
                    onExit = {
                        viewModel.stopSession()
                        navController.popBackStack()
                    },
                    onFinished = { navController.popBackStack() },
                )
            }
        }
        composable(Routes.PLANS) {
            val plans by viewModel.plans.collectAsState()
            PlanListScreen(
                plans = plans,
                onRun = ::startPlan,
                onEdit = { id -> navController.navigate("${Routes.PLAN_EDIT}/$id") },
                onNew = { navController.navigate("${Routes.PLAN_EDIT}/0") },
                onDelete = viewModel::deletePlan,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            "${Routes.PLAN_EDIT}/{planId}",
            arguments = listOf(navArgument("planId") { type = NavType.LongType }),
        ) { entry ->
            val planId = entry.arguments?.getLong("planId") ?: 0L
            val plans by viewModel.plans.collectAsState()
            val existing = plans.find { it.plan.id == planId }
            // For an edit, wait until the plan has loaded before seeding the form.
            if (planId == 0L || existing != null) {
                PlanEditScreen(
                    existing = existing,
                    onSave = { id, name, exercises ->
                        viewModel.savePlan(id, name, exercises)
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() },
                )
            }
        }
        composable(Routes.HISTORY) {
            val sessions by viewModel.history.collectAsState()
            HistoryScreen(
                sessions = sessions,
                onOpenSession = { id -> navController.navigate("${Routes.SESSION}/$id") },
                onBack = { navController.popBackStack() },
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
        composable(Routes.STATS) {
            val sessions by viewModel.history.collectAsState()
            StatsScreen(
                sessions = sessions,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.PROFILE) {
            val profile by viewModel.profile.collectAsState()
            val ctx = LocalContext.current
            ProfileScreen(
                profile = profile,
                onDisplayName = viewModel::setDisplayName,
                onWeightUnit = viewModel::setWeightUnit,
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
        composable(Routes.RECORDINGS) {
            RecordingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            val soundSettings by viewModel.soundSettings.collectAsState()
            SettingsScreen(
                settings = soundSettings,
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
