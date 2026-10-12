package com.daniel.runcoach.ui.prestart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.daniel.runcoach.R
import com.daniel.runcoach.data.SettingsRepository
import com.daniel.runcoach.engine.EngineConfig
import com.daniel.runcoach.engine.gps.GpsFixQuality
import com.daniel.runcoach.engine.gps.gpsFixQuality
import com.daniel.runcoach.engine.model.Limit
import com.daniel.runcoach.engine.model.Repeat
import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.model.SegmentKind
import com.daniel.runcoach.engine.model.Workout
import com.daniel.runcoach.engine.voice.VoiceLanguage
import com.daniel.runcoach.service.RunForegroundService
import com.daniel.runcoach.service.location.LocationSourceFactory
import com.daniel.runcoach.ui.Formatters
import com.daniel.runcoach.ui.components.Banner
import com.daniel.runcoach.ui.components.RcBannerVariant
import com.daniel.runcoach.ui.components.RcButton
import com.daniel.runcoach.ui.components.RcButtonSize
import com.daniel.runcoach.ui.components.RcCard
import com.daniel.runcoach.ui.components.RcIconButton
import com.daniel.runcoach.ui.components.RcStatus
import com.daniel.runcoach.ui.components.RcTextButton
import com.daniel.runcoach.ui.components.RcTextButtonTone
import com.daniel.runcoach.ui.components.ResultChip
import com.daniel.runcoach.ui.components.SegmentStrip
import com.daniel.runcoach.ui.components.SegmentedControl
import com.daniel.runcoach.ui.components.segmentKindColor
import com.daniel.runcoach.ui.components.segmentKindIcon
import com.daniel.runcoach.ui.onboarding.hasLocationPermission
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.LocalRcShapes
import com.daniel.runcoach.ui.theme.LocalRcSpacing
import com.daniel.runcoach.ui.theme.LocalRcTypography
import com.daniel.runcoach.ui.theme.RcIcons
import kotlinx.coroutines.flow.first

/** S04 Pre-start (DESIGN-RESKIN section 3): GPS readiness, segment list, quick settings, sticky Start. */
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

    fun copyStructure() {
        val text = com.daniel.runcoach.engine.model.WorkoutTextFormatter.format(workout, voiceLanguage)
        val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("workout", text))
        android.widget.Toast.makeText(context, R.string.copied, android.widget.Toast.LENGTH_SHORT).show()
    }

    val colors = LocalRcColors.current
    val spacing = LocalRcSpacing.current
    val type = LocalRcTypography.current

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(44.dp).padding(horizontal = spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RcIconButton(RcIcons.ArrowBack, contentDescription = null, onClick = onBack, tint = colors.textSecondary)
            Text(
                stringResource(R.string.s04_title).uppercase(),
                style = type.label,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            RcTextButton(stringResource(R.string.copy_text), onClick = { copyStructure() }, tone = RcTextButtonTone.Accent)
        }
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = spacing.screenH),
            verticalArrangement = Arrangement.spacedBy(spacing.cardGap),
        ) {
            Text(workout.name.uppercase(), style = type.titleXL, color = colors.textPrimary)
            Text(
                stringResource(
                    R.string.s04_summary,
                    segments.size,
                    Formatters.distanceKm(segments.sumOf { (it.limit as? Limit.Distance)?.meters?.toDouble() ?: 0.0 }),
                ),
                style = type.bodySmall,
                color = colors.textSecondary,
            )
            SegmentStrip(kinds = segments.map { it.kind }, height = 14.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .background(colors.surface, RoundedCornerShape(LocalRcShapes.current.radiusL))
                    .border(1.dp, colors.passContainer, RoundedCornerShape(LocalRcShapes.current.radiusL))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s3),
            ) {
                SignalBars(accuracy)
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text((accuracy?.toInt() ?: 0).toString(), style = type.displayS, color = colors.textPrimary)
                        Text("m", style = type.numberXS, color = colors.textSecondary)
                    }
                    Text(stringResource(R.string.s04_accuracy).uppercase(), style = type.label, color = colors.textSecondary)
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

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.s04_segments).uppercase(), style = type.label, color = colors.textSecondary)
                Text(stringResource(R.string.s04_segment_count, segments.size), style = type.caption, color = colors.textSecondary)
            }
            RcCard {
                workout.items.forEach { block ->
                    when (block) {
                        is Segment -> SegmentRow(block)
                        is Repeat -> RepeatBlock(block)
                    }
                }
            }

            Text(stringResource(R.string.s04_quick_settings).uppercase(), style = type.label, color = colors.textSecondary)
            RcCard {
                QuickSettingRow(stringResource(R.string.s04_voice_language)) {
                    SegmentedControl(listOf(VoiceLanguage.ID to "ID", VoiceLanguage.EN to "EN"), voiceLanguage, { voiceLanguage = it }, modifier = Modifier.width(120.dp))
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outline))
                QuickSettingRow(stringResource(R.string.s04_countdown)) {
                    SegmentedControl(listOf(0, 3, 5, 10).map { it to it.toString() }, countdownSec, { countdownSec = it }, modifier = Modifier.width(200.dp))
                }
            }
            if (!permissionGranted) Banner(stringResource(R.string.s04_no_permission), variant = RcBannerVariant.Warning)
            Box(Modifier.padding(bottom = spacing.s4))
        }

        Column(
            modifier = Modifier.fillMaxWidth().background(colors.bg).padding(horizontal = spacing.screenH).padding(vertical = spacing.s3),
        ) {
            RcButton(
                stringResource(R.string.s04_start),
                onClick = { if (permissionGranted) start() },
                enabled = permissionGranted,
                icon = RcIcons.PlayArrow,
                size = RcButtonSize.Large,
                fillWidth = true,
            )
        }
    }
}

