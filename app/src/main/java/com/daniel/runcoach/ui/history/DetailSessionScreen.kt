package com.daniel.runcoach.ui.history

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.daniel.runcoach.R
import com.daniel.runcoach.data.KmSplitEntity
import com.daniel.runcoach.data.SegmentResultEntity
import com.daniel.runcoach.data.SessionEntity
import com.daniel.runcoach.data.SessionRepository
import com.daniel.runcoach.data.SettingsRepository
import com.daniel.runcoach.data.TrackPointEntity
import com.daniel.runcoach.engine.gps.CalibrationCalculator
import com.daniel.runcoach.engine.gps.haversineM
import com.daniel.runcoach.engine.voice.VoiceLanguage
import com.daniel.runcoach.export.GpxExporter
import com.daniel.runcoach.ui.Formatters
import com.daniel.runcoach.ui.charts.Bar
import com.daniel.runcoach.ui.charts.ChartBarPace
import com.daniel.runcoach.ui.charts.ChartLine
import com.daniel.runcoach.ui.charts.LinePoint
import com.daniel.runcoach.ui.charts.TargetStep
import com.daniel.runcoach.ui.components.MetricBlock
import com.daniel.runcoach.ui.components.NumberStepper
import com.daniel.runcoach.ui.components.RcButton
import com.daniel.runcoach.ui.components.RcButtonVariant
import com.daniel.runcoach.ui.components.RcCard
import com.daniel.runcoach.ui.components.RcDialog
import com.daniel.runcoach.ui.components.RcIconButton
import com.daniel.runcoach.ui.components.RcSheet
import com.daniel.runcoach.ui.components.RcStatus
import com.daniel.runcoach.ui.components.RcTextField
import com.daniel.runcoach.ui.components.RcTextButton
import com.daniel.runcoach.ui.components.RcTextButtonTone
import com.daniel.runcoach.ui.components.ResultChip
import com.daniel.runcoach.ui.components.ResultStrip
import com.daniel.runcoach.ui.components.TopBar
import com.daniel.runcoach.ui.map.RouteMapView
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.LocalRcShapes
import com.daniel.runcoach.ui.theme.LocalRcSpacing
import com.daniel.runcoach.ui.theme.LocalRcTypography
import com.daniel.runcoach.ui.theme.RcIcons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** S08 Session detail (DESIGN.md section 8): header, tabs, per-segment cards, splits table, charts, map. */
@Composable
fun DetailSessionScreen(sessionId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { SessionRepository(context) }
    val settingsRepository = remember { SettingsRepository(context) }
    val settings by settingsRepository.settings.collectAsState(initial = com.daniel.runcoach.data.AppSettings())
    val scope = rememberCoroutineScope()
    var session by remember { mutableStateOf<SessionEntity?>(null) }
    var segments by remember { mutableStateOf<List<SegmentResultEntity>>(emptyList()) }
    var splits by remember { mutableStateOf<List<KmSplitEntity>>(emptyList()) }
    var trackPoints by remember { mutableStateOf<List<TrackPointEntity>>(emptyList()) }
    var bestEfforts by remember { mutableStateOf<List<com.daniel.runcoach.data.BestEffortEntity>>(emptyList()) }
    var tab by remember { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    var showCalibrate by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(sessionId) {
        var loaded = repository.get(sessionId)
        var tries = 0
        while (loaded == null && tries < 10) {
            delay(150)
            loaded = repository.get(sessionId)
            tries++
        }
        session = loaded
        if (loaded != null) {
            segments = repository.segments(sessionId)
            splits = repository.splits(sessionId)
            trackPoints = repository.trackPoints(sessionId)
            bestEfforts = repository.bestEfforts(sessionId)
        }
    }

    LaunchedEffect(settings.historyMap, trackPoints.size) {
        if (tab == 3 && (trackPoints.size < 2 || !settings.historyMap)) tab = 0
    }

    val colors = LocalRcColors.current
    val spacing = LocalRcSpacing.current
    val type = LocalRcTypography.current

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        TopBar(
            title = "",
            onBack = onBack,
            actions = {
                RcIconButton(
                    icon = RcIcons.Map,
                    contentDescription = stringResource(R.string.s08_tab_map),
                    onClick = { scope.launch { settingsRepository.setHistoryMap(!settings.historyMap) } },
                    tint = if (settings.historyMap) colors.accentText else colors.textSecondary,
                )
                Box {
                    RcIconButton(RcIcons.MoreVert, stringResource(R.string.s08_more), onClick = { menu = true }, tint = colors.textPrimary)
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = colors.surfaceRaised) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.s08_edit)) }, onClick = { menu = false; showEdit = true })
                        DropdownMenuItem(text = { Text(stringResource(R.string.s08_export_gpx)) }, onClick = {
                            menu = false
                            val current = session
                            if (current != null && trackPoints.size >= 2) {
                                scope.launch {
                                    val file = GpxExporter.write(context, current.id, current.name, current.startedAtEpochMs, trackPoints)
                                    context.startActivity(Intent.createChooser(GpxExporter.shareIntent(context, file), null))
                                }
                            }
                        })
                        DropdownMenuItem(text = { Text(stringResource(R.string.s08_calibrate)) }, onClick = { menu = false; showCalibrate = true })
                        DropdownMenuItem(text = { Text(stringResource(R.string.s07_delete)) }, onClick = { menu = false; showDelete = true })
                    }
                }
            },
        )

        val current = session
        if (current == null) {
            Text(stringResource(R.string.s08_loading), modifier = Modifier.padding(spacing.screenH), color = colors.textSecondary)
            return@Column
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = spacing.screenH).padding(bottom = spacing.s6)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                Text(current.name, style = type.titleL, color = colors.textPrimary, maxLines = 1, modifier = Modifier.weight(1f))
                RcIconButton(RcIcons.Edit, stringResource(R.string.s08_edit), onClick = { showEdit = true }, tint = colors.accentText)
            }
            Text(formatRange(current), style = type.bodySmall, color = colors.textSecondary)
            Text(current.templateNameSnapshot, style = type.caption, color = colors.textTertiary)

            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = spacing.s2)) {
                Text(Formatters.distanceKm(current.totalDistanceM).removeSuffix(" km"), style = type.displayM, color = colors.textPrimary)
                Text("km", style = type.numberS, color = colors.textSecondary)
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = spacing.s3), horizontalArrangement = Arrangement.spacedBy(spacing.s5)) {
                MetricBlock(stringResource(R.string.s08_metric_time), Formatters.duration(current.movingMs))
                MetricBlock(stringResource(R.string.s08_metric_avg_pace), Formatters.pace(current.avgPaceSecPerKm), unit = "/km")
                MetricBlock(
                    stringResource(R.string.s08_metric_result),
                    stringResource(R.string.s07_chip_pass, current.segPass, current.segPass + current.segFail + current.segBorderline),
                    valueColor = colors.accentText,
                )
            }
            if (current.pausedMs > 0) {
                Text(stringResource(R.string.s08_paused, Formatters.duration(current.pausedMs)), style = type.bodySmall, color = colors.textSecondary)
            }
            Text(
                if (current.planCompleted) stringResource(R.string.s08_plan_done) else stringResource(R.string.s08_plan_stopped),
                style = type.bodySmall,
                color = colors.textSecondary,
            )

            if (segments.isNotEmpty()) {
                ResultStrip(
                    statuses = segments.map { statusFor(it.status) },
                    weights = segments.map { it.distanceM.toFloat() },
                    modifier = Modifier.padding(vertical = spacing.s3),
                )
            }

            RcCard(modifier = Modifier.padding(top = spacing.s2)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                    Icon(painterResource(RcIcons.Notes), contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.s08_notes), style = type.titleS, color = colors.textPrimary)
                }
                Text(current.note?.takeIf { it.isNotBlank() } ?: stringResource(R.string.s08_no_note), style = type.body, color = colors.textSecondary)
                if (current.rpe != null) Text(stringResource(R.string.s08_rpe, current.rpe), style = type.titleS, color = colors.textPrimary)
            }

            TabRow(tab, hasMap = trackPoints.size >= 2 && settings.historyMap) { tab = it }
            when (tab) {
                0 -> SegmentsTab(segments, splits)
                1 -> SplitsTab(splits, bestEfforts)
                2 -> ChartsTab(segments, splits, trackPoints)
                else -> RouteMapView(
                    points = trackPoints.map { it.lat to it.lon },
                    segments = segmentPolylines(trackPoints),
                    boundaryIndices = boundaryIndices(trackPoints, segments),
                    modifier = Modifier.fillMaxWidth().height(360.dp),
                )
            }
        }
    }

    if (showCalibrate) {
        session?.let { current ->
            CalibrateSheet(
                session = current,
                onDismiss = { showCalibrate = false },
                onApply = { factor -> scope.launch { settingsRepository.setCalibrationFactor(factor) }; showCalibrate = false },
            )
        }
    }
    if (showEdit) {
        session?.let { current ->
            EditSheet(
                session = current,
                onSave = { name, note, rpe ->
                    scope.launch {
                        repository.updateForm(current.id, name, note, rpe, current.excludeFromRecords, currentLanguage(), System.currentTimeMillis())
                        session = repository.get(current.id)
                        showEdit = false
                    }
                },
                onDismiss = { showEdit = false },
            )
        }
    }
    if (showDelete) {
        RcDialog(
            onDismiss = { showDelete = false },
            title = stringResource(R.string.d09_delete_session_title),
            content = { Text(stringResource(R.string.d09_delete_session_message, session?.name ?: ""), style = type.body, color = colors.textSecondary) },
            dismiss = { RcTextButton(stringResource(R.string.editor_discard_keep), onClick = { showDelete = false }, tone = RcTextButtonTone.Neutral) },
            confirm = {
                RcTextButton(stringResource(R.string.s07_delete), onClick = {
                    scope.launch { session?.let { repository.delete(it.id) }; showDelete = false; onBack() }
                }, tone = RcTextButtonTone.Destructive)
            },
        )
    }
}

