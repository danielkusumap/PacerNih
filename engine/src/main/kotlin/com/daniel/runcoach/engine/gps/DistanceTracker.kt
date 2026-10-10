package com.daniel.runcoach.engine.gps

import com.daniel.runcoach.engine.EngineConfig

/** A cumulative-distance threshold that was passed, with the moving-clock time interpolated there. */
data class BoundaryCrossing(
    val boundaryM: Double,
    val timeMs: Long,
)

/**
 * Accumulates calibrated distance from filtered samples and reports when configured cumulative-distance
 * boundaries are passed (plan F4, F6).
 *
 * The time axis is supplied by the caller as the **moving clock** `mt` (F5), so pause handling lives in
 * the runner and the tracker stays dumb about it. Input is a [FilteredSample]; only MOVING/RESET advance
 * the reference, and stationary/jitter samples earn no distance.
 *
 * Each boundary `B` with `D0 < B <= D1` between two samples is emitted with the time interpolated
 * linearly: `mtB = mt0 + (B - D0) / (D1 - D0) * (mt1 - mt0)`. Several boundaries in one sample are
 * emitted in order. The overshoot stays in [totalDistanceM], so the runner can carry `D1 - B` into the
 * next segment (F6).
 */
class DistanceTracker(
    private val config: EngineConfig = EngineConfig(),
    calibrationFactor: Double = 1.0,
) {

    private val calibration: Double = calibrationFactor.coerceIn(config.calibrationMin, config.calibrationMax)

    /** Cumulative calibrated distance from the first accepted sample, in metres. */
    var totalDistanceM: Double = 0.0
        private set

    private var lastMtMs: Long? = null
    private val pendingBoundaries = ArrayDeque<Double>()

    /** Registers a boundary at [cumulativeDistanceM], added in non-decreasing order by the runner. */
    fun addBoundary(cumulativeDistanceM: Double) {
        pendingBoundaries.addLast(cumulativeDistanceM)
    }

    /**
     * Feeds a filtered sample with its moving-clock time in milliseconds and returns the boundaries
     * passed since the previous reference. Rejected/stationary samples and zero-distance steps yield none.
     */
    fun add(mtMs: Long, filtered: FilteredSample): List<BoundaryCrossing> {
        if (filtered.outcome == GpsOutcome.REJECTED || filtered.outcome == GpsOutcome.STATIONARY) {
            return emptyList()
        }

        val previousMtMs = lastMtMs
        lastMtMs = mtMs

        val stepM = filtered.stepM * calibration
        if (stepM <= 0.0) return emptyList()

        val distanceBeforeM = totalDistanceM
        totalDistanceM += stepM
        if (previousMtMs == null || pendingBoundaries.isEmpty()) return emptyList()

        val dtMs = mtMs - previousMtMs
        val crossings = mutableListOf<BoundaryCrossing>()
        while (pendingBoundaries.isNotEmpty() && pendingBoundaries.first() <= totalDistanceM) {
            val boundary = pendingBoundaries.removeFirst()
            if (boundary <= distanceBeforeM) continue
            val fraction = (boundary - distanceBeforeM) / stepM
            crossings += BoundaryCrossing(
                boundaryM = boundary,
                timeMs = previousMtMs + (fraction * dtMs).toLong(),
            )
        }
        return crossings
    }

    /** Clears accumulated distance, the reference time base, and all pending boundaries. */
    fun reset() {
        totalDistanceM = 0.0
        lastMtMs = null
        pendingBoundaries.clear()
    }
}
