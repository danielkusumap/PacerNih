package com.daniel.runcoach.engine.runner

import com.daniel.runcoach.engine.EngineConfig
import com.daniel.runcoach.engine.model.Limit
import com.daniel.runcoach.engine.model.LocationSample
import com.daniel.runcoach.engine.model.Repeat
import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.model.SegmentKind
import com.daniel.runcoach.engine.model.Workout
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * B2: the run/walk (minute-based) workout must raise the whole alert sequence. This is the engine half of the
 * golden sequence; the announcer turns these into speech (see RunAnnouncerTest).
 */
class RunWalkGoldenTest {

    private class Sim(val runner: WorkoutRunner) {
        var tMs = 0L
        var northM = 0.0
        fun run(seconds: Int, speedMps: Double) {
            repeat(seconds) {
                tMs += 1000
                northM += speedMps
                runner.onTick(tMs)
                runner.onSample(
                    LocationSample(
                        timeNanos = tMs * 1_000_000L,
                        lat = 0.0,
                        lon = northM / 111_195.0,
                        accuracyM = 5.0,
                        speedMps = speedMps,
                    ),
                )
            }
        }
    }

    private fun runWalk() = Workout(
        "rw", "Run/Walk",
        items = listOf(
            Repeat(
                untilTotalDistanceM = 5000,
                children = listOf(
                    Segment("Lari", SegmentKind.RUN, Limit.Duration(360), null),
                    Segment("Jalan", SegmentKind.WALK, Limit.Duration(120), null),
                ),
            ),
        ),
        goalDistanceM = 5000,
    )

    @Test
    fun runWalkRaisesTheFullAlertSequence() {
        val runner = WorkoutRunner(runWalk(), EngineConfig(startCountdownSec = 5, segmentCountdownSec = 3))
        val sim = Sim(runner)
        runner.onStart(0)
        sim.run(1850, 3.0) // past the 5 km goal and the end of the running child that crosses it

        val events = runner.emittedEvents

        // A01: start countdown 5..1.
        val startCountdown = events.filterIsInstance<AlertEvent.Countdown>().map { it.seconds }.take(5)
        assertTrue(startCountdown == listOf(5, 4, 3, 2, 1), "start countdown 5..1, was $startCountdown")
        // A02 + A03.
        assertTrue(events.contains(AlertEvent.WorkoutStarted), "A02")
        assertTrue(events.filterIsInstance<AlertEvent.SegmentStart>().any { it.index == 1 }, "A03 segment 1")
        // A04 pre-warning then A05 3-2-1 before the second segment starts.
        val firstWarning = events.indexOfFirst { it is AlertEvent.PreSegmentWarning }
        val secondStart = events.indexOfFirst { it is AlertEvent.SegmentStart && it.index == 2 }
        assertTrue(firstWarning in 1 until secondStart, "A04 before A03 of segment 2")
        val segmentCountdowns = events.drop(firstWarning).take(secondStart - firstWarning)
            .filterIsInstance<AlertEvent.Countdown>().map { it.seconds }
        assertTrue(segmentCountdowns == listOf(3, 2, 1), "A05 3-2-1, was $segmentCountdowns")
        // A07 SegmentEnd before the next A03.
        val firstEnd = events.indexOfFirst { it is AlertEvent.SegmentEnd }
        assertTrue(firstEnd in 1 until secondStart, "A07 before A03 of segment 2")
        // A11 km markers, A12 goal, A13 plan complete.
        assertTrue(events.any { it is AlertEvent.KmMarker }, "A11")
        assertTrue(events.contains(AlertEvent.GoalDistanceReached), "A12")
        assertTrue(events.contains(AlertEvent.WorkoutPlanCompleted), "A13")
        // No correction alerts for a target-less workout.
        assertTrue(events.none { it is AlertEvent.TooSlow || it is AlertEvent.TooFast || it is AlertEvent.BackToStable }, "no A08-A10")
    }
}
