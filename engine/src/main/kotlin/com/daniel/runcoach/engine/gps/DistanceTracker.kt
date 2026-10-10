package com.daniel.runcoach.engine.gps

import com.daniel.runcoach.engine.EngineConfig
import com.daniel.runcoach.engine.model.LocationSample

/** A cumulative-distance threshold that was passed, with the time interpolated where it fell. */
data class BoundaryCrossing(
    val boundaryM: Double,
    val timeNanos: Long,
)

/**
 * Accumulates distance from accepted samples and reports when configured cumulative-distance
 * boundaries are passed (plan 6.3).
 *
 * Boundaries are absolute distances from the run's origin. When a sample jumps past one or more of
 * them, each is emitted with the time interpolated linearly between the two samples:
 * `t = t1 + (boundary - d1) / (d2 - d1) * (t2 - t1)`. The distance overshoot is not lost: it stays
 * in [totalDistanceM], so the remainder naturally counts toward the next segment.
 *
 * GPS gaps need no special handling here: an implausible jump is rejected upstream by
 * [SampleFilter], while a plausible straight-line jump (a tunnel/short outage) is simply counted.
 *
 * Not thread safe. All time comes from sample timestamps; nothing reads the system clock.
 */
class DistanceTracker(private val config: EngineConfig = EngineConfig()) {

    /** Cumulative distance from the first accepted sample, in metres. */
    var totalDistanceM: Double = 0.0
        private set

    private var lastSample: LocationSample? = null
    private val pendingBoundaries = ArrayDeque<Double>()

    /**
     * Registers a boundary at [cumulativeDistanceM]. Boundaries must be added in non-decreasing
     * order; the runner does this as segments and km markers advance.
     */
    fun addBoundary(cumulativeDistanceM: Double) {
        pendingBoundaries.addLast(cumulativeDistanceM)
    }

    /**
     * Feeds an already-filtered sample and returns the boundaries passed by the movement from the
     * previous sample to this one, in order. Returns nothing for the first sample.
     */
    fun add(sample: LocationSample): List<BoundaryCrossing> {
        val previous = lastSample
        lastSample = sample
        if (previous == null) return emptyList()

        val distanceBeforeM = totalDistanceM
        val stepM = haversineM(
            previous.lat, previous.lon, sample.lat, sample.lon, config.earthRadiusM,
        )
        totalDistanceM += stepM

        if (pendingBoundaries.isEmpty()) return emptyList()

        val crossings = mutableListOf<BoundaryCrossing>()
        val dtNanos = sample.timeNanos - previous.timeNanos
        while (pendingBoundaries.isNotEmpty() && pendingBoundaries.first() <= totalDistanceM) {
            val boundary = pendingBoundaries.removeFirst()
            // Already passed on an earlier sample: never emit it twice.
            if (boundary <= distanceBeforeM) continue
            val fraction = if (stepM > 0.0) (boundary - distanceBeforeM) / stepM else 0.0
            crossings += BoundaryCrossing(
                boundaryM = boundary,
                timeNanos = previous.timeNanos + (fraction * dtNanos).toLong(),
            )
        }
        return crossings
    }

    /** Clears accumulated distance, the last sample, and all pending boundaries. */
    fun reset() {
        totalDistanceM = 0.0
        lastSample = null
        pendingBoundaries.clear()
    }
}
