package com.daniel.runcoach.ui.editor

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
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

/**
 * S03 Editor Workout (plan section 8): name, optional goal, ordered items (segments and repeats), live
 * validation, summary estimate (F24), presets (D02), and the info sheet (D11). Saving goes through the
 * template repository.
 */
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

    val workout = Workout(
        id = initial?.id ?: "",
        name = name,
        items = items.toList(),
        goalDistanceM = goal.toIntOrNull(),
    )
    val errors = WorkoutValidator.validate(workout)
    val estimate = WorkoutEstimator.estimate(workout)
    val expandedCount = runCatching { WorkoutExpander.expandFinite(items).size }.getOrElse {
        items.sumOf { block -> when (block) { is Segment -> 1; is Repeat -> (block.times ?: 1) * block.children.size } }
    }

    BackHandler { if (dirty) showDiscard = true else onCancel() }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.editor_title), style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(value = name, onValueChange = { name = it; dirty = true }, label = { Text(stringResource(R.string.editor_name)) }, modifier = Modifier.fillMaxWidth().testTag("field_name"))
            OutlinedTextField(
                value = goal,
                onValueChange = { goal = it.filter(Char::isDigit); dirty = true },
                label = { Text(stringResource(R.string.editor_goal)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            items.forEachIndexed { index, block ->
                when (block) {
                    is Segment -> SegmentRow(
                        segment = block,
                        isLast = index == items.lastIndex,
                        onEdit = { segmentEditIndex = index },
                        onInfo = { infoTopic = kindTopic(block.kind) },
                        onUp = { if (index > 0) { val t = items[index]; items[index] = items[index - 1]; items[index - 1] = t; dirty = true } },
                        onDown = { if (index < items.lastIndex) { val t = items[index]; items[index] = items[index + 1]; items[index + 1] = t; dirty = true } },
                        onDuplicate = { items.add(index + 1, block.copy()); dirty = true },
                        onDelete = { items.removeAt(index); dirty = true },
                    )

                    is Repeat -> {
                        RepeatHeader(
                            repeat = block,
                            onEditBound = { repeatBoundIndex = index },
                            onInfo = { infoTopic = InfoTopic.REPEAT },
                            onAddChild = { items[index] = block.copy(children = block.children + defaultChild()); dirty = true },
                            onDelete = { items.removeAt(index); dirty = true },
                        )
                        block.children.forEachIndexed { childIndex, child ->
                            Text(
                                "   ↳ ${describe(child)}",
                                modifier = Modifier.padding(start = 16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { childEdit = index to childIndex }) { Text(stringResource(R.string.editor_up)) }
                                TextButton(onClick = {
                                    items[index] = block.copy(children = block.children.filterIndexed { i, _ -> i != childIndex })
                                    dirty = true
                                }) { Text(stringResource(R.string.editor_delete)) }
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { items.add(defaultSegment()); dirty = true }, modifier = Modifier.testTag("btn_add_segment")) { Text(stringResource(R.string.editor_add_segment)) }
                OutlinedButton(onClick = { items.add(defaultRepeat()); dirty = true }) { Text(stringResource(R.string.editor_add_repeat)) }
            }
            OutlinedButton(onClick = { showPresets = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.editor_preset)) }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.editor_summary), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.editor_seg_count, expandedCount))
                    estimate.distanceM?.let {
                        Text(stringResource(R.string.editor_distance) + ": " + estimateText(Formatters.distanceKm(it), estimate.distanceIsAtLeast))
                    }
                    estimate.durationSec?.let {
                        Text(stringResource(R.string.editor_duration) + ": " + estimateText(Formatters.duration((it * 1000).toLong()), estimate.durationIsAtLeast))
                    }
                }
            }

            if (errors.isNotEmpty()) {
                Text(
                    stringResource(R.string.editor_invalid, errors.size),
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.editor_discard_confirm)) }
                Button(
                    onClick = {
                        scope.launch {
                            repository.create(workout)
                            onSaved()
                        }
                    },
                    enabled = editorCanSave(name, errors.size),
                    modifier = Modifier.weight(1f).testTag("btn_save"),
                ) { Text(stringResource(R.string.editor_save)) }
            }
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
        RepeatBoundDialog(
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
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text(stringResource(R.string.editor_discard_title)) },
            text = { Text(stringResource(R.string.editor_discard_message)) },
            confirmButton = { TextButton(onClick = { showDiscard = false; onCancel() }) { Text(stringResource(R.string.editor_discard_confirm)) } },
            dismissButton = { TextButton(onClick = { showDiscard = false }) { Text(stringResource(R.string.editor_discard_keep)) } },
        )
    }
}

