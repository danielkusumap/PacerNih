package com.daniel.runcoach.ui.editor

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.daniel.runcoach.R
import com.daniel.runcoach.data.WorkoutTemplateRepository
import com.daniel.runcoach.engine.model.Block
import com.daniel.runcoach.engine.model.Limit
import com.daniel.runcoach.engine.model.Repeat
import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.model.SegmentKind
import com.daniel.runcoach.engine.model.Workout
import com.daniel.runcoach.engine.model.WorkoutEstimator
import com.daniel.runcoach.engine.model.WorkoutExpander
import com.daniel.runcoach.engine.model.WorkoutPresets
import com.daniel.runcoach.engine.model.WorkoutValidator
import com.daniel.runcoach.ui.Formatters
import com.daniel.runcoach.ui.components.InfoIcon
import com.daniel.runcoach.ui.components.RcButton
import com.daniel.runcoach.ui.components.RcButtonVariant
import com.daniel.runcoach.ui.components.RcCard
import com.daniel.runcoach.ui.components.RcDialog
import com.daniel.runcoach.ui.components.RcIconButton
import com.daniel.runcoach.ui.components.RcSheet
import com.daniel.runcoach.ui.components.RcSwitchRow
import com.daniel.runcoach.ui.components.RcTextField
import com.daniel.runcoach.ui.components.SegmentStrip
import com.daniel.runcoach.ui.components.SegmentedControl
import com.daniel.runcoach.ui.components.TopBar
import com.daniel.runcoach.ui.components.segmentKindColor
import com.daniel.runcoach.ui.components.segmentKindIcon
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.LocalRcSpacing
import com.daniel.runcoach.ui.theme.LocalRcTypography
import com.daniel.runcoach.ui.theme.RcIcons
import kotlinx.coroutines.launch

/** Topics for the D11 info sheet (plan 8.3). Content lives in strings.xml (ID + EN). */
enum class InfoTopic(@StringRes val titleRes: Int, @StringRes val bodyRes: Int) {
    WARMUP(R.string.info_warmup_title, R.string.info_warmup_body),
    RUN(R.string.info_run_title, R.string.info_run_body),
    WALK(R.string.info_walk_title, R.string.info_walk_body),
    RECOVERY(R.string.info_recovery_title, R.string.info_recovery_body),
    COOLDOWN(R.string.info_cooldown_title, R.string.info_cooldown_body),
    FREE(R.string.info_free_title, R.string.info_free_body),
    LIMIT(R.string.info_limit_title, R.string.info_limit_body),
    TARGET(R.string.info_target_title, R.string.info_target_body),
    MARGIN(R.string.info_margin_title, R.string.info_margin_body),
    PER_KM(R.string.info_perkm_title, R.string.info_perkm_body),
    REPEAT(R.string.info_repeat_title, R.string.info_repeat_body),
    CUE(R.string.info_cue_title, R.string.info_cue_body),
}

private fun kindTopic(kind: SegmentKind) = when (kind) {
    SegmentKind.WARMUP -> InfoTopic.WARMUP
    SegmentKind.RUN -> InfoTopic.RUN
    SegmentKind.WALK -> InfoTopic.WALK
    SegmentKind.RECOVERY -> InfoTopic.RECOVERY
    SegmentKind.COOLDOWN -> InfoTopic.COOLDOWN
}

