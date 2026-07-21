package com.gymgym.app.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        WorkoutSession::class,
        PlanEntity::class,
        CycleEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        CustomExercise::class,
        BodyMeasurement::class,
        CompletedWorkout::class,
        CompletedExercise::class,
        WorkoutProgress::class,
        AchievementUnlock::class,
    ],
    version = 11,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3), // adds workout_session.goodReps (default 0)
        AutoMigration(from = 3, to = 4), // adds custom_exercise table
        AutoMigration(from = 5, to = 6), // adds body_measurement table
        AutoMigration(from = 6, to = 7), // adds completed_workout + completed_exercise
        AutoMigration(from = 7, to = 8), // adds workout_progress table
        AutoMigration(from = 8, to = 9), // adds completed_exercise.targetReps/targetSets
        AutoMigration(from = 9, to = 10), // adds completed_workout.planName/cycleName
        AutoMigration(from = 10, to = 11), // adds achievement_unlock table
    ],
)
abstract class GymGymDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun planDao(): PlanDao
    abstract fun customExerciseDao(): CustomExerciseDao
    abstract fun bodyMeasurementDao(): BodyMeasurementDao
    abstract fun completedWorkoutDao(): CompletedWorkoutDao
    abstract fun workoutProgressDao(): WorkoutProgressDao
    abstract fun achievementUnlockDao(): AchievementUnlockDao

    companion object {
        @Volatile private var instance: GymGymDatabase? = null

        fun get(context: Context): GymGymDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GymGymDatabase::class.java,
                    "gymgym.db",
                ).addMigrations(MIGRATION_4_5).build().also { instance = it }
            }
    }
}
