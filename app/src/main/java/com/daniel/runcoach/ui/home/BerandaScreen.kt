package com.daniel.runcoach.ui.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daniel.runcoach.R
import com.daniel.runcoach.data.RunPersistence
import com.daniel.runcoach.data.RunSnapshot
import com.daniel.runcoach.data.SessionRepository
import com.daniel.runcoach.data.WorkoutTemplate
import com.daniel.runcoach.data.WorkoutTemplateRepository
import com.daniel.runcoach.engine.model.Limit
import com.daniel.runcoach.engine.model.Repeat
import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.model.SegmentKind
import com.daniel.runcoach.engine.model.Workout
import com.daniel.runcoach.engine.model.WorkoutEstimator
import com.daniel.runcoach.engine.model.WorkoutExpander
import com.daniel.runcoach.engine.model.WorkoutJson
import com.daniel.runcoach.engine.stats.WeeklySession
import com.daniel.runcoach.engine.stats.WeeklyStatsAggregator
import com.daniel.runcoach.service.RunForegroundService
import com.daniel.runcoach.ui.Formatters
import com.daniel.runcoach.ui.MainBottomBar
import com.daniel.runcoach.ui.MainTab
import com.daniel.runcoach.ui.components.EmptyState
import com.daniel.runcoach.ui.components.MetricBlock
import com.daniel.runcoach.ui.components.RcButton
import com.daniel.runcoach.ui.components.RcButtonSize
import com.daniel.runcoach.ui.components.RcButtonVariant
import com.daniel.runcoach.ui.components.RcCard
import com.daniel.runcoach.ui.components.RcDialog
import com.daniel.runcoach.ui.components.RcIconButton
import com.daniel.runcoach.ui.components.RcScreenTitle
import com.daniel.runcoach.ui.components.RcTextButton
import com.daniel.runcoach.ui.components.RcTextButtonTone
import com.daniel.runcoach.ui.components.SectionHeader
import com.daniel.runcoach.ui.components.SegmentStrip
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.LocalRcShapes
import com.daniel.runcoach.ui.theme.LocalRcSpacing
import com.daniel.runcoach.ui.theme.LocalRcTypography
import com.daniel.runcoach.ui.theme.RcIcons
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/** S02 Beranda (DESIGN.md section 8): one big Free-run card, weekly summary, template cards, create button. */
@Composable
fun BerandaScreen(
    debugBuild: Boolean,
    onOpenPrestart: (Workout) -> Unit,
    onResumeRun: () -> Unit,
    onOpenEditor: (Workout?) -> Unit,
    onOpenSimulator: () -> Unit,
    onOpenStats: () -> Unit,
    onSelectTab: (MainTab) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember(context) { WorkoutTemplateRepository(context) }
    val sessionRepository = remember(context) { SessionRepository(context) }
    val persistence = remember(context) { RunPersistence(context) }
    val templates by repository.templates.collectAsStateWithLifecycle(initialValue = emptyList())
    var snapshot by remember { mutableStateOf<RunSnapshot?>(null) }
    var pendingDelete by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var weekDistanceM by remember { mutableStateOf(0.0) }
    var weekSessions by remember { mutableStateOf(0) }
    var weekDeltaM by remember { mutableStateOf<Double?>(null) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repository.ensureBuiltIn()
        snapshot = persistence.loadSnapshot()
        val sessions = sessionRepository.allSessions()
        val weekly = sessions.map {
            WeeklySession(it.startedAtEpochMs, it.zoneId, it.totalDistanceM, it.movingMs / 1000.0)
        }
        val week = WeeklyStatsAggregator.lastWeeks(weekly, System.currentTimeMillis(), ZoneId.systemDefault().id, 12).lastOrNull()
        weekDistanceM = week?.totalDistanceM ?: 0.0
        weekSessions = week?.sessionCount ?: 0
        weekDeltaM = week?.deltaM
        loaded = true
    }

    val freeRun = templates.firstOrNull { it.id == WorkoutTemplateRepository.FREE_RUN_ID }
    val userTemplates = templates.filterNot { it.isBuiltIn }
    val colors = LocalRcColors.current
    val spacing = LocalRcSpacing.current

    fun start(workout: Workout, templateId: String?) {
        if (templateId != null) scope.launch { repository.markUsed(templateId) }
        onOpenPrestart(workout)
    }

    androidx.compose.material3.Scaffold(bottomBar = { MainBottomBar(MainTab.WORKOUT, onSelectTab) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(colors.bg).padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = spacing.screenH)
                    .padding(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(spacing.cardGap),
            ) {
                RcScreenTitle(stringResource(R.string.s02_title))

                snapshot?.let { snap ->
                    val name = runCatching { WorkoutJson.decode(snap.workoutJson).name }.getOrDefault("")
                    ResumeCard(
                        name = name,
                        savedLabel = stringResource(R.string.s02_resume_saved, formatTimestamp(snap.startedAtEpochMs)),
                        onResume = { RunForegroundService.resume(context, snap.sessionId); onResumeRun() },
                        onDiscard = { scope.launch { persistence.clear(snap.sessionId) }; snapshot = null },
                    )
                }

                FreeRunCard(onStart = { freeRun?.let { start(it.workout, null) } ?: start(WorkoutTemplateRepository.freeRunWorkout(), null) })

                RcCard(interactive = true, onClick = onOpenStats) {
                    Text(stringResource(R.string.s02_week_label).uppercase(), style = LocalRcTypography.current.label, color = colors.textSecondary)
                    Row(modifier = Modifier.fillMaxWidth().padding(top = spacing.s2), horizontalArrangement = Arrangement.spacedBy(spacing.s5)) {
                        MetricBlock(stringResource(R.string.s02_metric_distance), Formatters.distanceKm(weekDistanceM))
                        MetricBlock(stringResource(R.string.s02_metric_sessions), weekSessions.toString())
                        WeekDelta(weekDeltaM)
                    }
                }

                SectionHeader(stringResource(R.string.s02_workouts_header))

                if (!loaded) {
                    repeat(3) { com.daniel.runcoach.ui.components.Skeleton(modifier = Modifier.fillMaxWidth().height(88.dp)) }
                } else if (userTemplates.isEmpty()) {
                    EmptyState(
                        title = stringResource(R.string.s02_empty_title),
                        message = stringResource(R.string.s02_empty_message),
                        action = { RcButton(stringResource(R.string.s02_empty_action), onClick = { onOpenEditor(null) }, variant = RcButtonVariant.Secondary) },
                    )
                } else {
                    userTemplates.forEach { template ->
                        TemplateCard(
                            template = template,
                            onStart = { start(template.workout, template.id) },
                            onEdit = { onOpenEditor(template.workout) },
                            onDuplicate = { scope.launch { repository.duplicate(template.id) } },
                            onDelete = { pendingDelete = template },
                        )
                    }
                }

                if (debugBuild) {
                    RcButton(stringResource(R.string.home_simulator), onClick = onOpenSimulator, variant = RcButtonVariant.Secondary, fillWidth = true)
                }
            }

            // Extended "create" button, not a default round FAB (DESIGN.md S02).
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(spacing.screenH)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surfaceRaised)
                    .border(1.dp, colors.outlineStrong, RoundedCornerShape(18.dp))
                    .clickable { onOpenEditor(null) }
                    .padding(horizontal = spacing.s4, vertical = spacing.s3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                Icon(painterResource(RcIcons.Add), contentDescription = null, tint = colors.accentText, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.s02_create).uppercase(), style = LocalRcTypography.current.tab, color = colors.textPrimary)
            }
        }
    }

    pendingDelete?.let { template ->
        RcDialog(
            onDismiss = { pendingDelete = null },
            title = stringResource(R.string.s02_delete_title),
            content = { Text(stringResource(R.string.s02_delete_message, template.name), style = LocalRcTypography.current.body, color = colors.textSecondary) },
            dismiss = { RcTextButton(stringResource(R.string.editor_discard_keep), onClick = { pendingDelete = null }, tone = RcTextButtonTone.Neutral) },
            confirm = {
                RcTextButton(
                    stringResource(R.string.s02_delete),
                    onClick = { scope.launch { repository.delete(template.id) }; pendingDelete = null },
                    tone = RcTextButtonTone.Destructive,
                )
            },
        )
    }
}

