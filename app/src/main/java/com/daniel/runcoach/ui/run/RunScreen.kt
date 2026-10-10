package com.daniel.runcoach.ui.run

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.collectAsState
import com.daniel.runcoach.data.AppSettings
import com.daniel.runcoach.data.SettingsRepository
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
import com.daniel.runcoach.ui.map.RouteMapView
import com.daniel.runcoach.ui.theme.RunCoachTheme
import kotlinx.coroutines.launch

/** S05 route: observes the run state, keeps the screen on, and forwards controls to the service. */
@Composable
fun RunRoute(onStop: () -> Unit) {
    val ui by RunStateHolder.state.collectAsStateWithLifecycle()
    val view = LocalView.current
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val settings by settingsRepository.settings.collectAsState(initial = AppSettings())
    val scope = rememberCoroutineScope()
    DisposableEffect(settings.keepScreenOn) {
        view.keepScreenOn = settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }
    var showBackDialog by remember { mutableStateOf(false) }
    val active = ui.phase != RunnerPhase.IDLE && ui.phase != RunnerPhase.STOPPED

    BackHandler(enabled = active) { showBackDialog = true }
    if (showBackDialog) {
        AlertDialog(
            onDismissRequest = { showBackDialog = false },
            title = { Text(stringResource(R.string.run_back_title)) },
            text = { Text(stringResource(R.string.run_back_message)) },
            confirmButton = { TextButton(onClick = { showBackDialog = false }) { Text(stringResource(R.string.run_stay)) } },
            dismissButton = {
                TextButton(onClick = {
                    showBackDialog = false
                    RunForegroundService.stop(context)
                    onStop()
                }) { Text(stringResource(R.string.action_stop_run)) }
            },
        )
    }

    RunScreen(
        ui = ui,
        onStop = { RunForegroundService.stop(context); onStop() },
        onCancelCountdown = { RunForegroundService.cancelCountdown(context) },
        onPause = { RunForegroundService.pause(context) },
        onResumeRun = { RunForegroundService.resumeRun(context) },
        onSkip = { RunForegroundService.skip(context) },
        onExtend = { meters, seconds -> RunForegroundService.extend(context, meters, seconds) },
        showMap = settings.liveMap,
        onToggleMap = { scope.launch { settingsRepository.setLiveMap(!settings.liveMap) } },
    )
}

