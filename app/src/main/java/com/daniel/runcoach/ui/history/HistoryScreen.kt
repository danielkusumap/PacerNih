package com.daniel.runcoach.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daniel.runcoach.R
import com.daniel.runcoach.data.SessionEntity
import com.daniel.runcoach.data.SessionRepository
import com.daniel.runcoach.engine.voice.VoiceLanguage
import com.daniel.runcoach.ui.Formatters
import com.daniel.runcoach.ui.MainBottomBar
import com.daniel.runcoach.ui.MainTab
import com.daniel.runcoach.ui.components.EmptyState
import com.daniel.runcoach.ui.components.RcButton
import com.daniel.runcoach.ui.components.RcButtonVariant
import com.daniel.runcoach.ui.components.RcCard
import com.daniel.runcoach.ui.components.RcDialog
import com.daniel.runcoach.ui.components.RcIconButton
import com.daniel.runcoach.ui.components.RcStatus
import com.daniel.runcoach.ui.components.RcTextField
import com.daniel.runcoach.ui.components.ResultChip
import com.daniel.runcoach.ui.components.TopBar
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.LocalRcSpacing
import com.daniel.runcoach.ui.theme.LocalRcTypography
import com.daniel.runcoach.ui.theme.RcIcons
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/** S07 History (DESIGN.md section 8): sessions grouped by month, search, edit (D03), delete (D09). */
@Composable
fun HistoryScreen(onOpenSession: (String) -> Unit, onSelectTab: (MainTab) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { SessionRepository(context) }
    val sessions by repository.sessions.collectAsStateWithLifecycle(initialValue = emptyList())
    var query by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<SessionEntity?>(null) }
    var deleting by remember { mutableStateOf<SessionEntity?>(null) }
    val colors = LocalRcColors.current
    val spacing = LocalRcSpacing.current

    val filtered = sessions.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
    val grouped = filtered.groupBy { YearMonth.from(Instant.ofEpochMilli(it.startedAtEpochMs).atZone(ZoneId.of(it.zoneId))) }
        .toSortedMap(compareByDescending { it })

    androidx.compose.material3.Scaffold(bottomBar = { MainBottomBar(MainTab.HISTORY, onSelectTab) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(colors.bg).padding(padding)) {
            TopBar(
                title = stringResource(R.string.s07_title),
                tabPage = true,
                actions = {
                    RcIconButton(RcIcons.Search, stringResource(R.string.s07_search), onClick = { searchOpen = !searchOpen }, tint = colors.textPrimary)
                },
            )
            if (searchOpen) {
                RcTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = stringResource(R.string.s07_search),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.screenH),
                )
            }
            if (sessions.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.s07_empty),
                    message = stringResource(R.string.s07_no_match),
                    icon = RcIcons.DirectionsRun,
                    action = { RcButton(stringResource(R.string.s07_start), onClick = { onSelectTab(MainTab.WORKOUT) }, variant = RcButtonVariant.Secondary) },
                )
            } else if (filtered.isEmpty()) {
                EmptyState(title = stringResource(R.string.s07_no_match), message = "", icon = RcIcons.Search)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = spacing.screenH),
                    verticalArrangement = Arrangement.spacedBy(spacing.cardGap),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(top = spacing.s2, bottom = 24.dp),
                ) {
                    grouped.forEach { (month, monthSessions) ->
                        item { MonthHeader(month, monthSessions) }
                        items(monthSessions, key = { it.id }) { session ->
                            SessionRow(
                                session = session,
                                onOpen = { onOpenSession(session.id) },
                                onEdit = { editing = session },
                                onDelete = { deleting = session },
                            )
                        }
                    }
                }
            }
        }
    }

    editing?.let { session ->
        EditSessionDialog(
            session = session,
            onSave = { name, note, rpe, exclude ->
                scope.launch {
                    repository.updateForm(session.id, name, note, rpe, exclude, currentLanguage(), System.currentTimeMillis())
                    editing = null
                }
            },
            onDismiss = { editing = null },
        )
    }

    deleting?.let { session ->
        RcDialog(
            onDismiss = { deleting = null },
            title = stringResource(R.string.d09_delete_session_title),
            content = { Text(stringResource(R.string.d09_delete_session_message, session.name), style = LocalRcTypography.current.body, color = colors.textSecondary) },
            dismiss = { RcButton(stringResource(R.string.editor_discard_keep), onClick = { deleting = null }, variant = RcButtonVariant.Tertiary) },
            confirm = { RcButton(stringResource(R.string.s07_delete), onClick = { scope.launch { repository.delete(session.id) }; deleting = null }, variant = RcButtonVariant.DestructiveFilled) },
        )
    }
}

