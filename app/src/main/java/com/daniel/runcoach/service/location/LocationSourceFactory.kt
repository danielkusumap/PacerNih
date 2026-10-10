package com.daniel.runcoach.service.location

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

/** Picks Play Services when present, else the framework GPS fallback (plan P1.2). */
object LocationSourceFactory {

    fun create(context: Context): LocationSource {
        val playServicesAvailable =
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
        return if (playServicesAvailable) FusedLocationSource(context) else AndroidLocationSource(context)
    }
}
