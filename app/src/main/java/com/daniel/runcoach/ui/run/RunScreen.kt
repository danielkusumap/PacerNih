package com.daniel.runcoach.ui.run

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daniel.runcoach.R
import com.daniel.runcoach.data.AppSettings
import com.daniel.runcoach.data.SettingsRepository
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
import com.daniel.runcoach.ui.components.HeroPace
import com.daniel.runcoach.ui.components.MetricBlock
import com.daniel.runcoach.ui.components.RcButton
import com.daniel.runcoach.ui.components.RcButtonVariant
import com.daniel.runcoach.ui.components.RcCard
import com.daniel.runcoach.ui.components.RcDialog
import com.daniel.runcoach.ui.components.RcIconButton
import com.daniel.runcoach.ui.components.RcIconButtonVariant
import com.daniel.runcoach.ui.components.RcStatus
import com.daniel.runcoach.ui.components.SegmentProgressBar
import com.daniel.runcoach.ui.components.StatusBadge
import com.daniel.runcoach.ui.components.segmentKindColor
import com.daniel.runcoach.ui.components.segmentKindIcon
import com.daniel.runcoach.ui.map.RouteMapView
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.LocalRcShapes
import com.daniel.runcoach.ui.theme.LocalRcSpacing
import com.daniel.runcoach.ui.theme.LocalRcTypography
import com.daniel.runcoach.ui.theme.RcIcons
import com.daniel.runcoach.ui.theme.RcTheme
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.abs
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
        RcDialog(
            onDismiss = { showBackDialog = false },
            title = stringResource(R.string.run_back_title),
            content = { Text(stringResource(R.string.run_back_message), style = LocalRcTypography.current.body, color = LocalRcColors.current.textSecondary) },
            dismiss = { RcButton(stringResource(R.string.run_stay), onClick = { showBackDialog = false }, variant = RcButtonVariant.Tertiary) },
            confirm = {
                RcButton(
                    stringResource(R.string.action_stop_run),
                    onClick = { showBackDialog = false; RunForegroundService.stop(context); onStop() },
                    variant = RcButtonVariant.DestructiveFilled,
                )
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
 * S05 Layar Lari (DESIGN.md section 8). Not scrollable: header, segment, hero, segment metrics, totals, next,
 * controls. Banners overlay the header area; only one status colour at a time.
 */
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
    val colors = LocalRcColors.current
    val spacing = LocalRcSpacing.current
    val type = LocalRcTypography.current
    val state = ui.state
    val segment = state.segment
    val paused = ui.phase == RunnerPhase.PAUSED_MANUAL || ui.phase == RunnerPhase.PAUSED_AUTO
    var showExtend by remember { mutableStateOf(false) }
    var showSegments by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = spacing.screenH),
        ) {
            // A. Header
            Header(ui, showMap, onToggleMap)
            // B. Segment
            segment?.let { SegmentBlock(state, it) }
            // Banners (do not push content, sit right under the header)
            BannerRow(ui, onCancelCountdown)

            Column(modifier = Modifier.weight(1f).alpha(if (paused) 0.55f else 1f)) {
                // C. Hero
                if (ui.phase == RunnerPhase.WAITING_GPS) {
                    WaitingGps(Modifier.weight(1f), onPause)
                } else {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                        HeroPace(state.paceLiveSecPerKm)
                        TargetRow(segment)
                        Box(modifier = Modifier.height(52.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            if (segment?.targetPaceSecPerKm != null) StatusBadgeRow(ui, segment)
                        }
                    }
                }
                // D. Segment metrics
                SegmentMetrics(ui)
                // E. Totals
                Totals(state)
                // F. Next
                NextBlock(state.nextSegment)
            }

            // H. Controls
            Controls(ui, onPause, onResumeRun, onSkip, onStop, onExtend = { showExtend = true }, onSegments = { showSegments = true }, onToggleMap = onToggleMap, showMap = showMap)
            if (showMap) {
                RouteMapView(points = ui.trackPoints, modifier = Modifier.fillMaxWidth().height(160.dp))
            }
        }

        if (ui.phase == RunnerPhase.COUNTDOWN) {
            CountdownOverlay(ui.workoutName, state.countdownSec ?: 0, onCancelCountdown)
        }
    }

    if (showExtend && segment != null) {
        ExtendDialog(segment = segment, onDismiss = { showExtend = false }, onExtend = { m, s -> onExtend(m, s); showExtend = false })
    }
    if (showSegments) {
        SegmentListSheet(ui = ui, onDismiss = { showSegments = false })
    }
}

