package com.daniel.runcoach.ui.history

import android.content.Intent
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
import com.daniel.runcoach.ui.components.RcButton
import com.daniel.runcoach.ui.components.RcButtonVariant
import com.daniel.runcoach.ui.components.RcCard
import com.daniel.runcoach.ui.components.RcDialog
import com.daniel.runcoach.ui.components.RcIconButton
import com.daniel.runcoach.ui.components.RcSheet
import com.daniel.runcoach.ui.components.RcStatus
import com.daniel.runcoach.ui.components.RcTextField
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
                Text(current.name, style = type.titleL, color = colors.textPrimary, modifier = Modifier.weight(1f))
                RcIconButton(RcIcons.Edit, stringResource(R.string.s08_edit), onClick = { showEdit = true }, tint = colors.textSecondary)
            }
            Text(formatRange(current), style = type.bodySmall, color = colors.textSecondary)
            Text(current.templateNameSnapshot, style = type.caption, color = colors.textTertiary)

            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = spacing.s2)) {
                Text(Formatters.distanceKm(current.totalDistanceM).removeSuffix(" km"), style = type.numberXL, color = colors.textPrimary)
                Text("km", style = type.titleS, color = colors.textSecondary)
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = spacing.s3), horizontalArrangement = Arrangement.spacedBy(spacing.s5)) {
                MetricBlock(stringResource(R.string.s08_metric_time), Formatters.duration(current.movingMs))
                MetricBlock(stringResource(R.string.s08_metric_avg_pace), Formatters.pace(current.avgPaceSecPerKm), unit = "/km")
                MetricBlock(stringResource(R.string.s08_metric_result), stringResource(R.string.s07_chip_pass, current.segPass, current.segPass + current.segFail + current.segBorderline))
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
                1 -> SplitsTab(splits)
                2 -> ChartsTab(segments, splits, trackPoints)
                else -> RouteMapView(
                    points = trackPoints.map { it.lat to it.lon },
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
            dismiss = { RcButton(stringResource(R.string.editor_discard_keep), onClick = { showDelete = false }, variant = RcButtonVariant.Tertiary) },
            confirm = {
                RcButton(stringResource(R.string.s07_delete), onClick = {
                    scope.launch { session?.let { repository.delete(it.id) }; showDelete = false; onBack() }
                }, variant = RcButtonVariant.DestructiveFilled)
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
                Text(label, style = type.titleS, color = if (index == selected) colors.textPrimary else colors.textSecondary, modifier = Modifier.padding(vertical = 8.dp))
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
                Text(stringResource(R.string.s08_seg_limit, limitText(segment)), style = type.bodySmall, color = colors.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.s4)) {
                    MetricBlock("Jarak", Formatters.distanceKm(segment.distanceM).removeSuffix(" km"), unit = "km")
                    MetricBlock("Waktu", Formatters.duration(segment.durationMs))
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = spacing.s1), horizontalArrangement = Arrangement.spacedBy(spacing.s4)) {
                    MetricBlock(stringResource(R.string.s08_seg_target), targetText(segment))
                    MetricBlock(
                        stringResource(R.string.s08_seg_actual),
                        Formatters.pace(segment.avgPace),
                        unit = "/km",
                        valueColor = when (segment.status) {
                            "PASS" -> colors.pass
                            "FAIL" -> colors.fail
                            "BORDERLINE" -> colors.warn
                            else -> colors.textPrimary
                        },
                    )
                    segment.deltaSec?.let {
                        MetricBlock(stringResource(R.string.s08_seg_delta), Formatters.deltaSeconds(it), valueColor = if (it <= 0) colors.pass else colors.fail)
                    }
                }
                if (segment.status == "BORDERLINE") Text(stringResource(R.string.s08_borderline_note), style = type.caption, color = colors.warn)
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
private fun SplitsTab(splits: List<KmSplitEntity>) {
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
                Text(Formatters.duration(split.durationMs), style = type.numberM, color = colors.textPrimary, modifier = Modifier.weight(1f))
                Text(Formatters.pace(split.paceSecPerKm), style = type.numberM, color = colors.textPrimary, modifier = Modifier.weight(1f))
                Text(split.deltaSec?.let { Formatters.deltaSeconds(it) } ?: "", style = type.bodySmall, color = if ((split.deltaSec ?: 0) <= 0) colors.pass else colors.fail, modifier = Modifier.weight(1f))
            }
            if (split.idx == fastestIndex) Text(stringResource(R.string.s08_fastest), style = type.caption, color = colors.accentText)
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.outline))
        }
    }
}

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
        Text(stringResource(R.string.s08_calibrate), style = type.titleM, color = colors.textPrimary)
        Text(stringResource(R.string.s08_calibrate_measured, Formatters.distanceKm(session.totalDistanceM)), style = type.bodySmall, color = colors.textSecondary)
        RcTextField(value = text, onValueChange = { text = it }, label = stringResource(R.string.s08_calibrate_actual), modifier = Modifier.padding(top = 12.dp))
        if (factor != null) Text(stringResource(R.string.s08_calibrate_new, "%.4f".format(factor)), style = type.titleS, color = colors.textPrimary)
        else if (text.isNotBlank()) Text(stringResource(R.string.s08_calibrate_invalid), style = type.bodySmall, color = colors.fail)
        RcButton(stringResource(R.string.s08_calibrate_apply), onClick = { factor?.let(onApply) }, enabled = factor != null, fillWidth = true, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun EditSheet(session: SessionEntity, onSave: (String, String?, Int?) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    var name by remember { mutableStateOf(if (session.nameIsCustom) session.name else "") }
    var note by remember { mutableStateOf(session.note ?: "") }
    var rpe by remember { mutableStateOf(session.rpe?.toString() ?: "") }
    RcSheet(onDismiss = onDismiss) {
        Text(stringResource(R.string.d03_title), style = type.titleM, color = colors.textPrimary)
        RcTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.d03_name), placeholder = session.name, modifier = Modifier.padding(top = 12.dp))
        RcTextField(value = rpe, onValueChange = { rpe = it.filter(Char::isDigit).take(2) }, label = stringResource(R.string.d03_rpe), modifier = Modifier.padding(top = 8.dp))
        RcTextField(value = note, onValueChange = { note = it }, label = stringResource(R.string.d03_note), multiline = true, modifier = Modifier.padding(top = 8.dp))
        RcButton(stringResource(R.string.s06_save), onClick = { onSave(name, note.ifBlank { null }, rpe.toIntOrNull()?.coerceIn(1, 10)) }, fillWidth = true, modifier = Modifier.padding(top = 12.dp))
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

private fun targetText(segment: SegmentResultEntity): String =
    segment.targetPace?.let { "≤ ${Formatters.pace(it.toDouble())}" } ?: "Tanpa target"

private fun formatRange(session: SessionEntity): String {
    val fmt = SimpleDateFormat("d MMM yyyy · HH:mm", Locale.getDefault())
    return "${fmt.format(Date(session.startedAtEpochMs))} - ${fmt.format(Date(session.endedAtEpochMs))}"
}

private fun currentLanguage(): VoiceLanguage {
    val language = Locale.getDefault().language
    return if (language.startsWith("in") || language == "id") VoiceLanguage.ID else VoiceLanguage.EN
}
