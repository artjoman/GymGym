package com.gymgym.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v4 → v5: replace the flat plan model (plan + plan_exercise) with the
 * Plan → Cycle → Workout → WorkoutExercise hierarchy. Existing flat plans are
 * dropped (per the pre-release decision); workout history and custom exercises
 * are preserved. SQL mirrors Room's generated schema for v5 exactly.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `plan_exercise`")
        db.execSQL("DROP TABLE IF EXISTS `plan`")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `plan` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`endDate` INTEGER, " +
                "`isActive` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `cycle` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`planId` INTEGER NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`position` INTEGER NOT NULL, " +
                "FOREIGN KEY(`planId`) REFERENCES `plan`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cycle_planId` ON `cycle` (`planId`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `workout` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`cycleId` INTEGER NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`position` INTEGER NOT NULL, " +
                "`weekday` INTEGER, " +
                "FOREIGN KEY(`cycleId`) REFERENCES `cycle`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_cycleId` ON `workout` (`cycleId`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `workout_exercise` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`workoutId` INTEGER NOT NULL, " +
                "`exerciseRef` TEXT NOT NULL, " +
                "`targetReps` INTEGER NOT NULL, " +
                "`targetSets` INTEGER NOT NULL, " +
                "`targetSeconds` INTEGER, " +
                "`position` INTEGER NOT NULL, " +
                "FOREIGN KEY(`workoutId`) REFERENCES `workout`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_workout_exercise_workoutId` " +
                "ON `workout_exercise` (`workoutId`)",
        )
    }
}
