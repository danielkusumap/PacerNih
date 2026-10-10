package com.daniel.runcoach.ui.prestart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.daniel.runcoach.R
import com.daniel.runcoach.engine.EngineConfig
import com.daniel.runcoach.engine.gps.GpsFixQuality
import com.daniel.runcoach.engine.gps.gpsFixQuality
import com.daniel.runcoach.engine.model.Limit
import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.model.SegmentKind
import com.daniel.runcoach.engine.model.Workout
import com.daniel.runcoach.engine.model.WorkoutExpander
import com.daniel.runcoach.engine.voice.VoiceLanguage
import com.daniel.runcoach.service.RunForegroundService
import com.daniel.runcoach.service.location.LocationSourceFactory
import com.daniel.runcoach.ui.Formatters
import com.daniel.runcoach.ui.onboarding.hasLocationPermission
import com.daniel.runcoach.data.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * S04 Pre-start (plan section 8): workout segment summary, live GPS readiness, permission status, quick
 * settings (voice language, start countdown), and Start / Wait for GPS / Start anyway.
 */
@Composable
fun PraStartScreen(workout: Workout, onStarted: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var accuracy by remember { mutableStateOf<Double?>(null) }
    var voiceLanguage by remember { mutableStateOf(VoiceLanguage.ID) }
    var countdownSec by remember { mutableIntStateOf(EngineConfig().startCountdownSec) }

    val permissionGranted = hasLocationPermission(context)
    val quality = gpsFixQuality(accuracy)

    LaunchedEffect(Unit) {
        val settingsRepository = SettingsRepository(context)
        val stored = settingsRepository.settings.first()
        voiceLanguage = stored.voiceLanguage
        countdownSec = stored.startCountdownSec
        val source = LocationSourceFactory.create(context)
        runCatching { source.samples().collect { accuracy = it.accuracyM } }
    }

    fun start() {
        RunForegroundService.start(context, workout, countdownSec, voiceLanguage)
        onStarted()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.s04_title), style = MaterialTheme.typography.headlineSmall)
            Text(workout.name, style = MaterialTheme.typography.titleMedium)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.s04_segments), style = MaterialTheme.typography.titleSmall)
                    expandedSegments(workout).forEach { Text(describe(it), style = MaterialTheme.typography.bodySmall) }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.s04_gps_status), style = MaterialTheme.typography.titleSmall)
                    Text(
                        when (quality) {
                            GpsFixQuality.READY -> stringResource(R.string.s04_gps_ready, (accuracy?.toInt() ?: 0).toString())
                            GpsFixQuality.WEAK -> stringResource(R.string.s04_gps_weak, (accuracy?.toInt() ?: 0).toString())
                            GpsFixQuality.SEARCHING -> stringResource(R.string.s04_gps_searching)
                        },
                    )
                }
            }

            Text(stringResource(R.string.s04_quick_settings), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.s04_voice_language) + ": ")
                TextButton(onClick = { voiceLanguage = VoiceLanguage.ID }) { Text("ID" + if (voiceLanguage == VoiceLanguage.ID) " ✓" else "") }
                TextButton(onClick = { voiceLanguage = VoiceLanguage.EN }) { Text("EN" + if (voiceLanguage == VoiceLanguage.EN) " ✓" else "") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.s04_countdown) + ": ")
                listOf(0, 3, 5, 10).forEach { value ->
                    TextButton(onClick = { countdownSec = value }) { Text("$value" + if (countdownSec == value) " ✓" else "") }
                }
            }

            if (!permissionGranted) {
                Text(stringResource(R.string.s04_no_permission), color = MaterialTheme.colorScheme.error)
            }

            if (permissionGranted && quality == GpsFixQuality.READY) {
                Button(onClick = { start() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.s04_start)) }
            } else {
                Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.s04_wait_gps)) }
                OutlinedButton(onClick = { if (permissionGranted) start() }, enabled = permissionGranted, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.s04_start_anyway))
                }
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_back)) }
        }
    }
}

private fun expandedSegments(workout: Workout): List<Segment> =
    runCatching { WorkoutExpander.expandFinite(workout.items) }
        .getOrElse { WorkoutExpander.lazySteps(workout.items).take(60).toList() }

@Composable
private fun describe(segment: Segment): String {
    val limit = when (val l = segment.limit) {
        is Limit.Distance -> "${l.meters} m"
        is Limit.Duration -> Formatters.duration(l.seconds * 1000L)
        Limit.Open -> stringResource(R.string.run_no_target)
    }
    val target = segment.targetPaceSecPerKm?.let { ", ≤ ${Formatters.pace(it.toDouble())}" } ?: ""
    return "${kindWord(segment.kind)} $limit$target"
}

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
