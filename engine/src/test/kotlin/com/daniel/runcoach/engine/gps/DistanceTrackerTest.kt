package com.daniel.runcoach.engine.gps

import com.daniel.runcoach.engine.model.LocationSample
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DistanceTrackerTest {

    private fun sample(timeSec: Double, metersNorth: Double) = LocationSample(
        timeNanos = (timeSec * 1_000_000_000).toLong(),
        lat = metersNorth / 111_195.0,
        lon = 0.0,
        accuracyM = 5.0,
    )

    private fun seconds(nanos: Long) = nanos / 1_000_000_000.0

    @Test
    fun `first sample adds no distance`() {
        val tracker = DistanceTracker()
        assertEquals(0.0, tracker.add(sample(0.0, 0.0)).size.toDouble())
        assertEquals(0.0, tracker.totalDistanceM, 1e-9)
    }

    @Test
    fun `accumulates straight line distance`() {
        val tracker = DistanceTracker()
        tracker.add(sample(0.0, 0.0))
        tracker.add(sample(1.0, 111.19))
        assertEquals(111.19, tracker.totalDistanceM, 0.5)
    }

    @Test
    fun `interpolates the time at 200 400 and 1000 metres`() {
        val tracker = DistanceTracker()
        tracker.addBoundary(200.0)
        tracker.addBoundary(400.0)
        tracker.addBoundary(1000.0)

        val crossings = mutableListOf<BoundaryCrossing>()
        // 3 m/s, 1 Hz, covering a little over 1000 m.
        for (i in 0..334) {
            crossings += tracker.add(sample(i.toDouble(), i * 3.0))
        }

        assertEquals(listOf(200.0, 400.0, 1000.0), crossings.map { it.boundaryM })
        assertEquals(200.0 / 3.0, seconds(crossings[0].timeNanos), 0.2)
        assertEquals(400.0 / 3.0, seconds(crossings[1].timeNanos), 0.2)
        assertEquals(1000.0 / 3.0, seconds(crossings[2].timeNanos), 0.2)
    }

    @Test
    fun `a single sample can cross two boundaries in order`() {
        val tracker = DistanceTracker()
        tracker.addBoundary(10.0)
        tracker.addBoundary(20.0)

        tracker.add(sample(0.0, 0.0))
        val crossings = tracker.add(sample(3.0, 30.0))

        assertEquals(2, crossings.size)
        assertEquals(10.0, crossings[0].boundaryM)
        assertEquals(20.0, crossings[1].boundaryM)
        assertEquals(1.0, seconds(crossings[0].timeNanos), 0.05)
        assertEquals(2.0, seconds(crossings[1].timeNanos), 0.05)
    }

    @Test
    fun `overshoot is carried into the running total`() {
        val tracker = DistanceTracker()
        tracker.addBoundary(10.0)
        tracker.addBoundary(20.0)

        tracker.add(sample(0.0, 0.0))
        tracker.add(sample(5.0, 25.0))

        // 25 m covered: 5 m past the last boundary is not discarded.
        assertEquals(25.0, tracker.totalDistanceM, 0.05)
    }

    @Test
    fun `no crossing is reported before its boundary`() {
        val tracker = DistanceTracker()
        tracker.addBoundary(1000.0)
        tracker.add(sample(0.0, 0.0))
        assertTrue(tracker.add(sample(1.0, 3.0)).isEmpty())
    }

    @Test
    fun `a boundary is never reported twice`() {
        val tracker = DistanceTracker()
        tracker.addBoundary(10.0)
        tracker.add(sample(0.0, 0.0))
        assertEquals(1, tracker.add(sample(5.0, 12.0)).size)
        assertTrue(tracker.add(sample(6.0, 15.0)).isEmpty())
    }

    @Test
    fun `reset clears distance and pending boundaries`() {
        val tracker = DistanceTracker()
        tracker.addBoundary(10.0)
        tracker.add(sample(0.0, 0.0))
        tracker.add(sample(5.0, 30.0))
        tracker.reset()
        assertEquals(0.0, tracker.totalDistanceM, 1e-9)
        // Boundary is gone too: a fresh run from zero does not re-fire at 10 m.
        tracker.add(sample(0.0, 0.0))
        assertTrue(tracker.add(sample(5.0, 30.0)).isEmpty())
    }

    @Test
    fun `a plausible gap across a tunnel is counted, an implausible jump is filtered out`() {
        val filter = SampleFilter()
        val tracker = DistanceTracker()

        // Start, then a 15 s outage with 75 m of straight-line travel: 5 m/s, counted as a tunnel.
        tracker.add(sample(0.0, 0.0).also { filter.accept(it) })
        val tunnelOut = sample(15.0, 75.0)
        assertTrue(filter.accept(tunnelOut))
        tracker.add(tunnelOut)
        assertEquals(75.0, tracker.totalDistanceM, 1.0)

        // A 150 m jump in 15 s (10 m/s) is impossible: the filter drops it, distance does not move.
        val impossible = sample(30.0, 225.0)
        assertTrue(!filter.accept(impossible))
        assertEquals(75.0, tracker.totalDistanceM, 1.0)
    }
}
