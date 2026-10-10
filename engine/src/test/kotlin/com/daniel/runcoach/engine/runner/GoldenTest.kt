package com.daniel.runcoach.engine.runner

import com.daniel.runcoach.engine.EngineConfig
import com.daniel.runcoach.engine.model.Samples
import com.daniel.runcoach.engine.model.Workout
import com.daniel.runcoach.engine.sim.SimScenario
import com.daniel.runcoach.engine.sim.TrackSimulator
import java.io.File
import kotlin.math.floor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * End-to-end golden tests: each workout run over a deterministic scenario must produce the same event
 * summary every time (plan P0.9). Regenerate the golden files with the `RC_UPDATE_GOLDEN` environment
 * variable set; otherwise a missing or changed summary fails the test.
 */
class GoldenTest {

    @Test
    fun stable5kSteady() = assertGolden("stable5k_steady", Samples.stable5k(), SimScenario.STEADY, 2000)

    @Test
    fun stable5kSlowDown() = assertGolden("stable5k_slowdown", Samples.stable5k(), SimScenario.SLOW_DOWN, 2000)

    @Test
    fun stable5kSpeedUp() = assertGolden("stable5k_speedup", Samples.stable5k(), SimScenario.SPEED_UP, 2000)

    @Test
    fun stable5kNoisy() = assertGolden("stable5k_noisy", Samples.stable5k(), SimScenario.NOISY, 2000)

    @Test
    fun stable5kGpsJump() = assertGolden("stable5k_gpsjump", Samples.stable5k(), SimScenario.GPS_JUMP, 2000)

    @Test
    fun stable5kStopAndGo() = assertGolden("stable5k_stopandgo", Samples.stable5k(), SimScenario.STOP_AND_GO, 2000)

    @Test
    fun stable5kTunnelGap() = assertGolden("stable5k_tunnelgap", Samples.stable5k(), SimScenario.TUNNEL_GAP, 2000)

    @Test
    fun intervalSteady() = assertGolden("interval_steady", Samples.interval(), SimScenario.STEADY, 2000)

    @Test
    fun intervalIntervals() = assertGolden("interval_intervals", Samples.interval(), SimScenario.INTERVALS, 2000)

    @Test
    fun intervalNoisy() = assertGolden("interval_noisy", Samples.interval(), SimScenario.NOISY, 2000)

    @Test
    fun tempoSteady() = assertGolden("tempo_steady", Samples.tempo(), SimScenario.STEADY, 2000)

    @Test
    fun tempoSlowDown() = assertGolden("tempo_slowdown", Samples.tempo(), SimScenario.SLOW_DOWN, 2000)

    @Test
    fun runWalkSteady() = assertGolden("runwalk_steady", Samples.runWalk(), SimScenario.STEADY, 2400)

    @Test
    fun freeRunSteady() = assertGolden("freerun_steady", Samples.freeRun(), SimScenario.STEADY, 600)

    private fun assertGolden(
        name: String,
        workout: Workout,
        scenario: SimScenario,
        seconds: Int,
    ) {
        val actual = summary(workout, scenario, seconds)
        if (System.getenv("RC_UPDATE_GOLDEN") != null) {
            File("src/test/resources/golden/$name.txt").apply { parentFile.mkdirs() }.writeText(actual)
            return
        }
        val expected = javaClass.getResourceAsStream("/golden/$name.txt")
            ?.bufferedReader()
            ?.readText()
        assertNotNull(expected, "missing golden '$name'; run tests once with RC_UPDATE_GOLDEN=1")
        assertEquals(expected.replace("\r\n", "\n").trimEnd(), actual.trimEnd(), "golden mismatch: $name")
    }

    private fun summary(workout: Workout, scenario: SimScenario, seconds: Int): String {
        val runner = WorkoutRunner(workout, EngineConfig(startCountdownSec = 0))
        runner.onStart(0)
        for (sample in TrackSimulator().generate(scenario, seconds)) {
            runner.onTick(sample.timeNanos / 1_000_000L)
            runner.onSample(sample)
        }
        val state = runner.state.value
        val counts = runner.emittedEvents
            .groupingBy { it::class.simpleName ?: "?" }
            .eachCount()
            .toSortedMap()
        return buildString {
            appendLine("workout=${workout.id}")
            appendLine("scenario=$scenario seconds=$seconds")
            appendLine("totalDistanceM=${floor(state.totalDistanceM).toInt()}")
            appendLine("movingMs=${state.movingMs}")
            appendLine("planCompleted=${state.planCompleted}")
            appendLine("goalReached=${state.goalReached}")
            counts.forEach { (name, count) -> appendLine("event.$name=$count") }
        }
    }
}
