package com.daniel.runcoach.ui.stats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.daniel.runcoach.R
import com.daniel.runcoach.data.RecordRepository
import com.daniel.runcoach.data.RecordRow
import com.daniel.runcoach.engine.result.BestEffortKey
import com.daniel.runcoach.ui.Formatters
import com.daniel.runcoach.ui.MainBottomBar
import com.daniel.runcoach.ui.MainTab
import com.daniel.runcoach.ui.charts.LineChart
import com.daniel.runcoach.ui.charts.LinePoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Tab 3 Statistik (plan 8.1): sub-tabs Weekly (P2.10) and Records (P2.9). */
@Composable
fun StatsScreen(onOpenRecord: (String) -> Unit, onSelectTab: (MainTab) -> Unit) {
    var tab by remember { mutableIntStateOf(1) }
    Scaffold(bottomBar = { MainBottomBar(MainTab.STATS, onSelectTab) }) { padding ->
        Surface(modifier = Modifier.fillMaxSize().padding(padding), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.sub_weekly)) })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.sub_records)) })
                }
                if (tab == 0) {
                    Text(stringResource(R.string.sub_weekly) + " — P2.10")
                } else {
                    RecordsScreen(onOpenRecord)
                }
            }
        }
    }
}

/** S10 Personal records (plan section 8). */
@Composable
fun RecordsScreen(onOpenRecord: (String) -> Unit) {
    val context = LocalContext.current
    val repository = remember { RecordRepository(context) }
    var records by remember { mutableStateOf<Map<BestEffortKey, RecordRow?>>(emptyMap()) }
    var recent by remember { mutableStateOf<List<RecordRow>>(emptyList()) }

    LaunchedEffect(Unit) {
        records = repository.currentRecords()
        recent = repository.recentRecords()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.s10_title), style = MaterialTheme.typography.headlineSmall)
        BestEffortKey.entries.forEach { key ->
            val row = records[key]
            Card(modifier = Modifier.fillMaxWidth().clickable { onOpenRecord(key.name) }) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(keyLabel(key), style = MaterialTheme.typography.titleMedium)
                    if (row == null) {
                        Text(stringResource(R.string.s10_none))
                    } else {
                        Text(
                            "${Formatters.duration(row.durationMs)} · ${Formatters.pace(paceOf(row))} · ${formatDate(row.startedAtEpochMs)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(row.sessionName, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Text(stringResource(R.string.s10_note), style = MaterialTheme.typography.labelSmall)
        if (recent.isNotEmpty()) {
            Text(stringResource(R.string.s10_recent), style = MaterialTheme.typography.titleSmall)
            recent.forEach { row ->
                Text("${keyLabel(row.key)}: ${Formatters.duration(row.durationMs)} · ${formatDate(row.startedAtEpochMs)}")
            }
        }
    }
}

/** S11 Record detail (plan section 8). */
@Composable
fun RecordDetailScreen(keyName: String, onOpenSession: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { RecordRepository(context) }
    val key = remember(keyName) { runCatching { BestEffortKey.valueOf(keyName) }.getOrDefault(BestEffortKey.K1) }
    var current by remember { mutableStateOf<RecordRow?>(null) }
    var top by remember { mutableStateOf<List<RecordRow>>(emptyList()) }
    var series by remember { mutableStateOf<List<Pair<Long, Long>>>(emptyList()) }
    var byPace by remember { mutableStateOf(false) }

    LaunchedEffect(key) {
        current = repository.currentRecords()[key]
        top = repository.topN(key, 10)
        series = repository.series(key).map { it.atEpochMs to it.durationMs }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.s11_title), style = MaterialTheme.typography.headlineSmall)
        Text(keyLabel(key), style = MaterialTheme.typography.titleMedium)
        current?.let { row ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onOpenSession(row.sessionId) }) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.s11_current), style = MaterialTheme.typography.titleSmall)
                    Text("${Formatters.duration(row.durationMs)} · ${Formatters.pace(paceOf(row))} · ${formatDate(row.startedAtEpochMs)}")
                    Text(row.sessionName)
                    Text(stringResource(R.string.s11_from, "%.1f".format(row.startOffsetM / 1000.0)))
                }
            }
        } ?: Text(stringResource(R.string.s10_none))

        if (series.size >= 2) {
            val pace = byPace
            LineChart(
                points = series.map { (at, ms) -> LinePoint(at.toDouble(), if (pace) paceOfMillis(ms, key) else ms / 1000.0) },
                valueFormatter = { value -> if (pace) Formatters.pace(value) else Formatters.duration((value * 1000).toLong()) },
            )
        }

        TextButton(onClick = { byPace = !byPace }) {
            Text(if (byPace) stringResource(R.string.s11_by_time) else stringResource(R.string.s11_by_pace))
        }

        Text(stringResource(R.string.s11_top10), style = MaterialTheme.typography.titleSmall)
        top.forEachIndexed { index, row ->
            Text(
                "${index + 1}. ${Formatters.duration(row.durationMs)} · ${Formatters.pace(paceOf(row))} · ${formatDate(row.startedAtEpochMs)}",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_back)) }
    }
}

private fun keyLabel(key: BestEffortKey): String = when (key) {
    BestEffortKey.K1 -> "1 km"
    BestEffortKey.K5 -> "5 km"
    BestEffortKey.K10 -> "10 km"
    BestEffortKey.HM -> "Half Marathon"
    BestEffortKey.FM -> "Marathon"
}

private fun paceOf(row: RecordRow): Double = paceOfMillis(row.durationMs, row.key)

private fun paceOfMillis(durationMs: Long, key: BestEffortKey): Double =
    durationMs / 1000.0 / (key.meters / 1000.0)

private fun formatDate(epochMs: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(epochMs))