@Composable
private fun TabRow(selected: Int, hasMap: Boolean, onSelect: (Int) -> Unit) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    val labels = buildList {
        add(stringResource(R.string.s08_tab_segments))
        add(stringResource(R.string.s08_tab_splits))
        add(stringResource(R.string.s08_tab_charts))
        if (hasMap) add(stringResource(R.string.s08_tab_map))
    }
    Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
        labels.forEachIndexed { index, label ->
            Column(modifier = Modifier.weight(1f).clickable { onSelect(index) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label.uppercase(), style = type.tab, color = if (index == selected) colors.textPrimary else colors.textSecondary, maxLines = 1, modifier = Modifier.padding(vertical = 8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(if (index == selected) colors.accent else androidx.compose.ui.graphics.Color.Transparent))
            }
        }
    }
}

@Composable
private fun SegmentsTab(segments: List<SegmentResultEntity>, splits: List<KmSplitEntity>) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    val spacing = LocalRcSpacing.current
    var expanded by remember { mutableStateOf<Set<Int>>(emptySet()) }
    // Cumulative distance at each segment boundary, to find the km splits inside a segment.
    val bounds = segments.runningFold(0.0) { acc, s -> acc + s.distanceM }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.cardGap), modifier = Modifier.padding(top = spacing.s3)) {
        segments.forEach { segment ->
            val start = bounds.getOrElse(segment.idx - 1) { 0.0 }
            val end = bounds.getOrElse(segment.idx) { start + segment.distanceM }
            val inside = splits.filter { it.idx * 1000.0 - 1000.0 < end && it.idx * 1000.0 > start }
            val canExpand = segment.distanceM >= 1000.0 && segment.targetPace != null && inside.isNotEmpty()
            RcCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                    Text("#${segment.idx} ${segment.label}", style = type.titleS, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    ResultChip(statusFor(segment.status), statusLabel(segment.status))
                }
                Text(
                    stringResource(R.string.s08_seg_limit, limitText(segment)) + " · " + segActualText(segment),
                    style = type.bodySmall,
                    color = colors.textSecondary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.s4)) {
                    MetricBlock("Jarak", Formatters.distanceKm(segment.distanceM).removeSuffix(" km"), unit = "km", valueStyle = type.numberS)
                    MetricBlock("Waktu", Formatters.duration(segment.durationMs), valueStyle = type.numberS)
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = spacing.s1), horizontalArrangement = Arrangement.spacedBy(spacing.s4)) {
                    MetricBlock(stringResource(R.string.s08_seg_target), targetText(segment), valueStyle = type.numberS)
                    MetricBlock(
                        stringResource(R.string.s08_seg_actual),
                        Formatters.pace(segment.avgPace),
                        unit = "/km",
                        valueStyle = type.numberS,
                        valueColor = when (segment.status) {
                            "PASS" -> colors.pass
                            "FAIL" -> colors.fail
                            "BORDERLINE" -> colors.warn
                            else -> colors.textPrimary
                        },
                    )
                    segment.deltaSec?.let {
                        MetricBlock(stringResource(R.string.s08_seg_delta), Formatters.deltaSeconds(it), valueStyle = type.numberS, valueColor = if (it <= 0) colors.pass else colors.fail)
                    } ?: MetricBlock(stringResource(R.string.s08_seg_delta), "—", valueStyle = type.numberS, valueColor = colors.textSecondary)
                }
                if (segment.status == "BORDERLINE") Text(stringResource(R.string.s08_borderline_note), style = type.caption, color = colors.warn)
                if (segment.stopped) Text(stringResource(R.string.stopped_early), style = type.caption, color = colors.textSecondary, modifier = Modifier.padding(top = spacing.s1))
                if (canExpand) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = spacing.s2).clickable {
                            expanded = if (segment.idx in expanded) expanded - segment.idx else expanded + segment.idx
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.s1),
                    ) {
                        Icon(
                            painterResource(if (segment.idx in expanded) RcIcons.ChevronLeft else RcIcons.ChevronRight),
                            contentDescription = null,
                            tint = colors.accentText,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(stringResource(R.string.s08_km_inside), style = type.bodySmall, color = colors.accentText)
                    }
                    if (segment.idx in expanded) {
                        inside.forEach { split ->
                            Row(modifier = Modifier.fillMaxWidth().padding(start = spacing.s2, top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (split.distanceM < 1000.0) "%.2f km".format(split.distanceM / 1000.0) else "Km ${split.idx}", style = type.bodySmall, color = colors.textPrimary)
                                Text(Formatters.duration(split.durationMs), style = type.bodySmall, color = colors.textSecondary)
                                Text(Formatters.pace(split.paceSecPerKm), style = type.bodySmall, color = colors.textSecondary)
                                ResultChip(statusFor(split.status ?: ""), statusLabel(split.status ?: ""))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SplitsTab(splits: List<KmSplitEntity>, bestEfforts: List<com.daniel.runcoach.data.BestEffortEntity>) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    val spacing = LocalRcSpacing.current
    val fastestIndex = splits.filter { it.distanceM >= 1000.0 && it.paceSecPerKm != null }.minByOrNull { it.paceSecPerKm!! }?.idx
    Column(modifier = Modifier.padding(top = spacing.s3)) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = spacing.s2)) {
            listOf(stringResource(R.string.s08_col_km), stringResource(R.string.s08_col_time), stringResource(R.string.s08_col_pace), stringResource(R.string.s08_col_delta)).forEach {
                Text(it.uppercase(), style = type.label, color = colors.textSecondary, modifier = Modifier.weight(1f))
            }
        }
        splits.forEach { split ->
            val partial = split.distanceM < 1000.0
            Row(modifier = Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (partial) "%.2f km".format(split.distanceM / 1000.0) else "Km ${split.idx}", style = type.titleS, color = colors.textPrimary, modifier = Modifier.weight(1f))
                Text(Formatters.duration(split.durationMs), style = type.numberS, color = colors.textPrimary, modifier = Modifier.weight(1f))
                Text(Formatters.pace(split.paceSecPerKm), style = type.numberS, color = colors.textPrimary, modifier = Modifier.weight(1f))
                Text(split.deltaSec?.let { Formatters.deltaSeconds(it) } ?: "", style = type.bodySmall, color = if ((split.deltaSec ?: 0) <= 0) colors.pass else colors.fail, modifier = Modifier.weight(1f))
            }
            if (split.idx == fastestIndex) Text(stringResource(R.string.s08_fastest), style = type.caption, color = colors.accentText)
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.outline))
        }
        // I1: the fastest 1 km may start anywhere in the session (sliding window), not only at a km boundary.
        val k1 = bestEfforts.firstOrNull { it.distanceKey == "K1" }
        if (k1 != null) {
            val startKm = k1.startOffsetM / 1000.0
            val endKm = startKm + 1.0
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = spacing.s3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.fastest_km), style = type.bodySmall, color = colors.textSecondary)
                Text(
                    "${Formatters.duration(k1.durationMs)} · km ${oneDecimal(startKm)}–${oneDecimal(endKm)}",
                    style = type.numberS,
                    color = colors.accentText,
                )
            }
        }
    }
}

