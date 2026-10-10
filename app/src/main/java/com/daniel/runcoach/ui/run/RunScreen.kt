package com.daniel.runcoach.ui.run

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.daniel.runcoach.R
import com.daniel.runcoach.engine.alert.CorrectionState
import com.daniel.runcoach.engine.model.Limit
import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.model.SegmentKind
import com.daniel.runcoach.engine.runner.RunnerPhase
import com.daniel.runcoach.engine.runner.RunnerState
import com.daniel.runcoach.service.RunForegroundService
import com.daniel.runcoach.service.RunStateHolder
import com.daniel.runcoach.service.RunUiState
import com.daniel.runcoach.ui.Formatters
import com.daniel.runcoach.ui.theme.RunCoachTheme

/** S05 route: observes the run state and keeps the screen on when the setting asks for it. */
@Composable
fun RunRoute(onStop: () -> Unit) {
    val ui by RunStateHolder.state.collectAsStateWithLifecycle()
    val view = LocalView.current
    DisposableEffect(ui.keepScreenOn) {
        view.keepScreenOn = ui.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }
    val context = LocalContext.current
    RunScreen(ui = ui, onStop = { RunForegroundService.stop(context); onStop() })
}

/**
 * S05 Layar Lari (plan section 8): header, segment, live-pace hero with the correction badge, segment
 * metrics, totals, and the next-segment preview. Stateless, so every state can be previewed.
 */
