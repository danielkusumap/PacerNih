package com.daniel.runcoach.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.daniel.runcoach.R
import com.daniel.runcoach.data.KmSplitEntity
import com.daniel.runcoach.data.SegmentResultEntity
import com.daniel.runcoach.data.SessionEntity
import com.daniel.runcoach.data.SessionRepository
import com.daniel.runcoach.data.TrackPointEntity
import com.daniel.runcoach.engine.gps.haversineM
import com.daniel.runcoach.ui.Formatters
import com.daniel.runcoach.ui.map.RouteMapView
import com.daniel.runcoach.ui.charts.Bar
import com.daniel.runcoach.ui.charts.BarChart
import com.daniel.runcoach.ui.charts.LineChart
import com.daniel.runcoach.ui.charts.LinePoint
import com.daniel.runcoach.ui.charts.TargetStep
import kotlinx.coroutines.delay

/**
 * S08 Session detail (plan section 8): fixed header with stats plus Segments / Km splits / Charts tabs.
 * The Map tab arrives in P3.1.
 */
@Composable
fun DetailSessionScreen(sessionId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { SessionRepository(context) }
    var session by remember { mutableStateOf<SessionEntity?>(null) }
    var segments by remember { mutableStateOf<List<SegmentResultEntity>>(emptyList()) }
    var splits by remember { mutableStateOf<List<KmSplitEntity>>(emptyList()) }
    var trackPoints by remember { mutableStateOf<List<TrackPointEntity>>(emptyList()) }
    var tab by remember { mutableIntStateOf(0) }

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

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val current = session
            if (current == null) {
                Text(stringResource(R.string.s08_loading))
                return@Column
            }

            Text(current.name, style = MaterialTheme.typography.headlineSmall)
            Text(current.templateNameSnapshot, style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Stat(Formatters.distanceKm(current.totalDistanceM))
                Stat(Formatters.duration(current.movingMs))
                Stat(Formatters.pace(current.avgPaceSecPerKm))
            }
            Text(
                stringResource(R.string.s06_seg_counts, current.segPass, current.segFail, current.segBorderline),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                if (current.planCompleted) stringResource(R.string.s08_plan_done) else stringResource(R.string.s08_plan_stopped),
                style = MaterialTheme.typography.bodySmall,
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.s08_note_title), style = MaterialTheme.typography.titleSmall)
                    Text(current.note?.takeIf { it.isNotBlank() } ?: stringResource(R.string.s08_no_note))
                    if (current.rpe != null) Text("RPE: ${current.rpe}")
                }
            }

            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.s08_tab_segments)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.s08_tab_splits)) })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text(stringResource(R.string.s08_tab_charts)) })
                if (trackPoints.size >= 2) {
                    Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text(stringResource(R.string.s08_tab_map)) })
                }
            }
            when (tab) {
                0 -> SegmentsTab(segments)
                1 -> SplitsTab(splits)
                2 -> ChartsTab(segments, splits)
                else -> RouteMapView(
                    points = trackPoints.map { it.lat to it.lon },
                    boundaryIndices = boundaryIndices(trackPoints, segments),
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                )
            }

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_back)) }
        }
    }
}

@Composable
private fun Stat(value: String) {
    Text(value, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun SegmentsTab(segments: List<SegmentResultEntity>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        segments.forEach { segment ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${segment.idx}. ${segment.label} (${segment.kind})", style = MaterialTheme.typography.titleSmall)
                    Text("${limitText(segment)} · ${targetText(segment)}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${Formatters.distanceKm(segment.distanceM)} · ${Formatters.duration(segment.durationMs)} · ${Formatters.pace(segment.avgPace)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "${segment.deltaSec?.let { Formatters.deltaSeconds(it) } ?: ""} ${statusText(segment.status)}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    if (segment.status == "BORDERLINE") Text("dalam batas error GPS", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun SplitsTab(splits: List<KmSplitEntity>) {
    val fastestIndex = splits.filter { !it.isPartialRow() && it.paceSecPerKm != null }.minByOrNull { it.paceSecPerKm!! }?.idx
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        splits.forEach { split ->
            val partial = split.distanceM < 1000.0
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (partial) "%.2f km".format(split.distanceM / 1000.0) else "Km ${split.idx}")
                Text(Formatters.duration(split.durationMs))
                Text(Formatters.pace(split.paceSecPerKm))
                Text(if (split.idx == fastestIndex) "★" else "")
            }
        }
        if (fastestIndex != null) Text(stringResource(R.string.s08_fastest) + ": Km $fastestIndex", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ChartsTab(segments: List<SegmentResultEntity>, splits: List<KmSplitEntity>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val fullSplits = splits.filter { it.distanceM >= 1000.0 && it.paceSecPerKm != null }
        BarChart(
            bars = fullSplits.map { Bar("Km ${it.idx}", it.paceSecPerKm, it.targetPace?.toDouble()) },
            valueFormatter = { Formatters.pace(it) },
        )
        val points = fullSplits.mapIndexed { index, split -> LinePoint((index + 1) * 1000.0, split.paceSecPerKm) }
        val steps = targetSteps(segments)
        LineChart(
            points = points,
            steps = steps,
            boundaries = segments.runningFold(0.0) { acc, s -> acc + s.distanceM }.drop(1).dropLast(1),
            valueFormatter = { Formatters.pace(it) },
        )
    }
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

private fun targetSteps(segments: List<SegmentResultEntity>): List<TargetStep> {    var cumulative = 0.0
    val steps = mutableListOf<TargetStep>()
    segments.forEach { segment ->
        val start = cumulative
        cumulative += segment.distanceM
        segment.targetPace?.let { steps += TargetStep(start, cumulative, it.toDouble()) }
    }
    return steps
}

private fun KmSplitEntity.isPartialRow(): Boolean = distanceM < 1000.0

private fun limitText(segment: SegmentResultEntity): String = when (segment.limitType) {
    "distance" -> "${segment.limitValue.toInt()} m"
    "duration" -> Formatters.duration(segment.limitValue.toLong() * 1000L)
    else -> "Open"
}

private fun targetText(segment: SegmentResultEntity): String =
    segment.targetPace?.let { "≤ ${Formatters.pace(it.toDouble())}" } ?: "Tanpa target"

private fun statusText(status: String): String = status
