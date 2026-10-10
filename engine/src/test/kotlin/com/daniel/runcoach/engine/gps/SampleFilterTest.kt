package com.daniel.runcoach.engine.gps

import com.daniel.runcoach.engine.model.LocationSample
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SampleFilterTest {

    private fun sample(
        timeSec: Long,
        lat: Double,
        lon: Double,
        accuracyM: Double = 5.0,
        speedMps: Double? = null,
    ) = LocationSample(
        timeNanos = timeSec * 1_000_000_000L,
        lat = lat,
        lon = lon,
        accuracyM = accuracyM,
        speedMps = speedMps,
    )

    /** ~1 metre north per 0.000009 degrees, so sequences read as a straight northward path. */
    private fun metersNorth(baseLat: Double, meters: Double) = baseLat + meters / 111_195.0

    @Test
    fun `first sample with good accuracy becomes the reference with no distance`() {
        val filter = SampleFilter()
        val result = filter.accept(sample(0, 0.0, 0.0))
        assertEquals(GpsOutcome.MOVING, result.outcome)
        assertEquals(0.0, result.stepM, 1e-9)
        assertEquals(0, filter.droppedCount)
    }

    @Test
    fun `sample with poor accuracy is dropped and counted`() {
        val filter = SampleFilter()
        assertEquals(GpsOutcome.REJECTED, filter.accept(sample(0, 0.0, 0.0, accuracyM = 25.0)).outcome)
        assertEquals(1, filter.droppedCount)
    }

    @Test
    fun `a dropped first sample does not become the reference`() {
        val filter = SampleFilter()
        filter.accept(sample(0, 0.0, 0.0, accuracyM = 25.0))
        // Next sample is treated as the first: reference set, no distance.
        val result = filter.accept(sample(1, 0.0, 0.0))
        assertEquals(GpsOutcome.MOVING, result.outcome)
        assertEquals(0.0, result.stepM, 1e-9)
    }

    @Test
    fun `accuracy exactly at the limit is accepted`() {
        val filter = SampleFilter()
        filter.accept(sample(0, 0.0, 0.0))
        assertEquals(GpsOutcome.MOVING, filter.accept(sample(1, metersNorth(0.0, 3.0), 0.0, accuracyM = 20.0)).outcome)
    }

    @Test
    fun `timestamp that does not advance is dropped`() {
        val filter = SampleFilter()
        filter.accept(sample(10, 0.0, 0.0))
        assertEquals(GpsOutcome.REJECTED, filter.accept(sample(10, metersNorth(0.0, 1.0), 0.0)).outcome)
        assertEquals(1, filter.droppedCount)
    }

    @Test
    fun `timestamp going backwards is dropped`() {
        val filter = SampleFilter()
        filter.accept(sample(10, 0.0, 0.0))
        assertEquals(GpsOutcome.REJECTED, filter.accept(sample(9, metersNorth(0.0, 1.0), 0.0)).outcome)
    }

    @Test
    fun `normal straight run earns distance`() {
        val filter = SampleFilter()
        filter.accept(sample(0, 0.0, 0.0))
        val result = filter.accept(sample(1, metersNorth(0.0, 3.0), 0.0))
        assertEquals(GpsOutcome.MOVING, result.outcome)
        assertEquals(3.0, result.stepM, 0.05)
        assertEquals(0, filter.droppedCount)
    }

    @Test
    fun `implied speed above the limit is dropped`() {
        val filter = SampleFilter()
        filter.accept(sample(0, 0.0, 0.0))
        // 100 m in 1 s = 100 m/s.
        assertEquals(GpsOutcome.REJECTED, filter.accept(sample(1, metersNorth(0.0, 100.0), 0.0)).outcome)
        assertEquals(1, filter.droppedCount)
    }

    @Test
    fun `implied speed just below the limit is accepted`() {
        val filter = SampleFilter()
        filter.accept(sample(0, 0.0, 0.0))
        // 30 m in 4 s = 7.5 m/s.
        assertEquals(GpsOutcome.MOVING, filter.accept(sample(4, metersNorth(0.0, 30.0), 0.0)).outcome)
    }

    @Test
    fun `stationary jitter earns no distance and does not move the reference`() {
        val filter = SampleFilter()
        filter.accept(sample(0, 0.0, 0.0))
        // Static speed and < 5 m displacement -> STATIONARY.
        val jitter = filter.accept(sample(1, metersNorth(0.0, 2.0), 0.0, speedMps = 0.1))
        assertEquals(GpsOutcome.STATIONARY, jitter.outcome)
        assertEquals(0.0, jitter.stepM, 1e-9)
        // Reference is still the origin: the next moving sample measures from it.
        val moving = filter.accept(sample(2, metersNorth(0.0, 6.0), 0.0, speedMps = 3.0))
        assertEquals(GpsOutcome.MOVING, moving.outcome)
        assertEquals(6.0, moving.stepM, 0.1)
    }

    @Test
    fun `stationary rule needs a reported speed`() {
        val filter = SampleFilter()
        filter.accept(sample(0, 0.0, 0.0))
        // No speed reported -> handled as normal movement even though small.
        assertEquals(GpsOutcome.MOVING, filter.accept(sample(1, metersNorth(0.0, 2.0), 0.0)).outcome)
    }

    @Test
    fun `displacement at or above the stationary limit is movement`() {
        val filter = SampleFilter()
        filter.accept(sample(0, 0.0, 0.0))
        assertEquals(GpsOutcome.MOVING, filter.accept(sample(2, metersNorth(0.0, 6.0), 0.0, speedMps = 0.1)).outcome)
    }

    @Test
    fun `three consecutive speed rejections reset the reference`() {
        val filter = SampleFilter()
        filter.accept(sample(0, 0.0, 0.0))
        assertEquals(GpsOutcome.REJECTED, filter.accept(sample(1, metersNorth(0.0, 100.0), 0.0)).outcome)
        assertEquals(GpsOutcome.REJECTED, filter.accept(sample(2, metersNorth(0.0, 200.0), 0.0)).outcome)
        assertEquals(GpsOutcome.RESET, filter.accept(sample(3, metersNorth(0.0, 300.0), 0.0)).outcome)
        // Reference is now the 300 m point: the next sample is measured from there.
        val result = filter.accept(sample(4, metersNorth(0.0, 303.0), 0.0))
        assertEquals(GpsOutcome.MOVING, result.outcome)
        assertEquals(3.0, result.stepM, 0.05)
    }

    @Test
    fun `a plausible long gap is counted as straight line movement`() {
        val filter = SampleFilter()
        filter.accept(sample(0, 0.0, 0.0))
        // 75 m over a 15 s outage = 5 m/s: a tunnel, still MOVING.
        val result = filter.accept(sample(15, metersNorth(0.0, 75.0), 0.0))
        assertEquals(GpsOutcome.MOVING, result.outcome)
        assertEquals(75.0, result.stepM, 0.5)
    }

    @Test
    fun `dropped count accumulates across many drops`() {
        val filter = SampleFilter()
        filter.accept(sample(0, 0.0, 0.0, accuracyM = 50.0))
        filter.accept(sample(1, 0.0, 0.0, accuracyM = 50.0))
        filter.accept(sample(2, 0.0, 0.0, accuracyM = 50.0))
        assertEquals(3, filter.droppedCount)
    }

    @Test
    fun `a jitter sample does not count as dropped`() {
        val filter = SampleFilter()
        filter.accept(sample(0, 0.0, 0.0))
        filter.accept(sample(1, metersNorth(0.0, 1.0), 0.0, speedMps = 0.0))
        assertTrue(filter.droppedCount == 0)
    }
}
