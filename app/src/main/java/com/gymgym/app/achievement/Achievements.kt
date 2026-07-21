package com.gymgym.app.achievement

import androidx.annotation.StringRes
import com.gymgym.app.R
import com.gymgym.app.data.BodyMeasurement
import com.gymgym.app.data.CompletedWorkoutRepository
import com.gymgym.app.data.CompletedWorkoutWithExercises
import com.gymgym.app.exercise.Category
import com.gymgym.app.exercise.ExerciseCatalog
import java.util.Calendar
import java.util.TimeZone

/** A badge the user can earn. [target] drives the "42/100" progress on locked badges. */
data class AchievementDef(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val descRes: Int,
    val target: Int,
)

/** A definition plus the user's current standing against it. */
data class AchievementState(
    val def: AchievementDef,
    val progress: Int,
    val unlockedAt: Long? = null,
) {
    val earned: Boolean get() = progress >= def.target
}

/**
 * Achievement rules, evaluated purely from stored history — no I/O — so they are
 * directly unit-testable, in the same style as `cycle/CycleSummaries`.
 *
 * Counting rules (judgement calls, fixed here so they stay consistent):
 * - **Reps** are `completed_exercise.reps` from completed workouts only. AI
 *   library tests write a `WorkoutSession` but no `completed_workout`, and
 *   `logSession` also writes a session per exercise *inside* a plan run — so
 *   summing sessions as well would double-count.
 * - **A 100% workout** uses the same hierarchical maths as the rest of the app
 *   (`exercisePercent` per exercise, averaged), not a reps-pooled figure.
 * - **Streaks** bucket timestamps by **local calendar day**, so two sessions on
 *   one day count once and 23:50 → 00:10 is a two-day streak.
 * - **Muscle groups** come from the catalog; custom exercises have no category
 *   and so count toward totals but never toward muscle-group badges.
 */
object Achievements {

    // --- Catalogue -----------------------------------------------------------

    val ALL: List<AchievementDef> = listOf(
        AchievementDef("first_workout", R.string.ach_first_workout, R.string.ach_first_workout_desc, 1),
        AchievementDef("workouts_10", R.string.ach_workouts_10, R.string.ach_workouts_10_desc, 10),
        AchievementDef("workouts_100", R.string.ach_workouts_100, R.string.ach_workouts_100_desc, 100),
        AchievementDef("workouts_1000", R.string.ach_workouts_1000, R.string.ach_workouts_1000_desc, 1000),
        AchievementDef("streak_3", R.string.ach_streak_3, R.string.ach_streak_3_desc, 3),
        AchievementDef("streak_7", R.string.ach_streak_7, R.string.ach_streak_7_desc, 7),
        AchievementDef("streak_30", R.string.ach_streak_30, R.string.ach_streak_30_desc, 30),
        AchievementDef("reps_1000", R.string.ach_reps_1000, R.string.ach_reps_1000_desc, 1000),
        AchievementDef("reps_10000", R.string.ach_reps_10000, R.string.ach_reps_10000_desc, 10_000),
        AchievementDef("legday_3", R.string.ach_legday_3, R.string.ach_legday_3_desc, 3),
        AchievementDef("legday_25", R.string.ach_legday_25, R.string.ach_legday_25_desc, 25),
        AchievementDef("full_body", R.string.ach_full_body, R.string.ach_full_body_desc, Category.entries.size),
        AchievementDef("perfect_1", R.string.ach_perfect_1, R.string.ach_perfect_1_desc, 1),
        AchievementDef("perfect_10", R.string.ach_perfect_10, R.string.ach_perfect_10_desc, 10),
        AchievementDef("goodreps_500", R.string.ach_goodreps_500, R.string.ach_goodreps_500_desc, 500),
        AchievementDef("cycle_1", R.string.ach_cycle_1, R.string.ach_cycle_1_desc, 1),
        AchievementDef("cycle_10", R.string.ach_cycle_10, R.string.ach_cycle_10_desc, 10),
        AchievementDef("early_bird", R.string.ach_early_bird, R.string.ach_early_bird_desc, 10),
        AchievementDef("night_owl", R.string.ach_night_owl, R.string.ach_night_owl_desc, 10),
        AchievementDef("measure_streak_7", R.string.ach_measure_7, R.string.ach_measure_7_desc, 7),
    )