/** S03 Editor Workout (DESIGN.md section 8): name, goal, block list, summary, presets, D01/D02/D11. */
@Composable
fun EditorScreen(initial: Workout? = null, onSaved: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember(context) { WorkoutTemplateRepository(context) }

    var name by remember { mutableStateOf(initial?.name ?: "") }
    var goal by remember { mutableStateOf(initial?.goalDistanceM?.toString() ?: "") }
    val items = remember { (initial?.items ?: emptyList()).toMutableStateList() }
    var dirty by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }
    var infoTopic by remember { mutableStateOf<InfoTopic?>(null) }
    var showPresets by remember { mutableStateOf(false) }
    var segmentEditIndex by remember { mutableStateOf(-1) }
    var repeatBoundIndex by remember { mutableStateOf(-1) }
    var childEdit by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val workout = Workout(id = initial?.id ?: "", name = name, items = items.toList(), goalDistanceM = goal.toIntOrNull())
    val errors = WorkoutValidator.validate(workout)
    val estimate = WorkoutEstimator.estimate(workout)
    val expandedCount = runCatching { WorkoutExpander.expandFinite(items).size }.getOrElse {
        items.sumOf { block -> when (block) { is Segment -> 1; is Repeat -> (block.times ?: 1) * block.children.size } }
    }
    val kinds = runCatching { WorkoutExpander.expandFinite(items).map { it.kind } }.getOrElse {
        items.flatMap { block -> when (block) { is Segment -> listOf(block.kind); is Repeat -> block.children.map { it.kind } } }
    }
    val canSave = editorCanSave(name, errors.size)
    val colors = LocalRcColors.current
    val spacing = LocalRcSpacing.current
    val type = LocalRcTypography.current

    fun save() {
        scope.launch {
            if (!initial?.id.isNullOrBlank()) repository.update(workout) else repository.create(workout)
            onSaved()
        }
    }

    BackHandler { if (dirty) showDiscard = true else onCancel() }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        TopBar(
            title = stringResource(if (initial == null) R.string.editor_title else R.string.editor_title_edit),
            onBack = { if (dirty) showDiscard = true else onCancel() },
            actions = {
                RcButton(
                    stringResource(R.string.editor_save),
                    onClick = { save() },
                    variant = RcButtonVariant.Tertiary,
                    enabled = canSave,
                    modifier = Modifier.testTag("btn_save"),
                )
            },
        )

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = spacing.screenH), verticalArrangement = Arrangement.spacedBy(spacing.cardGap)) {
            RcTextField(value = name, onValueChange = { name = it; dirty = true }, label = stringResource(R.string.editor_name), modifier = Modifier.fillMaxWidth(), textFieldTag = "field_name")
            RcTextField(value = goal, onValueChange = { goal = it.filter(Char::isDigit); dirty = true }, label = stringResource(R.string.editor_goal), modifier = Modifier.fillMaxWidth())

            // Summary bar
            RcCard {
                SegmentStrip(kinds = kinds)
                Text(
                    "${stringResource(R.string.editor_seg_count, expandedCount)} · " +
                        (estimate.distanceM?.let { estimateText(Formatters.distanceKm(it), estimate.distanceIsAtLeast) } ?: "") +
                        (estimate.durationSec?.let { " · " + estimateText(Formatters.duration((it * 1000).toLong()), estimate.durationIsAtLeast) } ?: ""),
                    style = type.bodySmall,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = spacing.s2),
                )
            }

            items.forEachIndexed { index, block ->
                when (block) {
                    is Segment -> SegmentCard(
                        segment = block,
                        onEdit = { segmentEditIndex = index },
                        onInfo = { infoTopic = kindTopic(block.kind) },
                        onUp = { if (index > 0) { val t = items[index]; items[index] = items[index - 1]; items[index - 1] = t; dirty = true } },
                        onDown = { if (index < items.lastIndex) { val t = items[index]; items[index] = items[index + 1]; items[index + 1] = t; dirty = true } },
                        onDuplicate = { items.add(index + 1, block.copy()); dirty = true },
                        onDelete = { items.removeAt(index); dirty = true },
                    )

                    is Repeat -> {
                        RepeatBlock(
                            repeat = block,
                            onEditBound = { repeatBoundIndex = index },
                            onInfo = { infoTopic = InfoTopic.REPEAT },
                            onAddChild = { items[index] = block.copy(children = block.children + defaultChild()); dirty = true },
                            onDelete = { items.removeAt(index); dirty = true },
                            onEditChild = { childIndex -> childEdit = index to childIndex },
                            onDeleteChild = { childIndex ->
                                items[index] = block.copy(children = block.children.filterIndexed { i, _ -> i != childIndex })
                                dirty = true
                            },
                        )
                    }
                }
            }

            if (errors.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(painterResource(RcIcons.Error), contentDescription = null, tint = colors.fail, modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.editor_invalid, errors.size), style = type.bodySmall, color = colors.fail)
                }
            }
            Box(Modifier.padding(bottom = spacing.s2))
        }

        // Bottom action row (sticky)
        Row(modifier = Modifier.fillMaxWidth().background(colors.bg).padding(horizontal = spacing.screenH).padding(vertical = spacing.s3), horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
            RcButton(stringResource(R.string.editor_add_segment), onClick = { items.add(defaultSegment()); dirty = true }, variant = RcButtonVariant.Secondary, modifier = Modifier.weight(1f).testTag("btn_add_segment"))
            RcButton(stringResource(R.string.editor_add_repeat), onClick = { items.add(defaultRepeat()); dirty = true }, variant = RcButtonVariant.Secondary, modifier = Modifier.weight(1f))
            RcButton(stringResource(R.string.editor_preset), onClick = { showPresets = true }, variant = RcButtonVariant.Secondary, modifier = Modifier.weight(1f))
        }
    }

    if (segmentEditIndex in items.indices) {
        val segment = items[segmentEditIndex] as Segment
        SegmentSheet(
            initial = segment,
            allowOpen = segmentEditIndex == items.lastIndex,
            onInfo = { infoTopic = it },
            onDone = { items[segmentEditIndex] = it; dirty = true; segmentEditIndex = -1 },
            onDismiss = { segmentEditIndex = -1 },
        )
    }
    childEdit?.let { (repeatIndex, childIndex) ->
        val repeat = items[repeatIndex] as Repeat
        SegmentSheet(
            initial = repeat.children[childIndex],
            allowOpen = false,
            onInfo = { infoTopic = it },
            onDone = { edited ->
                val newChildren = repeat.children.toMutableList()
                newChildren[childIndex] = edited
                items[repeatIndex] = repeat.copy(children = newChildren)
                dirty = true
                childEdit = null
            },
            onDismiss = { childEdit = null },
        )
    }
    if (repeatBoundIndex in items.indices) {
        RepeatBoundSheet(
            initial = items[repeatBoundIndex] as Repeat,
            onDone = { items[repeatBoundIndex] = it; dirty = true; repeatBoundIndex = -1 },
            onDismiss = { repeatBoundIndex = -1 },
        )
    }
    if (showPresets) {
        PresetSheet(
            onGenerate = { preset -> items.clear(); items.addAll(preset.items); goal = preset.goalDistanceM?.toString() ?: goal; dirty = true; showPresets = false },
            onDismiss = { showPresets = false },
        )
    }
    infoTopic?.let { topic -> InfoSheet(topic = topic, onDismiss = { infoTopic = null }) }

    if (showDiscard) {
        RcDialog(
            onDismiss = { showDiscard = false },
            title = stringResource(R.string.editor_discard_title),
            content = { Text(stringResource(R.string.editor_discard_message), style = type.body, color = colors.textSecondary) },
            dismiss = { RcButton(stringResource(R.string.editor_discard_keep), onClick = { showDiscard = false }, variant = RcButtonVariant.Tertiary) },
            confirm = { RcButton(stringResource(R.string.editor_discard_confirm), onClick = { showDiscard = false; onCancel() }, variant = RcButtonVariant.DestructiveFilled) },
        )
    }
}

