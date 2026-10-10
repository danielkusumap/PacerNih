package com.daniel.runcoach.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.daniel.runcoach.R
import com.daniel.runcoach.data.BackupRepository
import com.daniel.runcoach.data.BackupResult
import com.daniel.runcoach.data.BackupSummary
import com.daniel.runcoach.data.SettingsRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** S13 Backup & data (plan section 8, P3.5). */
@Composable
fun BackupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { BackupRepository(context) }
    val settingsRepository = remember { SettingsRepository(context) }
    var summary by remember { mutableStateOf<BackupSummary?>(null) }
    var lastBackup by remember { mutableStateOf<Long?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var showDelete by remember { mutableStateOf(false) }
    var deleteText by remember { mutableStateOf("") }

    suspend fun refresh() {
        summary = repository.summary()
        lastBackup = settingsRepository.settings.first().lastBackupEpochMs
    }
    LaunchedEffect(Unit) { refresh() }

    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            scope.launch {
                context.contentResolver.openOutputStream(uri)?.use { repository.backupTo(it) }
                settingsRepository.setLastBackup(System.currentTimeMillis())
                message = context.getString(R.string.s13_backup_done)
                refresh()
            }
        }
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val result = context.contentResolver.openInputStream(uri)?.use { repository.restoreFrom(it) }
                message = when (result) {
                    is BackupResult.Success -> context.getString(R.string.s13_restore_ok)
                    is BackupResult.Failure -> context.getString(R.string.s13_restore_fail, result.message)
                    null -> context.getString(R.string.s13_restore_fail, "?")
                }
                refresh()
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.s13_title), style = MaterialTheme.typography.headlineSmall)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.s13_summary), style = MaterialTheme.typography.titleSmall)
                    summary?.let {
                        Text("${stringResource(R.string.s13_sessions)}: ${it.sessionCount}")
                        Text("${stringResource(R.string.s13_templates)}: ${it.templateCount}")
                        Text("${stringResource(R.string.s13_db_size)}: ${"%.1f".format(it.databaseBytes / 1024.0)} KB")
                    }
                    Text(
                        "${stringResource(R.string.s13_last_backup)}: " +
                            (lastBackup?.let { SimpleDateFormat("d MMM yyyy HH:mm", Locale.getDefault()).format(Date(it)) }
                                ?: stringResource(R.string.s13_never)),
                    )
                }
            }

            message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            Button(onClick = { createBackup.launch("runcoach-backup.zip") }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.s13_backup))
            }
            OutlinedButton(onClick = { openBackup.launch(arrayOf("application/zip", "*/*")) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.s13_restore))
            }
            Text(stringResource(R.string.s13_restore_warning), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { showDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.s13_delete_all), color = MaterialTheme.colorScheme.error)
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_back)) }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.s13_delete_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.s13_delete_message))
                    OutlinedTextField(value = deleteText, onValueChange = { deleteText = it }, label = { Text("HAPUS") })
                }
            },
            confirmButton = {
                TextButton(
                    enabled = deleteText.trim().uppercase() == "HAPUS",
                    onClick = {
                        scope.launch { repository.deleteAllData(); showDelete = false; deleteText = ""; refresh() }
                    },
                ) { Text(stringResource(R.string.s13_delete_all)) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false; deleteText = "" }) { Text(stringResource(R.string.editor_discard_keep)) } },
        )
    }
}
