package com.daniel.runcoach.ui.finish

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.daniel.runcoach.R
import com.daniel.runcoach.data.RecordRepository
import com.daniel.runcoach.data.RunPersistence
import com.daniel.runcoach.data.SessionRepository
import com.daniel.runcoach.engine.result.BestEffort
import com.daniel.runcoach.engine.voice.VoiceLanguage
import com.daniel.runcoach.service.FinishedSessionHolder
import com.daniel.runcoach.service.PendingSession
import com.daniel.runcoach.ui.Formatters
import com.daniel.runcoach.ui.components.Banner
import com.daniel.runcoach.ui.components.MetricBlock
import com.daniel.runcoach.ui.components.RcBannerVariant
import com.daniel.runcoach.ui.components.RcButton
import com.daniel.runcoach.ui.components.RcButtonVariant
import com.daniel.runcoach.ui.components.RcDialog
import com.daniel.runcoach.ui.components.RcStatus
import com.daniel.runcoach.ui.components.RcTextField
import com.daniel.runcoach.ui.components.ResultStrip
import com.daniel.runcoach.ui.components.SectionHeader
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.LocalRcSpacing
import com.daniel.runcoach.ui.theme.LocalRcTypography
import com.daniel.runcoach.ui.theme.RcIcons
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * S06 Finish & save (DESIGN.md section 8). The run is **not** stored yet: this screen edits the name and notes
 * and only writes the session when the user taps Save. Discard asks for confirmation and then drops the run.
 */
