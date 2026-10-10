package com.daniel.runcoach.debug

import com.daniel.runcoach.engine.EngineConfig
import com.daniel.runcoach.engine.runner.AlertEvent
import com.daniel.runcoach.engine.runner.RunnerState
import com.daniel.runcoach.engine.runner.WorkoutRunner
import com.daniel.runcoach.engine.model.Workout
import com.daniel.runcoach.engine.sim.SimScenario
import com.daniel.runcoach.service.location.SimulatedLocationSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlin.math.floor

/** Result of one simulated run, mirroring the golden-test summary so the two can be compared. */
data class SimulationResult(val state: RunnerState, val events: List<AlertEvent>) {
    fun summary(workoutId: String, scenario: SimScenario, seconds: Int): String {
        val counts = events.groupingBy { it::class.simpleName ?: "?" }.eachCount().toSortedMap()
        return buildString {
            appendLine("workout=$workoutId")
            appendLine("scenario=$scenario seconds=$seconds")
            appendLine("totalDistanceM=${floor(state.totalDistanceM).toInt()}")
            appendLine("movingMs=${state.movingMs}")
            appendLine("planCompleted=${state.planCompleted}")
            appendLine("goalReached=${state.goalReached}")
            counts.forEach { (name, count) -> appendLine("event.$name=$count") }
        }
    }
}

/**
 * Drives a [WorkoutRunner] with a [SimulatedLocationSource] on a synthetic clock, so a whole workout can be
 * played back at 1x/10x/60x (plan P1.6). Debug builds only.
 */
class SimulationController(
    private val workout: Workout,
    private val scenario: SimScenario,
    private val seconds: Int,
    private val timeScale: Int,
) {

    suspend fun run(): SimulationResult {
        val runner = WorkoutRunner(workout, EngineConfig(startCountdownSec = 0))
        runner.onStart(0L)
        val stepDelayMs = if (timeScale > 0) 1000L / timeScale else 0L
        SimulatedLocationSource(scenario, seconds).samples().collect { sample ->
            val simMs = sample.timeNanos / 1_000_000L
            runner.onTick(simMs)
            runner.onSample(sample)
            if (stepDelayMs > 0L) delay(stepDelayMs)
        }
        return SimulationResult(runner.state.value, runner.emittedEvents)
    }
}
