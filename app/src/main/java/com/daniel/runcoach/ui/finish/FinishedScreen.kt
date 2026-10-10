package com.daniel.runcoach.ui.finish

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.daniel.runcoach.R
import com.daniel.runcoach.data.BestEffortEntity
import com.daniel.runcoach.data.SessionEntity
import com.daniel.runcoach.data.SessionRepository
import com.daniel.runcoach.engine.voice.VoiceLanguage
import com.daniel.runcoach.service.FinishedSessionHolder
import com.daniel.runcoach.ui.Formatters
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * S06 Finish & save (plan section 8). The session is already saved when this screen opens; the form only
 * updates name/RPE/note. Empty name keeps the default; a note over 500 chars is rejected.
 */
@Composable
fun FinishedScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { SessionRepository(context) }
    var session by remember { mutableStateOf<SessionEntity?>(null) }
    var records by remember { mutableStateOf<List<BestEffortEntity>>(emptyList()) }
    var name by remember { mutableStateOf("") }
    var rpeText by remember { mutableStateOf("") }
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

    val current = session
    val noteTooLong = note.length > SessionRepository.MAX_NOTE

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.s06_title), style = MaterialTheme.typography.headlineSmall)
            if (current == null) {
                Text(stringResource(R.string.s06_loading))
                return@Column
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.s06_summary), style = MaterialTheme.typography.titleMedium)
                    Text(current.name, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(Formatters.distanceKm(current.totalDistanceM))
                        Text(Formatters.duration(current.movingMs))
                        Text(Formatters.pace(current.avgPaceSecPerKm))
                    }
                    Text(stringResource(R.string.s06_seg_counts, current.segPass, current.segFail, current.segBorderline))
                    records.forEach { record ->
                        Text(
                            stringResource(R.string.s06_record_badge, keyLabel(record.distanceKey), Formatters.duration(record.durationMs)),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.s06_name)) },
                placeholder = { Text(current.name) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = rpeText,
                onValueChange = { value -> rpeText = value.filter(Char::isDigit).take(2) },
                label = { Text(stringResource(R.string.s06_rpe)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.s06_note)) },
                isError = noteTooLong,
                modifier = Modifier.fillMaxWidth(),
            )
            if (noteTooLong) {
                Text(stringResource(R.string.s06_note_too_long), color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    scope.launch {
                        repository.updateForm(
                            id = current.id,
                            name = name,
                            note = note.ifBlank { null },
                            rpe = rpeText.toIntOrNull()?.coerceIn(1, 10),
                            excludeFromRecords = false,
                            language = currentLanguage(),
                            nowMs = System.currentTimeMillis(),
                        )
                        FinishedSessionHolder.sessionId = null
                        onDone()
                    }
                },
                enabled = !noteTooLong,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.s06_save)) }

            OutlinedButton(onClick = { FinishedSessionHolder.sessionId = null; onDone() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.s06_skip))
            }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        repository.delete(current.id)
                        FinishedSessionHolder.sessionId = null
                        onDone()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.s06_discard)) }
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
