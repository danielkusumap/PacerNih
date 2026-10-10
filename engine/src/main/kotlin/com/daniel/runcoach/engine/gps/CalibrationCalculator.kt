package com.daniel.runcoach.engine.gps

import com.daniel.runcoach.engine.EngineConfig

/**
 * Distance calibration (plan F25). Given the measured distance of a finished session, the factor that was
 * applied to it, and the real distance the runner actually covered, works out the factor to use for the
 * **next** session.
 *
 * It never compounds: the measured distance is first divided by the factor that produced it to recover the
 * raw distance, then the new factor is `actual / raw`, clamped to `CALIBRATION_MIN..CALIBRATION_MAX`.
 */
object CalibrationCalculator {

    fun factorFor(
        actualDistanceM: Double,
        sessionDistanceM: Double,
        factorUsed: Double,
        config: EngineConfig = EngineConfig(),
    ): Double {
        if (actualDistanceM <= 0.0 || sessionDistanceM <= 0.0) return 1.0
        val safeFactor = factorUsed.coerceIn(config.calibrationMin, config.calibrationMax)
        val rawM = sessionDistanceM / safeFactor
        if (rawM <= 0.0) return 1.0
        return (actualDistanceM / rawM).coerceIn(config.calibrationMin, config.calibrationMax)
    }
}
