package com.daniel.runcoach.engine.gps

import com.daniel.runcoach.engine.model.LocationSample
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    fun `first sample with good accuracy is accepted`() {
        val filter = SampleFilter()
        assertTrue(filter.accept(sample(0, 0.0, 0.0)))
        assertEquals(0, filter.droppedCount)
    }

    @Test
    fun `sample with poor accuracy is dropped and counted`() {
        val filter = SampleFilter()
        assertFalse(filter.accept(sample(0, 0.0, 0.0, accuracyM = 25.0)))
        assertEquals(1, filter.droppedCount)
    }

    @Test
    fun `accuracy exactly at the limit is accepted`() {
        val filter = SampleFilter()
        assertTrue(filter.accept(sample(0, 0.0, 0.0, accuracyM = 20.0)))
    }

    @Test
    fun `timestamp that does not advance is dropped`() {
        val filter = SampleFilter()
        assertTrue(filter.accept(sample(10, 0.0, 0.0)))
        assertFalse(filter.accept(sample(10, metersNorth(0.0, 1.0), 0.0)))
        assertEquals(1, filter.droppedCount)
    }

    @Test
    fun `timestamp going backwards is dropped`() {
        val filter = SampleFilter()
        assertTrue(filter.accept(sample(10, 0.0, 0.0)))
        assertFalse(filter.accept(sample(9, metersNorth(0.0, 1.0), 0.0)))
    }

    @Test
    fun `normal straight run at a jogging pace is accepted`() {
        val filter = SampleFilter()
        // 3 m per second, 1 Hz order.
        var accepted = 0
        for (i in 0..9) {
            if (filter.accept(sample(i.toLong(), metersNorth(0.0, i * 3.0), 0.0))) accepted++
        }
        assertEquals(10, accepted)
        assertEquals(0, filter.droppedCount)
    }

    @Test
    fun `implied speed above the limit is dropped`() {
        val filter = SampleFilter()
        assertTrue(filter.accept(sample(0, 0.0, 0.0)))
        // 100 m in 1 s = 100 m/s, far above 9 m/s.
        assertFalse(filter.accept(sample(1, metersNorth(0.0, 100.0), 0.0)))
        assertEquals(1, filter.droppedCount)
    }

    @Test
    fun `implied speed just below the limit is accepted`() {
        val filter = SampleFilter()
        assertTrue(filter.accept(sample(0, 0.0, 0.0)))
        // 30 m in 4 s = 7.5 m/s.
        assertTrue(filter.accept(sample(4, metersNorth(0.0, 30.0), 0.0)))
    }

    @Test
    fun `a dropped sample does not become the speed reference`() {
        val filter = SampleFilter()
        assertTrue(filter.accept(sample(0, 0.0, 0.0)))
        // Bad accuracy, also a huge jump: must be ignored entirely.
        assertFalse(filter.accept(sample(1, metersNorth(0.0, 500.0), 0.0, accuracyM = 40.0)))
        // Next real sample is close to the first accepted one, so it stays valid.
        assertTrue(filter.accept(sample(2, metersNorth(0.0, 6.0), 0.0)))
        assertEquals(1, filter.droppedCount)
    }

    @Test
    fun `dropped count accumulates across many drops`() {
        val filter = SampleFilter()
        filter.accept(sample(0, 0.0, 0.0, accuracyM = 50.0))
        filter.accept(sample(1, 0.0, 0.0, accuracyM = 50.0))
        filter.accept(sample(2, 0.0, 0.0, accuracyM = 50.0))
        assertEquals(3, filter.droppedCount)
    }
}
