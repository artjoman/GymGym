package com.gymgym.app.program

import androidx.annotation.StringRes
import com.gymgym.app.R

/**
 * Preset workout programs. Selecting one materializes it into the Plan → Cycle →
 * Workout hierarchy as the active plan (name = program name, end date = today +
 * 3 months). Exercise refs are catalog ids from
 * [com.gymgym.app.exercise.ExerciseCatalog]. Program display name/description are
 * localized; cycle/workout names are baked into the materialized plan as editable
 * data.
 */
data class Program(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val descRes: Int,
    val cycles: List<ProgramCycle>,
) {
    val workoutCount: Int get() = cycles.sumOf { it.workouts.size }
}

data class ProgramCycle(val name: String, val workouts: List<ProgramWorkout>)

data class ProgramWorkout(val name: String, val exercises: List<ProgramExercise>)

data class ProgramExercise(val ref: String, val reps: Int, val sets: Int, val seconds: Int? = null)

private fun ex(ref: String, reps: Int, sets: Int) = ProgramExercise(ref, reps, sets)
private fun hold(ref: String, seconds: Int, sets: Int) = ProgramExercise(ref, reps = seconds, sets = sets, seconds = seconds)

object Programs {

    val ALL: List<Program> = listOf(
        Program(
            id = "full_beginner",
            nameRes = R.string.prog_full_beginner,
            descRes = R.string.desc_full_beginner,
            cycles = listOf(
                ProgramCycle(
                    "Full Body",
                    listOf(
                        ProgramWorkout("Workout A", listOf(ex("squat", 12, 3), ex("pushup", 10, 3), hold("plank", 30, 3))),
                        ProgramWorkout("Workout B", listOf(ex("pullup", 8, 3), ex("forward_lunges", 12, 3), ex("lying_crunch", 15, 3))),
                        ProgramWorkout("Workout C", listOf(ex("incline_pushup", 12, 3), ex("standing_calf_raise", 15, 3), ex("hanging_knee_raise", 12, 3))),
                    ),
                ),
            ),
        ),
        Program(
            id = "full_standard",
            nameRes = R.string.prog_full_standard,
            descRes = R.string.desc_full_standard,
            cycles = listOf(
                ProgramCycle(
                    "Full Body",
                    listOf(
                        ProgramWorkout("Workout A", listOf(ex("squat", 12, 4), ex("pushup", 12, 4), ex("pullup", 8, 4), hold("plank", 45, 3))),
                        ProgramWorkout("Workout B", listOf(ex("forward_lunges", 12, 4), ex("parallel_bar_dips", 10, 4), ex("barbell_curl", 12, 3), ex("lying_crunch", 20, 3))),
                        ProgramWorkout("Workout C", listOf(ex("pushup", 15, 4), ex("pullup", 10, 4), ex("standing_calf_raise", 20, 4), ex("hanging_leg_raise", 12, 3))),
                    ),
                ),
            ),
        ),
        Program(
            id = "home",
            nameRes = R.string.prog_home,
            descRes = R.string.desc_home,
            cycles = listOf(
                ProgramCycle(
                    "Home",
                    listOf(
                        ProgramWorkout("Workout A", listOf(ex("squat", 15, 3), ex("pushup", 12, 3), hold("plank", 40, 3), ex("lying_crunch", 20, 3))),
                        ProgramWorkout("Workout B", listOf(ex("forward_lunges", 14, 3), ex("incline_pushup", 12, 3), ex("single_leg_calf_raise", 15, 3), ex("hanging_knee_raise", 12, 3))),
                    ),
                ),
            ),
        ),
        Program(
            id = "street",
            nameRes = R.string.prog_street,
            descRes = R.string.desc_street,
            cycles = listOf(
                ProgramCycle(
                    "Street",
                    listOf(
                        ProgramWorkout("Workout A", listOf(ex("pullup", 8, 4), ex("parallel_bar_dips", 10, 4), ex("pushup", 15, 4), ex("hanging_leg_raise", 12, 4))),
                        ProgramWorkout("Workout B", listOf(ex("close_grip_pullup", 8, 4), ex("wide_grip_pullup", 8, 4), ex("incline_pushup", 15, 3), hold("plank", 60, 3))),
                    ),
                ),
            ),
        ),
        Program(
            id = "weighted",
            nameRes = R.string.prog_weighted,
            descRes = R.string.desc_weighted,
            cycles = listOf(
                ProgramCycle(
                    "Weighted",
                    listOf(
                        ProgramWorkout("Workout A", listOf(ex("weighted_squat", 8, 5), ex("weighted_parallel_bar_dips", 8, 5), ex("weighted_pullup", 6, 5))),
                        ProgramWorkout("Workout B", listOf(ex("deadlift", 6, 4), ex("dumbbell_bench_press", 10, 4), ex("weighted_single_leg_calf_raise", 12, 4))),
                    ),
                ),
            ),
        ),
        Program(
            id = "ppl",
            nameRes = R.string.prog_ppl,
            descRes = R.string.desc_ppl,
            cycles = listOf(
                ProgramCycle(
                    "Push / Pull / Legs",
                    listOf(
                        ProgramWorkout("Push", listOf(ex("barbell_bench_press", 8, 4), ex("dumbbell_seated_press", 10, 4), ex("dumbbell_lateral_raise", 12, 3), ex("lying_barbell_extension", 12, 3))),
                        ProgramWorkout("Pull", listOf(ex("pullup", 8, 4), ex("barbell_curl", 12, 4), ex("hammer_curl", 12, 3), ex("barbell_shrugs", 15, 3))),
                        ProgramWorkout("Legs", listOf(ex("squat", 10, 4), ex("forward_lunges", 12, 3), ex("standing_calf_raise", 20, 4), ex("hanging_leg_raise", 15, 3))),
                    ),
                ),
            ),
        ),
    )
}
