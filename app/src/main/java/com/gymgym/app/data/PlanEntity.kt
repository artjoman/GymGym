package com.gymgym.app.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * Workout plan hierarchy: Plan → Cycle → Workout → WorkoutExercise.
 *
 * A plan holds ordered cycles (e.g. "Leg focus", "Back focus") that repeat until
 * the plan's [endDate]. Each cycle holds ordered workouts (e.g. "Workout A"), and
 * each workout holds ordered exercises referenced by ExerciseRef
 * ([exerciseRef][WorkoutExerciseEntity.exerciseRef]) with rep/set (or hold-second)
 * targets. Exactly one plan may be [active][PlanEntity.isActive].
 */
@Entity(tableName = "plan")
data class PlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Plan end date (epoch ms), or null for open-ended. */
    val endDate: Long? = null,
    val isActive: Boolean = false,
    val createdAt: Long = 0,
)

@Entity(
    tableName = "cycle",
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("planId")],
)
data class CycleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val name: String,
    val position: Int,
)

@Entity(
    tableName = "workout",
    foreignKeys = [
        ForeignKey(
            entity = CycleEntity::class,
            parentColumns = ["id"],
            childColumns = ["cycleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("cycleId")],
)
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long,
    val name: String,
    val position: Int,
    /** Weekday (1=Mon..7=Sun) for the Weekly Schedule mode, or null. */
    val weekday: Int? = null,
)

@Entity(
    tableName = "workout_exercise",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workoutId")],
)
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    /** ExerciseRef: a catalog id ("squat") or "custom:<rowId>". */
    val exerciseRef: String,
    val targetReps: Int,
    val targetSets: Int,
    /** Hold seconds for timed exercises (e.g. plank), or null for rep-based. */
    val targetSeconds: Int? = null,
    val position: Int,
)

// --- Relation graph ---

data class WorkoutWithExercises(
    @Embedded val workout: WorkoutEntity,
    @Relation(parentColumn = "id", entityColumn = "workoutId")
    val exercises: List<WorkoutExerciseEntity>,
) {
    val orderedExercises: List<WorkoutExerciseEntity> get() = exercises.sortedBy { it.position }
}

data class CycleWithWorkouts(
    @Embedded val cycle: CycleEntity,
    @Relation(entity = WorkoutEntity::class, parentColumn = "id", entityColumn = "cycleId")
    val workouts: List<WorkoutWithExercises>,
) {
    val orderedWorkouts: List<WorkoutWithExercises> get() = workouts.sortedBy { it.workout.position }
}

data class PlanWithCycles(
    @Embedded val plan: PlanEntity,
    @Relation(entity = CycleEntity::class, parentColumn = "id", entityColumn = "planId")
    val cycles: List<CycleWithWorkouts>,
) {
    val orderedCycles: List<CycleWithWorkouts> get() = cycles.sortedBy { it.cycle.position }

    val workoutCount: Int get() = cycles.sumOf { it.workouts.size }
}
