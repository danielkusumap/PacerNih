package com.daniel.runcoach.engine.chart

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** DESIGN 6.2 acceptance: the pure chart helpers with exact values. */
class ChartMathTest {

    /** A steady 5:00/km curve: 300 s per km, 30 s per 100 m bucket. */
    private fun steadyCurve(points: Int = 11): List<DistTimePoint> =
        (0 until points).map { DistTimePoint(it * 100.0, it * 30_000L) }

    @Test
    fun bucketByDistanceOnSyntheticTrack() {
        val buckets = ChartMath.bucketByDistance(steadyCurve(), bucketM = 100.0)
        assertEquals(10, buckets.size)
        buckets.forEach { assertEquals(300.0, it.paceSecPerKm!!, 1e-6) }
    }

    @Test
    fun bucketIsNullWhenCurveDoesNotSpan() {
        val curve = listOf(
            DistTimePoint(0.0, 0L),
            DistTimePoint(100.0, 30_000L),
            DistTimePoint(200.0, null),
            DistTimePoint(300.0, 90_000L),
        )
        val buckets = ChartMath.bucketByDistance(curve, bucketM = 100.0)
        assertEquals(3, buckets.size)
        assertNull(buckets[1].paceSecPerKm)
        assertTrue(buckets[0].paceSecPerKm != null)
    }

    @Test
    fun emptyAndSinglePointCurves() {
        assertTrue(ChartMath.bucketByDistance(emptyList()).isEmpty())
        assertTrue(ChartMath.bucketByDistance(listOf(DistTimePoint(0.0, 0L))).isEmpty())
    }

    @Test
    fun paceAxisBoundsAreRoundAndWideEnough() {
        val (min, max) = ChartMath.paceAxisBounds(listOf(312.0, 468.0))
        assertEquals(300.0, min)
        assertEquals(480.0, max)
        assertTrue(max - min >= 60.0)

        val (min2, max2) = ChartMath.paceAxisBounds(listOf(350.0, 360.0))
        assertTrue(max2 - min2 >= 60.0)
    }

    @Test
    fun paceTicksForFiveTwelveToSevenFortyEight() {
        val (min, max) = ChartMath.paceAxisBounds(listOf(312.0, 468.0))
        val ticks = ChartMath.paceTicks(min, max)
        assertTrue(ticks.size in 3..4, "ticks=$ticks")
        assertTrue(ticks.all { it >= min && it <= max })
        // Evenly spaced by the chosen step.
        val step = ticks[1] - ticks[0]
        ticks.zipWithNext().forEach { (a, b) -> assertEquals(step, b - a, 1e-9) }
    }

    @Test
    fun invertPaceScalePutsFasterOnTop() {
        assertTrue(ChartMath.invertPaceFraction(300.0, 300.0, 420.0) > ChartMath.invertPaceFraction(400.0, 300.0, 420.0))
        assertEquals(1.0, ChartMath.invertPaceFraction(300.0, 300.0, 420.0), 1e-9)
        assertEquals(0.0, ChartMath.invertPaceFraction(420.0, 300.0, 420.0), 1e-9)
    }

    @Test
    fun downsampleKeepsTheEndsAndLimit() {
        val curve = steadyCurve(points = 500)
        val reduced = ChartMath.downsample(curve, maxPoints = 100)
        assertTrue(reduced.size <= 100)
        assertEquals(curve.last(), reduced.last())
        assertTrue(reduced.zipWithNext().all { (a, b) -> b.distanceM >= a.distanceM })
    }

    @Test
    fun downsampleLeavesSmallInputUntouched() {
        val curve = steadyCurve(points = 10)
        assertEquals(curve, ChartMath.downsample(curve, maxPoints = 200))
    }
}
