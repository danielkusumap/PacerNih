package com.daniel.runcoach.ui.finish

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.daniel.runcoach.R
import com.daniel.runcoach.data.BestEffortEntity
import com.daniel.runcoach.data.SessionEntity
import com.daniel.runcoach.data.SessionRepository
import com.daniel.runcoach.engine.voice.VoiceLanguage
import com.daniel.runcoach.service.FinishedSessionHolder
import com.daniel.runcoach.ui.Formatters
import com.daniel.runcoach.ui.components.Banner
import com.daniel.runcoach.ui.components.MetricBlock
import com.daniel.runcoach.ui.components.RcBannerVariant
import com.daniel.runcoach.ui.components.RcButton
import com.daniel.runcoach.ui.components.RcButtonVariant
import com.daniel.runcoach.ui.components.RcStatus
import com.daniel.runcoach.ui.components.RcTextField
import com.daniel.runcoach.ui.components.ResultStrip
import com.daniel.runcoach.ui.components.SectionHeader
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.LocalRcSpacing
import com.daniel.runcoach.ui.theme.LocalRcTypography
import com.daniel.runcoach.ui.theme.RcIcons
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** S06 Finish & save (DESIGN.md section 8): calm completion, distance hero, optional notes, sticky save. */
@Composable
fun FinishedScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { SessionRepository(context) }
    var session by remember { mutableStateOf<SessionEntity?>(null) }
    var records by remember { mutableStateOf<List<BestEffortEntity>>(emptyList()) }
    var name by remember { mutableStateOf("") }
    var rpe by remember { mutableStateOf<Int?>(null) }
    var note by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val id = FinishedSessionHolder.sessionId
        if (id != null) {
            var loaded = repository.get(id)
            var tries = 0
            while (loaded == null && tries < 25) {
                delay(200)
                loaded = repository.get(id)
                tries++
            }
            session = loaded
            if (loaded != null) records = repository.newRecords(id)
        }
    }

    val colors = LocalRcColors.current
    val spacing = LocalRcSpacing.current
    val type = LocalRcTypography.current
    val current = session
    val noteTooLong = note.length > SessionRepository.MAX_NOTE

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = spacing.screenH),
            verticalArrangement = Arrangement.spacedBy(spacing.cardGap),
        ) {
            Row(modifier = Modifier.padding(top = spacing.s4), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                Box(modifier = Modifier.size(48.dp).background(colors.accentContainer, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(painterResource(RcIcons.Check), contentDescription = null, tint = colors.accentText, modifier = Modifier.size(24.dp))
                }
                Text(stringResource(R.string.s06_saved), style = type.bodySmall, color = colors.textSecondary)
            }

            if (current == null) {
                Text(stringResource(R.string.s06_loading), color = colors.textSecondary)
                return@Column
            }

            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(Formatters.distanceKm(current.totalDistanceM).removeSuffix(" km"), style = type.displayL, color = colors.textPrimary)
                Text("km", style = type.titleS, color = colors.textSecondary)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.s5)) {
                MetricBlock(stringResource(R.string.s06_metric_time), Formatters.duration(current.movingMs))
                MetricBlock(stringResource(R.string.s06_metric_avg_pace), Formatters.pace(current.avgPaceSecPerKm), unit = "/km")
                MetricBlock(stringResource(R.string.s06_metric_pass), "${current.segPass}/${current.segPass + current.segFail + current.segBorderline}")
            }
            ResultStrip(statuses = buildList {
                repeat(current.segPass) { add(RcStatus.Pass) }
                repeat(current.segBorderline) { add(RcStatus.Warn) }
                repeat(current.segFail) { add(RcStatus.Fail) }
            })
            Text(stringResource(R.string.s06_seg_counts, current.segPass, current.segFail, current.segBorderline), style = type.caption, color = colors.textTertiary)

            records.forEach { record ->
                Banner(
                    text = stringResource(R.string.s06_record_badge, keyLabel(record.distanceKey), Formatters.duration(record.durationMs)),
                    variant = RcBannerVariant.Accent,
                )
            }

            SectionHeader(stringResource(R.string.s06_optional_notes))
            RcTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.s06_name), placeholder = current.name, modifier = Modifier.fillMaxWidth())
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

        // Sticky footer
        Column(modifier = Modifier.fillMaxWidth().background(colors.bg).padding(horizontal = spacing.screenH).padding(vertical = spacing.s3), verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
            RcButton(
                text = stringResource(R.string.s06_save),
                onClick = {
                    val c = session ?: return@RcButton
                    scope.launch {
                        repository.updateForm(c.id, name, note.ifBlank { null }, rpe, false, currentLanguage(), System.currentTimeMillis())
                        FinishedSessionHolder.sessionId = null
                        onDone()
                    }
                },
                enabled = !noteTooLong,
                fillWidth = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                RcButton(stringResource(R.string.s06_skip), onClick = { FinishedSessionHolder.sessionId = null; onDone() }, variant = RcButtonVariant.Tertiary, modifier = Modifier.weight(1f))
                RcButton(
                    stringResource(R.string.s06_discard),
                    onClick = { scope.launch { session?.let { repository.delete(it.id) }; FinishedSessionHolder.sessionId = null; onDone() } },
                    variant = RcButtonVariant.Destructive,
                    modifier = Modifier.weight(1f),
                )
            }
        }
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
                    .background(if (active) colors.accent else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                    .border(1.dp, if (active) colors.accent else colors.outlineStrong, CircleShape)
                    .clickable { onSelect(if (active) null else value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(value.toString(), style = type.caption, color = if (active) colors.onAccent else colors.textSecondary)
            }
        }
    }
}

private fun currentLanguage(): VoiceLanguage {
    val language = Locale.getDefault().language
    return if (language.startsWith("in") || language == "id") VoiceLanguage.ID else VoiceLanguage.EN
}

private fun keyLabel(key: String): String = when (key) {
    "K1" -> "1 km"
    "K5" -> "5 km"
    "K10" -> "10 km"
    "HM" -> "Half"
    "FM" -> "Marathon"
    else -> key
}
