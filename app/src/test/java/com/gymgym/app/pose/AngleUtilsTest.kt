package com.gymgym.app.pose

import org.junit.Assert.assertEquals
import org.junit.Test

/** The angle geometry every counter relies on. */
class AngleUtilsTest {

    @Test
    fun straightLineIs180() {
        val a = angleBetween(Point2D(0f, 0f), Point2D(1f, 0f), Point2D(2f, 0f))
        assertEquals(180f, a, 0.01f)
    }

    @Test
    fun rightAngleIs90() {
        // vertex at origin, rays up and right
        val a = angleBetween(Point2D(0f, 1f), Point2D(0f, 0f), Point2D(1f, 0f))
        assertEquals(90f, a, 0.01f)
    }

    @Test
    fun sameDirectionIs0() {
        val a = angleBetween(Point2D(1f, 0f), Point2D(0f, 0f), Point2D(2f, 0f))
        assertEquals(0f, a, 0.01f)
    }

    @Test
    fun fortyFiveDegrees() {
        val a = angleBetween(Point2D(1f, 0f), Point2D(0f, 0f), Point2D(1f, 1f))
        assertEquals(45f, a, 0.01f)
    }

    @Test
    fun neverExceeds180() {
        // A reflex configuration must still report the interior (<=180) angle.
        val a = angleBetween(Point2D(-1f, -1f), Point2D(0f, 0f), Point2D(1f, -1f))
        assertEquals(90f, a, 0.01f)
    }
}