@Composable
fun FinishedScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { SessionRepository(context) }
    val recordRepository = remember { RecordRepository(context) }
    val persistence = remember { RunPersistence(context) }
    var pending by remember { mutableStateOf<PendingSession?>(FinishedSessionHolder.pending) }
    var newRecords by remember { mutableStateOf<List<BestEffort>>(emptyList()) }
    var name by remember { mutableStateOf("") }
    var rpe by remember { mutableStateOf<Int?>(null) }
    var note by remember { mutableStateOf("") }
    var showDiscard by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        var p = FinishedSessionHolder.pending
        var tries = 0
        while (p == null && tries < 25) {
            kotlinx.coroutines.delay(200)
            p = FinishedSessionHolder.pending
            tries++
        }
        if (p == null) return@LaunchedEffect
        pending = p
        val current = recordRepository.currentRecords()
        newRecords = p.results.bestEfforts.filter { e ->
            val cur = current[e.key]
            cur == null || e.durationMs < cur.durationMs
        }
    }

    val colors = LocalRcColors.current
    val spacing = LocalRcSpacing.current
    val type = LocalRcTypography.current
    val current = pending
    val noteTooLong = note.length > SessionRepository.MAX_NOTE

    fun save() {
        val p = current ?: return
        scope.launch {
            repository.saveSession(
                p.entity.copy(
                    name = name.ifBlank { p.entity.name },
                    nameIsCustom = name.isNotBlank(),
                    note = note.ifBlank { null },
                    rpe = rpe,
                    updatedAt = System.currentTimeMillis(),
                ),
                p.results,
            )
            FinishedSessionHolder.clear()
            onDone()
        }
    }

    fun discard() {
        val p = current ?: return
        scope.launch {
            runCatching { persistence.clear(p.entity.id) }
            FinishedSessionHolder.clear()
            onDone()
        }
    }

    BackHandler { showDiscard = true }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = spacing.screenH),
            verticalArrangement = Arrangement.spacedBy(spacing.cardGap),
        ) {
            Row(modifier = Modifier.padding(top = spacing.s4), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                Box(modifier = Modifier.size(48.dp).background(colors.accentContainer, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(painterResource(RcIcons.Check), contentDescription = null, tint = colors.accentText, modifier = Modifier.size(24.dp))
                }
                Text(stringResource(R.string.s06_saved_pending), style = type.bodySmall, color = colors.textSecondary)
            }

            if (current == null) {
                Text(stringResource(R.string.s06_nothing), color = colors.textSecondary)
                return@Column
            }

            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(Formatters.distanceKm(current.entity.totalDistanceM).removeSuffix(" km"), style = type.displayL, color = colors.textPrimary)
                Text("km", style = type.titleS, color = colors.textSecondary)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.s5)) {
                MetricBlock(stringResource(R.string.s06_metric_time), Formatters.duration(current.entity.movingMs))
                MetricBlock(stringResource(R.string.s06_metric_avg_pace), Formatters.pace(current.entity.avgPaceSecPerKm), unit = "/km")
                MetricBlock(
                    stringResource(R.string.s06_metric_pass),
                    "${current.entity.segPass}/${current.entity.segPass + current.entity.segFail + current.entity.segBorderline}",
                )
            }
            ResultStrip(statuses = buildList {
                repeat(current.entity.segPass) { add(RcStatus.Pass) }
                repeat(current.entity.segBorderline) { add(RcStatus.Warn) }
                repeat(current.entity.segFail) { add(RcStatus.Fail) }
            })
            Text(
                stringResource(R.string.s06_seg_counts, current.entity.segPass, current.entity.segFail, current.entity.segBorderline),
                style = type.caption,
                color = colors.textTertiary,
            )

            newRecords.forEach { record ->
                Banner(
                    text = stringResource(R.string.s06_record_badge, keyLabel(record.key), Formatters.duration(record.durationMs)),
                    variant = RcBannerVariant.Accent,
                )
            }

            SectionHeader(stringResource(R.string.s06_optional_notes))
            RcTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.s06_name),
                placeholder = current.entity.name,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.s06_effort), style = type.bodySmall, color = colors.textSecondary)
            EffortRow(rpe) { rpe = it }
            RcTextField(
                value = note,
                onValueChange = { note = it },
                label = stringResource(R.string.s06_note),
                multiline = true,
                isError = noteTooLong,
                errorText = if (noteTooLong) stringResource(R.string.s06_note_too_long) else null,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.s06_note_counter, note.length), style = type.caption, color = colors.textTertiary, modifier = Modifier.align(Alignment.End))
            Box(Modifier.padding(bottom = spacing.s3))
        }

        // Sticky footer: Save (primary) + Discard (destructive).
        Column(modifier = Modifier.fillMaxWidth().background(colors.bg).padding(horizontal = spacing.screenH).padding(vertical = spacing.s3), verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
            RcButton(
                text = stringResource(R.string.s06_save),
                onClick = { save() },
                enabled = !noteTooLong && current != null,
                fillWidth = true,
            )
            RcButton(
                text = stringResource(R.string.s06_discard),
                onClick = { showDiscard = true },
                variant = RcButtonVariant.Destructive,
                fillWidth = true,
            )
        }
    }

    if (showDiscard) {
        RcDialog(
            onDismiss = { showDiscard = false },
            title = stringResource(R.string.s06_discard_title),
            content = { Text(stringResource(R.string.s06_discard_message), style = type.body, color = colors.textSecondary) },
            dismiss = { RcButton(stringResource(R.string.editor_discard_keep), onClick = { showDiscard = false }, variant = RcButtonVariant.Tertiary) },
            confirm = { RcButton(stringResource(R.string.s06_discard_confirm), onClick = { showDiscard = false; discard() }, variant = RcButtonVariant.DestructiveFilled) },
        )
    }
}

@Composable
private fun EffortRow(selected: Int?, onSelect: (Int?) -> Unit) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..10).forEach { value ->
            val active = selected == value
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(if (active) colors.accent else Color.Transparent, CircleShape)
                    .border(1.dp, if (active) colors.accent else colors.outlineStrong, CircleShape)
                    .clickable { onSelect(if (active) null else value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(value.toString(), style = type.caption, color = if (active) colors.onAccent else colors.textSecondary)
            }
        }
    }
}

private fun keyLabel(key: com.daniel.runcoach.engine.result.BestEffortKey): String = when (key) {
    com.daniel.runcoach.engine.result.BestEffortKey.K1 -> "1 km"
    com.daniel.runcoach.engine.result.BestEffortKey.K5 -> "5 km"
    com.daniel.runcoach.engine.result.BestEffortKey.K10 -> "10 km"
    com.daniel.runcoach.engine.result.BestEffortKey.HM -> "Half"
    com.daniel.runcoach.engine.result.BestEffortKey.FM -> "Marathon"
}
