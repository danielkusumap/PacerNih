package com.daniel.runcoach.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.daniel.runcoach.R
import com.daniel.runcoach.data.AppSettings
import com.daniel.runcoach.data.AppTheme
import com.daniel.runcoach.data.CountdownStyleSetting
import com.daniel.runcoach.data.SettingsRepository
import com.daniel.runcoach.engine.voice.VoiceLanguage
import com.daniel.runcoach.service.voice.TtsSpeaker
import com.daniel.runcoach.ui.MainBottomBar
import com.daniel.runcoach.ui.MainTab
import com.daniel.runcoach.ui.components.ListRow
import com.daniel.runcoach.ui.components.NumberStepper
import com.daniel.runcoach.ui.components.RcButton
import com.daniel.runcoach.ui.components.RcButtonVariant
import com.daniel.runcoach.ui.components.RcCard
import com.daniel.runcoach.ui.components.RcDivider
import com.daniel.runcoach.ui.components.RcIconButton
import com.daniel.runcoach.ui.components.RcSwitchRow
import com.daniel.runcoach.ui.components.SectionHeader
import com.daniel.runcoach.ui.components.SegmentedControl
import com.daniel.runcoach.ui.components.TopBar
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.LocalRcSpacing
import com.daniel.runcoach.ui.theme.LocalRcTypography
import com.daniel.runcoach.ui.theme.RcIcons
import kotlinx.coroutines.launch