@Composable
private fun Header(ui: RunUiState, showMap: Boolean, onToggleMap: () -> Unit) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    val state = ui.state
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(ui.workoutName, style = type.bodySmall, color = colors.textSecondary, maxLines = 1, modifier = Modifier.weight(1f))
        when {
            ui.phase == RunnerPhase.PAUSED_MANUAL -> HeaderChip(stringResource(R.string.run_paused))
            ui.phase == RunnerPhase.PAUSED_AUTO -> HeaderChip(stringResource(R.string.run_paused_auto))
            else -> {
                val (icon, tint) = if (state.gpsLost) RcIcons.GpsOff to colors.fail else RcIcons.GpsFixed to colors.pass
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                    Text(ui.accuracyM?.let { "${it.toInt()} m" } ?: "--", style = type.caption, color = colors.textSecondary)
                }
            }
        }
        RcIconButton(
            icon = RcIcons.Map,
            contentDescription = stringResource(R.string.s08_tab_map),
            onClick = onToggleMap,
            tint = if (showMap) colors.accentText else colors.textSecondary,
        )
    }
}

@Composable
private fun HeaderChip(text: String) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    val shapes = LocalRcShapes.current
    Text(
        text.uppercase(),
        style = type.label,
        color = colors.neutral,
        modifier = Modifier.background(colors.neutralContainer, RoundedCornerShape(shapes.radiusS)).padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun SegmentBlock(state: RunnerState, segment: Segment) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    val spacing = LocalRcSpacing.current
    Column(modifier = Modifier.fillMaxWidth().height(96.dp), verticalArrangement = Arrangement.spacedBy(spacing.s1)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
            Icon(painterResource(segmentKindIcon(segment.kind)), contentDescription = null, tint = segmentKindColor(segment.kind), modifier = Modifier.size(22.dp))
            Text(segmentLabel(segment).uppercase(), style = type.numberXS, color = colors.textPrimary, maxLines = 1, modifier = Modifier.weight(1f))
            Text(
                stringResource(R.string.run_segment_of, state.stepIndex, state.stepCount ?: state.stepIndex).uppercase(),
                style = type.label,
                color = colors.textSecondary,
            )
        }
        if (segment.limit != Limit.Open) SegmentProgressBar(segmentProgress(state, segment), segment.kind)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(segmentRemainingValue(state, segment), style = type.numberXS, color = colors.textPrimary)
                Text(stringResource(R.string.run_remaining), style = type.bodySmall, color = colors.textSecondary)
            }
            Text(segmentLimitCaption(segment), style = type.caption, color = colors.textTertiary)
        }
    }
}

@Composable
private fun BannerRow(ui: RunUiState, onCancelCountdown: () -> Unit) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    val spacing = LocalRcSpacing.current
    val shapes = LocalRcShapes.current
    val state = ui.state
    val segCountdown = state.segmentCountdownSec
    val cueKind = state.distanceCueNextKind
    when {
        state.gpsLost -> BannerLine(stringResource(R.string.run_gps_lost), colors.warnContainer, colors.warn)
        state.phase == RunnerPhase.RUNNING && segCountdown != null ->
            BannerLine(stringResource(R.string.run_change_in, state.nextSegment?.kind?.let { kindLabel(it) } ?: "", segCountdown), colors.accentContainer, colors.accentText)
        state.distanceCueActive && cueKind != null ->
            BannerLine(stringResource(R.string.run_get_ready, kindLabel(cueKind)), colors.accentContainer, colors.accentText)
        state.planCompleted -> BannerLine(stringResource(R.string.run_plan_done_keep), colors.accentContainer, colors.accentText)
        state.goalReached -> BannerLine(stringResource(R.string.run_goal_reached), colors.passContainer, colors.pass)
    }
}

