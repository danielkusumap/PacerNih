package com.daniel.runcoach.ui

import com.daniel.runcoach.engine.model.Workout

/** Hand-off for the pre-start route: the workout chosen on Beranda. */
object StartRequestHolder {
    var workout: Workout? = null
}
