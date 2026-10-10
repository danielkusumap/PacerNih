package com.daniel.runcoach.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daniel.runcoach.R
import com.daniel.runcoach.data.RunPersistence
import com.daniel.runcoach.data.RunSnapshot
import com.daniel.runcoach.data.WorkoutTemplate
import com.daniel.runcoach.data.WorkoutTemplateRepository
import com.daniel.runcoach.engine.model.Limit
import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.model.Workout
import com.daniel.runcoach.engine.model.WorkoutEstimator
import com.daniel.runcoach.engine.model.WorkoutExpander
import com.daniel.runcoach.engine.model.WorkoutJson
import com.daniel.runcoach.service.RunForegroundService
import com.daniel.runcoach.ui.Formatters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * S02 Beranda (plan section 8): quick Free run, template list with per-template actions, the unfinished-session
 * banner, and a FAB to create a workout. The weekly summary card arrives in P2.10.
 */
@Composable
fun BerandaScreen(
    debugBuild: Boolean,
    onOpenRun: () -> Unit,
    onOpenEditor: (Workout?) -> Unit,
    onOpenSimulator: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember(context) { WorkoutTemplateRepository(context) }
    val persistence = remember(context) { RunPersistence(context) }
    val templates by repository.templates.collectAsStateWithLifecycle(initialValue = emptyList())
    var snapshot by remember { mutableStateOf<RunSnapshot?>(null) }
    var pendingDelete by remember { mutableStateOf<WorkoutTemplate?>(null) }

    LaunchedEffect(Unit) {
        repository.ensureBuiltIn()
        snapshot = persistence.loadSnapshot()
    }

    val freeRun = templates.firstOrNull { it.id == WorkoutTemplateRepository.FREE_RUN_ID }
    val userTemplates = templates.filterNot { it.isBuiltIn }

    fun start(workout: Workout, templateId: String?) {
        RunForegroundService.start(context, workout)
        if (templateId != null) scope.launch { repository.markUsed(templateId) }
        onOpenRun()
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { onOpenEditor(null) }) { Text(stringResource(R.string.home_editor)) }
        },
    ) { padding ->
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.s02_title), style = MaterialTheme.typography.headlineSmall)

                // Unfinished-session banner
                snapshot?.let { snap ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.s02_resume_title), style = MaterialTheme.typography.titleMedium)
                            val name = runCatching { WorkoutJson.decode(snap.workoutJson).name }.getOrDefault("")
                            Text(stringResource(R.string.s02_resume_detail, name, formatTimestamp(snap.startedAtEpochMs)))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { RunForegroundService.resume(context, snap.sessionId); onOpenRun() }) {
                                    Text(stringResource(R.string.action_resume))
                                }
                                OutlinedButton(onClick = {
                                    scope.launch { persistence.clear(snap.sessionId) }
                                    snapshot = null
                                }) { Text(stringResource(R.string.action_discard)) }
                            }
                        }
                    }
                }

                // Free run quick start
                Button(
                    onClick = { freeRun?.let { start(it.workout, null) } ?: start(WorkoutTemplateRepository.freeRunWorkout(), null) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.s02_free_run), style = MaterialTheme.typography.titleMedium) }

                // Template list
                if (userTemplates.isEmpty()) {
                    Text(stringResource(R.string.s02_create_first), style = MaterialTheme.typography.bodyMedium)
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
                    OutlinedButton(onClick = onOpenSimulator, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.home_simulator))
                    }
                }
            }
        }
    }

    pendingDelete?.let { template ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.s02_delete_title)) },
            text = { Text(stringResource(R.string.s02_delete_message, template.name)) },
            confirmButton = {
                TextButton(onClick = { scope.launch { repository.delete(template.id) }; pendingDelete = null }) {
                    Text(stringResource(R.string.s02_delete))
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.editor_discard_keep)) } },
        )
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(template.name, style = MaterialTheme.typography.titleMedium)
            Text(describe(template.workout), style = MaterialTheme.typography.bodySmall)
            if (template.isBuiltIn) {
                Text(stringResource(R.string.s02_builtin_note), style = MaterialTheme.typography.labelSmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = onStart) { Text(stringResource(R.string.s02_mulai)) }
                if (!template.isBuiltIn) {
                    TextButton(onClick = onEdit) { Text(stringResource(R.string.s02_edit)) }
                    TextButton(onClick = onDuplicate) { Text(stringResource(R.string.s02_duplicate)) }
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.s02_delete)) }
                }
            }
        }
    }
}

@Composable
private fun describe(workout: Workout): String {
    val segmentCount = runCatching { WorkoutExpander.expandFinite(workout.items).size }.getOrElse {
        workout.items.sumOf { block ->
            when (block) {
                is Segment -> 1
                is com.daniel.runcoach.engine.model.Repeat -> block.children.size
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
            is com.daniel.runcoach.engine.model.Repeat -> block.children.map { it.targetPaceSecPerKm }
        }
    }

private fun formatTimestamp(epochMs: Long): String =
    SimpleDateFormat("d MMM HH:mm", Locale.getDefault()).format(Date(epochMs))
