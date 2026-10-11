package com.daniel.runcoach.ui.prestart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.daniel.runcoach.R
import com.daniel.runcoach.data.SettingsRepository
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
import com.daniel.runcoach.ui.components.Banner
import com.daniel.runcoach.ui.components.RcBannerVariant
import com.daniel.runcoach.ui.components.RcButton
import com.daniel.runcoach.ui.components.RcButtonVariant
import com.daniel.runcoach.ui.components.RcCard
import com.daniel.runcoach.ui.components.RcDivider
import com.daniel.runcoach.ui.components.RcStatus
import com.daniel.runcoach.ui.components.ResultChip
import com.daniel.runcoach.ui.components.SegmentStrip
import com.daniel.runcoach.ui.components.SegmentedControl
import com.daniel.runcoach.ui.components.TopBar
import com.daniel.runcoach.ui.components.segmentKindColor
import com.daniel.runcoach.ui.components.segmentKindIcon
import com.daniel.runcoach.ui.onboarding.hasLocationPermission
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.LocalRcShapes
import com.daniel.runcoach.ui.theme.LocalRcSpacing
import com.daniel.runcoach.ui.theme.LocalRcTypography
import com.daniel.runcoach.ui.theme.RcIcons
import kotlinx.coroutines.flow.first

/** S04 Pre-start (DESIGN.md section 8): GPS readiness, segment list, quick settings, sticky Start. */
@Composable
fun PraStartScreen(workout: Workout, onStarted: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var accuracy by remember { mutableStateOf<Double?>(null) }
    var voiceLanguage by remember { mutableStateOf(VoiceLanguage.ID) }
    var countdownSec by remember { mutableIntStateOf(EngineConfig().startCountdownSec) }

    val permissionGranted = hasLocationPermission(context)
    val quality = gpsFixQuality(accuracy)
    val segments = expandedSegments(workout)

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

    val colors = LocalRcColors.current
    val spacing = LocalRcSpacing.current
    val type = LocalRcTypography.current

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        TopBar(title = workout.name, onBack = onBack)
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = spacing.screenH), verticalArrangement = Arrangement.spacedBy(spacing.cardGap)) {
            SegmentStrip(kinds = segments.map { it.kind })
            Text(stringResource(R.string.s04_summary, segments.size, Formatters.distanceKm(segments.sumOf { (it.limit as? Limit.Distance)?.meters?.toDouble() ?: 0.0 })), style = type.bodySmall, color = colors.textSecondary)

            RcCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.s4)) {
                    SignalBars(accuracy)
                    Column(modifier = Modifier.weight(1f)) {
                        Text((accuracy?.toInt() ?: 0).toString(), style = type.numberL, color = colors.textPrimary)
                        Text(stringResource(R.string.s04_accuracy), style = type.label, color = colors.textSecondary)
                    }
                    ResultChip(
                        status = when (quality) {
                            GpsFixQuality.READY -> RcStatus.Pass
                            GpsFixQuality.WEAK -> RcStatus.Warn
                            GpsFixQuality.SEARCHING -> RcStatus.Neutral
                        },
                        label = when (quality) {
                            GpsFixQuality.READY -> stringResource(R.string.s04_status_ready)
                            GpsFixQuality.WEAK -> stringResource(R.string.s04_status_weak)
                            GpsFixQuality.SEARCHING -> stringResource(R.string.s04_status_searching)
                        },
                    )
                }
            }

            Text(stringResource(R.string.s04_segments), style = type.label, color = colors.textSecondary)
            segments.forEach { segment ->
                Row(modifier = Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                    Icon(painterResource(segmentKindIcon(segment.kind)), contentDescription = null, tint = segmentKindColor(segment.kind), modifier = Modifier.size(20.dp))
                    Text(describe(segment), style = type.body, color = colors.textPrimary, modifier = Modifier.weight(1f))
                }
                RcDivider()
            }

            Column(verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
                Text(stringResource(R.string.s04_voice_language), style = type.bodySmall, color = colors.textSecondary)
                SegmentedControl(listOf(VoiceLanguage.ID to "ID", VoiceLanguage.EN to "EN"), voiceLanguage, { voiceLanguage = it })
                Text(stringResource(R.string.s04_countdown), style = type.bodySmall, color = colors.textSecondary)
                SegmentedControl(listOf(0, 3, 5, 10).map { it to it.toString() }, countdownSec, { countdownSec = it })
            }
            if (!permissionGranted) Banner(stringResource(R.string.s04_no_permission), variant = RcBannerVariant.Warning)
            Box(Modifier.padding(bottom = spacing.s4))
        }

        Column(modifier = Modifier.fillMaxWidth().background(colors.bg).padding(horizontal = spacing.screenH).padding(vertical = spacing.s3), verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
            RcButton(
                stringResource(R.string.s04_start).uppercase(),
                onClick = { if (permissionGranted) start() },
                enabled = permissionGranted,
                icon = RcIcons.PlayArrow,
                fillWidth = true,
                modifier = Modifier.height(64.dp),
            )
        }
    }
}

@Composable
private fun SignalBars(accuracy: Double?) {
    val colors = LocalRcColors.current
    val shapes = LocalRcShapes.current
    val filled = when {
        accuracy == null -> 0
        accuracy <= 8 -> 5
        accuracy <= 12 -> 4
        accuracy <= 20 -> 3
        else -> 2
    }
    val tint = when {
        filled >= 4 -> colors.pass
        filled >= 3 -> colors.warn
        else -> colors.neutral
    }
    Row(modifier = Modifier.height(40.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        (1..5).forEach { bar ->
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height((8 * bar).dp)
                    .background(if (bar <= filled) tint else colors.outline, RoundedCornerShape(2.dp)),
            )
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
