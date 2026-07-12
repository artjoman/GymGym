package com.gymgym.app.pose

import kotlin.math.abs
import kotlin.math.atan2

/** Angle at point [b], between rays b->a and b->c, in degrees [0, 180]. */
fun angleBetween(a: Point2D, b: Point2D, c: Point2D): Float {
    val angleA = atan2(a.y - b.y, a.x - b.x)
    val angleC = atan2(c.y - b.y, c.x - b.x)
    var degrees = Math.toDegrees((angleA - angleC).toDouble()).toFloat()
    degrees = abs(degrees)
    if (degrees > 180f) degrees = 360f - degrees
    return degrees
}