@Composable
private fun QuickSettingRow(label: String, control: @Composable () -> Unit) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    Row(
        modifier = Modifier.fillMaxWidth().height(50.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = type.body, color = colors.textPrimary)
        control()
    }
}

@Composable
private fun RepeatBlock(repeat: Repeat) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Box(Modifier.width(3.dp).height(84.dp).background(colors.accent, RoundedCornerShape(2.dp)))
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(stringResource(R.string.s04_repeat, repeat.times ?: 0), style = type.label, color = colors.accentText, modifier = Modifier.padding(vertical = 2.dp))
            repeat.children.forEach { SegmentRow(it) }
        }
    }
}

@Composable
private fun SegmentRow(segment: Segment) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    Row(
        modifier = Modifier.fillMaxWidth().height(42.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val kindColor = segmentKindColor(segment.kind)
        Box(
            modifier = Modifier.size(32.dp).background(kindColor.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(segmentKindIcon(segment.kind)), contentDescription = null, tint = kindColor, modifier = Modifier.size(18.dp))
        }
        Text(kindWord(segment.kind), style = type.titleS, color = colors.textPrimary, modifier = Modifier.weight(1f))
        Text(segmentDetail(segment), style = type.bodySmall, color = colors.textSecondary)
    }
}

@Composable
private fun SignalBars(accuracy: Double?) {
    val colors = LocalRcColors.current
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
    Row(modifier = Modifier.height(34.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        (1..5).forEach { bar ->
            Box(
                modifier = Modifier
                    .width(9.dp)
                    .height((bar * 6 + 4).dp)
                    .background(if (bar <= filled) tint else colors.outline, RoundedCornerShape(3.dp)),
            )
        }
    }
}

private fun expandedSegments(workout: Workout): List<Segment> =
    runCatching { com.daniel.runcoach.engine.model.WorkoutExpander.expandFinite(workout.items) }
        .getOrElse { com.daniel.runcoach.engine.model.WorkoutExpander.lazySteps(workout.items).take(60).toList() }

@Composable
private fun segmentDetail(segment: Segment): String {
    val limit = when (val l = segment.limit) {
        is Limit.Distance -> Formatters.segmentDistance(l.meters.toDouble())
        is Limit.Duration -> Formatters.duration(l.seconds * 1000L)
        Limit.Open -> stringResource(R.string.run_no_target)
    }
    val target = Formatters.segmentTarget(segment.targetPaceSecPerKm, segment.targetPace2SecPerKm, segment.targetBound)?.let { " · $it" } ?: ""
    return "$limit$target"
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
