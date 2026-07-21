package com.gymgym.app.achievement

import com.gymgym.app.data.BodyMeasurement
import com.gymgym.app.data.CompletedExercise
import com.gymgym.app.data.CompletedWorkout
import com.gymgym.app.data.CompletedWorkoutWithExercises
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class AchievementsTest {

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    /** Epoch millis for a UTC wall-clock time, so tests don't depend on the host zone. */
    private fun at(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }.timeInMillis

    private fun workout(
        id: Long,
        startedAt: Long,
        ref: String = "squat",
        reps: Int = 10,
        good: Int = 10,
        targetReps: Int = 10,
        targetSets: Int = 1,
    ) = CompletedWorkoutWithExercises(
        CompletedWorkout(id, 1, 1, id, "W$id", startedAt, 0, 0),
        listOf(
            CompletedExercise(
                id = id, completedWorkoutId = id, exerciseRef = ref,
                reps = reps, goodReps = good,
                targetReps = targetReps, targetSets = targetSets,
                durationMs = 0, position = 0,
            ),
        ),
    )

    private fun state(list: List<AchievementState>, id: String) = list.first { it.def.id == id }

    private fun evaluate(
        completed: List<CompletedWorkoutWithExercises> = emptyList(),
        measurements: List<BodyMeasurement> = emptyList(),
        cycles: Int = 0,
    ) = Achievements.evaluate(completed, measurements, cycles, emptyMap(), utc)

    // --- longestDayStreak ----------------------------------------------------

    @Test
    fun streakOfEmptyHistoryIsZero() {
        assertEquals(0, Achievements.longestDayStreak(emptyList(), utc))
    }

    @Test
    fun singleDayIsAStreakOfOne() {
        assertEquals(1, Achievements.longestDayStreak(listOf(at(2026, 7, 21)), utc))
    }

    @Test
    fun consecutiveDaysAccumulate() {
        val days = (1..5).map { at(2026, 7, it) }
        assertEquals(5, Achievements.longestDayStreak(days, utc))
    }

    @Test
    fun gapBreaksTheStreakAndLongestRunWins() {
        // 1,2,3 then a gap, then 10,11 → longest is 3.
        val days = listOf(1, 2, 3, 10, 11).map { at(2026, 7, it) }
        assertEquals(3, Achievements.longestDayStreak(days, utc))
    }

    @Test
    fun twoWorkoutsOnTheSameDayCountOnce() {
        val sameDay = listOf(at(2026, 7, 21, 8), at(2026, 7, 21, 19))
        assertEquals(1, Achievements.longestDayStreak(sameDay, utc))
    }

    @Test
    fun lateNightAndEarlyMorningAreTwoDays() {
        // 23:50 then 00:10 the next day — 20 minutes apart, but two calendar days.
        val times = listOf(at(2026, 7, 21, 23, 50), at(2026, 7, 22, 0, 10))
        assertEquals(2, Achievements.longestDayStreak(times, utc))
    }

    // --- Thresholds ----------------------------------------------------------

    @Test
    fun workoutCountBoundaryAtNineAndTen() {
        val nine = (1..9).map { workout(it.toLong(), at(2026, 7, it)) }
        assertFalse(state(evaluate(nine), "workouts_10").earned)

        val ten = (1..10).map { workout(it.toLong(), at(2026, 7, it)) }
        val s = state(evaluate(ten), "workouts_10")
        assertTrue(s.earned)
        assertEquals(10, s.progress)
    }

    @Test
    fun progressIsCappedAtTheTarget() {
        val many = (1..30).map { workout(it.toLong(), at(2026, 7, it)) }
        // 30 workouts shouldn't report 30/10 on the ten-workout badge.
        assertEquals(10, state(evaluate(many), "workouts_10").progress)
    }

    @Test
    fun firstWorkoutUnlocksImmediately() {
        val one = listOf(workout(1, at(2026, 7, 21)))
        assertTrue(state(evaluate(one), "first_workout").earned)
    }

    // --- Muscle groups -------------------------------------------------------

    @Test
    fun legDayStreakUsesCatalogCategory() {
        // squat is a LEGS exercise in the catalog.
        val legs = (1..3).map { workout(it.toLong(), at(2026, 7, it), ref = "squat") }
        assertTrue(state(evaluate(legs), "legday_3").earned)
    }

    @Test
    fun nonLegWorkoutsDoNotCountAsLegDays() {
        val chest = (1..3).map { workout(it.toLong(), at(2026, 7, it), ref = "pushup") }
        assertFalse(state(evaluate(chest), "legday_3").earned)
    }

    @Test
    fun customExercisesDoNotCountTowardFullBody() {
        // "custom:5" has no catalog category, so it contributes no muscle group.
        val custom = listOf(workout(1, at(2026, 7, 1), ref = "custom:5"))
        assertEquals(0, state(evaluate(custom), "full_body").progress)
    }

    @Test
    fun distinctCategoriesAccumulateTowardFullBody() {
        val mixed = listOf(
            workout(1, at(2026, 7, 1), ref = "squat"),    // LEGS
            workout(2, at(2026, 7, 2), ref = "pushup"),   // CHEST
            workout(3, at(2026, 7, 3), ref = "pullup"),   // BACK
            workout(4, at(2026, 7, 4), ref = "squat"),    // LEGS again — no new category
        )
        assertEquals(3, state(evaluate(mixed), "full_body").progress)
    }

    // --- Quality -------------------------------------------------------------

    @Test
    fun perfectRequiresEveryExerciseToHitTarget() {
        val short = listOf(workout(1, at(2026, 7, 1), reps = 27, targetReps = 10, targetSets = 3))
        assertFalse(state(evaluate(short), "perfect_1").earned)

        val full = listOf(workout(2, at(2026, 7, 2), reps = 30, targetReps = 10, targetSets = 3))
        assertTrue(state(evaluate(full), "perfect_1").earned)
    }

    @Test
    fun workoutWithNoStoredTargetsIsNotPerfect() {
        // Legacy rows have no targets — they must not silently count as 100%.
        val legacy = listOf(workout(1, at(2026, 7, 1), reps = 10, targetReps = 0, targetSets = 0))
        assertFalse(state(evaluate(legacy), "perfect_1").earned)
    }

    @Test
    fun repsAndGoodRepsAccumulate() {
        val list = (1..10).map { workout(it.toLong(), at(2026, 7, it), reps = 120, good = 60) }
        assertEquals(1000, state(evaluate(list), "reps_1000").progress) // capped at target
        assertTrue(state(evaluate(list), "reps_1000").earned)
        assertEquals(500, state(evaluate(list), "goodreps_500").progress)
    }

    // --- Time of day ---------------------------------------------------------

    @Test
    fun earlyBirdAndNightOwlBucketByLocalHour() {
        val early = (1..10).map { workout(it.toLong(), at(2026, 7, it, hour = 6)) }
        assertTrue(state(evaluate(early), "early_bird").earned)
        assertFalse(state(evaluate(early), "night_owl").earned)

        val late = (1..10).map { workout(it.toLong(), at(2026, 7, it, hour = 22)) }
        assertTrue(state(evaluate(late), "night_owl").earned)
        assertFalse(state(evaluate(late), "early_bird").earned)
    }

    @Test
    fun sevenAmIsNotEarlyBird() {
        val seven = (1..10).map { workout(it.toLong(), at(2026, 7, it, hour = 7)) }
        assertFalse(state(evaluate(seven), "early_bird").earned)
    }

    // --- Other sources -------------------------------------------------------

    @Test
    fun measurementStreakComesFromBodyMeasurements() {
        val logs = (1..7).map {
            BodyMeasurement(it.toLong(), "WEIGHT", 80.0, "kg", at(2026, 7, it))
        }
        assertTrue(state(evaluate(measurements = logs), "measure_streak_7").earned)
    }

    @Test
    fun cycleBadgesUseTheCompletedCycleCount() {
        assertTrue(state(evaluate(cycles = 1), "cycle_1").earned)
        assertFalse(state(evaluate(cycles = 1), "cycle_10").earned)
        assertTrue(state(evaluate(cycles = 12), "cycle_10").earned)
    }

    @Test
    fun emptyHistoryEarnsNothing() {
        assertTrue(evaluate().none { it.earned })
        assertEquals(Achievements.ALL.size, evaluate().size)
    }

    @Test
    fun existingUnlockTimeIsPreserved() {
        val one = listOf(workout(1, at(2026, 7, 21)))
        val stamped = Achievements.evaluate(one, emptyList(), 0, mapOf("first_workout" to 1234L), utc)
        assertEquals(1234L, state(stamped, "first_workout").unlockedAt)
    }
}