@Composable
private fun SegmentCard(
    segment: Segment,
    onEdit: () -> Unit,
    onInfo: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalRcColors.current
    val spacing = LocalRcSpacing.current
    val type = LocalRcTypography.current
    var menu by remember { mutableStateOf(false) }
    RcCard(interactive = true, onClick = onEdit) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.s3)) {
            Box(Modifier.width(4.dp).height(40.dp).background(segmentKindColor(segment.kind), RoundedCornerShape(2.dp)))
            Icon(painterResource(segmentKindIcon(segment.kind)), contentDescription = null, tint = segmentKindColor(segment.kind), modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(segment.label.ifBlank { kindWord(segment.kind) }, style = type.titleS, color = colors.textPrimary)
                Text(describe(segment), style = type.bodySmall, color = colors.textSecondary)
            }
            InfoIcon(topic = segment.label, onClick = onInfo)
            Box {
                RcIconButton(RcIcons.MoreVert, stringResource(R.string.editor_item_menu), onClick = { menu = true })
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = colors.surfaceRaised) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.editor_up)) }, onClick = { menu = false; onUp() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.editor_down)) }, onClick = { menu = false; onDown() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.editor_duplicate)) }, onClick = { menu = false; onDuplicate() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.editor_delete)) }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun RepeatBlock(
    repeat: Repeat,
    onEditBound: () -> Unit,
    onInfo: () -> Unit,
    onAddChild: () -> Unit,
    onDelete: () -> Unit,
    onEditChild: (Int) -> Unit,
    onDeleteChild: (Int) -> Unit,
) {
    val colors = LocalRcColors.current
    val spacing = LocalRcSpacing.current
    val type = LocalRcTypography.current
    var menu by remember { mutableStateOf(false) }
    Row {
        Box(Modifier.width(3.dp).height(if (repeat.children.isEmpty()) 56.dp else (56 * repeat.children.size).dp).background(colors.accent, RoundedCornerShape(2.dp)))
        Column(modifier = Modifier.weight(1f).padding(start = spacing.s2), verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                Icon(painterResource(RcIcons.Repeat), contentDescription = null, tint = colors.accentText, modifier = Modifier.size(20.dp))
                Text(
                    repeat.times?.let { stringResource(R.string.editor_repeat_times, it) } ?: stringResource(R.string.editor_repeat_until, repeat.untilTotalDistanceM ?: 0),
                    style = type.titleS,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f).clickable(onClick = onEditBound),
                )
                InfoIcon(topic = "repeat", onClick = onInfo)
                Box {
                    RcIconButton(RcIcons.MoreVert, stringResource(R.string.editor_item_menu), onClick = { menu = true })
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = colors.surfaceRaised) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.editor_repeat_bound)) }, onClick = { menu = false; onEditBound() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.editor_delete)) }, onClick = { menu = false; onDelete() })
                    }
                }
            }
            repeat.children.forEachIndexed { childIndex, child ->
                Row(modifier = Modifier.fillMaxWidth().clickable { onEditChild(childIndex) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                    Icon(painterResource(segmentKindIcon(child.kind)), contentDescription = null, tint = segmentKindColor(child.kind), modifier = Modifier.size(20.dp))
                    Text(describe(child), style = type.body, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    RcIconButton(RcIcons.Delete, stringResource(R.string.editor_delete), onClick = { onDeleteChild(childIndex) }, tint = colors.fail, iconSize = 20)
                }
            }
            RcButton(stringResource(R.string.editor_add_child), onClick = onAddChild, variant = RcButtonVariant.Tertiary, icon = RcIcons.Add)
        }
    }
}