@Composable
private fun SegmentRow(
    segment: Segment,
    isLast: Boolean,
    onEdit: () -> Unit,
    onInfo: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(describe(segment), style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onEdit) { Text(stringResource(R.string.d01_kind)) }
                TextButton(onClick = onInfo) { Text("ⓘ") }
                TextButton(onClick = onUp) { Text(stringResource(R.string.editor_up)) }
                TextButton(onClick = onDown) { Text(stringResource(R.string.editor_down)) }
                TextButton(onClick = onDuplicate) { Text(stringResource(R.string.editor_duplicate)) }
                TextButton(onClick = onDelete) { Text(stringResource(R.string.editor_delete)) }
            }
        }
    }
}

@Composable
private fun RepeatHeader(repeat: Repeat, onEditBound: () -> Unit, onInfo: () -> Unit, onAddChild: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.editor_add_repeat), modifier = Modifier.padding(end = 8.dp))
            TextButton(onClick = onEditBound) { Text(stringResource(R.string.editor_up)) }
            TextButton(onClick = onInfo) { Text("ⓘ") }
            TextButton(onClick = onAddChild) { Text("+") }
            TextButton(onClick = onDelete) { Text(stringResource(R.string.editor_delete)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var limitType by remember { mutableStateOf(when (initial.limit) { is Limit.Distance -> 0; is Limit.Duration -> 1; Limit.Open -> 2 }) }
    var distance by remember { mutableStateOf((initial.limit as? Limit.Distance)?.meters?.toString() ?: "1000") }
    var duration by remember { mutableStateOf((initial.limit as? Limit.Duration)?.let { formatPace(it.seconds) } ?: "6:00") }
    var target by remember { mutableStateOf(initial.targetPaceSecPerKm?.let { formatPace(it) } ?: "") }
    var margin by remember { mutableStateOf(initial.fastMarginSec?.toString() ?: "") }
    var perKm by remember { mutableStateOf(initial.perKmCheck) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.d01_title), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(label = { Text(stringResource(R.string.d01_label)) }, value = label, onValueChange = { label = it }, modifier = Modifier.fillMaxWidth())

            Text(stringResource(R.string.d01_kind), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SegmentKind.entries.forEach { k ->
                    TextButton(onClick = { kind = k }) { Text(kindWord(k) + if (k == kind) " ✓" else "") }
                }
            }
            TextButton(onClick = { onInfo(kindTopic(kind)) }) { Text("ⓘ " + stringResource(R.string.info_open)) }

            Text(stringResource(R.string.d01_limit), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = { limitType = 0 }) { Text(stringResource(R.string.d01_distance_m)) }
                TextButton(onClick = { limitType = 1 }) { Text(stringResource(R.string.d01_duration_ms)) }
                if (allowOpen) TextButton(onClick = { limitType = 2 }) { Text(stringResource(R.string.d01_open)) }
            }
            when (limitType) {
                0 -> OutlinedTextField(label = { Text(stringResource(R.string.d01_distance_m)) }, value = distance, onValueChange = { distance = it.filter(Char::isDigit) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                1 -> OutlinedTextField(label = { Text(stringResource(R.string.d01_duration_ms)) }, value = duration, onValueChange = { duration = it }, modifier = Modifier.fillMaxWidth())
                else -> Text(stringResource(R.string.d01_open))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(label = { Text(stringResource(R.string.d01_target)) }, value = target, onValueChange = { target = it }, modifier = Modifier.weight(1f))
                TextButton(onClick = { onInfo(InfoTopic.TARGET) }) { Text("ⓘ") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(label = { Text(stringResource(R.string.d01_fast_margin)) }, value = margin, onValueChange = { margin = it.filter(Char::isDigit) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                TextButton(onClick = { onInfo(InfoTopic.MARGIN) }) { Text("ⓘ") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { perKm = !perKm }) { Text(stringResource(R.string.d01_per_km) + if (perKm) " ✓" else "") }
                TextButton(onClick = { onInfo(InfoTopic.PER_KM) }) { Text("ⓘ") }
            }

            val fallbackLabel = kindWord(kind)
            Button(
                onClick = {
                    val limit = when (limitType) {
                        0 -> Limit.Distance(distance.toIntOrNull() ?: 0)
                        1 -> Limit.Duration(parsePace(duration) ?: 0)
                        else -> Limit.Open
                    }
                    val marginValue = margin.toIntOrNull()
                    onDone(
                        Segment(
                            label = label.ifBlank { fallbackLabel },
                            kind = kind,
                            limit = limit,
                            targetPaceSecPerKm = target.takeIf { it.isNotBlank() }?.let { parsePace(it) },
                            fastMarginSec = if (margin.isBlank()) null else marginValue,
                            perKmCheck = perKm,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.d01_done)) }
        }
    }
}

@Composable
private fun RepeatBoundDialog(initial: Repeat, onDone: (Repeat) -> Unit, onDismiss: () -> Unit) {
    var times by remember { mutableStateOf(initial.times?.toString() ?: "3") }
    var until by remember { mutableStateOf(initial.untilTotalDistanceM?.toString() ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.info_repeat_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(label = { Text("Ulangan (times)") }, value = times, onValueChange = { times = it.filter(Char::isDigit) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(label = { Text("Sampai jarak (m)") }, value = until, onValueChange = { until = it.filter(Char::isDigit) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val t = times.toIntOrNull()
                val u = until.toIntOrNull()
                val updated = when {
                    u != null && u > 0 -> initial.copy(times = null, untilTotalDistanceM = u)
                    else -> initial.copy(times = t ?: 1, untilTotalDistanceM = null)
                }
                onDone(updated)
            }) { Text(stringResource(R.string.d01_done)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.editor_discard_confirm)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.d02_title), style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(label = { Text(stringResource(R.string.d02_distance_m)) }, value = distance, onValueChange = { distance = it.filter(Char::isDigit) }, modifier = Modifier.weight(1f))
                OutlinedTextField(label = { Text(stringResource(R.string.d02_start_pace)) }, value = pace, onValueChange = { pace = it }, modifier = Modifier.weight(1f))
            }
            Button(onClick = { onGenerate(WorkoutPresets.stableRun(distance.toIntOrNull() ?: 5000, parsePace(pace) ?: 420)) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.d02_stable)) }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(label = { Text(stringResource(R.string.d02_km)) }, value = km, onValueChange = { km = it.filter(Char::isDigit) }, modifier = Modifier.weight(1f))
                OutlinedTextField(label = { Text(stringResource(R.string.d02_step)) }, value = step, onValueChange = { step = it.filter(Char::isDigit) }, modifier = Modifier.weight(1f))
            }
            Button(onClick = { onGenerate(WorkoutPresets.progressiveTempo(km.toIntOrNull() ?: 5, parsePace(pace) ?: 450, step.toIntOrNull() ?: 20, warmUpMeters = 500, coolDownMeters = 500)) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.d02_tempo)) }

            OutlinedTextField(label = { Text(stringResource(R.string.d02_reps)) }, value = reps, onValueChange = { reps = it.filter(Char::isDigit) }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                onGenerate(
                    WorkoutPresets.interval(
                        warmUpMeters = 1000,
                        work = Limit.Distance(400),
                        workTargetSecPerKm = parsePace(pace) ?: 360,
                        recovery = Limit.Distance(200),
                        recoveryTargetSecPerKm = null,
                        repetitions = reps.toIntOrNull() ?: 3,
                        coolDownMeters = 1000,
                    ),
                )
            }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.d02_interval)) }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(label = { Text(stringResource(R.string.d02_run_s)) }, value = runS, onValueChange = { runS = it.filter(Char::isDigit) }, modifier = Modifier.weight(1f))
                OutlinedTextField(label = { Text(stringResource(R.string.d02_walk_s)) }, value = walkS, onValueChange = { walkS = it.filter(Char::isDigit) }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(label = { Text(stringResource(R.string.d02_goal_m)) }, value = goal, onValueChange = { goal = it.filter(Char::isDigit) }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { onGenerate(WorkoutPresets.runWalk(runS.toIntOrNull() ?: 360, walkS.toIntOrNull() ?: 120, goal.toIntOrNull() ?: 5000)) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.d02_runwalk)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InfoSheet(topic: InfoTopic, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(topic.titleRes), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(topic.bodyRes), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun describe(segment: Segment): String {
    val limit = when (val l = segment.limit) {
        is Limit.Distance -> "${l.meters} m"
        is Limit.Duration -> Formatters.duration(l.seconds * 1000L)
        Limit.Open -> stringResource(R.string.d01_open)
    }
    val target = segment.targetPaceSecPerKm?.let { ", ≤ ${Formatters.pace(it.toDouble())}" } ?: ""
    return "${kindWord(segment.kind)} $limit$target"
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