private fun oneDecimal(km: Double): String = String.format(java.util.Locale.getDefault(), "%.1f", km)

@Composable
private fun ChartsTab(segments: List<SegmentResultEntity>, splits: List<KmSplitEntity>, trackPoints: List<TrackPointEntity>) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
        val fullSplits = splits.filter { it.paceSecPerKm != null }
        Text(stringResource(R.string.s08_chart_per_km), style = type.titleS, color = colors.textPrimary)
        ChartBarPace(
            bars = fullSplits.map {
                Bar(
                    label = if (it.distanceM < 1000.0) "%.2f".format(it.distanceM / 1000.0) else "${it.idx}",
                    value = it.paceSecPerKm,
                    target = it.targetPace?.toDouble(),
                    status = if (it.distanceM < 1000.0) null else statusFor(it.status ?: ""),
                    weight = (it.distanceM / 1000.0).toFloat().coerceAtLeast(0.1f),
                    partial = it.distanceM < 1000.0,
                )
            },
            valueFormatter = { Formatters.pace(it) },
        )
        Text(stringResource(R.string.s08_chart_over_distance), style = type.titleS, color = colors.textPrimary)
        val curve = remember(trackPoints) { buildCurve(trackPoints) }
        val buckets = remember(curve) { com.daniel.runcoach.engine.chart.ChartMath.bucketByDistance(curve, 100.0) }
        ChartLine(
            points = buckets.map { LinePoint(it.startM + 50.0, it.paceSecPerKm) },
            steps = targetSteps(segments),
            boundaries = segments.runningFold(0.0) { acc, s -> acc + s.distanceM }.drop(1).dropLast(1),
            fasterLabel = stringResource(R.string.chart_faster),
            valueFormatter = { Formatters.pace(it) },
        )
    }
}