@Composable
private fun MonthHeader(month: YearMonth, sessions: List<SessionEntity>) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    val spacing = LocalRcSpacing.current
    val totalKm = sessions.sumOf { it.totalDistanceM / 1000.0 }
    val label = (month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + month.year).uppercase()
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = spacing.s3, bottom = spacing.s1),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = type.label, color = colors.textSecondary)
        Text(stringResource(R.string.s07_month_summary, "%.1f km".format(Locale.getDefault(), totalKm), sessions.size), style = type.bodySmall, color = colors.textSecondary)
    }
}

@Composable
private fun SessionRow(session: SessionEntity, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    val spacing = LocalRcSpacing.current
    var menu by remember { mutableStateOf(false) }
    val targetCount = session.segPass + session.segFail + session.segBorderline
    RcCard(interactive = true, onClick = onOpen) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(session.name, style = type.titleS, color = colors.textPrimary)
                Text(formatDateTime(session.startedAtEpochMs), style = type.bodySmall, color = colors.textSecondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(Formatters.distanceKm(session.totalDistanceM).removeSuffix(" km"), style = type.numberM, color = colors.textPrimary)
                    Text("km", style = type.bodySmall, color = colors.textSecondary)
                }
                Text(Formatters.duration(session.movingMs), style = type.bodySmall, color = colors.textSecondary)
            }
            Box {
                RcIconButton(RcIcons.MoreVert, stringResource(R.string.s07_more), onClick = { menu = true })
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = colors.surfaceRaised) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.s07_edit)) }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.s07_delete)) }, onClick = { menu = false; onDelete() })
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = spacing.s2), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(Formatters.pace(session.avgPaceSecPerKm), style = type.bodySmall, color = colors.textSecondary, modifier = Modifier.weight(1f))
            ResultChip(
                status = if (targetCount == 0) RcStatus.Neutral else if (session.segFail == 0) RcStatus.Pass else RcStatus.Warn,
                label = if (targetCount == 0) stringResource(R.string.s07_chip_no_target) else stringResource(R.string.s07_chip_short, session.segPass, targetCount),
            )
        }
    }
}

@Composable
private fun EditSessionDialog(
    session: SessionEntity,
    onSave: (name: String, note: String?, rpe: Int?, exclude: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    var name by remember { mutableStateOf(if (session.nameIsCustom) session.name else "") }
    var note by remember { mutableStateOf(session.note ?: "") }
    var rpe by remember { mutableStateOf(session.rpe?.toString() ?: "") }
    var exclude by remember { mutableStateOf(session.excludeFromRecords) }
    val noteTooLong = note.length > SessionRepository.MAX_NOTE

    RcDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.d03_title),
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RcTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.d03_name), placeholder = session.name)
                RcTextField(value = rpe, onValueChange = { rpe = it.filter(Char::isDigit).take(2) }, label = stringResource(R.string.d03_rpe))
                RcTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = stringResource(R.string.d03_note),
                    multiline = true,
                    isError = noteTooLong,
                    errorText = if (noteTooLong) stringResource(R.string.s06_note_too_long) else null,
                )
                com.daniel.runcoach.ui.components.RcSwitchRow(stringResource(R.string.d03_exclude), exclude, { exclude = it })
            }
        },
        dismiss = { RcButton(stringResource(R.string.editor_discard_keep), onClick = onDismiss, variant = RcButtonVariant.Tertiary) },
        confirm = {
            RcButton(
                stringResource(R.string.s06_save),
                onClick = { onSave(name, note.ifBlank { null }, rpe.toIntOrNull()?.coerceIn(1, 10), exclude) },
                enabled = !noteTooLong,
            )
        },
    )
}

private fun formatDateTime(epochMs: Long): String =
    SimpleDateFormat("EEE, d MMM · HH:mm", Locale.getDefault()).format(Date(epochMs))

private fun currentLanguage(): VoiceLanguage {
    val language = Locale.getDefault().language
    return if (language.startsWith("in") || language == "id") VoiceLanguage.ID else VoiceLanguage.EN
}
