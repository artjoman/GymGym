package com.gymgym.app.data

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

    /** Draft of one exercise's result, before the parent workout has a row id. */
    data class ExerciseResult(
        val exerciseRef: String,
        val reps: Int,
        val goodReps: Int,
        val durationMs: Long,
    )

    @Transaction
    suspend fun record(
        planId: Long?,
        cycleId: Long?,
        workoutId: Long?,
        name: String,
        startedAt: Long,
        durationMs: Long,
        exercises: List<ExerciseResult>,
    ) {
        val avg = if (exercises.isEmpty()) {
            0
        } else {
            exercises.map { r -> if (r.reps > 0) r.goodReps * 100 / r.reps else 0 }.average().toInt()
        }
        val id = dao.insertWorkout(
            CompletedWorkout(
                planId = planId,
                cycleId = cycleId,
                workoutId = workoutId,
                name = name,
                startedAt = startedAt,
                durationMs = durationMs,
                avgPercent = avg,
            ),
        )
        dao.insertExercises(
            exercises.mapIndexed { index, r ->
                CompletedExercise(
                    completedWorkoutId = id,
                    exerciseRef = r.exerciseRef,
                    reps = r.reps,
                    goodReps = r.goodReps,
                    durationMs = r.durationMs,
                    position = index,
                )
            },
        )
    }
}
