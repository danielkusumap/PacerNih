package com.daniel.runcoach.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.daniel.runcoach.R
import com.daniel.runcoach.data.RecordRepository
import com.daniel.runcoach.data.RecordRow
import com.daniel.runcoach.engine.result.BestEffortKey
import com.daniel.runcoach.ui.Formatters
import com.daniel.runcoach.ui.MainBottomBar
import com.daniel.runcoach.ui.MainTab
import com.daniel.runcoach.ui.charts.ChartLine
import com.daniel.runcoach.ui.charts.LinePoint
import com.daniel.runcoach.ui.charts.TargetStep
import com.daniel.runcoach.ui.components.ListRow
import com.daniel.runcoach.ui.components.RcButton
import com.daniel.runcoach.ui.components.RcButtonVariant
import com.daniel.runcoach.ui.components.RcCard
import com.daniel.runcoach.ui.components.RcDivider
import com.daniel.runcoach.ui.components.RcScreenTitle
import com.daniel.runcoach.ui.components.SectionHeader
import com.daniel.runcoach.ui.components.SegmentedControl
import com.daniel.runcoach.ui.components.TopBar
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.LocalRcShapes
import com.daniel.runcoach.ui.theme.LocalRcSpacing
import com.daniel.runcoach.ui.theme.LocalRcTypography
import com.daniel.runcoach.ui.theme.RcIcons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Tab 3 Statistik (DESIGN.md S09/S10): SegmentedControl Weekly | Records. */
@Composable
fun StatsScreen(onOpenRecord: (String) -> Unit, onOpenSession: (String) -> Unit, onSelectTab: (MainTab) -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val colors = LocalRcColors.current
    val spacing = LocalRcSpacing.current
    androidx.compose.material3.Scaffold(bottomBar = { MainBottomBar(MainTab.STATS, onSelectTab) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(colors.bg).padding(padding)) {
            RcScreenTitle(stringResource(R.string.tab_stats), modifier = Modifier.padding(horizontal = spacing.screenH, vertical = spacing.s2))
            SegmentedControl(
                options = listOf(0 to stringResource(R.string.sub_weekly), 1 to stringResource(R.string.sub_records)),
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier.padding(horizontal = spacing.screenH),
            )
            if (tab == 0) {
                WeeklyScreen(onOpenSession)
            } else {
                RecordsScreen(onOpenRecord)
            }
        }
    }
}

/** S10 Personal records (DESIGN.md section 8). */
@Composable
fun RecordsScreen(onOpenRecord: (String) -> Unit) {
    val context = LocalContext.current
    val repository = remember { RecordRepository(context) }
    var records by remember { mutableStateOf<Map<BestEffortKey, RecordRow?>>(emptyMap()) }
    var recent by remember { mutableStateOf<List<RecordRow>>(emptyList()) }
    val colors = LocalRcColors.current
    val spacing = LocalRcSpacing.current
    val type = LocalRcTypography.current

    LaunchedEffect(Unit) {
        records = repository.currentRecords()
        recent = repository.recentRecords()
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = spacing.screenH),
        verticalArrangement = Arrangement.spacedBy(spacing.cardGap),
    ) {
        BestEffortKey.entries.forEach { key ->
            RecordCard(key = key, row = records[key], onClick = { onOpenRecord(key.name) })
        }
        SectionHeader(stringResource(R.string.s10_recent))
        if (recent.isEmpty()) {
            Text(stringResource(R.string.s10_none), style = type.bodySmall, color = colors.textSecondary)
        } else {
            recent.forEach { row ->
                ListRow(
                    title = keyLabel(row.key),
                    subtitle = formatDate(row.startedAtEpochMs),
                    trailing = { Text(Formatters.duration(row.durationMs), style = type.titleS, color = colors.textPrimary) },
                )
                RcDivider()
            }
        }
        Text(stringResource(R.string.s10_note), style = type.caption, color = colors.textTertiary, modifier = Modifier.padding(bottom = spacing.s4))
    }
}

@Composable
private fun RecordCard(key: BestEffortKey, row: RecordRow?, onClick: () -> Unit) {
    val colors = LocalRcColors.current
    val shapes = LocalRcShapes.current
    val spacing = LocalRcSpacing.current
    val type = LocalRcTypography.current
    if (row == null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp)
                .dashedBorder(colors.outlineStrong, shapes.radiusL)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(keyLabel(key).uppercase(), style = type.label, color = colors.textSecondary)
                Text(stringResource(R.string.s10_none), style = type.bodySmall, color = colors.textTertiary, modifier = Modifier.padding(top = 6.dp))
            }
            Text("--:--", style = type.numberXL, color = colors.textTertiary)
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp)
                .background(colors.surface, RoundedCornerShape(shapes.radiusL))
                .border(1.dp, colors.outline, RoundedCornerShape(shapes.radiusL))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(keyLabel(key).uppercase(), style = type.label, color = colors.textSecondary)
                Text("${formatDate(row.startedAtEpochMs)} · ${row.sessionName}", style = type.bodySmall, color = colors.textSecondary, modifier = Modifier.padding(top = 6.dp))
                Text(stringResource(R.string.s10_pace, Formatters.pace(paceOf(row))), style = type.bodySmall, color = colors.textSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                Text(Formatters.duration(row.durationMs), style = type.numberXL, color = colors.textPrimary)
                Icon(painterResource(RcIcons.EmojiEvents), contentDescription = null, tint = colors.accentText, modifier = Modifier.size(24.dp))
            }
        }
    }
}

