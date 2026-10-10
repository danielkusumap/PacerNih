package com.daniel.runcoach.service.location

import com.daniel.runcoach.engine.model.LocationSample
import com.daniel.runcoach.engine.sim.SimScenario
import com.daniel.runcoach.engine.sim.TrackSimulator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

/**
 * A [LocationSource] that replays a synthetic [TrackSimulator] track instead of real GPS (plan P1.6).
 * Debug builds only; used by S15. Samples carry synthetic timestamps, so the consumer must pace them.
 */
class SimulatedLocationSource(
    private val scenario: SimScenario,
    private val seconds: Int,
    private val baseSpeedMps: Double = 2.78,
    private val seed: Long = 42L,
) : LocationSource {

    override fun samples(): Flow<LocationSample> =
        TrackSimulator(seed).generate(scenario, seconds, baseSpeedMps).asFlow()
}
