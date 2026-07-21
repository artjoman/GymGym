package com.gymgym.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Rules behind the Current mission "Make next" button. */
class MakeNextTest {

    private fun w(id: Long, position: Int, weekday: Int? = null) =
        WorkoutEntity(id = id, cycleId = 1, name = "W$id", position = position, weekday = weekday)

    @Test
    fun selectedMovesToFrontAndOthersKeepRelativeOrder() {
        val ordered = listOf(w(1, 0), w(2, 1), w(3, 2), w(4, 3))
        val result = makeNextAssignment(ordered, doneIds = emptySet(), selectedId = 3, weekly = false)
        // W3 first, then W1, W2, W4 in their original relative order.
        assertEquals(listOf(3L, 1L, 2L, 4L), result.map { it.id })
        assertEquals(listOf(0, 1, 2, 3), result.map { it.position })
    }

    @Test
    fun executedWorkoutsKeepTheirSlots() {
        // W1 executed → only slots 1..3 are reshuffled.
        val ordered = listOf(w(1, 0), w(2, 1), w(3, 2), w(4, 3))
        val result = makeNextAssignment(ordered, doneIds = setOf(1L), selectedId = 4, weekly = false)
        assertEquals(listOf(4L, 2L, 3L), result.map { it.id })
        assertEquals(listOf(1, 2, 3), result.map { it.position })
        assertTrue("executed workout must not move", result.none { it.id == 1L })
    }

    @Test
    fun weekdaysBelongToSlotsNotWorkouts() {
        // Mon/Wed/Fri slots; promoting the Friday workout gives it Monday.
        val ordered = listOf(w(1, 0, weekday = 1), w(2, 1, weekday = 3), w(3, 2, weekday = 5))
        val result = makeNextAssignment(ordered, doneIds = emptySet(), selectedId = 3, weekly = true)
        assertEquals(listOf(3L, 1L, 2L), result.map { it.id })
        assertEquals(listOf(1, 3, 5), result.map { it.weekday })
    }

    @Test
    fun weekdaysUntouchedWhenNotWeekly() {
        val ordered = listOf(w(1, 0, weekday = 1), w(2, 1, weekday = 3))
        val result = makeNextAssignment(ordered, doneIds = emptySet(), selectedId = 2, weekly = false)
        // Each workout keeps its own weekday; only positions change.
        assertEquals(listOf(2L, 1L), result.map { it.id })
        assertEquals(listOf(3, 1), result.map { it.weekday })
    }

    @Test
    fun nothingIsAddedRemovedOrDuplicated() {
        val ordered = listOf(w(1, 0), w(2, 1), w(3, 2), w(4, 3), w(5, 4))
        val result = makeNextAssignment(ordered, doneIds = setOf(2L), selectedId = 5, weekly = false)
        val ids = result.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        // Every unfinished workout appears exactly once, and slots are reused as-is.
        assertEquals(setOf(1L, 3L, 4L, 5L), ids.toSet())
        assertEquals(listOf(0, 2, 3, 4), result.map { it.position })
    }

    @Test
    fun promotingTheCurrentWorkoutIsANoOpOrdering() {
        val ordered = listOf(w(1, 0), w(2, 1), w(3, 2))
        val result = makeNextAssignment(ordered, doneIds = emptySet(), selectedId = 1, weekly = false)
        assertEquals(listOf(1L, 2L, 3L), result.map { it.id })
        assertEquals(listOf(0, 1, 2), result.map { it.position })
    }
}