@Composable
fun RunScreen(ui: RunUiState, onStop: () -> Unit) {
    val state = ui.state
    val segment = state.segment
    val monospace = MaterialTheme.typography.displayMedium.copy(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
    )
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // A. Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(ui.workoutName, style = MaterialTheme.typography.titleSmall)
                if (state.gpsLost) {
                    Text(stringResource(R.string.run_gps_lost), color = MaterialTheme.colorScheme.error)
                }
            }

            // B. Segment
            if (ui.phase == RunnerPhase.WAITING_GPS || ui.phase == RunnerPhase.COUNTDOWN) {
                Text(stringResource(R.string.run_waiting_gps), style = MaterialTheme.typography.headlineSmall)
            }
            if (segment != null) {
                val total = state.stepCount
                Text(
                    text = stringResource(R.string.run_segment_of, state.stepIndex, total ?: state.stepIndex) +
                        " · ${kindLabel(segment.kind)}",
                    style = MaterialTheme.typography.titleMedium,
                )
                LinearProgressIndicator(
                    progress = { segmentProgress(state, segment) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(segmentRemainingText(state, segment), style = MaterialTheme.typography.bodySmall)
            }

            // C. Hero: live pace + target + badge
            Text(Formatters.pace(state.paceLiveSecPerKm), style = monospace)
            Text(
                text = segment?.targetPaceSecPerKm
                    ?.let { stringResource(R.string.run_target, Formatters.pace(it.toDouble())) }
                    ?: stringResource(R.string.run_no_target),
                style = MaterialTheme.typography.titleMedium,
            )
            if (segment?.targetPaceSecPerKm != null) {
                Text(
                    text = correctionLabel(ui.correction),
                    color = when (ui.correction) {
                        CorrectionState.STABLE -> MaterialTheme.colorScheme.primary
                        CorrectionState.TOO_SLOW -> MaterialTheme.colorScheme.error
                        CorrectionState.TOO_FAST -> MaterialTheme.colorScheme.tertiary
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            // D. Segment metrics
            if (segment?.targetPaceSecPerKm != null && ui.requiredPaceSecPerKm != null) {
                Text(
                    stringResource(R.string.run_required, Formatters.pace(ui.requiredPaceSecPerKm)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            // E. Totals
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TotalItem(stringResource(R.string.run_label_total), Formatters.duration(state.movingMs))
                    TotalItem("", Formatters.distanceKm(state.totalDistanceM))
                    TotalItem("", Formatters.pace(if (state.totalDistanceM > 0) state.movingMs / state.totalDistanceM else null))
                }
            }

            // F. Next segment preview
            Text(stringResource(R.string.run_next), style = MaterialTheme.typography.labelLarge)
            Text(
                text = state.nextSegment?.let { describeSegment(it) } ?: stringResource(R.string.run_next_free),
                style = MaterialTheme.typography.bodyMedium,
            )

            if (state.planCompleted) Text(stringResource(R.string.run_plan_completed))
            if (state.goalReached) Text(stringResource(R.string.run_goal_reached))
            if (ui.phase == RunnerPhase.PAUSED_MANUAL || ui.phase == RunnerPhase.PAUSED_AUTO) {
                Text(stringResource(R.string.run_paused), style = MaterialTheme.typography.headlineSmall)
            }

            Button(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_stop_run))
            }
        }
    }
}

@Composable
private fun TotalItem(label: String, value: String) {
    Column {
        if (label.isNotEmpty()) Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun kindLabel(kind: SegmentKind): String = stringResource(
    when (kind) {
        SegmentKind.WARMUP -> R.string.kind_warmup
        SegmentKind.RUN -> R.string.kind_run
        SegmentKind.WALK -> R.string.kind_walk
        SegmentKind.RECOVERY -> R.string.kind_recovery
        SegmentKind.COOLDOWN -> R.string.kind_cooldown
    },
)

@Composable
private fun correctionLabel(correction: CorrectionState): String = stringResource(
    when (correction) {
        CorrectionState.STABLE -> R.string.run_status_stable
        CorrectionState.TOO_SLOW -> R.string.run_status_too_slow
        CorrectionState.TOO_FAST -> R.string.run_status_too_fast
    },
)

private fun segmentProgress(state: RunnerState, segment: Segment): Float = when (val limit = segment.limit) {
    is Limit.Distance -> (state.segmentDistanceM / limit.meters.toDouble()).coerceIn(0.0, 1.0).toFloat()
    is Limit.Duration -> (state.segmentMovingMs / (limit.seconds * 1000.0)).coerceIn(0.0, 1.0).toFloat()
    Limit.Open -> 0f
}

private fun segmentRemainingText(state: RunnerState, segment: Segment): String = when (val limit = segment.limit) {
    is Limit.Distance -> "${state.segmentRemainingM.toInt()} m"
    is Limit.Duration -> Formatters.duration(state.segmentRemainingMs)
    Limit.Open -> Formatters.distanceKm(state.segmentDistanceM)
}

@Composable
private fun describeSegment(segment: Segment): String {
    val limit = when (val l = segment.limit) {
        is Limit.Distance -> "${l.meters} m"
        is Limit.Duration -> Formatters.duration(l.seconds * 1000L)
        Limit.Open -> stringResource(R.string.run_no_target)
    }
    val target = segment.targetPaceSecPerKm?.let { ", ≤ ${Formatters.pace(it.toDouble())}" } ?: ""
    return "${kindLabel(segment.kind)} $limit$target"
}

// --- Previews -------------------------------------------------------------------

private fun sampleState(
    phase: RunnerPhase = RunnerPhase.RUNNING,
    correction: CorrectionState = CorrectionState.STABLE,
    required: Double? = null,
    paceLive: Double? = 360.0,
    gpsLost: Boolean = false,
    planCompleted: Boolean = false,
): RunUiState {
    val segment = Segment("Interval 1", SegmentKind.RUN, Limit.Distance(400), targetPaceSecPerKm = 390)
    return RunUiState(
        phase = phase,
        workoutName = "Interval 200/400",
        correction = correction,
        requiredPaceSecPerKm = required,
        state = RunnerState(
            phase = phase,
            stepIndex = 2,
            stepCount = 9,
            segment = segment,
            nextSegment = Segment("Recovery", SegmentKind.RECOVERY, Limit.Distance(200), 480),
            segmentDistanceM = 220.0,
            segmentRemainingM = 180.0,
            segmentMovingMs = 80_000,
            totalDistanceM = 1820.0,
            movingMs = 645_000,
            paceLiveSecPerKm = paceLive,
            gpsLost = gpsLost,
            planCompleted = planCompleted,
        ),
    )
}

@Preview(name = "Running light")
@Composable
private fun RunRunningPreview() = RunCoachTheme { RunScreen(sampleState(), onStop = {}) }

@Preview(name = "Running dark", uiMode = 0x20)
@Composable
private fun RunRunningDarkPreview() = RunCoachTheme { RunScreen(sampleState(), onStop = {}) }

@Preview(name = "Too slow")
@Composable
private fun RunTooSlowPreview() =
    RunCoachTheme { RunScreen(sampleState(correction = CorrectionState.TOO_SLOW, required = 350.0, paceLive = 420.0), onStop = {}) }

@Preview(name = "Too fast")
@Composable
private fun RunTooFastPreview() =
    RunCoachTheme { RunScreen(sampleState(correction = CorrectionState.TOO_FAST, paceLive = 340.0), onStop = {}) }

@Preview(name = "Paused")
@Composable
private fun RunPausedPreview() = RunCoachTheme { RunScreen(sampleState(phase = RunnerPhase.PAUSED_MANUAL), onStop = {}) }

@Preview(name = "Waiting GPS")
@Composable
private fun RunWaitingPreview() =
    RunCoachTheme { RunScreen(sampleState(phase = RunnerPhase.WAITING_GPS, paceLive = null), onStop = {}) }

@Preview(name = "Plan completed")
@Composable
private fun RunPlanDonePreview() = RunCoachTheme { RunScreen(sampleState(planCompleted = true), onStop = {}) }
