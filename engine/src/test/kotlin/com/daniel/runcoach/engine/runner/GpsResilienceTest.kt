package com.daniel.runcoach.engine.runner

import com.daniel.runcoach.engine.EngineConfig
import com.daniel.runcoach.engine.model.Samples
import com.daniel.runcoach.engine.sim.SimScenario
import com.daniel.runcoach.engine.sim.TrackSimulator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * P3.6 AC: a simulated `tunnelGap` raises GpsLost and GpsRecovered exactly once each, and the runner keeps a
 * dropped-sample count (GPS quality) that is stored with the session.
 */
class GpsResilienceTest {

    @Test
    fun tunnelGapRaisesGpsLostAndRecoveredExactlyOnce() {
        val runner = WorkoutRunner(Samples.freeRun(), EngineConfig(startCountdownSec = 0))
        runner.onStart(0)
        for (sample in TrackSimulator().generate(SimScenario.TUNNEL_GAP, 400)) {
            runner.onTick(sample.timeNanos / 1_000_000L)
            runner.onSample(sample)
        }
        assertEquals(1, runner.emittedEvents.count { it == AlertEvent.GpsLost })
        assertEquals(1, runner.emittedEvents.count { it == AlertEvent.GpsRecovered })
    }

    @Test
    fun droppedSamplesAreCountedForTheSession() {
        val runner = WorkoutRunner(Samples.freeRun(), EngineConfig(startCountdownSec = 0))
        runner.onStart(0)
        for (sample in TrackSimulator().generate(SimScenario.GPS_JUMP, 400)) {
            runner.onTick(sample.timeNanos / 1_000_000L)
            runner.onSample(sample)
        }
        assertTrue(runner.state.value.droppedSampleCount >= 1, "an 80 m jump must be dropped")
    }
}
