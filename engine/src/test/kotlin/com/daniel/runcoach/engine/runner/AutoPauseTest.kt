package com.daniel.runcoach.engine.runner

import com.daniel.runcoach.engine.EngineConfig
import com.daniel.runcoach.engine.model.LocationSample
import com.daniel.runcoach.engine.model.Samples
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AutoPauseTest {

    private fun sample(t: Int, north: Double, speed: Double) = LocationSample(
        timeNanos = t * 1_000L * 1_000_000L,
        lat = north / 111_195.0,
        lon = 0.0,
        accuracyM = 5.0,
        speedMps = speed,
        speedAccuracyMps = 0.2,
    )

    /** P3.3 AC: auto-pause removes stopped time from the moving clock (not retroactive, F26). */
    @Test
    fun autoPauseExcludesStoppedTime() {
        val runner = WorkoutRunner(Samples.stable5k(), EngineConfig(startCountdownSec = 0, autoPauseEnabled = true))
        runner.onStart(0L)
        var north = 0.0
        fun step(t: Int, speed: Double) {
            north += speed
            runner.onTick(t * 1000L)
            runner.onSample(sample(t, north, speed))
        }
        for (t in 1..10) step(t, 3.0)
        for (t in 11..20) step(t, 0.0)
        for (t in 21..30) step(t, 3.0)

        assertTrue(runner.emittedEvents.any { it is AlertEvent.AutoPaused }, "expected AutoPaused")
        assertTrue(runner.emittedEvents.any { it is AlertEvent.AutoResumed }, "expected AutoResumed")
        // Pause starts 3 s after stopping (t=14) and resumes 2 s after moving (t=23): 9 s excluded from 30 s.
        assertEquals(21_000.0, runner.state.value.movingMs.toDouble(), 1_500.0)
        assertEquals(RunnerPhase.RUNNING, runner.state.value.phase)
    }

    @Test
    fun disabledByDefault() {
        val runner = WorkoutRunner(Samples.stable5k(), EngineConfig(startCountdownSec = 0))
        runner.onStart(0L)
        runner.onTick(1_000L)
        runner.onSample(sample(1, 0.0, 0.0))
        runner.onTick(5_000L)
        assertTrue(runner.emittedEvents.none { it is AlertEvent.AutoPaused })
        assertEquals(RunnerPhase.RUNNING, runner.state.value.phase)
    }
}
