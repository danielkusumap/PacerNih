package com.daniel.runcoach.engine.runner

import com.daniel.runcoach.engine.EngineConfig
import com.daniel.runcoach.engine.model.Limit
import com.daniel.runcoach.engine.model.LocationSample
import com.daniel.runcoach.engine.model.Samples
import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.model.SegmentKind
import com.daniel.runcoach.engine.model.Workout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkoutRunnerTest {

    private fun timeSegment(label: String, seconds: Int) =
        Segment(label, SegmentKind.RUN, Limit.Duration(seconds), targetPaceSecPerKm = null)

    private fun distanceSegment(label: String, meters: Int) =
        Segment(label, SegmentKind.RUN, Limit.Distance(meters), targetPaceSecPerKm = null)

    /** Drives a runner with a straight northward path, 1 Hz. */
    private class Sim(val runner: WorkoutRunner) {
        var tMs = 0L
        var northM = 0.0

        fun run(seconds: Int, speedMps: Double) {
            repeat(seconds) {
                tMs += 1000
                northM += speedMps
                runner.onTick(tMs)
                runner.onSample(sample(speedMps))
            }
        }

        fun jump(deltaMeters: Double, deltaSeconds: Int) {
            tMs += deltaSeconds * 1000L
            northM += deltaMeters
            runner.onTick(tMs)
            runner.onSample(sample(deltaMeters / deltaSeconds))
        }

        fun idle(seconds: Int) {
            repeat(seconds) {
                tMs += 1000
                runner.onTick(tMs)
            }
        }

        private fun sample(speedMps: Double) = LocationSample(
            timeNanos = tMs * 1_000_000L,
            lat = northM / 111_195.0,
            lon = 0.0,
            accuracyM = 5.0,
            speedMps = speedMps,
            speedAccuracyMps = 0.2,
        )
    }

    private inline fun <reified T : AlertEvent> WorkoutRunner.countOf(): Int =
        emittedEvents.count { it is T }

    private inline fun <reified T : AlertEvent> WorkoutRunner.lastOf(): T? =
        emittedEvents.filterIsInstance<T>().lastOrNull()

    @Test
    fun `initial countdown does not count time or distance`() {
        val runner = WorkoutRunner(Samples.stable5k(), EngineConfig(startCountdownSec = 5))
        runner.onStart(0)
        assertEquals(RunnerPhase.COUNTDOWN, runner.state.value.phase)
        assertEquals(5, runner.lastOf<AlertEvent.Countdown>()?.seconds)

        runner.onSample(LocationSample(1_000_000_000L, 0.001, 0.0, 5.0))
        runner.onTick(2_000)
        assertEquals(0.0, runner.state.value.totalDistanceM, 1e-9)
        assertEquals(0L, runner.state.value.movingMs)

        runner.onTick(5_000)
        assertEquals(RunnerPhase.RUNNING, runner.state.value.phase)
        assertTrue(runner.emittedEvents.contains(AlertEvent.WorkoutStarted))
    }

    @Test
    fun `countdown disabled starts running immediately`() {
        val runner = WorkoutRunner(Samples.stable5k(), EngineConfig(startCountdownSec = 0))
        runner.onStart(0)
        assertEquals(RunnerPhase.RUNNING, runner.state.value.phase)
        assertEquals(1, (runner.lastOf<AlertEvent.SegmentStart>())?.index)
    }

    /** V21: pre-warning and 3-2-1 countdown before a time-based segment change. */
    @Test
    fun v21_timeSegmentCountdownAndPreWarning() {
        val workout = Workout("t", "t", listOf(timeSegment("A", 360), timeSegment("B", 60)))
        val runner = WorkoutRunner(workout, EngineConfig(startCountdownSec = 0, segmentCountdownSec = 3))
        val sim = Sim(runner)
        runner.onStart(0)

        sim.run(356, 3.0)
        assertTrue(runner.emittedEvents.contains(AlertEvent.PreSegmentWarning))
        assertEquals(0, runner.countOf<AlertEvent.Countdown>())

        sim.run(1, 3.0)
        assertEquals(3, runner.lastOf<AlertEvent.Countdown>()?.seconds)
        sim.run(1, 3.0)
        assertEquals(2, runner.lastOf<AlertEvent.Countdown>()?.seconds)
        sim.run(1, 3.0)
        assertEquals(1, runner.lastOf<AlertEvent.Countdown>()?.seconds)
        sim.run(1, 3.0)
        assertEquals(2, runner.lastOf<AlertEvent.SegmentStart>()?.index)
    }

    /** V22: distance cue fires exactly once, and not when the same sample already crossed the boundary. */
    @Test
    fun v22_distanceCueFiresOnce() {
        val workout = Workout("d", "d", listOf(distanceSegment("A", 400), distanceSegment("B", 100)))
        val runner = WorkoutRunner(workout, EngineConfig(startCountdownSec = 0, cueDistanceM = 20))
        val sim = Sim(runner)
        runner.onStart(0)

        sim.run(127, 3.0) // distance ~378 m, cue not yet due
        assertEquals(0, runner.countOf<AlertEvent.DistanceCue>())
        sim.run(1, 3.0) // ~381 m, within 20 m of the 400 m boundary
        assertEquals(1, runner.countOf<AlertEvent.DistanceCue>())
        sim.run(3, 3.0)
        assertEquals(1, runner.countOf<AlertEvent.DistanceCue>())
    }

    @Test
    fun `no distance cue when the sample already crossed the boundary`() {
        val workout = Workout("d", "d", listOf(distanceSegment("A", 400), distanceSegment("B", 100)))
        val runner = WorkoutRunner(workout, EngineConfig(startCountdownSec = 0, cueDistanceM = 20))
        val sim = Sim(runner)
        runner.onStart(0)

        sim.run(126, 3.0) // ~375 m
        sim.jump(30.0, 4) // to ~405 m, crossing the boundary in one sample
        assertEquals(0, runner.countOf<AlertEvent.DistanceCue>())
        assertEquals(2, runner.lastOf<AlertEvent.SegmentStart>()?.index)
    }

    @Test
    fun `short distance segment gets no cue`() {
        val workout = Workout("d", "d", listOf(distanceSegment("A", 30), distanceSegment("B", 10)))
        val runner = WorkoutRunner(workout, EngineConfig(startCountdownSec = 0, cueDistanceM = 20))
        val sim = Sim(runner)
        runner.onStart(0)
        sim.run(20, 3.0) // crosses 30 m
        assertEquals(0, runner.countOf<AlertEvent.DistanceCue>())
    }

    @Test
    fun `the last segment gets no cue`() {
        val runner = WorkoutRunner(Samples.stable5k(), EngineConfig(startCountdownSec = 0, cueDistanceM = 20))
        val sim = Sim(runner)
        runner.onStart(0)
        sim.run(20, 3.0)
        assertEquals(0, runner.countOf<AlertEvent.DistanceCue>())
    }

    @Test
    fun `plan completed keeps recording an open segment without stopping`() {
        val runner = WorkoutRunner(Samples.stable5k(), EngineConfig(startCountdownSec = 0))
        val sim = Sim(runner)
        runner.onStart(0)
        sim.run(1700, 3.0) // ~5100 m, past the 5 km plan

        assertTrue(runner.state.value.planCompleted)
        assertTrue(runner.emittedEvents.contains(AlertEvent.WorkoutPlanCompleted))
        assertEquals(RunnerPhase.RUNNING, runner.state.value.phase)

        val before = runner.state.value.totalDistanceM
        sim.run(5, 3.0)
        assertTrue(runner.state.value.totalDistanceM > before)
    }

    @Test
    fun `goal distance is announced once and the run continues`() {
        val workout = Workout(
            "g", "g",
            goalDistanceM = 1000,
            items = listOf(com.daniel.runcoach.engine.model.Repeat(untilTotalDistanceM = 1000, children = listOf(timeSegment("Run", 100)))),
        )
        val runner = WorkoutRunner(workout, EngineConfig(startCountdownSec = 0))
        val sim = Sim(runner)
        runner.onStart(0)
        sim.run(500, 3.0) // 1500 m over five 100 s rounds

        assertEquals(1, runner.countOf<AlertEvent.GoalDistanceReached>())
        assertTrue(runner.state.value.goalReached)
    }

    @Test
    fun `pause removes time from the moving clock`() {
        val runner = WorkoutRunner(Samples.stable5k(), EngineConfig(startCountdownSec = 0))
        val sim = Sim(runner)
        runner.onStart(0)
        sim.run(10, 3.0)
        assertEquals(10_000L, runner.state.value.movingMs)

        runner.onPause(sim.tMs)
        assertEquals(RunnerPhase.PAUSED_MANUAL, runner.state.value.phase)
        sim.idle(5)
        runner.onResume(sim.tMs)

        assertEquals(10_000L, runner.state.value.movingMs)
        assertEquals(15_000L, runner.state.value.elapsedMs)
        assertEquals(5_000L, runner.state.value.pausedMs)
    }

    @Test
    fun `skip moves to the next segment`() {
        val workout = Workout("s", "s", listOf(distanceSegment("A", 1000), distanceSegment("B", 500)))
        val runner = WorkoutRunner(workout, EngineConfig(startCountdownSec = 0))
        val sim = Sim(runner)
        runner.onStart(0)
        sim.run(10, 3.0)
        runner.onSkipSegment(sim.tMs)

        assertEquals(0, runner.countOf<AlertEvent.WorkoutPlanCompleted>())
        assertEquals(2, runner.lastOf<AlertEvent.SegmentStart>()?.index)
    }

    @Test
    fun `gps loss is announced after the timeout and recovery after a new sample`() {
        val runner = WorkoutRunner(Samples.stable5k(), EngineConfig(startCountdownSec = 0))
        val sim = Sim(runner)
        runner.onStart(0)
        sim.run(3, 3.0)
        sim.idle(11) // no samples for 11 s
        assertTrue(runner.emittedEvents.contains(AlertEvent.GpsLost))
        sim.run(1, 3.0)
        assertTrue(runner.emittedEvents.contains(AlertEvent.GpsRecovered))
        assertFalse(runner.state.value.gpsLost)
    }
}
