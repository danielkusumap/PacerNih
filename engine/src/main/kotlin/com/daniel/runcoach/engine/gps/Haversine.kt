package com.daniel.runcoach.engine.gps

import com.daniel.runcoach.engine.EngineConfig
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Great-circle distance between two lat/lon points (metres). */
fun haversineM(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double,
    earthRadiusM: Double = EngineConfig().earthRadiusM,
): Double {
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val dPhi = Math.toRadians(lat2 - lat1)
    val dLambda = Math.toRadians(lon2 - lon1)
    val a = sin(dPhi / 2).let { it * it } +
        cos(phi1) * cos(phi2) * sin(dLambda / 2).let { it * it }
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusM * c
}
