package com.gymgym.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.gymgym.app.ui.CameraScreen
import com.gymgym.app.ui.Exercise
import com.gymgym.app.ui.ExerciseSelectScreen
import com.gymgym.app.ui.HistoryScreen
import com.gymgym.app.ui.MainViewModel
import com.gymgym.app.ui.PlanEditScreen
import com.gymgym.app.ui.PlanListScreen
import com.gymgym.app.ui.ProfileScreen
import com.gymgym.app.ui.SettingsScreen
import com.gymgym.app.ui.StatsScreen
import com.gymgym.app.ui.theme.GymGymTheme

private object Routes {
    const val HOME = "home"
    const val CAMERA = "camera"
    const val PLANS = "plans"
    const val PLAN_EDIT = "plan_edit"
    const val HISTORY = "history"
    const val STATS = "stats"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GymGymTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(viewModel)
                }
            }
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

    fun startExercise(exercise: Exercise) = requireCameraThen {
        viewModel.selectExercise(exercise)
        navController.navigate(Routes.CAMERA)
    }

    fun startPlan(plan: PlanWithExercises) = requireCameraThen {
        viewModel.startPlan(plan)
        navController.navigate(Routes.CAMERA)
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val profile by viewModel.profile.collectAsState()
            ExerciseSelectScreen(
                greeting = profile.displayName,
                onExerciseSelected = ::startExercise,
                onOpenPlans = { navController.navigate(Routes.PLANS) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenStats = { navController.navigate(Routes.STATS) },
                onOpenProfile = { navController.navigate(Routes.PROFILE) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.CAMERA) {
            val exercise by viewModel.selectedExercise.collectAsState()
            exercise?.let { ex ->
                CameraScreen(
                    exercise = ex,
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
            HistoryScreen(sessions = sessions, onBack = { navController.popBackStack() })
        }
        composable(Routes.STATS) {
            val stats by viewModel.stats.collectAsState()
            val sessions by viewModel.history.collectAsState()
            StatsScreen(
                stats = stats,
                recentSessions = sessions,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.PROFILE) {
            val profile by viewModel.profile.collectAsState()
            ProfileScreen(
                profile = profile,
                onDisplayName = viewModel::setDisplayName,
                onWeightUnit = viewModel::setWeightUnit,
                onBack = { navController.popBackStack() },
            )
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
                onBack = { navController.popBackStack() },
            )
        }
    }
}