@Composable
private fun SegmentSheet(
    initial: Segment,
    allowOpen: Boolean,
    onInfo: (InfoTopic) -> Unit,
    onDone: (Segment) -> Unit,
    onDismiss: () -> Unit,
) {
    var label by remember { mutableStateOf(initial.label) }
    var kind by remember { mutableStateOf(initial.kind) }
    var limitType by remember { mutableIntStateOf(when (initial.limit) { is Limit.Distance -> 0; is Limit.Duration -> 1; Limit.Open -> 2 }) }
    var distance by remember { mutableStateOf((initial.limit as? Limit.Distance)?.meters?.toString() ?: "1000") }
    var duration by remember { mutableStateOf((initial.limit as? Limit.Duration)?.let { formatPace(it.seconds) } ?: "6:00") }
    var target by remember { mutableStateOf(initial.targetPaceSecPerKm?.let { formatPace(it) } ?: "") }
    var bound by remember { mutableStateOf(initial.targetBound) }
    var margin by remember { mutableStateOf(initial.fastMarginSec?.toString() ?: "") }
    var perKm by remember { mutableStateOf(initial.perKmCheck) }
    val colors = LocalRcColors.current
    val spacing = LocalRcSpacing.current
    val type = LocalRcTypography.current
    val fallbackLabel = kindWord(kind)

    RcSheet(onDismiss = onDismiss) {
        Text(stringResource(R.string.d01_title), style = type.titleM, color = colors.textPrimary)
        RcTextField(value = label, onValueChange = { label = it }, label = stringResource(R.string.d01_label), modifier = Modifier.padding(top = spacing.s2))

        Text(stringResource(R.string.d01_kind), style = type.label, color = colors.textSecondary, modifier = Modifier.padding(top = spacing.s2))
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = spacing.s2), horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
            SegmentKind.entries.forEach { k ->
                val active = k == kind
                Column(
                    modifier = Modifier.weight(1f).background(if (active) colors.accentContainer else colors.surfaceRaised, RoundedCornerShape(spacing.s3)).clickable { kind = k; bound = defaultBoundFor(k) }.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(painterResource(segmentKindIcon(k)), contentDescription = null, tint = segmentKindColor(k), modifier = Modifier.size(24.dp))
                    Text(kindWord(k), style = type.caption, color = colors.textPrimary)
                }
            }
        }

        Text(stringResource(R.string.d01_limit), style = type.label, color = colors.textSecondary)
        SegmentedControl(
            options = buildList {
                add(0 to stringResource(R.string.d01_distance_m))
                add(1 to stringResource(R.string.d01_duration_ms))
                if (allowOpen) add(2 to stringResource(R.string.d01_open))
            },
            selected = limitType,
            onSelect = { limitType = it },
        )
        when (limitType) {
            0 -> RcTextField(value = distance, onValueChange = { distance = it.filter(Char::isDigit) }, label = stringResource(R.string.d01_distance_m), modifier = Modifier.padding(top = spacing.s2))
            1 -> RcTextField(value = duration, onValueChange = { duration = it }, label = stringResource(R.string.d01_duration_ms), modifier = Modifier.padding(top = spacing.s2))
            else -> Text(stringResource(R.string.d01_open), style = type.body, color = colors.textSecondary, modifier = Modifier.padding(top = spacing.s2))
        }

        RcTextField(value = target, onValueChange = { target = it }, label = stringResource(R.string.d01_target), modifier = Modifier.padding(top = spacing.s2))
        Text(stringResource(R.string.d01_target_bound), style = type.label, color = colors.textSecondary, modifier = Modifier.padding(top = spacing.s2))
        SegmentedControl(
            options = listOf(
                com.daniel.runcoach.engine.model.PaceTargetBound.MAX to stringResource(R.string.d01_bound_max),
                com.daniel.runcoach.engine.model.PaceTargetBound.MIN to stringResource(R.string.d01_bound_min),
            ),
            selected = bound,
            onSelect = { bound = it },
        )
        RcTextField(value = margin, onValueChange = { margin = it.filter(Char::isDigit) }, label = stringResource(R.string.d01_fast_margin), modifier = Modifier.padding(top = spacing.s2))
        RcSwitchRow(stringResource(R.string.d01_per_km), perKm, onCheckedChange = { perKm = it })

        RcButton(
            text = stringResource(R.string.d01_done),
            onClick = {
                val limit = when (limitType) {
                    0 -> Limit.Distance(distance.toIntOrNull() ?: 0)
                    1 -> Limit.Duration(parsePace(duration) ?: 0)
                    else -> Limit.Open
                }
                val fallback = fallbackLabel
                onDone(
                    Segment(
                        label = label.ifBlank { fallback },
                        kind = kind,
                        limit = limit,
                        targetPaceSecPerKm = target.takeIf { it.isNotBlank() }?.let { parsePace(it) },
                        fastMarginSec = if (margin.isBlank()) null else margin.toIntOrNull(),
                        perKmCheck = perKm,
                        targetBound = bound,
                    ),
                )
            },
            fillWidth = true,
            modifier = Modifier.padding(top = spacing.s3),
        )
    }
}

