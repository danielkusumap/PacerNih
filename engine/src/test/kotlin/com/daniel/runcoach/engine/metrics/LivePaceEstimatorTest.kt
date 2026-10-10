package com.daniel.runcoach.engine.metrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LivePaceEstimatorTest {

    @Test
    fun `doppler speed is the time weighted average`() {
        val estimator = LivePaceEstimator()
        val speeds = listOf(3.2, 3.1, 3.0, 2.9, 3.0)
        var distanceM = 0.0
        speeds.forEachIndexed { index, speed ->
            estimator.add(index * 1000L, distanceM, speedMps = speed)
            distanceM += speed
        }
        assertEquals(3.04, estimator.liveSpeedMps()!!, 0.001)
        assertEquals(328.95, estimator.livePaceSecPerKm()!!, 0.05)
    }

    @Test
    fun `falls back to distance over the window when no speed is reported`() {
        val estimator = LivePaceEstimator()
        for (i in 0..19) {
            estimator.add(i * 1000L, i * 3.0) // 3 m/s
        }
        assertEquals(3.0, estimator.liveSpeedMps()!!, 0.01)
    }

    @Test
    fun `a stopped runner has no live pace`() {
        val estimator = LivePaceEstimator()
        var distanceM = 0.0
        for (i in 0..19) {
            estimator.add(i * 1000L, distanceM, speedMps = 0.1)
            distanceM += 0.1
        }
        assertNull(estimator.livePaceSecPerKm())
    }

    @Test
    fun `too few samples yield no estimate`() {
        val estimator = LivePaceEstimator()
        estimator.add(0L, 0.0, speedMps = 3.0)
        estimator.add(1000L, 3.0, speedMps = 3.0)
        assertNull(estimator.liveSpeedMps())
    }

    @Test
    fun `pace stays stable on a noisy constant run`() {
        val estimator = LivePaceEstimator()
        val baseSpeedMps = 1000.0 / 360.0 // 6:00 /km
        var distanceM = 0.0
        for (i in 0..29) {
            // Deterministic +-0.08 noise that cancels over the window.
            val speed = baseSpeedMps + 0.08 * ((i % 3) - 1)
            estimator.add(i * 1000L, distanceM, speedMps = speed, speedAccuracyMps = 0.2)
            distanceM += speed
        }
        val pace = estimator.livePaceSecPerKm()!!
        assertTrue(kotlin.math.abs(pace - 360.0) < 5.0, "pace deviated: $pace")
    }
}
