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
import com.daniel.runcoach.data.SessionEntity
import com.daniel.runcoach.data.SessionRepository
import com.daniel.runcoach.engine.stats.WeekStats
import com.daniel.runcoach.engine.stats.WeeklySession
import com.daniel.runcoach.engine.stats.WeeklyStatsAggregator
import com.daniel.runcoach.ui.Formatters
import com.daniel.runcoach.ui.charts.Bar
import com.daniel.runcoach.ui.charts.BarChart
import com.daniel.runcoach.ui.charts.LineChart
import com.daniel.runcoach.ui.charts.LinePoint
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** S09 Weekly summary (plan section 8): week navigator, number cards, three charts, and the week's sessions. */
@Composable
fun WeeklyScreen(onOpenSession: (String) -> Unit) {
    val context = LocalContext.current
    val repository = remember { SessionRepository(context) }
    var sessions by remember { mutableStateOf<List<SessionEntity>>(emptyList()) }
    var selected by remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
        sessions = repository.allSessions()
        selected = 11
    }

    val zone = ZoneId.systemDefault().id
    val weekly = sessions.map {
        WeeklySession(it.startedAtEpochMs, it.zoneId, it.totalDistanceM, it.movingMs / 1000.0, it.segPass, it.segFail, it.segBorderline)
    }
    val weeks = WeeklyStatsAggregator.lastWeeks(weekly, System.currentTimeMillis(), zone, 12)
    val index = selected.coerceIn(0, weeks.lastIndex.coerceAtLeast(0))
    val week = weeks.getOrNull(index)

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.s09_title), style = MaterialTheme.typography.headlineSmall)
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { if (index > 0) selected = index - 1 }) { Text("<") }
            Text(week?.let { weekLabel(it.weekStart) } ?: "")
            TextButton(onClick = { if (index < weeks.lastIndex) selected = index + 1 }) { Text(">") }
            TextButton(onClick = { selected = 11 }) { Text(stringResource(R.string.s09_this_week)) }
        }

        if (week != null) {
            if (week.sessionCount == 0) Text(stringResource(R.string.s09_empty))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${stringResource(R.string.s09_distance)}: ${Formatters.distanceKm(week.totalDistanceM)}")
                    Text("${stringResource(R.string.s09_time)}: ${Formatters.duration((week.totalMovingSec * 1000).toLong())}")
                    Text("${stringResource(R.string.s09_sessions)}: ${week.sessionCount}")
                    Text("${stringResource(R.string.s09_avg_pace)}: ${Formatters.pace(week.avgPaceSecPerKm)}")
                    Text("${stringResource(R.string.s09_longest)}: ${Formatters.distanceKm(week.longestM)}")
                    Text("${stringResource(R.string.s09_pass)}: ${week.passRatePercent?.let { "$it%" } ?: "--"}")
                    week.deltaM?.let {
                        Text(stringResource(R.string.s09_vs_last, "${Formatters.distanceKm(it)} (${week.deltaPct?.let { p -> "%.0f%%".format(p) } ?: "--"})"))
                    }
                }
            }

            Text(stringResource(R.string.s09_daily), style = MaterialTheme.typography.titleSmall)
            BarChart(bars = week.dailyDistanceM.mapIndexed { i, m -> Bar(dayLabel(i), m / 1000.0) })

            Text(stringResource(R.string.s09_weekly), style = MaterialTheme.typography.titleSmall)
            BarChart(
                bars = weeks.mapIndexed { i, w -> Bar("${i + 1}", w.totalDistanceM / 1000.0) },
                highlightIndex = index,
                onTap = { selected = it },
            )

            Text(stringResource(R.string.s09_pace_weekly), style = MaterialTheme.typography.titleSmall)
            LineChart(
                points = weeks.mapIndexed { i, w -> LinePoint(i.toDouble(), w.avgPaceSecPerKm) },
                valueFormatter = { Formatters.pace(it) },
            )

            val weekSessions = sessions.filter { weekStartOf(it, zone) == week.weekStart }
            weekSessions.forEach { session ->
                Text(
                    "${session.name} · ${Formatters.distanceKm(session.totalDistanceM)}",
                    modifier = Modifier.fillMaxWidth().clickable { onOpenSession(session.id) },
                )
            }
        }
    }
}

private fun weekStartOf(session: SessionEntity, fallbackZone: String): LocalDate =
    java.time.Instant.ofEpochMilli(session.startedAtEpochMs).atZone(ZoneId.of(session.zoneId))
        .toLocalDate().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))

private fun weekLabel(weekStart: LocalDate): String {
    val end = weekStart.plusDays(6)
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    return "${weekStart.dayOfMonth}–${end.format(formatter)}"
}

private fun dayLabel(index: Int): String = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")[index]