@Composable
private fun BannerLine(text: String, container: androidx.compose.ui.graphics.Color, content: androidx.compose.ui.graphics.Color) {
    val type = LocalRcTypography.current
    val shapes = LocalRcShapes.current
    Text(
        text,
        style = type.bodySmall.copy(fontWeight = FontWeight.Bold),
        color = content,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).background(container, RoundedCornerShape(shapes.radiusM)).padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

@Composable
private fun TargetRow(segment: Segment?) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.run_target_label).uppercase(), style = type.label, color = colors.textSecondary)
        val target = segment?.targetPaceSecPerKm
        if (target != null) Text(Formatters.targetPace(target, segment.targetBound), style = type.numberM, color = colors.textPrimary)
        else Text(stringResource(R.string.run_no_target), style = type.bodySmall, color = colors.textSecondary)
    }
}

@Composable
private fun StatusBadgeRow(ui: RunUiState, segment: Segment) {
    val status = when (ui.correction) {
        CorrectionState.STABLE -> RcStatus.Pass
        CorrectionState.TOO_SLOW -> RcStatus.Fail
        CorrectionState.TOO_FAST -> RcStatus.Fast
    }
    val label = when (ui.correction) {
        CorrectionState.STABLE -> stringResource(R.string.run_status_stable)
        CorrectionState.TOO_SLOW -> stringResource(R.string.run_status_too_slow)
        CorrectionState.TOO_FAST -> stringResource(R.string.run_status_too_fast)
    }
    val delta = ui.state.paceLiveSecPerKm?.let { live ->
        val d = live - segment.targetPaceSecPerKm!!.toDouble()
        if (abs(d) < 1.0) null else (if (d > 0) "+" else "-") + Formatters.duration((abs(d) * 1000).toLong())
    }
    StatusBadge(status, label, delta = delta)
}