@Composable
private fun RepeatBoundSheet(initial: Repeat, onDone: (Repeat) -> Unit, onDismiss: () -> Unit) {
    var times by remember { mutableStateOf(initial.times?.toString() ?: "3") }
    var until by remember { mutableStateOf(initial.untilTotalDistanceM?.toString() ?: "") }
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    RcSheet(onDismiss = onDismiss) {
        Text(stringResource(R.string.info_repeat_title), style = type.titleM, color = colors.textPrimary)
        RcTextField(value = times, onValueChange = { times = it.filter(Char::isDigit) }, label = stringResource(R.string.editor_repeat_times_label), modifier = Modifier.padding(top = 12.dp))
        RcTextField(value = until, onValueChange = { until = it.filter(Char::isDigit) }, label = stringResource(R.string.editor_repeat_until_label), modifier = Modifier.padding(top = 8.dp))
        RcButton(stringResource(R.string.d01_done), onClick = {
            val t = times.toIntOrNull()
            val u = until.toIntOrNull()
            onDone(if (u != null && u > 0) initial.copy(times = null, untilTotalDistanceM = u) else initial.copy(times = t ?: 1, untilTotalDistanceM = null))
        }, fillWidth = true, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun PresetSheet(onGenerate: (Workout) -> Unit, onDismiss: () -> Unit) {
    var distance by remember { mutableStateOf("5000") }
    var pace by remember { mutableStateOf("7:00") }
    var km by remember { mutableStateOf("5") }
    var step by remember { mutableStateOf("20") }
    var reps by remember { mutableStateOf("3") }
    var runS by remember { mutableStateOf("360") }
    var walkS by remember { mutableStateOf("120") }
    var goal by remember { mutableStateOf("5000") }
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    RcSheet(onDismiss = onDismiss) {
        Text(stringResource(R.string.d02_title), style = type.titleM, color = colors.textPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
            RcTextField(value = distance, onValueChange = { distance = it.filter(Char::isDigit) }, label = stringResource(R.string.d02_distance_m), modifier = Modifier.weight(1f))
            RcTextField(value = pace, onValueChange = { pace = it }, label = stringResource(R.string.d02_start_pace), modifier = Modifier.weight(1f))
        }
        RcButton(stringResource(R.string.d02_stable), onClick = { onGenerate(WorkoutPresets.stableRun(distance.toIntOrNull() ?: 5000, parsePace(pace) ?: 420)) }, variant = RcButtonVariant.Secondary, fillWidth = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RcTextField(value = km, onValueChange = { km = it.filter(Char::isDigit) }, label = stringResource(R.string.d02_km), modifier = Modifier.weight(1f))
            RcTextField(value = step, onValueChange = { step = it.filter(Char::isDigit) }, label = stringResource(R.string.d02_step), modifier = Modifier.weight(1f))
        }
        RcButton(stringResource(R.string.d02_tempo), onClick = { onGenerate(WorkoutPresets.progressiveTempo(km.toIntOrNull() ?: 5, parsePace(pace) ?: 450, step.toIntOrNull() ?: 20, warmUpMeters = 500, coolDownMeters = 500)) }, variant = RcButtonVariant.Secondary, fillWidth = true)
        RcTextField(value = reps, onValueChange = { reps = it.filter(Char::isDigit) }, label = stringResource(R.string.d02_reps), modifier = Modifier.fillMaxWidth())
        RcButton(
            stringResource(R.string.d02_interval),
            onClick = {
                onGenerate(WorkoutPresets.interval(warmUpMeters = 1000, work = Limit.Distance(400), workTargetSecPerKm = parsePace(pace) ?: 360, recovery = Limit.Distance(200), recoveryTargetSecPerKm = null, repetitions = reps.toIntOrNull() ?: 3, coolDownMeters = 1000))
            },
            variant = RcButtonVariant.Secondary,
            fillWidth = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RcTextField(value = runS, onValueChange = { runS = it.filter(Char::isDigit) }, label = stringResource(R.string.d02_run_s), modifier = Modifier.weight(1f))
            RcTextField(value = walkS, onValueChange = { walkS = it.filter(Char::isDigit) }, label = stringResource(R.string.d02_walk_s), modifier = Modifier.weight(1f))
        }
        RcTextField(value = goal, onValueChange = { goal = it.filter(Char::isDigit) }, label = stringResource(R.string.d02_goal_m), modifier = Modifier.fillMaxWidth())
        RcButton(stringResource(R.string.d02_runwalk), onClick = { onGenerate(WorkoutPresets.runWalk(runS.toIntOrNull() ?: 360, walkS.toIntOrNull() ?: 120, goal.toIntOrNull() ?: 5000)) }, variant = RcButtonVariant.Secondary, fillWidth = true)
    }
}

@Composable
private fun InfoSheet(topic: InfoTopic, onDismiss: () -> Unit) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    RcSheet(onDismiss = onDismiss) {
        Text(stringResource(topic.titleRes), style = type.titleM, color = colors.textPrimary)
        Text(stringResource(topic.bodyRes), style = type.body, color = colors.textSecondary, modifier = Modifier.padding(top = 12.dp))
        RcButton(stringResource(R.string.info_understand), onClick = onDismiss, fillWidth = true, modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun describe(segment: Segment): String {
    val limit = when (val l = segment.limit) {
        is Limit.Distance -> "${l.meters} m"
        is Limit.Duration -> Formatters.duration(l.seconds * 1000L)
        Limit.Open -> stringResource(R.string.d01_open)
    }
    val target = segment.targetPaceSecPerKm?.let { ", ${Formatters.targetPace(it, segment.targetBound)}" } ?: ""
    return "$limit$target"
}

@Composable
private fun estimateText(value: String, atLeast: Boolean): String =
    if (atLeast) stringResource(R.string.editor_at_least, value) else value

@Composable
private fun kindWord(kind: SegmentKind): String = stringResource(
    when (kind) {
        SegmentKind.WARMUP -> R.string.kind_warmup
        SegmentKind.RUN -> R.string.kind_run
        SegmentKind.WALK -> R.string.kind_walk
        SegmentKind.RECOVERY -> R.string.kind_recovery
        SegmentKind.COOLDOWN -> R.string.kind_cooldown
    },
)

private fun defaultSegment() = Segment("Run", SegmentKind.RUN, Limit.Distance(1000), targetPaceSecPerKm = null)
private fun defaultChild() = Segment("Recovery", SegmentKind.RECOVERY, Limit.Distance(400), targetPaceSecPerKm = null)
private fun defaultRepeat() = Repeat(times = 3, children = listOf(defaultSegment(), defaultChild()))

/** Warm-up/cool-down/recovery default to a floor target (don't go faster); run/walk to a ceiling. */
private fun defaultBoundFor(kind: SegmentKind) = when (kind) {
    SegmentKind.WARMUP, SegmentKind.COOLDOWN, SegmentKind.RECOVERY -> com.daniel.runcoach.engine.model.PaceTargetBound.MIN
    SegmentKind.RUN, SegmentKind.WALK -> com.daniel.runcoach.engine.model.PaceTargetBound.MAX
}
