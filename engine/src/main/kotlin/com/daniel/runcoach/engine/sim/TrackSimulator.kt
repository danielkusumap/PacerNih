package com.daniel.runcoach.engine.sim

import com.daniel.runcoach.engine.model.LocationSample
import kotlin.random.Random

/** Synthetic GPS scenarios used by the simulator and golden tests (plan P0.9). */
enum class SimScenario { STEADY, SLOW_DOWN, SPEED_UP, INTERVALS, GPS_JUMP, STOP_AND_GO, TUNNEL_GAP, NOISY }

/**
 * Deterministic synthetic GPS track generator, 1 Hz (plan P0.9). Same seed and inputs always produce the
 * same samples, so it can back golden tests. Uses a seeded [Random] only for the NOISY scenario; nothing
 * reads the system clock.
 */
class TrackSimulator(private val seed: Long = 42L) {

    fun generate(scenario: SimScenario, seconds: Int, baseSpeedMps: Double = 2.78): List<LocationSample> {
        val random = Random(seed)
        val samples = ArrayList<LocationSample>(seconds)
        var northM = 0.0
        var second = 0
        while (second < seconds) {
            if (scenario == SimScenario.TUNNEL_GAP && second in GAP_START_SEC until GAP_END_SEC) {
                northM += baseSpeedMps // keep moving through the outage, just emit no fixes
                second++
                continue
            }
            val speed = speedFor(scenario, second, seconds, baseSpeedMps, random)
            val sampleNorthM = if (scenario == SimScenario.GPS_JUMP && second == JUMP_SEC) {
                northM + GPS_JUMP_M
            } else {
                northM
            }
            samples += LocationSample(
                timeNanos = second.toLong() * 1_000_000_000L,
                lat = sampleNorthM / 111_195.0,
                lon = 0.0,
                accuracyM = 5.0,
                speedMps = speed,
                speedAccuracyMps = 0.2,
            )
            northM += speed
            second++
        }
        return samples
    }

    private fun speedFor(
        scenario: SimScenario,
        second: Int,
        seconds: Int,
        base: Double,
        random: Random,
    ): Double = when (scenario) {
        SimScenario.STEADY -> base
        SimScenario.SLOW_DOWN -> base + 0.7 - 1.3 * (second.toDouble() / seconds)
        SimScenario.SPEED_UP -> base - 0.6 + 1.3 * (second.toDouble() / seconds)
        SimScenario.INTERVALS -> if ((second / 30) % 2 == 0) base + 1.4 else base - 0.6
        SimScenario.GPS_JUMP -> base
        SimScenario.STOP_AND_GO -> if (second % 90 < 60) base else 0.0
        SimScenario.TUNNEL_GAP -> base
        SimScenario.NOISY -> (base + random.nextDouble(-0.3, 0.3)).coerceAtLeast(0.0)
    }

    private companion object {
        const val GAP_START_SEC = 200
        const val GAP_END_SEC = 215
        const val JUMP_SEC = 300
        const val GPS_JUMP_M = 80.0
    }
}
