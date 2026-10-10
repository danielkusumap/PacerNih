package com.daniel.runcoach.service.location

import com.daniel.runcoach.engine.model.LocationSample
import kotlinx.coroutines.flow.Flow

/**
 * A source of GPS fixes handed to the engine. Implementations: [FusedLocationSource] (Play Services)
 * and [AndroidLocationSource] (framework fallback). The engine never knows which one is used (plan 3).
 */
interface LocationSource {
    /** Cold flow of ~1 Hz fixes: collecting starts updates, cancelling stops them. */
    fun samples(): Flow<LocationSample>
}
