package com.daniel.runcoach.engine.gps

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DistanceTrackerTest {

    private fun moving(stepM: Double) = FilteredSample(GpsOutcome.MOVING, stepM)

    @Test
    fun `first sample sets the time base without distance`() {
        val tracker = DistanceTracker()
        assertTrue(tracker.add(0L, moving(0.0)).isEmpty())
        assertEquals(0.0, tracker.totalDistanceM, 1e-9)
    }

    @Test
    fun `accumulates straight line distance`() {
        val tracker = DistanceTracker()
        tracker.add(0L, moving(0.0))
        tracker.add(1_000L, moving(3.0))
        assertEquals(3.0, tracker.totalDistanceM, 1e-9)
    }

    @Test
    fun `interpolates the time at a boundary`() {
        val tracker = DistanceTracker()
        tracker.addBoundary(10.0)
        tracker.add(0L, moving(0.0))
        val crossings = tracker.add(3_000L, moving(30.0))
        assertEquals(1, crossings.size)
        assertEquals(1_000L, crossings[0].timeMs)
    }

    @Test
    fun `a single sample can cross two boundaries in order`() {
        val tracker = DistanceTracker()
        tracker.addBoundary(10.0)
        tracker.addBoundary(20.0)
        tracker.add(0L, moving(0.0))
        val crossings = tracker.add(3_000L, moving(30.0))
        assertEquals(listOf(10.0, 20.0), crossings.map { it.boundaryM })
        assertEquals(1_000L, crossings[0].timeMs)
        assertEquals(2_000L, crossings[1].timeMs)
    }

    @Test
    fun `overshoot is carried into the running total`() {
        val tracker = DistanceTracker()
        tracker.addBoundary(10.0)
        tracker.addBoundary(20.0)
        tracker.add(0L, moving(0.0))
        tracker.add(5_000L, moving(25.0))
        assertEquals(25.0, tracker.totalDistanceM, 1e-9)
    }

    @Test
    fun `no crossing is reported before its boundary`() {
        val tracker = DistanceTracker()
        tracker.addBoundary(1000.0)
        tracker.add(0L, moving(0.0))
        assertTrue(tracker.add(1_000L, moving(3.0)).isEmpty())
    }

    @Test
    fun `a boundary is never reported twice`() {
        val tracker = DistanceTracker()
        tracker.addBoundary(10.0)
        tracker.add(0L, moving(0.0))
        assertEquals(1, tracker.add(5_000L, moving(12.0)).size)
        assertTrue(tracker.add(6_000L, moving(3.0)).isEmpty())
    }

    @Test
    fun `calibration factor scales distance`() {
        val tracker = DistanceTracker(calibrationFactor = 1.05)
        tracker.add(0L, moving(0.0))
        tracker.add(1_000L, moving(100.0))
        assertEquals(105.0, tracker.totalDistanceM, 1e-9)
    }

    @Test
    fun `calibration factor above the maximum is clamped`() {
        val tracker = DistanceTracker(calibrationFactor = 2.0)
        tracker.add(0L, moving(0.0))
        tracker.add(1_000L, moving(100.0))
        assertEquals(105.0, tracker.totalDistanceM, 1e-9)
    }

    @Test
    fun `calibration factor below the minimum is clamped`() {
        val tracker = DistanceTracker(calibrationFactor = 0.1)
        tracker.add(0L, moving(0.0))
        tracker.add(1_000L, moving(100.0))
        assertEquals(95.0, tracker.totalDistanceM, 1e-9)
    }

    @Test
    fun `a stationary sample adds no distance and does not move the time base`() {
        val tracker = DistanceTracker()
        tracker.addBoundary(10.0)
        tracker.add(0L, moving(0.0))
        assertTrue(tracker.add(1_000L, FilteredSample(GpsOutcome.STATIONARY, 0.0)).isEmpty())
        // Interpolation still runs from mt=0, not mt=1000.
        val crossings = tracker.add(3_000L, moving(30.0))
        assertEquals(1_000L, crossings[0].timeMs)
    }

    @Test
    fun `a rejected sample is ignored`() {
        val tracker = DistanceTracker()
        tracker.add(0L, moving(0.0))
        assertTrue(tracker.add(1_000L, FilteredSample(GpsOutcome.REJECTED, 0.0)).isEmpty())
        assertEquals(0.0, tracker.totalDistanceM, 1e-9)
    }

    @Test
    fun `reset clears distance and pending boundaries`() {
        val tracker = DistanceTracker()
        tracker.addBoundary(10.0)
        tracker.add(0L, moving(0.0))
        tracker.add(5_000L, moving(30.0))
        tracker.reset()
        assertEquals(0.0, tracker.totalDistanceM, 1e-9)
        tracker.add(0L, moving(0.0))
        assertTrue(tracker.add(5_000L, moving(30.0)).isEmpty())
    }
}