@Composable
private fun ResumeCard(name: String, savedLabel: String, onResume: () -> Unit, onDiscard: () -> Unit) {
    val colors = LocalRcColors.current
    val shapes = LocalRcShapes.current
    val type = LocalRcTypography.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.accentContainer, RoundedCornerShape(shapes.radiusL))
            .border(1.dp, colors.accent, RoundedCornerShape(shapes.radiusL))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.s02_resume_label).uppercase(), style = type.label, color = colors.accentText)
            Text("$name · $savedLabel", style = type.bodySmall, color = colors.textSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RcTextButton(stringResource(R.string.action_resume), onResume, tone = RcTextButtonTone.Accent)
            RcTextButton(stringResource(R.string.action_discard), onDiscard, tone = RcTextButtonTone.Destructive)
        }
    }
}

@Composable
private fun FreeRunCard(onStart: () -> Unit) {
    val colors = LocalRcColors.current
    val shapes = LocalRcShapes.current
    val spacing = LocalRcSpacing.current
    val type = LocalRcTypography.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(shapes.radiusL))
            .background(colors.accent)
            .clickable(onClick = onStart)
            .padding(spacing.cardPad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(stringResource(R.string.s02_free_run).uppercase(), style = type.titleL, color = colors.onAccent)
            Text(stringResource(R.string.s02_free_run_sub), style = type.bodySmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = colors.onAccent)
        }
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(colors.onAccent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(RcIcons.PlayArrow), contentDescription = null, tint = colors.accent, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun WeekDelta(deltaM: Double?) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(R.string.s02_metric_delta).uppercase(), style = type.label, color = colors.textSecondary)
        if (deltaM == null) {
            Text(stringResource(R.string.s02_new), style = type.numberM, color = colors.textSecondary)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    painterResource(if (deltaM >= 0) RcIcons.TrendingUp else RcIcons.TrendingDown),
                    contentDescription = null,
                    tint = if (deltaM >= 0) colors.pass else colors.neutral,
                    modifier = Modifier.size(20.dp),
                )
                Text(Formatters.distanceKm(deltaM), style = type.numberM, color = colors.accentText)
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: WorkoutTemplate,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    val spacing = LocalRcSpacing.current
    var menu by remember { mutableStateOf(false) }
    RcCard(interactive = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(template.name, style = type.titleS, color = colors.textPrimary, modifier = Modifier.weight(1f))
            Box {
                RcIconButton(RcIcons.MoreVert, stringResource(R.string.s02_more), onClick = { menu = true })
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = colors.surfaceRaised) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.s02_edit)) }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.s02_duplicate)) }, onClick = { menu = false; onDuplicate() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.s02_delete)) }, onClick = { menu = false; onDelete() })
                }
            }
        }
        SegmentStrip(kinds = segmentKinds(template.workout), modifier = Modifier.padding(vertical = spacing.s3))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
            Text(describe(template.workout), style = type.bodySmall, color = colors.textSecondary, modifier = Modifier.weight(1f))
            RcButton(stringResource(R.string.s02_mulai), onClick = onStart, size = RcButtonSize.Small)
        }
        template.lastUsedAt?.let {
            Text(stringResource(R.string.s02_last_used, formatTimestamp(it)), style = type.caption, color = colors.textTertiary)
        }
    }
}