private fun buildCurve(trackPoints: List<TrackPointEntity>): List<com.daniel.runcoach.engine.chart.DistTimePoint> {
    if (trackPoints.isEmpty()) return emptyList()
    var distance = 0.0
    val start = trackPoints.first().timeMs
    return trackPoints.mapIndexed { i, p ->
        if (i > 0) {
            distance += haversineM(trackPoints[i - 1].lat, trackPoints[i - 1].lon, p.lat, p.lon)
        }
        com.daniel.runcoach.engine.chart.DistTimePoint(distance, p.timeMs - start)
    }
}

@Composable
private fun CalibrateSheet(session: SessionEntity, onDismiss: () -> Unit, onApply: (Double) -> Unit) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    var text by remember { mutableStateOf("") }
    val actualKm = text.trim().replace(',', '.').toDoubleOrNull()
    val factor = actualKm?.let { CalibrationCalculator.factorFor(it * 1000.0, session.totalDistanceM, session.distanceCalibrationFactorUsed) }
    RcSheet(onDismiss = onDismiss) {
        Text(stringResource(R.string.s08_calibrate).uppercase(), style = type.titleM, color = colors.textPrimary)
        Text(stringResource(R.string.s08_calibrate_measured_label).uppercase(), style = type.label, color = colors.textSecondary, modifier = Modifier.padding(top = 12.dp))
        Text(Formatters.distanceKm(session.totalDistanceM), style = type.numberL, color = colors.textPrimary)
        NumberStepper(
            value = text.ifBlank { "—" },
            onDecrement = { text = "%.1f".format(((text.trim().replace(',', '.').toDoubleOrNull() ?: 5.0) - 0.1).coerceAtLeast(0.1)) },
            onIncrement = { text = "%.1f".format((text.trim().replace(',', '.').toDoubleOrNull() ?: 5.0) + 0.1) },
            stepLabel = stringResource(R.string.s08_calibrate_actual),
            modifier = Modifier.padding(top = 12.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.s08_calibrate_factor).uppercase(), style = type.label, color = colors.textSecondary)
            Text(
                factor?.let { "%.4f".format(it) } ?: "—",
                style = type.numberL,
                color = colors.accentText,
            )
        }
        if (text.isNotBlank() && factor == null) {
            Text(stringResource(R.string.s08_calibrate_invalid), style = type.bodySmall, color = colors.fail)
        }
        RcButton(stringResource(R.string.s08_calibrate_apply), onClick = { factor?.let(onApply) }, enabled = factor != null, fillWidth = true, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun EditSheet(session: SessionEntity, onSave: (String, String?, Int?) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    var name by remember { mutableStateOf(if (session.nameIsCustom) session.name else "") }
    var note by remember { mutableStateOf(session.note ?: "") }
    var rpe by remember { mutableStateOf(session.rpe) }
    RcSheet(onDismiss = onDismiss) {
        Text(stringResource(R.string.d03_title).uppercase(), style = type.titleM, color = colors.textPrimary)
        RcTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.d03_name), placeholder = session.name, modifier = Modifier.padding(top = 12.dp))
        Text(stringResource(R.string.d03_rpe).uppercase(), style = type.label, color = colors.textSecondary, modifier = Modifier.padding(top = 8.dp))
        EffortCircles(rpe) { rpe = it }
        RcTextField(value = note, onValueChange = { note = it }, label = stringResource(R.string.d03_note), multiline = true, modifier = Modifier.padding(top = 8.dp))
        RcButton(stringResource(R.string.s06_save), onClick = { onSave(name, note.ifBlank { null }, rpe) }, fillWidth = true, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun EffortCircles(selected: Int?, onSelect: (Int?) -> Unit) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        (1..10).forEach { value ->
            val active = selected == value
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(if (active) colors.accent else androidx.compose.ui.graphics.Color.Transparent, androidx.compose.foundation.shape.CircleShape)
                    .border(1.5.dp, if (active) colors.accent else colors.outlineStrong, androidx.compose.foundation.shape.CircleShape)
                    .clickable { onSelect(if (active) null else value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(value.toString(), style = type.bodySmall, color = if (active) colors.onAccent else colors.textSecondary)
            }
        }
    }
}

private fun statusFor(status: String): RcStatus = when (status) {
    "PASS" -> RcStatus.Pass
    "FAIL" -> RcStatus.Fail
    "BORDERLINE" -> RcStatus.Warn
    else -> RcStatus.Neutral
}

@Composable
private fun statusLabel(status: String): String = stringResource(
    when (status) {
        "PASS" -> R.string.result_pass
        "FAIL" -> R.string.result_fail
        "BORDERLINE" -> R.string.result_borderline
        "SKIPPED" -> R.string.result_skipped
        else -> R.string.result_no_target
    },
)

/** F6: split the track into per-segment polylines, sharing the boundary point so there is no visual gap. */
private fun segmentPolylines(trackPoints: List<TrackPointEntity>): List<List<Pair<Double, Double>>> {
    if (trackPoints.size < 2) return emptyList()
    val out = mutableListOf<List<Pair<Double, Double>>>()
    var current = mutableListOf<Pair<Double, Double>>()
    var lastIdx = trackPoints.first().segmentIdx
    for (point in trackPoints) {
        if (point.segmentIdx != lastIdx && current.isNotEmpty()) {
            out.add(current.toList())
            current = mutableListOf(current.last())
            lastIdx = point.segmentIdx
        }
        current.add(point.lat to point.lon)
    }
    if (current.size >= 2) out.add(current.toList())
    return out
}

private fun boundaryIndices(trackPoints: List<TrackPointEntity>, segments: List<SegmentResultEntity>): List<Int> {
    if (trackPoints.size < 2 || segments.size < 2) return emptyList()
    val cumulative = DoubleArray(trackPoints.size)
    for (i in 1 until trackPoints.size) {
        cumulative[i] = cumulative[i - 1] +
            haversineM(trackPoints[i - 1].lat, trackPoints[i - 1].lon, trackPoints[i].lat, trackPoints[i].lon)
    }
    var boundary = 0.0
    val indices = mutableListOf<Int>()
    segments.dropLast(1).forEach { segment ->
        boundary += segment.distanceM
        val index = cumulative.indexOfFirst { it >= boundary }
        if (index >= 0) indices += index
    }
    return indices
}

private fun targetSteps(segments: List<SegmentResultEntity>): List<TargetStep> {
    var cumulative = 0.0
    val steps = mutableListOf<TargetStep>()
    segments.forEach { segment ->
        val start = cumulative
        cumulative += segment.distanceM
        segment.targetPace?.let { steps += TargetStep(start, cumulative, it.toDouble()) }
    }
    return steps
}

private fun limitText(segment: SegmentResultEntity): String = when (segment.limitType) {
    "distance" -> "${segment.limitValue.toInt()} m"
    "duration" -> Formatters.duration(segment.limitValue.toLong() * 1000L)
    else -> "Open"
}

/** Actual distance and time of a segment as one string, e.g. `210 m · 01:20`. */
private fun segActualText(segment: SegmentResultEntity): String {
    val distance = if (segment.distanceM < 1000.0) "${segment.distanceM.toInt()} m" else Formatters.distanceKm(segment.distanceM)
    return "$distance · ${Formatters.duration(segment.durationMs)}"
}

private fun targetText(segment: SegmentResultEntity): String =
    segment.targetPace?.let {
        Formatters.targetPace(it, runCatching { com.daniel.runcoach.engine.model.PaceTargetBound.valueOf(segment.targetBound) }.getOrDefault(com.daniel.runcoach.engine.model.PaceTargetBound.MAX))
    } ?: "—"

private fun formatRange(session: SessionEntity): String {
    val fmt = SimpleDateFormat("d MMM yyyy · HH:mm", Locale.getDefault())
    return "${fmt.format(Date(session.startedAtEpochMs))} - ${fmt.format(Date(session.endedAtEpochMs))}"
}

private fun currentLanguage(): VoiceLanguage {
    val language = Locale.getDefault().language
    return if (language.startsWith("in") || language == "id") VoiceLanguage.ID else VoiceLanguage.EN
}
