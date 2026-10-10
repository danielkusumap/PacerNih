package com.daniel.runcoach.service.location

import android.location.Location
import com.daniel.runcoach.engine.model.LocationSample

/** Converts a framework [Location] into the engine's [LocationSample] (plan P1.2). */
internal fun Location.toLocationSample(): LocationSample = LocationSample(
    timeNanos = elapsedRealtimeNanos,
    lat = latitude,
    lon = longitude,
    accuracyM = accuracy.toDouble(),
    speedMps = if (hasSpeed()) speed.toDouble() else null,
    speedAccuracyMps = if (hasSpeedAccuracy()) speedAccuracyMetersPerSecond.toDouble() else null,
)