@Composable
private fun SegmentMetrics(ui: RunUiState) {
    val type = LocalRcTypography.current
    val spacing = LocalRcSpacing.current
    RcCard {
        Row(modifier = Modifier.fillMaxWidth().height(76.dp), horizontalArrangement = Arrangement.spacedBy(spacing.s5), verticalAlignment = Alignment.CenterVertically) {
            MetricBlock(stringResource(R.string.run_avg_segment), Formatters.pace(ui.state.segmentAvgPaceOrNull()), valueStyle = type.numberM, modifier = Modifier.weight(1f))
            MetricBlock(stringResource(R.string.run_need_rest), ui.requiredPaceSecPerKm?.let { Formatters.pace(it) } ?: "", valueStyle = type.numberM, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun Totals(state: RunnerState) {
    val colors = LocalRcColors.current
    val spacing = LocalRcSpacing.current
    Box(modifier = Modifier.fillMaxWidth().padding(top = spacing.s2)) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.outline))
            Row(modifier = Modifier.fillMaxWidth().height(63.dp).padding(top = spacing.s2), horizontalArrangement = Arrangement.spacedBy(spacing.s4)) {
                MetricBlock(stringResource(R.string.run_label_total), Formatters.duration(state.movingMs), valueStyle = LocalRcTypography.current.numberS, modifier = Modifier.weight(1f))
                MetricBlock(stringResource(R.string.run_label_distance), Formatters.distanceKm(state.totalDistanceM), valueStyle = LocalRcTypography.current.numberS, modifier = Modifier.weight(1f))
                MetricBlock(
                    stringResource(R.string.run_label_avg_pace),
                    Formatters.pace(if (state.totalDistanceM > 0) state.movingMs / state.totalDistanceM else null),
                    unit = "/km",
                    valueStyle = LocalRcTypography.current.numberS,
                    valueColor = colors.textSecondary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NextBlock(next: Segment?) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    val spacing = LocalRcSpacing.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        if (next != null) {
            Icon(painterResource(segmentKindIcon(next.kind)), contentDescription = null, tint = segmentKindColor(next.kind), modifier = Modifier.size(18.dp))
        }
        Text(stringResource(R.string.run_next).uppercase(), style = type.label, color = colors.textSecondary)
        Text(
            next?.let { describeSegmentText(it) } ?: stringResource(R.string.run_next_free),
            style = type.bodySmall,
            color = colors.textPrimary,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun WaitingGps(modifier: Modifier, onPause: () -> Unit) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(painterResource(RcIcons.GpsNotFixed), contentDescription = null, tint = colors.warn, modifier = Modifier.size(64.dp))
        Text(stringResource(R.string.run_waiting_gps), style = type.titleM, color = colors.textPrimary)
    }
}

@Composable
private fun Controls(
    ui: RunUiState,
    onPause: () -> Unit,
    onResumeRun: () -> Unit,
    onSkip: () -> Unit,
    onStop: () -> Unit,
    onExtend: () -> Unit,
    onSegments: () -> Unit,
    onToggleMap: () -> Unit,
    showMap: Boolean,
) {
    val colors = LocalRcColors.current
    val shapes = LocalRcShapes.current
    val spacing = LocalRcSpacing.current
    val paused = ui.phase == RunnerPhase.PAUSED_MANUAL || ui.phase == RunnerPhase.PAUSED_AUTO
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            RcIconButton(RcIcons.SkipNext, stringResource(R.string.run_skip), onClick = onSkip, variant = RcIconButtonVariant.Filled)
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(colors.accent)
                    .clickable { if (paused) onResumeRun() else onPause() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(if (paused) RcIcons.PlayArrow else RcIcons.Pause),
                    contentDescription = stringResource(if (paused) R.string.run_resume else R.string.run_pause),
                    tint = colors.onAccent,
                    modifier = Modifier.size(40.dp),
                )
            }
            com.daniel.runcoach.ui.components.ProgressRing(onConfirm = onStop) {
                Box(
                    modifier = Modifier.size(56.dp).background(colors.surfaceRaised, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(RcIcons.Stop),
                        contentDescription = stringResource(R.string.action_stop_run),
                        tint = colors.fail,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ControlTextButton(icon = RcIcons.Add, text = stringResource(R.string.run_extend), onClick = onExtend)
            ControlTextButton(icon = RcIcons.List, text = stringResource(R.string.run_list_segments), onClick = onSegments)
        }
    }
}

@Composable
private fun ControlTextButton(icon: Int, text: String, onClick: () -> Unit) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    Row(
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = colors.accentText, modifier = Modifier.size(18.dp))
        Text(text, style = type.titleS, color = colors.accentText, maxLines = 1)
    }
}

@Composable
private fun CountdownOverlay(workoutName: String, seconds: Int, onCancel: () -> Unit) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    Box(modifier = Modifier.fillMaxSize().background(colors.bg.copy(alpha = 0.92f))) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.run_countdown_starting).uppercase(), style = type.label, color = colors.textSecondary)
            Text(
                seconds.toString(),
                style = type.displayHero.copy(fontSize = 160.sp, fontWeight = FontWeight.ExtraBold),
                color = colors.accent,
            )
            Text(workoutName, style = type.body, color = colors.textPrimary)
            RcButton(stringResource(R.string.run_countdown_cancel), onClick = onCancel, variant = RcButtonVariant.Tertiary)
        }
    }
}

@Composable
private fun ExtendDialog(segment: Segment, onDismiss: () -> Unit, onExtend: (Double, Int) -> Unit) {
    RcDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.run_extend),
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (segment.limit is Limit.Distance) {
                    RcButton("+100 m", onClick = { onExtend(100.0, 0) }, variant = RcButtonVariant.Secondary, fillWidth = true)
                    RcButton("+500 m", onClick = { onExtend(500.0, 0) }, variant = RcButtonVariant.Secondary, fillWidth = true)
                } else {
                    RcButton("+30 s", onClick = { onExtend(0.0, 30) }, variant = RcButtonVariant.Secondary, fillWidth = true)
                    RcButton("+1 min", onClick = { onExtend(0.0, 60) }, variant = RcButtonVariant.Secondary, fillWidth = true)
                }
            }
        },
        confirm = { RcButton(stringResource(R.string.run_countdown_cancel), onClick = onDismiss, variant = RcButtonVariant.Tertiary) },
    )
}

