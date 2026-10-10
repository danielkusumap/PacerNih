package com.daniel.runcoach.engine.gps

import com.daniel.runcoach.engine.EngineConfig
import com.daniel.runcoach.engine.model.LocationSample

/**
 * Rejects unusable GPS fixes before they reach the distance tracker (plan 6.2).
 *
 * A sample is dropped when: its horizontal accuracy is worse than [EngineConfig.maxAccuracyM];
 * its timestamp does not move forward; or its implied speed from the last valid sample exceeds
 * [EngineConfig.maxSpeedMps]. Every drop is counted in [droppedCount] for the session's GPS
 * quality stats.
 *
 * Stateful and single-use per run: it remembers the last accepted sample as the reference for
 * the next speed check. Not thread safe; drive it from one coroutine.
 */
class SampleFilter(private val config: EngineConfig = EngineConfig()) {

    /** Number of samples rejected so far. Persisted per session as `droppedSampleCount`. */
    var droppedCount: Int = 0
        private set

    private var lastAccepted: LocationSample? = null

    /** Returns true if the sample is usable; false if it was dropped. */
    fun accept(sample: LocationSample): Boolean {
        if (sample.accuracyM > config.maxAccuracyM) {
            droppedCount++
            return false
        }
        val previous = lastAccepted
        if (previous != null) {
            if (sample.timeNanos <= previous.timeNanos) {
                droppedCount++
                return false
            }
            val distanceM = haversineM(
                previous.lat, previous.lon, sample.lat, sample.lon, config.earthRadiusM,
            )
            val dtSec = (sample.timeNanos - previous.timeNanos) / NANOS_PER_SEC
            val impliedSpeedMps = distanceM / dtSec
            if (impliedSpeedMps > config.maxSpeedMps) {
                droppedCount++
                return false
            }
        }
        lastAccepted = sample
        return true
    }

    private companion object {
        const val NANOS_PER_SEC = 1_000_000_000.0
    }
}
