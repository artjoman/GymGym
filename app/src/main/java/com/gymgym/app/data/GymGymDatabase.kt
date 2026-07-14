package com.gymgym.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WorkoutSession::class], version = 1, exportSchema = true)
abstract class GymGymDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile private var instance: GymGymDatabase? = null

        fun get(context: Context): GymGymDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GymGymDatabase::class.java,
                    "gymgym.db",
                ).build().also { instance = it }
            }
    }
}
