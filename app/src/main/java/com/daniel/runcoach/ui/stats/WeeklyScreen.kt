package com.daniel.runcoach.ui.stats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.daniel.runcoach.data.SessionEntity
import com.daniel.runcoach.data.SessionRepository
import com.daniel.runcoach.engine.stats.WeeklySession
import com.daniel.runcoach.engine.stats.WeeklyStatsAggregator
import com.daniel.runcoach.ui.Formatters
import com.daniel.runcoach.ui.charts.Bar
import com.daniel.runcoach.ui.charts.ChartBar
import com.daniel.runcoach.ui.charts.ChartLine
import com.daniel.runcoach.ui.charts.LinePoint
import com.daniel.runcoach.ui.components.MetricBlock
import com.daniel.runcoach.ui.components.RcButton
import com.daniel.runcoach.ui.components.RcButtonVariant
import com.daniel.runcoach.ui.components.RcCard
import com.daniel.runcoach.ui.components.RcDivider
import com.daniel.runcoach.ui.components.RcIconButton
import com.daniel.runcoach.ui.components.ResultChip
import com.daniel.runcoach.ui.components.RcStatus
import com.daniel.runcoach.ui.components.SectionHeader
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.LocalRcSpacing
import com.daniel.runcoach.ui.theme.LocalRcTypography
import com.daniel.runcoach.ui.theme.RcIcons
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** S09 Weekly summary (DESIGN.md section 8): week navigator, number grid, three charts, week sessions. */
@Composable
fun WeeklyScreen(onOpenSession: (String) -> Unit) {
    val context = LocalContext.current
    val repository = remember { SessionRepository(context) }
    var sessions by remember { mutableStateOf<List<SessionEntity>>(emptyList()) }
    var selected by remember { mutableIntStateOf(-1) }
    val colors = LocalRcColors.current
    val spacing = LocalRcSpacing.current
    val type = LocalRcTypography.current

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

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = spacing.screenH),
        verticalArrangement = Arrangement.spacedBy(spacing.cardGap),
    ) {
        // Week navigator
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
            RcIconButton(RcIcons.ChevronLeft, "prev", onClick = { if (index > 0) selected = index - 1 }, enabled = index > 0)
            Text(week?.let { weekLabel(it.weekStart) } ?: "", style = type.titleS, color = colors.textPrimary, modifier = Modifier.weight(1f))
            RcIconButton(RcIcons.ChevronRight, "next", onClick = { if (index < weeks.lastIndex) selected = index + 1 }, enabled = index < weeks.lastIndex)
            if (index != weeks.lastIndex) {
                RcButton(stringResource(R.string.s09_this_week), onClick = { selected = 11 }, variant = RcButtonVariant.Tertiary)
            }
        }

        if (week != null) {
            // Number grid 2x3
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.cardGap)) {
                NumberCard(stringResource(R.string.s09_distance), Formatters.distanceKm(week.totalDistanceM), Modifier.weight(1f), delta = week.deltaM)
                NumberCard(stringResource(R.string.s09_time), Formatters.duration((week.totalMovingSec * 1000).toLong()), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.cardGap)) {
                NumberCard(stringResource(R.string.s09_sessions), week.sessionCount.toString(), Modifier.weight(1f))
                NumberCard(stringResource(R.string.s09_avg_pace), Formatters.pace(week.avgPaceSecPerKm), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.cardGap)) {
                NumberCard(stringResource(R.string.s09_longest), Formatters.distanceKm(week.longestM), Modifier.weight(1f))
                NumberCard(stringResource(R.string.s09_pass), week.passRatePercent?.let { "$it%" } ?: "--", Modifier.weight(1f))
            }
            if (week.sessionCount == 0) {
                Text(stringResource(R.string.s09_empty), style = type.bodySmall, color = colors.textSecondary)
            }

            SectionHeader(stringResource(R.string.s09_daily))
            ChartBar(
                bars = week.dailyDistanceM.mapIndexed { i, m -> Bar(dayLabel(i), m / 1000.0) },
                minMax = 5.0,
                highlightIndex = todayIndex(zone),
                valueFormatter = { "%.1f".format(it) },
            )

            SectionHeader(stringResource(R.string.s09_weekly))
            ChartBar(
                bars = weeks.mapIndexed { i, w -> Bar(weekShortLabel(w.weekStart), w.totalDistanceM / 1000.0) },
                minMax = 5.0,
                highlightIndex = index,
                valueFormatter = { "%.0f".format(it) },
                onTap = { selected = it },
            )

            SectionHeader(stringResource(R.string.s09_pace_weekly))
            val pacePoints = weeks.mapIndexed { i, w -> LinePoint(i.toDouble(), w.avgPaceSecPerKm) }
            if (pacePoints.count { it.y != null } >= 2) {
                ChartLine(points = pacePoints, fasterLabel = stringResource(R.string.chart_faster), valueFormatter = { Formatters.pace(it) })
            } else {
                Text(stringResource(R.string.s09_more_data), style = type.caption, color = colors.textTertiary)
            }

            SectionHeader(stringResource(R.string.s09_sessions))
            val weekSessions = sessions.filter { weekStartOf(it) == week.weekStart }
            if (weekSessions.isEmpty()) {
                Text(stringResource(R.string.s09_empty), style = type.bodySmall, color = colors.textSecondary)
            } else {
                weekSessions.forEach { session ->
                    SessionLine(session) { onOpenSession(session.id) }
                    RcDivider()
                }
            }
            androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = spacing.s4))
        }
    }
}