/**
 * S05 Layar Lari (plan section 8): header, segment, live-pace hero with correction badge, segment metrics,
 * totals, next-segment preview, countdown overlay, banners, and the run controls. Stateless.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunScreen(
    ui: RunUiState,
    onStop: () -> Unit,
    onCancelCountdown: () -> Unit = {},
    onPause: () -> Unit = {},
    onResumeRun: () -> Unit = {},
    onSkip: () -> Unit = {},
    onExtend: (Double, Int) -> Unit = { _, _ -> },
    showMap: Boolean = false,
    onToggleMap: () -> Unit = {},
) {
    val state = ui.state
    val segment = state.segment
    val monospace = MaterialTheme.typography.displayMedium.copy(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
    )
    var showExtend by remember { mutableStateOf(false) }
    var showSegments by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val segCountdown = state.segmentCountdownSec
                val cueKind = state.distanceCueNextKind
                if (state.phase == RunnerPhase.RUNNING && segCountdown != null) {
                    Banner(stringResource(R.string.run_change_in, state.nextSegment?.kind?.let { kindLabel(it) } ?: "", segCountdown))
                }
                if (state.distanceCueActive && cueKind != null) {
                    Banner(stringResource(R.string.run_get_ready, kindLabel(cueKind)))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(ui.workoutName, style = MaterialTheme.typography.titleSmall)
                    if (state.gpsLost) Text(stringResource(R.string.run_gps_lost), color = MaterialTheme.colorScheme.error)
                }

                if (ui.phase == RunnerPhase.WAITING_GPS) {
                    Text(stringResource(R.string.run_waiting_gps), style = MaterialTheme.typography.headlineSmall)
                }
                if (segment != null) {
                    Text(
                        stringResource(R.string.run_segment_of, state.stepIndex, state.stepCount ?: state.stepIndex) +
                            " · ${kindLabel(segment.kind)}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    LinearProgressIndicator(progress = { segmentProgress(state, segment) }, modifier = Modifier.fillMaxWidth())
                    Text(segmentRemainingText(state, segment), style = MaterialTheme.typography.bodySmall)
                }

                Text(Formatters.pace(state.paceLiveSecPerKm), style = monospace)
                Text(
                    segment?.targetPaceSecPerKm
                        ?.let { stringResource(R.string.run_target, Formatters.pace(it.toDouble())) }
                        ?: stringResource(R.string.run_no_target),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (segment?.targetPaceSecPerKm != null) {
                    Text(
                        correctionLabel(ui.correction),
                        color = when (ui.correction) {
                            CorrectionState.STABLE -> MaterialTheme.colorScheme.primary
                            CorrectionState.TOO_SLOW -> MaterialTheme.colorScheme.error
                            CorrectionState.TOO_FAST -> MaterialTheme.colorScheme.tertiary
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                if (segment?.targetPaceSecPerKm != null && ui.requiredPaceSecPerKm != null) {
                    Text(
                        stringResource(R.string.run_required, Formatters.pace(ui.requiredPaceSecPerKm)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

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

                Text(stringResource(R.string.run_next), style = MaterialTheme.typography.labelLarge)
                Text(
                    state.nextSegment?.let { describeSegment(it) } ?: stringResource(R.string.run_next_free),
                    style = MaterialTheme.typography.bodyMedium,
                )

                if (state.planCompleted) Text(stringResource(R.string.run_plan_completed))
                if (state.goalReached) Text(stringResource(R.string.run_goal_reached))
                if (ui.phase == RunnerPhase.PAUSED_MANUAL || ui.phase == RunnerPhase.PAUSED_AUTO) {
                    Text(stringResource(R.string.run_paused), style = MaterialTheme.typography.headlineSmall)
                }

                // H. Controls
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (ui.phase == RunnerPhase.PAUSED_MANUAL || ui.phase == RunnerPhase.PAUSED_AUTO) {
                        Button(onClick = onResumeRun, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.run_resume)) }
                    } else {
                        OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.run_pause)) }
                    }
                    OutlinedButton(onClick = onSkip, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.run_skip)) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { showExtend = true }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.run_extend)) }
                    OutlinedButton(onClick = { showSegments = true }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.run_list_segments)) }
                }
                OutlinedButton(onClick = onToggleMap, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.s08_tab_map) + if (showMap) " ✓" else "")
                }
                if (showMap) {
                    RouteMapView(points = ui.trackPoints, modifier = Modifier.fillMaxWidth().height(220.dp))
                }
                Button(onClick = onStop, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_stop_run)) }
            }

            if (state.phase == RunnerPhase.COUNTDOWN) {
                CountdownOverlay(ui.workoutName, state.countdownSec ?: 0, onCancelCountdown)
            }
        }
    }

    if (showExtend && segment != null) {
        ExtendDialog(segment = segment, onDismiss = { showExtend = false }, onExtend = { m, s -> onExtend(m, s); showExtend = false })
    }
    if (showSegments) {
        SegmentListSheet(ui = ui, onDismiss = { showSegments = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SegmentListSheet(ui: RunUiState, onDismiss: () -> Unit) {
    val state = ui.state
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.run_list_segments), style = MaterialTheme.typography.titleLarge)
            ui.steps.forEachIndexed { index, seg ->
                val number = index + 1
                val marker = when {
                    number < state.stepIndex -> stringResource(R.string.run_state_done)
                    number == state.stepIndex -> stringResource(R.string.run_state_running)
                    else -> stringResource(R.string.run_state_upcoming)
                }
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        "$number. ${describeSegmentText(seg)}",
                        fontWeight = if (number == state.stepIndex) FontWeight.Bold else FontWeight.Normal,
                    )
                    Text(marker, style = MaterialTheme.typography.labelSmall)
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ExtendDialog(segment: Segment, onDismiss: () -> Unit, onExtend: (Double, Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.run_extend)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (segment.limit is Limit.Distance) {
                    Button(onClick = { onExtend(100.0, 0) }) { Text("+100 m") }
                    Button(onClick = { onExtend(500.0, 0) }) { Text("+500 m") }
                } else {
                    Button(onClick = { onExtend(0.0, 30) }) { Text("+30 s") }
                    Button(onClick = { onExtend(0.0, 60) }) { Text("+1 min") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.run_countdown_cancel)) } },
    )
}

@Composable
private fun Banner(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Text(text, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun CountdownOverlay(workoutName: String, seconds: Int, onCancel: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.85f)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(workoutName, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.inverseOnSurface)
            Text(
                seconds.toString(),
                style = MaterialTheme.typography.displayLarge.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.inverseOnSurface,
            )
            OutlinedButton(onClick = onCancel) { Text(stringResource(R.string.run_countdown_cancel)) }
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

@Composable
private fun describeSegmentText(segment: Segment): String {
    val limit = when (val l = segment.limit) {
        is Limit.Distance -> "${l.meters} m"
        is Limit.Duration -> Formatters.duration(l.seconds * 1000L)
        Limit.Open -> stringResource(R.string.run_no_target)
    }
    val target = segment.targetPaceSecPerKm?.let { ", ≤ ${Formatters.pace(it.toDouble())}" } ?: ""
    return "${kindLabel(segment.kind)} $limit$target"
}

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
private fun describeSegment(segment: Segment): String = describeSegmentText(segment)

// --- Previews -------------------------------------------------------------------

private fun sampleState(
    phase: RunnerPhase = RunnerPhase.RUNNING,
    correction: CorrectionState = CorrectionState.STABLE,
    required: Double? = null,
    paceLive: Double? = 360.0,
    gpsLost: Boolean = false,
    planCompleted: Boolean = false,
    countdownSec: Int? = null,
): RunUiState {
    val segment = Segment("Interval 1", SegmentKind.RUN, Limit.Distance(400), targetPaceSecPerKm = 390)
    return RunUiState(
        phase = phase,
        workoutName = "Interval 200/400",
        correction = correction,
        requiredPaceSecPerKm = required,
        steps = listOf(
            Segment("Warm up", SegmentKind.WARMUP, Limit.Distance(1500), 480),
            segment,
            Segment("Recovery", SegmentKind.RECOVERY, Limit.Distance(200), 480),
        ),
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
            countdownSec = countdownSec,
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
private fun RunTooSlowPreview() = RunCoachTheme {
    RunScreen(sampleState(correction = CorrectionState.TOO_SLOW, required = 350.0, paceLive = 420.0), onStop = {})
}

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

@Preview(name = "Countdown")
@Composable
private fun RunCountdownPreview() =
    RunCoachTheme { RunScreen(sampleState(phase = RunnerPhase.COUNTDOWN, paceLive = null, countdownSec = 3), onStop = {}) }

@Preview(name = "Plan completed")
@Composable
private fun RunPlanDonePreview() = RunCoachTheme { RunScreen(sampleState(planCompleted = true), onStop = {}) }