/** S12 Settings (DESIGN.md section 8). Theme applies immediately through the settings flow. */
@Composable
fun SettingsScreen(
    debugBuild: Boolean,
    onOpenBatteryGuide: () -> Unit,
    onOpenData: () -> Unit,
    onOpenSimulator: () -> Unit,
    onOpenCatalog: () -> Unit = {},
    onSelectTab: (MainTab) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { SettingsRepository(context) }
    val settings by repository.settings.collectAsState(initial = AppSettings())
    val tts = remember { TtsSpeaker(context).also { it.init() } }
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }
    val colors = LocalRcColors.current
    val spacing = LocalRcSpacing.current
    val type = LocalRcTypography.current

    androidx.compose.material3.Scaffold(bottomBar = { MainBottomBar(MainTab.SETTINGS, onSelectTab) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(colors.bg).padding(padding)) {
            TopBar(title = stringResource(R.string.s12_title), tabPage = true)
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = spacing.screenH),
                verticalArrangement = Arrangement.spacedBy(spacing.cardGap),
            ) {
                Section(stringResource(R.string.s12_display)) {
                    Text(stringResource(R.string.s12_theme), style = type.bodySmall, color = colors.textSecondary)
                    SegmentedControl(
                        options = listOf(
                            AppTheme.LIGHT to stringResource(R.string.s12_theme_light),
                            AppTheme.DARK to stringResource(R.string.s12_theme_dark),
                            AppTheme.SYSTEM to stringResource(R.string.s12_theme_system),
                        ),
                        selected = settings.theme,
                        onSelect = { scope.launch { repository.setTheme(it) } },
                    )
                    RcSwitchRow(stringResource(R.string.s12_keep_screen), settings.keepScreenOn, onCheckedChange = { scope.launch { repository.setKeepScreenOn(it) } })
                }

                Section(stringResource(R.string.s12_voice)) {
                    Text(stringResource(R.string.s12_voice_language), style = type.bodySmall, color = colors.textSecondary)
                    SegmentedControl(
                        options = listOf(VoiceLanguage.ID to "Indonesia", VoiceLanguage.EN to "English"),
                        selected = settings.voiceLanguage,
                        onSelect = { scope.launch { repository.setVoiceLanguage(it) } },
                    )
                    RcButton(
                        stringResource(R.string.s12_test_voice),
                        onClick = {
                            tts.setLanguage(settings.voiceLanguage)
                            tts.speak(if (settings.voiceLanguage == VoiceLanguage.ID) "Pace stabil. Sekarang 7 menit 2, target 7 menit." else "Pace is steady. Now 7:02, target 7:00.", flush = true)
                        },
                        variant = RcButtonVariant.Secondary,
                        fillWidth = true,
                    )
                    Text(
                        stringResource(R.string.s12_voice_pack, stringResource(if (tts.isLanguageAvailable(settings.voiceLanguage)) R.string.s12_available else R.string.s12_missing)),
                        style = type.bodySmall,
                        color = colors.textSecondary,
                    )
                    RcSwitchRow(stringResource(R.string.s12_km_marker), settings.kmMarker, onCheckedChange = { scope.launch { repository.setKmMarker(it) } })
                    RcSwitchRow(stringResource(R.string.s12_pace_alerts), settings.paceAlerts, onCheckedChange = { scope.launch { repository.setPaceAlerts(it) } })
                }

                Section(stringResource(R.string.s12_countdown)) {
                    Text(stringResource(R.string.s12_countdown_start), style = type.bodySmall, color = colors.textSecondary)
                    SegmentedControl(listOf(0, 3, 5, 10).map { it to it.toString() }, settings.startCountdownSec, { scope.launch { repository.setStartCountdown(it) } })
                    Text(stringResource(R.string.s12_countdown_segment), style = type.bodySmall, color = colors.textSecondary)
                    SegmentedControl(listOf(0, 3, 5).map { it to it.toString() }, settings.segmentCountdownSec, { scope.launch { repository.setSegmentCountdown(it) } })
                    Text(stringResource(R.string.s12_cue_distance), style = type.bodySmall, color = colors.textSecondary)
                    SegmentedControl(listOf(0, 10, 20, 30, 50).map { it to it.toString() }, settings.cueDistanceM, { scope.launch { repository.setCueDistance(it) } })
                    Text(stringResource(R.string.s12_countdown_style), style = type.bodySmall, color = colors.textSecondary)
                    SegmentedControl(
                        options = listOf(CountdownStyleSetting.BEEP to stringResource(R.string.s12_style_beep), CountdownStyleSetting.VOICE to stringResource(R.string.s12_style_voice)),
                        selected = settings.countdownStyle,
                        onSelect = { scope.launch { repository.setCountdownStyle(it) } },
                    )
                }

                Section(stringResource(R.string.s12_alert)) {
                    NumberStepper(
                        value = "${settings.fastMarginSec} s",
                        onDecrement = { scope.launch { repository.setFastMargin((settings.fastMarginSec - 5).coerceAtLeast(0)) } },
                        onIncrement = { scope.launch { repository.setFastMargin(settings.fastMarginSec + 5) } },
                        stepLabel = stringResource(R.string.s12_fast_margin),
                    )
                    RcSwitchRow(stringResource(R.string.s12_vibrate), settings.vibrate, onCheckedChange = { scope.launch { repository.setVibrate(it) } })
                }

                Section(stringResource(R.string.s12_tracking)) {
                    RcSwitchRow(stringResource(R.string.s12_auto_pause), settings.autoPause, onCheckedChange = { scope.launch { repository.setAutoPause(it) } })
                    Text(stringResource(R.string.s12_calibration_value, "%.4f".format(settings.calibrationFactor)), style = type.bodySmall, color = colors.textSecondary)
                    RcButton(stringResource(R.string.s12_calibration_reset), onClick = { scope.launch { repository.setCalibrationFactor(1.0) } }, variant = RcButtonVariant.Tertiary)
                }

                Section(stringResource(R.string.s12_data)) {
                    ListRow(
                        title = stringResource(R.string.s12_data),
                        trailing = { RcIconButton(RcIcons.ChevronRight, null, onClick = onOpenData) },
                        onClick = onOpenData,
                    )
                }

                Section(stringResource(R.string.s12_help)) {
                    ListRow(title = stringResource(R.string.action_battery_guide), trailing = { RcIconButton(RcIcons.ChevronRight, null, onClick = onOpenBatteryGuide) }, onClick = onOpenBatteryGuide)
                    RcDivider()
                    ListRow(title = stringResource(R.string.s12_version, versionName))
                }

                if (debugBuild) {
                    Section(stringResource(R.string.s12_dev)) {
                        ListRow(title = stringResource(R.string.home_simulator), trailing = { RcIconButton(RcIcons.ChevronRight, null, onClick = onOpenSimulator) }, onClick = onOpenSimulator)
                        RcDivider()
                        ListRow(title = stringResource(R.string.rc_catalog), trailing = { RcIconButton(RcIcons.ChevronRight, null, onClick = onOpenCatalog) }, onClick = onOpenCatalog)
                    }
                }
                androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = spacing.s4))
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val spacing = LocalRcSpacing.current
    Column {
        SectionHeader(title)
        RcCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.s2)) { content() }
        }
    }
}
