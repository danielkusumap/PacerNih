package com.daniel.runcoach.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.daniel.runcoach.engine.backup.BackupReminder
import com.daniel.runcoach.ui.components.Banner
import com.daniel.runcoach.ui.components.MetricBlock
import com.daniel.runcoach.ui.components.RcBannerVariant
import com.daniel.runcoach.ui.components.RcButton
import com.daniel.runcoach.ui.components.RcButtonVariant
import com.daniel.runcoach.ui.components.RcCard
import com.daniel.runcoach.ui.components.RcDialog
import com.daniel.runcoach.ui.components.RcStatus
import com.daniel.runcoach.ui.components.RcTextField
import com.daniel.runcoach.ui.components.ResultChip
import com.daniel.runcoach.ui.components.SectionHeader
import com.daniel.runcoach.ui.components.SegmentedControl
import com.daniel.runcoach.ui.components.TopBar
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.LocalRcShapes
import com.daniel.runcoach.ui.theme.LocalRcSpacing
import com.daniel.runcoach.ui.theme.LocalRcTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** S13 Backup & data (DESIGN.md section 8). */
@Composable
fun BackupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { BackupRepository(context) }
    val settingsRepository = remember { SettingsRepository(context) }
    var summary by remember { mutableStateOf<BackupSummary?>(null) }
    var lastBackup by remember { mutableStateOf<Long?>(null) }
    var reminderDays by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var showDelete by remember { mutableStateOf(false) }
    var deleteText by remember { mutableStateOf("") }

    suspend fun refresh() {
        summary = repository.summary()
        val current = settingsRepository.settings.first()
        lastBackup = current.lastBackupEpochMs
        reminderDays = current.backupReminderDays
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

    val colors = LocalRcColors.current
    val shapes = LocalRcShapes.current
    val spacing = LocalRcSpacing.current
    val type = LocalRcTypography.current

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        TopBar(title = stringResource(R.string.s13_title), onBack = onBack)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = spacing.screenH),
            verticalArrangement = Arrangement.spacedBy(spacing.cardGap),
        ) {
            RcCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.s4)) {
                    MetricBlock(stringResource(R.string.s13_sessions), (summary?.sessionCount ?: 0).toString())
                    MetricBlock(stringResource(R.string.s13_templates), (summary?.templateCount ?: 0).toString())
                    MetricBlock(stringResource(R.string.s13_db_size), "%.0f".format((summary?.databaseBytes ?: 0) / 1024.0), unit = "KB")
                }
            }

            RcCard {
                Text(stringResource(R.string.s13_last_backup).uppercase(), style = type.label, color = colors.textSecondary)
                Text(
                    lastBackup?.let { SimpleDateFormat("d MMM yyyy HH:mm", Locale.getDefault()).format(Date(it)) } ?: stringResource(R.string.s13_never),
                    style = type.titleS,
                    color = if (lastBackup == null) colors.warn else colors.textPrimary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (BackupReminder.isDue(lastBackup, System.currentTimeMillis(), reminderDays)) {
                Banner(stringResource(R.string.s13_backup_due), variant = RcBannerVariant.Warning)
            }

            message?.let { Text(it, style = type.bodySmall, color = colors.accentText) }

            RcButton(stringResource(R.string.s13_backup), onClick = { createBackup.launch("runcoach-backup.zip") }, fillWidth = true)
            RcButton(stringResource(R.string.s13_restore), onClick = { openBackup.launch(arrayOf("application/zip", "*/*")) }, variant = RcButtonVariant.Secondary, fillWidth = true)
            Text(stringResource(R.string.s13_restore_warning), style = type.caption, color = colors.textTertiary)

            SectionHeader(stringResource(R.string.s13_reminder))
            SegmentedControl(
                options = listOf(
                    0 to stringResource(R.string.s13_reminder_off),
                    14 to stringResource(R.string.s13_reminder_2w),
                    30 to stringResource(R.string.s13_reminder_month),
                ),
                selected = reminderDays,
                onSelect = { scope.launch { settingsRepository.setBackupReminderDays(it); refresh() } },
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.s4)
                    .border(1.dp, colors.fail, RoundedCornerShape(shapes.radiusL))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                Text(stringResource(R.string.s13_danger_zone).uppercase(), style = type.label, color = colors.fail)
                RcButton(stringResource(R.string.s13_delete_all), onClick = { showDelete = true }, variant = RcButtonVariant.Destructive, fillWidth = true)
                Text(stringResource(R.string.s13_delete_hint), style = type.caption, color = colors.textTertiary)
            }
            androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = spacing.s4))
        }
    }

    if (showDelete) {
        RcDialog(
            onDismiss = { showDelete = false; deleteText = "" },
            title = stringResource(R.string.s13_delete_title),
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.s13_delete_message), style = type.body, color = colors.textSecondary)
                    RcTextField(value = deleteText, onValueChange = { deleteText = it }, label = "HAPUS")
                }
            },
            dismiss = { RcButton(stringResource(R.string.editor_discard_keep), onClick = { showDelete = false; deleteText = "" }, variant = RcButtonVariant.Tertiary) },
            confirm = {
                RcButton(
                    stringResource(R.string.s13_delete_all),
                    onClick = { scope.launch { repository.deleteAllData(); showDelete = false; deleteText = ""; refresh() } },
                    enabled = deleteText.trim().uppercase() == "HAPUS",
                    variant = RcButtonVariant.DestructiveFilled,
                )
            },
        )
    }
}
