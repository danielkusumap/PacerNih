package com.daniel.runcoach.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daniel.runcoach.R
import com.daniel.runcoach.data.SessionEntity
import com.daniel.runcoach.data.SessionRepository
import com.daniel.runcoach.engine.voice.VoiceLanguage
import com.daniel.runcoach.ui.Formatters
import com.daniel.runcoach.ui.MainBottomBar
import com.daniel.runcoach.ui.MainTab
import java.time.Instant
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

/** S07 History (plan section 8): sessions grouped by month with search, edit (D03), and delete (D09). */
@Composable
fun HistoryScreen(onOpenSession: (String) -> Unit, onSelectTab: (MainTab) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { SessionRepository(context) }
    val sessions by repository.sessions.collectAsStateWithLifecycle(initialValue = emptyList())
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<SessionEntity?>(null) }
    var deleting by remember { mutableStateOf<SessionEntity?>(null) }

    val filtered = sessions.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
    val grouped = filtered.groupBy { YearMonth.from(Instant.ofEpochMilli(it.startedAtEpochMs).atZone(ZoneId.of(it.zoneId))) }
        .toSortedMap(compareByDescending { it })

    Scaffold(bottomBar = { MainBottomBar(MainTab.HISTORY, onSelectTab) }) { padding ->
        Surface(modifier = Modifier.fillMaxSize().padding(padding), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.s07_title), style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.s07_search)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (filtered.isEmpty()) {
                    Text(stringResource(R.string.s07_empty))
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.d09_delete_session_title)) },
            text = { Text(stringResource(R.string.d09_delete_session_message, session.name)) },
            confirmButton = {
                TextButton(onClick = { scope.launch { repository.delete(session.id) }; deleting = null }) {
                    Text(stringResource(R.string.s07_delete))
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.editor_discard_keep)) } },
        )
    }
}

@Composable
private fun MonthHeader(month: YearMonth, sessions: List<SessionEntity>) {
    val totalKm = sessions.sumOf { it.totalDistanceM / 1000.0 }
    val label = month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + month.year
    Text(
        stringResource(R.string.s07_month_header, label, "%.2f km".format(Locale.getDefault(), totalKm), sessions.size),
        style = MaterialTheme.typography.titleSmall,
    )
}

@Composable
private fun SessionRow(session: SessionEntity, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val targetCount = session.segPass + session.segFail + session.segBorderline
    Card(modifier = Modifier.fillMaxWidth().clickable { onOpen() }) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(session.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${formatDateTime(session.startedAtEpochMs, session.zoneId)} · ${Formatters.distanceKm(session.totalDistanceM)} · " +
                    "${Formatters.duration(session.movingMs)} · ${Formatters.pace(session.avgPaceSecPerKm)}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                if (targetCount == 0) stringResource(R.string.s07_chip_no_target)
                else stringResource(R.string.s07_chip_pass, session.segPass, targetCount),
                style = MaterialTheme.typography.labelMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onEdit) { Text(stringResource(R.string.s07_edit)) }
                TextButton(onClick = onDelete) { Text(stringResource(R.string.s07_delete)) }
            }
        }
    }
}

@Composable
private fun EditSessionDialog(
    session: SessionEntity,
    onSave: (name: String, note: String?, rpe: Int?, exclude: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(if (session.nameIsCustom) session.name else "") }
    var note by remember { mutableStateOf(session.note ?: "") }
    var rpe by remember { mutableStateOf(session.rpe?.toString() ?: "") }
    var exclude by remember { mutableStateOf(session.excludeFromRecords) }
    val noteTooLong = note.length > SessionRepository.MAX_NOTE

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.d03_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.d03_name)) }, placeholder = { Text(session.name) })
                OutlinedTextField(value = rpe, onValueChange = { rpe = it.filter(Char::isDigit).take(2) }, label = { Text(stringResource(R.string.d03_rpe)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(stringResource(R.string.d03_note)) }, isError = noteTooLong)
                if (noteTooLong) Text(stringResource(R.string.s06_note_too_long), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { exclude = !exclude }) {
                    Text(stringResource(R.string.d03_exclude) + if (exclude) " ✓" else "")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, note.ifBlank { null }, rpe.toIntOrNull()?.coerceIn(1, 10), exclude) },
                enabled = !noteTooLong,
            ) { Text(stringResource(R.string.s06_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.editor_discard_keep)) } },
    )
}

private fun formatDateTime(epochMs: Long, zoneId: String): String =
    java.text.SimpleDateFormat("d MMM HH:mm", Locale.getDefault()).format(java.util.Date(epochMs))

private fun currentLanguage(): VoiceLanguage {
    val language = Locale.getDefault().language
    return if (language.startsWith("in") || language == "id") VoiceLanguage.ID else VoiceLanguage.EN
}