/** Rounded dashed border (Compose's border() cannot dash). */
private fun Modifier.dashedBorder(color: Color, radius: androidx.compose.ui.unit.Dp) = drawBehind {
    drawRoundRect(
        color = color,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius.toPx()),
        style = Stroke(
            width = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
        ),
    )
}

/** S11 Record detail (DESIGN.md section 8). */
@Composable
fun RecordDetailScreen(keyName: String, onOpenSession: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { RecordRepository(context) }
    val key = remember(keyName) { runCatching { BestEffortKey.valueOf(keyName) }.getOrDefault(BestEffortKey.K1) }
    var current by remember { mutableStateOf<RecordRow?>(null) }
    var top by remember { mutableStateOf<List<RecordRow>>(emptyList()) }
    var series by remember { mutableStateOf<List<Pair<Long, Long>>>(emptyList()) }
    var byPace by remember { mutableStateOf(false) }
    val colors = LocalRcColors.current
    val spacing = LocalRcSpacing.current
    val type = LocalRcTypography.current

    LaunchedEffect(key) {
        current = repository.currentRecords()[key]
        top = repository.topN(key, 10)
        series = repository.series(key).map { it.atEpochMs to it.durationMs }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        TopBar(title = keyLabel(key), onBack = onBack)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = spacing.screenH),
            verticalArrangement = Arrangement.spacedBy(spacing.cardGap),
        ) {
            val hero = current
            if (hero == null) {
                Text(stringResource(R.string.s10_none), style = type.body, color = colors.textSecondary)
            } else {
                Text(Formatters.duration(hero.durationMs), style = type.displayL, color = colors.textPrimary)
                Text(stringResource(R.string.s10_pace, Formatters.pace(paceOf(hero))), style = type.body, color = colors.textSecondary)
                Row(
                    modifier = Modifier.clickable { onOpenSession(hero.sessionId) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("${formatDate(hero.startedAtEpochMs)} · ${hero.sessionName}", style = type.titleS, color = colors.accentText)
                    Icon(painterResource(RcIcons.ChevronRight), contentDescription = null, tint = colors.accentText, modifier = Modifier.size(18.dp))
                }
            }

            SegmentedControl(
                options = listOf(false to stringResource(R.string.s11_by_time), true to stringResource(R.string.s11_by_pace)),
                selected = byPace,
                onSelect = { byPace = it },
            )

            if (series.size >= 2) {
                val pace = byPace
                val points = series.mapIndexed { i, (_, ms) -> LinePoint(i.toDouble(), if (pace) paceOfMillis(ms, key) else ms / 1000.0) }
                val prSteps = if (pace) emptyList() else prStaircase(series)
                ChartLine(
                    points = points,
                    steps = prSteps,
                    reversedPace = pace,
                    fasterLabel = if (pace) stringResource(R.string.chart_faster) else null,
                    valueFormatter = { value -> if (pace) Formatters.pace(value) else Formatters.duration((value * 1000).toLong()) },
                )
            } else {
                Text(stringResource(R.string.s11_need_two), style = type.caption, color = colors.textTertiary)
            }

            SectionHeader(stringResource(R.string.s11_top10))
            top.forEachIndexed { index, row ->
                ListRow(
                    leading = { Text("${index + 1}", style = type.numberM, color = if (index == 0) colors.accentText else colors.textSecondary) },
                    title = Formatters.duration(row.durationMs),
                    subtitle = "${Formatters.pace(paceOf(row))} · ${formatDate(row.startedAtEpochMs)}",
                    trailing = { Text(row.sessionName, style = type.caption, color = colors.textTertiary) },
                    onClick = { onOpenSession(row.sessionId) },
                )
                RcDivider()
            }
            RcButton(stringResource(R.string.action_back), onClick = onBack, variant = RcButtonVariant.Secondary, fillWidth = true, modifier = Modifier.padding(bottom = spacing.s4))
        }
    }
}

private fun prStaircase(series: List<Pair<Long, Long>>): List<TargetStep> {
    val steps = mutableListOf<TargetStep>()
    var best = Long.MAX_VALUE
    series.forEachIndexed { i, (_, ms) ->
        best = minOf(best, ms)
        val end = if (i < series.lastIndex) i + 1.0 else i + 0.5
        steps += TargetStep(i.toDouble(), end, best / 1000.0)
    }
    return steps
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
