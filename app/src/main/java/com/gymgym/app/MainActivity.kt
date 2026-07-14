package com.gymgym.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gymgym.app.ui.CameraScreen
import com.gymgym.app.ui.Exercise
import com.gymgym.app.ui.ExerciseSelectScreen
import com.gymgym.app.ui.HistoryScreen
import com.gymgym.app.ui.MainViewModel
import com.gymgym.app.ui.ProfileScreen
import com.gymgym.app.ui.SettingsScreen
import com.gymgym.app.ui.StatsScreen
import com.gymgym.app.ui.theme.GymGymTheme

private object Routes {
    const val HOME = "home"
    const val CAMERA = "camera"
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
    var pendingExercise by remember { mutableStateOf<Exercise?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
        val pending = pendingExercise
        if (granted && pending != null) {
            viewModel.selectExercise(pending)
            navController.navigate(Routes.CAMERA)
        }
        pendingExercise = null
    }

    fun startExercise(exercise: Exercise) {
        if (hasCameraPermission) {
            viewModel.selectExercise(exercise)
            navController.navigate(Routes.CAMERA)
        } else {
            pendingExercise = exercise
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val profile by viewModel.profile.collectAsState()
            ExerciseSelectScreen(
                greeting = profile.displayName,
                onExerciseSelected = ::startExercise,
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
                        viewModel.exitToSelection()
                        navController.popBackStack()
                    },
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
