package com.gymgym.app

import android.app.Application
import com.gymgym.app.data.GymGymDatabase
import com.gymgym.app.data.PlanRepository
import com.gymgym.app.data.WorkoutRepository

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
}

class GymGymApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