@Composable
private fun SegmentListSheet(ui: RunUiState, onDismiss: () -> Unit) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    com.daniel.runcoach.ui.components.RcSheet(onDismiss = onDismiss) {
        Text(stringResource(R.string.run_list_segments), style = type.titleM, color = colors.textPrimary)
        ui.steps.forEachIndexed { index, seg ->
            val number = index + 1
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(painterResource(segmentKindIcon(seg.kind)), contentDescription = null, tint = segmentKindColor(seg.kind), modifier = Modifier.size(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("$number. ${seg.label}", style = type.titleS, color = colors.textPrimary)
                    Text(describeSegmentText(seg), style = type.bodySmall, color = colors.textSecondary)
                }
            }
            com.daniel.runcoach.ui.components.RcDivider()
        }
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

private fun segmentLabel(segment: Segment): String = segment.label

@Composable
private fun describeSegmentText(segment: Segment): String {
    val limit = when (val l = segment.limit) {
        is Limit.Distance -> "${l.meters} m"
        is Limit.Duration -> Formatters.duration(l.seconds * 1000L)
        Limit.Open -> stringResource(R.string.run_no_target)
    }
    val target = segment.targetPaceSecPerKm?.let { ", ${Formatters.targetPace(it, segment.targetBound)}" } ?: ""
    return "${kindLabel(segment.kind)} $limit$target"
}

private fun segmentProgress(state: RunnerState, segment: Segment): Float = when (val limit = segment.limit) {
    is Limit.Distance -> (state.segmentDistanceM / limit.meters.toDouble()).coerceIn(0.0, 1.0).toFloat()
    is Limit.Duration -> (state.segmentMovingMs / (limit.seconds * 1000.0)).coerceIn(0.0, 1.0).toFloat()
    Limit.Open -> 0f
}

@Composable
private fun segmentRemainingValue(state: RunnerState, segment: Segment): String = when (val limit = segment.limit) {
    is Limit.Distance -> "${state.segmentRemainingM.toInt()} m"
    is Limit.Duration -> Formatters.duration(state.segmentRemainingMs)
    Limit.Open -> Formatters.distanceKm(state.segmentDistanceM)
}

@Composable
private fun segmentLimitCaption(segment: Segment): String = when (val limit = segment.limit) {
    is Limit.Distance -> stringResource(R.string.run_of, "${limit.meters} m")
    is Limit.Duration -> stringResource(R.string.run_of, Formatters.duration(limit.seconds * 1000L))
    Limit.Open -> ""
}

private fun RunnerState.segmentAvgPaceOrNull(): Double? =
    if (segmentDistanceM > 0) segmentMovingMs / segmentDistanceM else null

// --- Previews -------------------------------------------------------------------

private fun sampleRunUi(
    phase: RunnerPhase = RunnerPhase.RUNNING,
    correction: CorrectionState = CorrectionState.STABLE,
    required: Double? = null,
    paceLive: Double? = 336.0,
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
            segmentDistanceM = 315.0,
            segmentRemainingM = 85.0,
            segmentMovingMs = 126_000,
            totalDistanceM = 2310.0,
            movingMs = 761_000,
            paceLiveSecPerKm = paceLive,
            gpsLost = gpsLost,
            planCompleted = planCompleted,
            countdownSec = countdownSec,
        ),
    )
}

@Preview(name = "S05 running light", showBackground = true)
@Composable
private fun RunRunningPreview() = RcTheme(darkTheme = false) { RunScreen(sampleRunUi(), onStop = {}) }

@Preview(name = "S05 running dark", showBackground = true)
@Composable
private fun RunRunningDarkPreview() = RcTheme(darkTheme = true) { RunScreen(sampleRunUi(), onStop = {}) }

@Preview(name = "S05 too slow", showBackground = true)
@Composable
private fun RunTooSlowPreview() = RcTheme(darkTheme = true) {
    RunScreen(sampleRunUi(correction = CorrectionState.TOO_SLOW, required = 350.0, paceLive = 420.0), onStop = {})
}

@Preview(name = "S05 gps lost", showBackground = true)
@Composable
private fun RunGpsLostPreview() = RcTheme(darkTheme = true) { RunScreen(sampleRunUi(gpsLost = true, paceLive = null), onStop = {}) }

@Preview(name = "S05 countdown", showBackground = true)
@Composable
private fun RunCountdownPreview() = RcTheme(darkTheme = true) {
    RunScreen(sampleRunUi(phase = RunnerPhase.COUNTDOWN, paceLive = null, countdownSec = 3), onStop = {})
}
