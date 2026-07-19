package com.gymgym.app

import android.app.Application
import android.content.Context
import com.gymgym.app.ads.AdManager
import com.gymgym.app.ads.provideAdManager
import com.gymgym.app.data.BodyMeasurementRepository
import com.gymgym.app.data.CompletedWorkoutRepository
import com.gymgym.app.data.CustomExerciseRepository
import com.gymgym.app.data.GymGymDatabase
import com.gymgym.app.data.PlanRepository
import com.gymgym.app.data.WorkoutProgressRepository
import com.gymgym.app.data.WorkoutRepository
import com.gymgym.app.settings.AppLocale

/**
 * Application-scoped service locator. Deliberately hand-rolled instead of a DI
 * framework — at this size a couple of lazy singletons are enough.
 */
class AppContainer(app: Application) {
    private val database by lazy { GymGymDatabase.get(app) }

    val workoutRepository: WorkoutRepository by lazy {
        WorkoutRepository(database.workoutDao())
    }

    val planRepository: PlanRepository by lazy {
        PlanRepository(database.planDao())
    }

    val customExerciseRepository: CustomExerciseRepository by lazy {
        CustomExerciseRepository(database.customExerciseDao())
    }

    val bodyMeasurementRepository: BodyMeasurementRepository by lazy {
        BodyMeasurementRepository(database.bodyMeasurementDao())
    }

    val completedWorkoutRepository: CompletedWorkoutRepository by lazy {
        CompletedWorkoutRepository(database.completedWorkoutDao())
    }

    val workoutProgressRepository: WorkoutProgressRepository by lazy {
        WorkoutProgressRepository(database.workoutProgressDao())
    }

    // Real ads on the free flavor; a no-op on paid (see provideAdManager).
    val adManager: AdManager by lazy { provideAdManager(app) }
}

class GymGymApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocale.attach(base))
    }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        com.gymgym.app.notify.Reminders.ensureChannel(this)
    }
}
