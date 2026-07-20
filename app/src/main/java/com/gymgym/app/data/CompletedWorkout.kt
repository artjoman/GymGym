package com.gymgym.app.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

/**
 * A finished workout run (a whole [WorkoutEntity] executed end-to-end), plus its
 * per-exercise results. Powers the hierarchical History (Phase 7) and the home
 * dashboard's Last Workout / cycle progress (Phase 6). [durationMs] excludes
 * paused time; [avgPercent] is the average good-form percentage across exercises.
 */
@Entity(tableName = "completed_workout")
data class CompletedWorkout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long?,
    val cycleId: Long?,
    val workoutId: Long?,
    val name: String,
    val startedAt: Long,
    val durationMs: Long,
    val avgPercent: Int,
    /**
     * Plan and cycle names captured at the time of the run. Editing a plan
     * replaces its cycle/workout rows (new ids), and deleting one cascades them
     * away — so history must not depend on those rows still existing.
     */
    @ColumnInfo(defaultValue = "") val planName: String = "",
    @ColumnInfo(defaultValue = "") val cycleName: String = "",
)

@Entity(
    tableName = "completed_exercise",
    foreignKeys = [
        ForeignKey(
            entity = CompletedWorkout::class,
            parentColumns = ["id"],
            childColumns = ["completedWorkoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("completedWorkoutId")],
)
data class CompletedExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val completedWorkoutId: Long,
    val exerciseRef: String,
    val reps: Int,
    val goodReps: Int,
    /** Planned reps per set and set count, for the completion-% calculation. */
    @ColumnInfo(defaultValue = "0") val targetReps: Int = 0,
    @ColumnInfo(defaultValue = "0") val targetSets: Int = 0,
    val durationMs: Long,
    val position: Int,
)

data class CompletedWorkoutWithExercises(
    @Embedded val workout: CompletedWorkout,
    @Relation(parentColumn = "id", entityColumn = "completedWorkoutId")
    val exercises: List<CompletedExercise>,
) {
    val orderedExercises: List<CompletedExercise> get() = exercises.sortedBy { it.position }
}

@Dao
interface CompletedWorkoutDao {

    @Transaction
    @Query("SELECT * FROM completed_workout ORDER BY startedAt DESC")
    fun allWithExercises(): Flow<List<CompletedWorkoutWithExercises>>

    @Insert
    suspend fun insertWorkout(workout: CompletedWorkout): Long

    @Insert
    suspend fun insertExercises(items: List<CompletedExercise>)

    @Query("DELETE FROM completed_workout")
    suspend fun deleteAll()
}

class CompletedWorkoutRepository(private val dao: CompletedWorkoutDao) {

    val all: Flow<List<CompletedWorkoutWithExercises>> = dao.allWithExercises()

    companion object {
        /**
         * Completion is hierarchical:
         *  1. exercise % = completed reps ÷ planned reps (e.g. 27/30 → 90);
         *  2. workout % = average of its exercise percentages;
         *  3. cycle %  = average of its workout percentages (a skipped workout is 0).
         * Averaging at each level keeps a long exercise from dominating a short one.
         */
        fun exercisePercent(completed: Int, planned: Int): Int =
            if (planned <= 0) 0 else (completed * 100f / planned).roundToInt().coerceIn(0, 100)

        /** Rounded mean of the given percentages; 0 when there are none. */
        fun averagePercent(percents: List<Int>): Int =
            if (percents.isEmpty()) 0 else percents.sum().toFloat().div(percents.size).roundToInt().coerceIn(0, 100)
    }

    /** Draft of one exercise's result, before the parent workout has a row id. */
    data class ExerciseResult(
        val exerciseRef: String,
        val reps: Int,
        val goodReps: Int,
        val targetReps: Int,
        val targetSets: Int,
        val durationMs: Long,
    ) {
        /** Completion % = reps done vs planned (targetReps × targetSets), capped at 100. */
        fun completionPercent(): Int {
            val target = targetReps * targetSets
            return if (target <= 0) 0 else (reps * 100 / target).coerceIn(0, 100)
        }
    }

    @Transaction
    suspend fun record(
        planId: Long?,
        cycleId: Long?,
        workoutId: Long?,
        name: String,
        startedAt: Long,
        durationMs: Long,
        exercises: List<ExerciseResult>,
        planName: String = "",
        cycleName: String = "",
    ) {
        // Workout % = average of its exercises' completion percentages.
        val avg = averagePercent(
            exercises.map { exercisePercent(it.reps, it.targetReps * it.targetSets) },
        )
        val id = dao.insertWorkout(
            CompletedWorkout(
                planId = planId,
                cycleId = cycleId,
                workoutId = workoutId,
                name = name,
                startedAt = startedAt,
                durationMs = durationMs,
                avgPercent = avg,
                planName = planName,
                cycleName = cycleName,
            ),
        )
        dao.insertExercises(
            exercises.mapIndexed { index, r ->
                CompletedExercise(
                    completedWorkoutId = id,
                    exerciseRef = r.exerciseRef,
                    reps = r.reps,
                    goodReps = r.goodReps,
                    targetReps = r.targetReps,
                    targetSets = r.targetSets,
                    durationMs = r.durationMs,
                    position = index,
                )
            },
        )
    }
}
