package com.daniel.runcoach.engine.gps

import com.daniel.runcoach.engine.EngineConfig

/** Quality of the current GPS fix shown on the pre-start and run screens (plan S04). */
enum class GpsFixQuality { READY, SEARCHING, WEAK }

/**
 * Maps a fix accuracy to its quality label: no fix yet -> SEARCHING, accuracy at or better than
 * [EngineConfig.goodFixAccuracyM] -> READY, otherwise WEAK. Pure, so it is unit tested.
 */
fun gpsFixQuality(accuracyM: Double?, config: EngineConfig = EngineConfig()): GpsFixQuality = when {
    accuracyM == null -> GpsFixQuality.SEARCHING
    accuracyM <= config.goodFixAccuracyM -> GpsFixQuality.READY
    else -> GpsFixQuality.WEAK
}
