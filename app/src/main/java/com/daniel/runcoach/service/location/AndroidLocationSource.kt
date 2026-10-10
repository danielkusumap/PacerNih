package com.daniel.runcoach.service.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import com.daniel.runcoach.engine.model.LocationSample
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Framework-only GPS fallback for devices without Google Play Services (plan P1.2). Uses the raw GPS
 * provider at 1 Hz. Callers must hold the location permission.
 */
class AndroidLocationSource(private val context: Context) : LocationSource {

    @SuppressLint("MissingPermission")
    override fun samples(): Flow<LocationSample> = callbackFlow {
        val manager = context.getSystemService(LocationManager::class.java)
        val listener = LocationListener { location -> trySend(location.toLocationSample()) }
        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, INTERVAL_MS, 0f, listener, Looper.getMainLooper())
        awaitClose { manager.removeUpdates(listener) }
    }

    private companion object {
        const val INTERVAL_MS = 1000L
    }
}
