package com.daniel.runcoach.service

import com.daniel.runcoach.engine.alert.CorrectionState
import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.runner.RunnerPhase
import com.daniel.runcoach.engine.runner.RunnerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * UI-facing snapshot of the running session (plan P1.7). Same-process bridge between the foreground service
 * (which owns the engine) and the run screen.
 */
data class RunUiState(
    val phase: RunnerPhase = RunnerPhase.IDLE,
    val workoutName: String = "",
    val state: RunnerState = RunnerState(),
    val correction: CorrectionState = CorrectionState.STABLE,
    val requiredPaceSecPerKm: Double? = null,
    val keepScreenOn: Boolean = false,
    val steps: List<Segment> = emptyList(),
    val trackPoints: List<Pair<Double, Double>> = emptyList(),
    val accuracyM: Double? = null,
)

/** Single source of run state for the UI. Updated by [RunForegroundService] every second. */
object RunStateHolder {
    private val _state = MutableStateFlow(RunUiState())
    val state: StateFlow<RunUiState> = _state

    fun update(value: RunUiState) {
        _state.value = value
    }

    fun reset() {
        _state.value = RunUiState()
    }
}
