package com.daniel.runcoach.engine.metrics

import com.daniel.runcoach.engine.EngineConfig
import kotlin.math.floor
import kotlin.math.min

/** Evaluation scope inside a segment: the stretch a pace is judged against (F10). */
data class PaceScope(val startM: Double, val distanceM: Double)

/**
 * Pure pace math: speed<->pace, averages, evaluation scope, projected and required pace
 * (plan F1, F9, F10, F11, F12). Live pace lives in [LivePaceEstimator]. No rounding is applied here
 * except where a formula says so; callers apply `roundHalfUp` for display.
 */
object PaceCalculator {

    fun paceSecPerKmFromSpeed(speedMps: Double): Double? =
        if (speedMps > 0.0) 1000.0 / speedMps else null

    fun speedMpsFromPace(paceSecPerKm: Double): Double? =
        if (paceSecPerKm > 0.0) 1000.0 / paceSecPerKm else null

    /** Average pace over a segment or the workout; null until at least `minPaceDistanceM` (F9). */
    fun averagePaceSecPerKm(
        movingSec: Double,
        distanceM: Double,
        config: EngineConfig = EngineConfig(),
    ): Double? = if (distanceM < config.minPaceDistanceM) null else movingSec / (distanceM / 1000.0)

    /**
     * Scope for alert/evaluation (F10). With per-km checking on a distance segment of at least 1 km the
     * scope is the current full/partial kilometre; otherwise the whole segment.
     */
    fun scope(segmentLimitM: Double, doneM: Double, perKmCheck: Boolean): PaceScope {
        if (perKmCheck && segmentLimitM >= 1000.0) {
            val km = floor(doneM / 1000.0)
            val startM = km * 1000.0
            return PaceScope(startM, min(1000.0, segmentLimitM - startM))
        }
        return PaceScope(0.0, segmentLimitM)
    }

    /** Projected pace to the end of the scope (F11). Null without live pace or remaining distance. */
    fun projectedPaceSecPerKm(
        paceLiveSecPerKm: Double,
        elapsedScopeSec: Double,
        scopeDistM: Double,
        remainingM: Double,
    ): Double? {
        if (paceLiveSecPerKm <= 0.0 || remainingM <= 0.0 || scopeDistM <= 0.0) return null
        return (elapsedScopeSec + (remainingM / 1000.0) * paceLiveSecPerKm) / (scopeDistM / 1000.0)
    }

    /**
     * Pace needed to still hit the target over the scope (F12). Null when "not realistic": no remaining
     * distance, the target is already beaten by more than the elapsed time, or it would require a pace
     * faster than the shared 150 s/km floor (`recordMinValidPaceSecPerKm`).
     */
    fun requiredPaceSecPerKm(
        targetSecPerKm: Double,
        elapsedScopeSec: Double,
        scopeDistM: Double,
        remainingM: Double,
        config: EngineConfig = EngineConfig(),
    ): Double? {
        val remainingKm = remainingM / 1000.0
        if (remainingKm <= 0.0) return null
        val numerator = targetSecPerKm * (scopeDistM / 1000.0) - elapsedScopeSec
        if (numerator <= 0.0) return null
        val required = numerator / remainingKm
        if (required < config.recordMinValidPaceSecPerKm) return null
        return required
    }
}
