package com.gymgym.app.exercise

import androidx.annotation.StringRes
import com.gymgym.app.R
import com.gymgym.app.ui.Exercise

/**
 * The exercise library — a data-driven catalog that replaces the fixed 5-value
 * [Exercise] enum as the *source* of selectable exercises. Only the moves with a
 * non-null [CatalogExercise.counter] can be AI rep-counted today; every other
 * entry (and any user [custom][com.gymgym.app.data.CustomExercise] exercise) runs
 * in manual mode (do the set, tap FINISH, edit the rep count).
 *
 * [CatalogExercise.id] is the stable [ExerciseRef] key persisted by plans and
 * sessions. Names/categories/muscles are localized via string resources.
 */

/** Browse categories in the library (weighted variants fold into their muscle group). */
enum class Category(@StringRes val labelRes: Int) {
    LEGS(R.string.cat_legs),
    BACK(R.string.cat_back),
    CHEST(R.string.cat_chest),
    SHOULDERS(R.string.cat_shoulders),
    TRICEPS(R.string.cat_triceps),
    BICEPS(R.string.cat_biceps),
    TRAPS(R.string.cat_traps),
    ABS(R.string.cat_abs),
}

/** Muscle tags shown under each exercise. */
enum class Muscle(@StringRes val labelRes: Int) {
    LEGS(R.string.muscle_legs),
    GLUTES(R.string.muscle_glutes),
    ADDUCTORS(R.string.muscle_adductors),
    CALVES(R.string.muscle_calves),
    HAMSTRINGS(R.string.muscle_hamstrings),
    BACK(R.string.muscle_back),
    CHEST(R.string.muscle_chest),
    SHOULDERS(R.string.muscle_shoulders),
    TRICEPS(R.string.muscle_triceps),
    BICEPS(R.string.muscle_biceps),
    TRAPS(R.string.muscle_traps),
    ABS(R.string.muscle_abs),
}

data class CatalogExercise(
    val id: String,
    @StringRes val nameRes: Int,
    val category: Category,
    val muscles: List<Muscle>,
    /** The AI counter for this move, or null for a manual exercise. */
    val counter: Exercise? = null,
) {
    val isAiCounted: Boolean get() = counter != null
    val isTimed: Boolean get() = counter?.timed == true
}

object ExerciseCatalog {