private fun segmentKinds(workout: Workout): List<SegmentKind> = runCatching {
    WorkoutExpander.expandFinite(workout.items).map { it.kind }
}.getOrElse {
    workout.items.flatMap { block ->
        when (block) {
            is Segment -> listOf(block.kind)
            is Repeat -> block.children.map { it.kind }
        }
    }
}

@Composable
private fun describe(workout: Workout): String {
    val segmentCount = runCatching { WorkoutExpander.expandFinite(workout.items).size }.getOrElse {
        workout.items.sumOf { block ->
            when (block) {
                is Segment -> 1
                is Repeat -> block.children.size
            }
        }
    }
    val estimate = WorkoutEstimator.estimate(workout)
    val parts = mutableListOf(stringResource(R.string.s02_segments, segmentCount))
    estimate.distanceM?.let { parts += Formatters.distanceKm(it) }
    estimate.durationSec?.let { parts += Formatters.duration((it * 1000).toLong()) }
    targetRange(workout)?.let { parts += stringResource(R.string.s02_target_range, it) }
    return parts.joinToString(" · ")
}

private fun targetRange(workout: Workout): String? {
    val targets = collectTargets(workout.items).filterNotNull()
    if (targets.isEmpty()) return null
    val min = targets.min()
    val max = targets.max()
    return if (min == max) Formatters.pace(min.toDouble()) else "${Formatters.pace(min.toDouble())}-${Formatters.pace(max.toDouble())}"
}

private fun collectTargets(items: List<com.daniel.runcoach.engine.model.Block>): List<Int?> =
    items.flatMap { block ->
        when (block) {
            is Segment -> listOf(block.targetPaceSecPerKm)
            is Repeat -> block.children.map { it.targetPaceSecPerKm }
        }
    }

private fun formatTimestamp(epochMs: Long): String =
    SimpleDateFormat("d MMM HH:mm", Locale.getDefault()).format(Date(epochMs))