    // --- Evaluation ----------------------------------------------------------

    /**
     * Current progress for every achievement. [unlocks] carries previously
     * recorded unlock times so an already-earned badge keeps its original date.
     */
    fun evaluate(
        completed: List<CompletedWorkoutWithExercises>,
        measurements: List<BodyMeasurement>,
        completedCycles: Int,
        unlocks: Map<String, Long> = emptyMap(),
        zone: TimeZone = TimeZone.getDefault(),
    ): List<AchievementState> {
        val workouts = completed.size
        val totalReps = completed.sumOf { w -> w.exercises.sumOf { it.reps } }
        val goodReps = completed.sumOf { w -> w.exercises.sumOf { it.goodReps } }

        val workoutDays = completed.map { it.workout.startedAt }
        val legDays = completed.filter { it.hasCategory(Category.LEGS) }.map { it.workout.startedAt }

        val perfect = completed.count { it.isPerfect() }
        val categories = completed
            .flatMap { w -> w.exercises.mapNotNull { ExerciseCatalog.byId(it.exerciseRef)?.category } }
            .toSet()

        val hours = completed.map { hourOfDay(it.workout.startedAt, zone) }

        val progress = mapOf(
            "first_workout" to workouts,
            "workouts_10" to workouts,
            "workouts_100" to workouts,
            "workouts_1000" to workouts,
            "streak_3" to longestDayStreak(workoutDays, zone),
            "streak_7" to longestDayStreak(workoutDays, zone),
            "streak_30" to longestDayStreak(workoutDays, zone),
            "reps_1000" to totalReps,
            "reps_10000" to totalReps,
            "legday_3" to longestDayStreak(legDays, zone),
            "legday_25" to legDays.size,
            "full_body" to categories.size,
            "perfect_1" to perfect,
            "perfect_10" to perfect,
            "goodreps_500" to goodReps,
            "cycle_1" to completedCycles,
            "cycle_10" to completedCycles,
            "early_bird" to hours.count { it < 7 },
            "night_owl" to hours.count { it >= 21 },
            "measure_streak_7" to longestDayStreak(measurements.map { it.loggedAt }, zone),
        )

        return ALL.map { def ->
            AchievementState(
                def = def,
                progress = (progress[def.id] ?: 0).coerceAtMost(def.target),
                unlockedAt = unlocks[def.id],
            )
        }
    }

    /**
     * Longest run of consecutive **local calendar days** present in [times].
     * Several timestamps on one day count once.
     */
    fun longestDayStreak(times: List<Long>, zone: TimeZone = TimeZone.getDefault()): Int {
        if (times.isEmpty()) return 0
        val days = times.map { dayIndex(it, zone) }.distinct().sorted()
        var best = 1
        var run = 1
        for (i in 1 until days.size) {
            run = if (days[i] == days[i - 1] + 1) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }

    // --- internals -----------------------------------------------------------

    /** Days since the epoch in [zone] — the local calendar day this instant falls on. */
    private fun dayIndex(millis: Long, zone: TimeZone): Long {
        val cal = Calendar.getInstance(zone).apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Offset-aware: adding the zone offset before flooring keeps DST days whole.
        return Math.floorDiv(cal.timeInMillis + zone.getOffset(cal.timeInMillis).toLong(), 86_400_000L)
    }

    private fun hourOfDay(millis: Long, zone: TimeZone): Int =
        Calendar.getInstance(zone).apply { timeInMillis = millis }.get(Calendar.HOUR_OF_DAY)

    private fun CompletedWorkoutWithExercises.hasCategory(category: Category): Boolean =
        exercises.any { ExerciseCatalog.byId(it.exerciseRef)?.category == category }

    /** Every exercise hit its planned reps — measured the same way the app shows it. */
    private fun CompletedWorkoutWithExercises.isPerfect(): Boolean {
        val scored = exercises.filter { it.targetReps * it.targetSets > 0 }
        if (scored.isEmpty()) return false
        val percents = scored.map {
            CompletedWorkoutRepository.exercisePercent(it.reps, it.targetReps * it.targetSets)
        }
        return CompletedWorkoutRepository.averagePercent(percents) >= 100
    }
}