    val ALL: List<CatalogExercise> = listOf(
        // --- Legs ---
        CatalogExercise("squat", R.string.exercise_squat, Category.LEGS, listOf(Muscle.LEGS, Muscle.GLUTES), Exercise.SQUAT),
        CatalogExercise("sumo_squat", R.string.ex_sumo_squat, Category.LEGS, listOf(Muscle.LEGS, Muscle.GLUTES, Muscle.ADDUCTORS)),
        CatalogExercise("weighted_squat", R.string.ex_weighted_squat, Category.LEGS, listOf(Muscle.LEGS)),
        CatalogExercise("forward_lunges", R.string.ex_forward_lunges, Category.LEGS, listOf(Muscle.LEGS, Muscle.GLUTES)),
        CatalogExercise("standing_calf_raise", R.string.ex_standing_calf_raise, Category.LEGS, listOf(Muscle.CALVES)),
        CatalogExercise("single_leg_calf_raise", R.string.ex_single_leg_calf_raise, Category.LEGS, listOf(Muscle.CALVES)),
        CatalogExercise("weighted_single_leg_calf_raise", R.string.ex_weighted_single_leg_calf_raise, Category.LEGS, listOf(Muscle.CALVES)),

        // --- Back ---
        CatalogExercise("pullup", R.string.exercise_pullup, Category.BACK, listOf(Muscle.BACK, Muscle.BICEPS), Exercise.PULLUP),
        CatalogExercise("weighted_pullup", R.string.ex_weighted_pullup, Category.BACK, listOf(Muscle.BACK, Muscle.BICEPS)),
        CatalogExercise("close_grip_pullup", R.string.ex_close_grip_pullup, Category.BACK, listOf(Muscle.BACK, Muscle.BICEPS)),
        CatalogExercise("wide_grip_pullup", R.string.ex_wide_grip_pullup, Category.BACK, listOf(Muscle.BACK)),
        CatalogExercise("deadlift", R.string.ex_deadlift, Category.BACK, listOf(Muscle.BACK, Muscle.LEGS, Muscle.TRAPS, Muscle.GLUTES, Muscle.HAMSTRINGS)),

        // --- Chest ---
        CatalogExercise("pushup", R.string.exercise_pushup, Category.CHEST, listOf(Muscle.CHEST, Muscle.TRICEPS), Exercise.PUSHUP),
        CatalogExercise("incline_pushup", R.string.ex_incline_pushup, Category.CHEST, listOf(Muscle.CHEST, Muscle.TRICEPS)),
        CatalogExercise("dumbbell_bench_press", R.string.ex_dumbbell_bench_press, Category.CHEST, listOf(Muscle.CHEST)),
        CatalogExercise("barbell_bench_press", R.string.ex_barbell_bench_press, Category.CHEST, listOf(Muscle.CHEST)),
        CatalogExercise("parallel_bar_dips", R.string.ex_parallel_bar_dips, Category.CHEST, listOf(Muscle.CHEST, Muscle.TRICEPS)),
        CatalogExercise("weighted_parallel_bar_dips", R.string.ex_weighted_parallel_bar_dips, Category.CHEST, listOf(Muscle.CHEST, Muscle.TRICEPS)),

        // --- Shoulders --- (the AI dumbbell press maps to the seated press)
        CatalogExercise("dumbbell_seated_press", R.string.exercise_dumbbell_press, Category.SHOULDERS, listOf(Muscle.SHOULDERS), Exercise.DUMBBELL_PRESS),
        CatalogExercise("dumbbell_lateral_raise", R.string.ex_dumbbell_lateral_raise, Category.SHOULDERS, listOf(Muscle.SHOULDERS)),
        CatalogExercise("dumbbell_front_raise", R.string.ex_dumbbell_front_raise, Category.SHOULDERS, listOf(Muscle.SHOULDERS)),

        // --- Triceps ---
        CatalogExercise("lying_barbell_extension", R.string.ex_lying_barbell_extension, Category.TRICEPS, listOf(Muscle.TRICEPS)),
        CatalogExercise("lying_dumbbell_extension", R.string.ex_lying_dumbbell_extension, Category.TRICEPS, listOf(Muscle.TRICEPS)),

        // --- Biceps ---
        CatalogExercise("barbell_curl", R.string.ex_barbell_curl, Category.BICEPS, listOf(Muscle.BICEPS)),
        CatalogExercise("dumbbell_curl", R.string.ex_dumbbell_curl, Category.BICEPS, listOf(Muscle.BICEPS)),
        CatalogExercise("alternating_dumbbell_curl", R.string.ex_alternating_dumbbell_curl, Category.BICEPS, listOf(Muscle.BICEPS)),
        CatalogExercise("hammer_curl", R.string.ex_hammer_curl, Category.BICEPS, listOf(Muscle.BICEPS)),
        CatalogExercise("concentration_curl", R.string.ex_concentration_curl, Category.BICEPS, listOf(Muscle.BICEPS)),

        // --- Traps ---
        CatalogExercise("dumbbell_shrugs", R.string.ex_dumbbell_shrugs, Category.TRAPS, listOf(Muscle.TRAPS)),
        CatalogExercise("barbell_shrugs", R.string.ex_barbell_shrugs, Category.TRAPS, listOf(Muscle.TRAPS)),

        // --- Abs ---
        CatalogExercise("lying_crunch", R.string.ex_lying_crunch, Category.ABS, listOf(Muscle.ABS)),
        CatalogExercise("hanging_leg_raise", R.string.ex_hanging_leg_raise, Category.ABS, listOf(Muscle.ABS)),
        CatalogExercise("hanging_knee_raise", R.string.ex_hanging_knee_raise, Category.ABS, listOf(Muscle.ABS)),
        CatalogExercise("plank", R.string.exercise_plank, Category.ABS, listOf(Muscle.ABS), Exercise.PLANK),
    )

    private val byId: Map<String, CatalogExercise> = ALL.associateBy { it.id }

    fun byId(id: String): CatalogExercise? = byId[id]

    /** Catalog entry backing an [Exercise] AI counter, if one exists. */
    fun forCounter(exercise: Exercise): CatalogExercise? = ALL.firstOrNull { it.counter == exercise }

    /** Exercises grouped by category, in enum (display) order. */
    fun byCategory(): Map<Category, List<CatalogExercise>> =
        Category.entries.associateWith { cat -> ALL.filter { it.category == cat } }
}

/**
 * A stable reference to an exercise from anywhere it's stored (plans, sessions).
 * Either a catalog id ("squat") or a custom exercise ("custom:<rowId>").
 */
object ExerciseRef {
    private const val CUSTOM_PREFIX = "custom:"

    fun forCustom(id: Long): String = "$CUSTOM_PREFIX$id"

    fun isCustom(ref: String): Boolean = ref.startsWith(CUSTOM_PREFIX)

    fun customId(ref: String): Long? =
        if (isCustom(ref)) ref.removePrefix(CUSTOM_PREFIX).toLongOrNull() else null

    /** The AI counter for a ref, or null if manual/custom. */
    fun counter(ref: String): Exercise? = ExerciseCatalog.byId(ref)?.counter
}
