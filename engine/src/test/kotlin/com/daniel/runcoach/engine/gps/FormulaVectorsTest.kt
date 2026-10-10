package com.daniel.runcoach.engine.gps

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Formula vectors from RUNCOACH_PLAN 6.14.2, each as one test named after its ID.
 * Grown per task; P0.11 turns this into the full V1–V22 audit.
 */
class FormulaVectorsTest {

    private fun moving(stepM: Double) = FilteredSample(GpsOutcome.MOVING, stepM)

    /** V2: Haversine for 0.0001 degrees of latitude. */
    @Test
    fun v02_haversineTenthMilliDegree() {
        assertEquals(11.12, haversineM(0.0, 0.0, 0.0001, 0.0), 0.01)
    }

    /** V3: boundary interpolation time. */
    @Test
    fun v03_boundaryInterpolation() {
        val tracker = DistanceTracker()
        tracker.addBoundary(1000.0)
        tracker.add(0L, moving(0.0))
        tracker.add(300_000L, moving(995.0))
        val crossings = tracker.add(302_000L, moving(8.0))
        assertEquals(1, crossings.size)
        assertEquals(1000.0, crossings[0].boundaryM)
        assertEquals(301_250L, crossings[0].timeMs)
    }

    /** V4: two boundaries in one sample, and the leftover distance carried forward. */
    @Test
    fun v04_multipleBoundariesAndLeftover() {
        val tracker = DistanceTracker()
        tracker.addBoundary(1010.0)
        tracker.addBoundary(1020.0)
        tracker.add(0L, moving(0.0))
        tracker.add(100_000L, moving(1008.0))
        val crossings = tracker.add(104_000L, moving(16.0))
        assertEquals(listOf(1010.0, 1020.0), crossings.map { it.boundaryM })
        assertEquals(100_500L, crossings[0].timeMs)
        assertEquals(103_000L, crossings[1].timeMs)
        // D1 - last boundary = 1024 - 1020 = 4 m carried into the next segment.
        assertEquals(4.0, tracker.totalDistanceM - 1020.0, 1e-9)
    }
}
