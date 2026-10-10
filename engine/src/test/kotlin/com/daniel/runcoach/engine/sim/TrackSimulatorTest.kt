package com.daniel.runcoach.engine.sim

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrackSimulatorTest {

    @Test
    fun `the same seed produces identical tracks`() {
        val a = TrackSimulator(7L).generate(SimScenario.NOISY, 120)
        val b = TrackSimulator(7L).generate(SimScenario.NOISY, 120)
        assertEquals(a, b)
    }

    @Test
    fun `steady keeps a constant speed`() {
        val samples = TrackSimulator().generate(SimScenario.STEADY, 60, baseSpeedMps = 3.0)
        assertTrue(samples.all { it.speedMps == 3.0 })
    }

    @Test
    fun `slow down decreases and speed up increases`() {
        val slow = TrackSimulator().generate(SimScenario.SLOW_DOWN, 600)
        assertTrue(slow.first().speedMps!! > slow.last().speedMps!!)
        val fast = TrackSimulator().generate(SimScenario.SPEED_UP, 600)
        assertTrue(fast.first().speedMps!! < fast.last().speedMps!!)
    }

    @Test
    fun `gps jump displaces one sample far from its neighbours`() {
        val samples = TrackSimulator().generate(SimScenario.GPS_JUMP, 400)
        val jumped = samples[300]
        val before = samples[299]
        // ~80 m north over one second is an impossible speed.
        val deltaLat = kotlin.math.abs(jumped.lat - before.lat)
        assertTrue(deltaLat * 111_195.0 > 50.0)
        assertTrue(kotlin.math.abs(samples[301].lat - before.lat) * 111_195.0 < 10.0)
    }

    @Test
    fun `tunnel gap leaves a multi second hole with no samples`() {
        val samples = TrackSimulator().generate(SimScenario.TUNNEL_GAP, 300)
        val gap = samples.zipWithNext { a, b -> (b.timeNanos - a.timeNanos) / 1_000_000_000L }.max()
        assertTrue(gap >= 15L, "gap was $gap s")
    }

    @Test
    fun `stop and go contains stopped samples`() {
        val samples = TrackSimulator().generate(SimScenario.STOP_AND_GO, 180)
        assertTrue(samples.any { it.speedMps == 0.0 })
        assertTrue(samples.any { it.speedMps!! > 0.0 })
    }

    @Test
    fun `intervals alternate between fast and slow`() {
        val samples = TrackSimulator().generate(SimScenario.INTERVALS, 120)
        assertTrue(samples[0].speedMps!! > samples[30].speedMps!!)
    }
}
