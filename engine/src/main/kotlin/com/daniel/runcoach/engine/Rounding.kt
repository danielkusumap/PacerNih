package com.daniel.runcoach.engine

import kotlin.math.floor

/** Round half up to the nearest integer (plan 6.14.0). Used wherever the plan says `roundHalfUp`. */
fun roundHalfUp(x: Double): Long = floor(x + 0.5).toLong()
