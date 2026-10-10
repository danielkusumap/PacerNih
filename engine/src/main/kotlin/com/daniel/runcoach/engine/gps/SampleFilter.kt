package com.daniel.runcoach.engine.gps

import com.daniel.runcoach.engine.EngineConfig
import com.daniel.runcoach.engine.model.LocationSample

/**
 * Decides whether a raw GPS fix is usable and how it moves the reference (plan F3).
 *
 * Every input is classified into a [GpsOutcome]:
 * - accuracy worse than [EngineConfig.maxAccuracyM], a non-advancing timestamp, or an implied speed
 *   above [EngineConfig.maxSpeedMps] -> REJECTED;
 * - a stationary jitter sample (has speed and speed < [EngineConfig.stationarySpeedMps] and moved less
 *   than [EngineConfig.stationaryMaxDispM]) -> STATIONARY, valid for GPS/live pace but earns no distance
 *   and does not advance the reference;
 * - otherwise MOVING, earning the straight-line distance since the reference. A long, plausible jump
 *   (a tunnel/short outage) is a normal MOVING sample.
 *
 * After [EngineConfig.speedRejectResetCount] consecutive speed rejections the reference is reset to the
 * latest fix (RESET) so the filter cannot stall forever after a real GPS jump.
 *
 * Stateful, single-use per run, not thread safe. Uses each sample's monotonic [LocationSample.timeNanos];
 * it never reads the system clock and is unaware of pause (the runner supplies the moving clock elsewhere).
 */
class SampleFilter(private val config: EngineConfig = EngineConfig()) {

    /** Number of dropped samples so far (accuracy, timestamp, or speed). Persisted per session. */
    var droppedCount: Int = 0
        private set

    private var reference: LocationSample? = null
    private var consecutiveSpeedRejections = 0

    fun accept(sample: LocationSample): FilteredSample {
        if (sample.accuracyM > config.maxAccuracyM) return reject()

        val previous = reference
        if (previous == null) {
            reference = sample
            consecutiveSpeedRejections = 0
            return FilteredSample(GpsOutcome.MOVING, 0.0)
        }

        val dtSec = (sample.timeNanos - previous.timeNanos) / NANOS_PER_SEC
        if (dtSec <= 0.0) return reject()

        val distanceM = haversineM(
            previous.lat, previous.lon, sample.lat, sample.lon, config.earthRadiusM,
        )
        if (distanceM / dtSec > config.maxSpeedMps) {
            droppedCount++
            consecutiveSpeedRejections++
            if (consecutiveSpeedRejections >= config.speedRejectResetCount) {
                reference = sample
                consecutiveSpeedRejections = 0
                return FilteredSample(GpsOutcome.RESET, 0.0)
            }
            return FilteredSample(GpsOutcome.REJECTED, 0.0)
        }

        consecutiveSpeedRejections = 0
        val speedMps = sample.speedMps
        if (speedMps != null && speedMps < config.stationarySpeedMps && distanceM < config.stationaryMaxDispM) {
            return FilteredSample(GpsOutcome.STATIONARY, 0.0)
        }

        reference = sample
        return FilteredSample(GpsOutcome.MOVING, distanceM)
    }

    private fun reject(): FilteredSample {
        droppedCount++
        return FilteredSample(GpsOutcome.REJECTED, 0.0)
    }

    private companion object {
        const val NANOS_PER_SEC = 1_000_000_000.0
    }
}
