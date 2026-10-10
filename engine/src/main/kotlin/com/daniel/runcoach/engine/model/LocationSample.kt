package com.daniel.runcoach.engine.model

/**
 * One raw GPS fix handed to the engine.
 *
 * [timeNanos] is a monotonic clock reading (`elapsedRealtimeNanos` on Android), never wall-clock:
 * it keeps the engine deterministic and immune to clock changes. [speedMps] is the Doppler speed
 * when the platform provides it, else null.
 */
data class LocationSample(
    val timeNanos: Long,
    val lat: Double,
    val lon: Double,
    val accuracyM: Double,
    val speedMps: Double? = null,
)
