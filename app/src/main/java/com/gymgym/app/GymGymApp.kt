package com.gymgym.app

import android.app.Application
import com.gymgym.app.data.GymGymDatabase
import com.gymgym.app.data.WorkoutRepository

/**
 * Application-scoped service locator. Deliberately hand-rolled instead of a DI
 * framework — at this size a couple of lazy singletons are enough.
 */
class AppContainer(app: Application) {
    val workoutRepository: WorkoutRepository by lazy {
        WorkoutRepository(GymGymDatabase.get(app).workoutDao())
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
