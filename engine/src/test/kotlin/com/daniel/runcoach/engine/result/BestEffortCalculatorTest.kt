package com.daniel.runcoach.engine.result

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BestEffortCalculatorTest {

    /**
     * Builds a cumulative-distance/moving-time curve. [paceAt] is evaluated at the start of each
     * step, so a pace change at an exact distance applies from that distance onward.
     */
    private fun track(totalM: Int, stepM: Int, paceAt: (Double) -> Double): List<DistTimePoint> {
        val points = mutableListOf(DistTimePoint(0.0, 0L))
        var timeMs = 0.0
        var distanceM = 0.0
        while (distanceM < totalM) {
            val previous = distanceM
            distanceM = minOf(distanceM + stepM, totalM.toDouble())
            // metres * (seconds per km) = milliseconds.
            timeMs += (distanceM - previous) * paceAt(previous)
            points += DistTimePoint(distanceM, timeMs.toLong())
        }
        return points
    }

    private fun best(result: List<BestEffort>, key: BestEffortKey) = result.first { it.key == key }

    private fun assertMs(expected: Double, actual: Long, toleranceMs: Double = 1.0) =
        assertEquals(expected, actual.toDouble(), toleranceMs)

    @Test
    fun `constant pace gives exact best efforts`() {
        val result = BestEffortCalculator.compute(track(10_000, 100) { 360.0 })
        assertMs(360_000.0, best(result, BestEffortKey.K1).durationMs, 50.0)
        assertEquals(0.0, best(result, BestEffortKey.K1).startOffsetM, 0.5)
        assertMs(1_800_000.0, best(result, BestEffortKey.K5).durationMs, 200.0)
    }

    @Test
    fun `a fast kilometre in the middle is found as the 1 km best effort`() {
        val result = BestEffortCalculator.compute(
            track(10_000, 100) { d -> if (d >= 4000.0 && d < 5000.0) 240.0 else 360.0 },
        )
        val k1 = best(result, BestEffortKey.K1)
        assertMs(240_000.0, k1.durationMs, 50.0)
        assertEquals(4000.0, k1.startOffsetM, 1.0)
        // A 5 km window can only contain that one fast km: 4 km slow + 1 km fast.
        assertMs(1_680_000.0, best(result, BestEffortKey.K5).durationMs, 200.0)
    }

    @Test
    fun `a session shorter than the distance produces no record for it`() {
        val result = BestEffortCalculator.compute(track(4_900, 100) { 360.0 })
        assertEquals(listOf(BestEffortKey.K1), result.map { it.key })
    }

    @Test
    fun `impossibly fast windows are discarded as GPS anomalies`() {
        // 100 s/km is faster than the 150 s/km minimum, so no valid 1 km effort remains.
        val result = BestEffortCalculator.compute(track(2_000, 100) { 100.0 })
        assertTrue(result.isEmpty())
    }

    @Test
    fun `window start is interpolated between samples`() {
        // Constant 300 s/km sampled every 300 m: the 1 km window start lands mid-step.
        val result = BestEffortCalculator.compute(track(5_000, 300) { 300.0 })
        assertMs(300_000.0, best(result, BestEffortKey.K1).durationMs, 1.0)
    }

    @Test
    fun `a session exactly the distance still earns the record`() {
        val result = BestEffortCalculator.compute(track(1_000, 100) { 360.0 })
        assertMs(360_000.0, best(result, BestEffortKey.K1).durationMs, 50.0)
    }

    @Test
    fun `too few points yield nothing`() {
        assertTrue(BestEffortCalculator.compute(emptyList()).isEmpty())
        assertTrue(BestEffortCalculator.compute(listOf(DistTimePoint(0.0, 0L))).isEmpty())
    }
}
