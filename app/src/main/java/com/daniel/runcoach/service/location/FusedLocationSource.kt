package com.daniel.runcoach.service.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.daniel.runcoach.engine.model.LocationSample
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * GPS fixes via Play Services' FusedLocationProviderClient (plan P1.2): 1 Hz, high accuracy, no API key.
 * Callers must hold the location permission.
 */
class FusedLocationSource(private val context: Context) : LocationSource {

    @SuppressLint("MissingPermission")
    override fun samples(): Flow<LocationSample> = callbackFlow {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVAL_MS)
            .setMinUpdateIntervalMillis(INTERVAL_MS)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it.toLocationSample()) }
            }
        }
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }

    private companion object {
        const val INTERVAL_MS = 1000L
    }
}
