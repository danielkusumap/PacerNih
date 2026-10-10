package com.daniel.runcoach

import android.app.Application
import org.osmdroid.config.Configuration

/** Sets the osmdroid User-Agent once (plan P3.1) so OpenStreetMap tiles are fetched politely. */
class RunCoachApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName
    }
}
