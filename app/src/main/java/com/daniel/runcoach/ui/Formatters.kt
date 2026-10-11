package com.daniel.runcoach.ui

import com.daniel.runcoach.engine.EngineConfig
import com.daniel.runcoach.engine.roundHalfUp
import java.util.Locale

/**
 * The single place numbers are formatted for display and notifications (plan rule #14, section 6.13).
 * Locale-aware so decimals use a comma under an Indonesian locale.
 */
object Formatters {

    /** Pace as `m:ss` per km, or `--:--` when null or slower than the display limit. */
    fun pace(secPerKm: Double?, config: EngineConfig = EngineConfig()): String {
        if (secPerKm == null || secPerKm > config.maxPaceDisplaySec) return "--:--"
        val rounded = roundHalfUp(secPerKm).toInt()
        return "%d:%02d".format(rounded / 60, rounded % 60)
    }

    /** Duration as `mm:ss`, or `h:mm:ss` from one hour up. */
    fun duration(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    /** Distance in kilometres with two decimals, locale-aware (e.g. `5,03 km`). */
    fun distanceKm(meters: Double): String = String.format(Locale.getDefault(), "%.2f km", meters / 1000.0)

    /** Segment distance: metres below 1 km (`400 m`), otherwise kilometres (`1,50 km`). */
    fun segmentDistance(meters: Double): String =
        if (meters < 1000.0) "${meters.toInt()} m" else distanceKm(meters)

    /** A signed pace difference in seconds, e.g. `+0:12` or `-0:05`. */
    fun deltaSeconds(seconds: Long): String {
        val sign = if (seconds < 0) "-" else "+"
        val magnitude = kotlin.math.abs(seconds)
        return "%s%d:%02d".format(sign, magnitude / 60, magnitude % 60)
    }

    /** Target pace with its bound: `≤ 6:30` (must not be slower) or `≥ 8:25` (must not be faster). */
    fun targetPace(secPerKm: Int?, bound: com.daniel.runcoach.engine.model.PaceTargetBound): String {
        val symbol = if (bound == com.daniel.runcoach.engine.model.PaceTargetBound.MIN) "≥" else "≤"
        return "$symbol ${pace(secPerKm?.toDouble())}"
    }
}