@Composable
private fun NumberCard(label: String, value: String, modifier: Modifier = Modifier, delta: Double? = null) {
    val colors = LocalRcColors.current
    val spacing = LocalRcSpacing.current
    val type = LocalRcTypography.current
    RcCard(modifier = modifier.heightIn(min = 84.dp)) {
        MetricBlock(label, value, valueStyle = type.numberM)
        delta?.let {
            Text(stringResource(R.string.s09_vs_last, Formatters.distanceKm(it)), style = type.caption, color = colors.textTertiary, modifier = Modifier.padding(top = spacing.s1))
        }
    }
}

@Composable
private fun SessionLine(session: SessionEntity, onClick: () -> Unit) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    val targetCount = session.segPass + session.segFail + session.segBorderline
    RcCard(interactive = true, onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(session.name, style = type.titleS, color = colors.textPrimary, maxLines = 1)
                Text("${Formatters.duration(session.movingMs)} · ${Formatters.pace(session.avgPaceSecPerKm)} /km", style = type.bodySmall, color = colors.textSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ResultChip(
                    status = if (targetCount == 0) RcStatus.Neutral else if (session.segFail == 0) RcStatus.Pass else RcStatus.Warn,
                    label = if (targetCount == 0) stringResource(R.string.s07_chip_no_target) else stringResource(R.string.s07_chip_pass, session.segPass, targetCount),
                )
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(Formatters.distanceKm(session.totalDistanceM).removeSuffix(" km"), style = type.numberL, color = colors.textPrimary)
                    Text("km", style = type.bodySmall, color = colors.textSecondary)
                }
            }
        }
    }
}

private fun weekStartOf(session: SessionEntity): LocalDate =
    Instant.ofEpochMilli(session.startedAtEpochMs).atZone(ZoneId.of(session.zoneId))
        .toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

private fun weekLabel(weekStart: LocalDate): String {
    val end = weekStart.plusDays(6)
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    return "${weekStart.dayOfMonth}–${end.format(formatter)}"
}

private fun weekShortLabel(weekStart: LocalDate): String =
    DateTimeFormatter.ofPattern("d/M", Locale.getDefault()).format(weekStart)

private fun dayLabel(index: Int): String {
    val day = DayOfWeek.MONDAY.plus(index.toLong())
    return day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3)
}

private fun todayIndex(zone: String): Int {
    val today = LocalDate.now(ZoneId.of(zone))
    return (today.dayOfWeek.value - 1).coerceIn(0, 6)
}
