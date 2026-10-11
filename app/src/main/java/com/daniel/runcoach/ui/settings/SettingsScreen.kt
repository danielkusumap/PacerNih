package com.daniel.runcoach.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
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
import com.daniel.runcoach.ui.components.NumberStepper
import com.daniel.runcoach.ui.components.RcButton
import com.daniel.runcoach.ui.components.RcButtonVariant
import com.daniel.runcoach.ui.components.RcCard
import com.daniel.runcoach.ui.components.RcIconButton
import com.daniel.runcoach.ui.components.RcScreenTitle
import com.daniel.runcoach.ui.components.RcSwitchRow
import com.daniel.runcoach.ui.components.RcTextButton
import com.daniel.runcoach.ui.components.RcTextButtonTone
import com.daniel.runcoach.ui.components.SectionHeader
import com.daniel.runcoach.ui.components.SegmentedControl
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.LocalRcSpacing
import com.daniel.runcoach.ui.theme.LocalRcTypography
import com.daniel.runcoach.ui.theme.RcIcons
import kotlinx.coroutines.launch

/** S12 Settings (DESIGN-RESKIN section 3). Theme applies immediately through the settings flow. */
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

    androidx.compose.material3.Scaffold(bottomBar = { MainBottomBar(MainTab.SETTINGS, onSelectTab) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(colors.bg).padding(padding)) {
            RcScreenTitle(stringResource(R.string.s12_title), modifier = Modifier.padding(horizontal = spacing.screenH, vertical = spacing.s2))
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = spacing.screenH),
                verticalArrangement = Arrangement.spacedBy(spacing.cardGap),
            ) {
                Section(stringResource(R.string.s12_display)) {
                    SettingRow(stringResource(R.string.s12_theme)) {
                        SegmentedControl(
                            options = listOf(
                                AppTheme.LIGHT to stringResource(R.string.s12_theme_light),
                                AppTheme.DARK to stringResource(R.string.s12_theme_dark),
                                AppTheme.SYSTEM to stringResource(R.string.s12_theme_system),
                            ),
                            selected = settings.theme,
                            onSelect = { scope.launch { repository.setTheme(it) } },
                            modifier = Modifier.width(200.dp),
                        )
                    }
                    RcSwitchRow(stringResource(R.string.s12_keep_screen), settings.keepScreenOn, onCheckedChange = { scope.launch { repository.setKeepScreenOn(it) } }, showDivider = false)
                }

                Section(stringResource(R.string.s12_voice)) {
                    SettingRow(stringResource(R.string.s12_voice_language)) {
                        SegmentedControl(
                            options = listOf(VoiceLanguage.ID to "ID", VoiceLanguage.EN to "EN"),
                            selected = settings.voiceLanguage,
                            onSelect = { scope.launch { repository.setVoiceLanguage(it) } },
                            modifier = Modifier.width(120.dp),
                        )
                    }
                    SettingRow(stringResource(R.string.s12_test_voice)) {
                        RcTextButton(
                            stringResource(R.string.s12_test_voice),
                            onClick = {
                                tts.setLanguage(settings.voiceLanguage)
                                tts.speak(if (settings.voiceLanguage == VoiceLanguage.ID) "Pace stabil. Sekarang 7 menit 2, target 7 menit." else "Pace is steady. Now 7:02, target 7:00.", flush = true)
                            },
                        )
                    }
                    Text(
                        stringResource(R.string.s12_voice_pack, stringResource(if (tts.isLanguageAvailable(settings.voiceLanguage)) R.string.s12_available else R.string.s12_missing)),
                        style = LocalRcTypography.current.caption,
                        color = colors.textTertiary,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    RcSwitchRow(stringResource(R.string.s12_km_marker), settings.kmMarker, onCheckedChange = { scope.launch { repository.setKmMarker(it) } })
                    RcSwitchRow(stringResource(R.string.s12_pace_alerts), settings.paceAlerts, onCheckedChange = { scope.launch { repository.setPaceAlerts(it) } }, showDivider = false)
                }

                Section(stringResource(R.string.s12_countdown)) {
                    SettingRow(stringResource(R.string.s12_countdown_start)) {
                        SegmentedControl(listOf(0, 3, 5, 10).map { it to it.toString() }, settings.startCountdownSec, { scope.launch { repository.setStartCountdown(it) } }, modifier = Modifier.width(200.dp))
                    }
                    SettingRow(stringResource(R.string.s12_countdown_segment)) {
                        SegmentedControl(listOf(0, 3, 5).map { it to it.toString() }, settings.segmentCountdownSec, { scope.launch { repository.setSegmentCountdown(it) } }, modifier = Modifier.width(160.dp))
                    }
                    SettingRow(stringResource(R.string.s12_cue_distance)) {
                        SegmentedControl(listOf(0, 10, 20, 30, 50).map { it to it.toString() }, settings.cueDistanceM, { scope.launch { repository.setCueDistance(it) } }, modifier = Modifier.width(230.dp))
                    }
                    SettingRow(stringResource(R.string.s12_countdown_style), showDivider = false) {
                        SegmentedControl(
                            options = listOf(CountdownStyleSetting.BEEP to stringResource(R.string.s12_style_beep), CountdownStyleSetting.VOICE to stringResource(R.string.s12_style_voice)),
                            selected = settings.countdownStyle,
                            onSelect = { scope.launch { repository.setCountdownStyle(it) } },
                            modifier = Modifier.width(160.dp),
                        )
                    }
                }

                Section(stringResource(R.string.s12_alert)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        NumberStepper(
                            value = "${settings.fastMarginSec} s",
                            onDecrement = { scope.launch { repository.setFastMargin((settings.fastMarginSec - 5).coerceAtLeast(0)) } },
                            onIncrement = { scope.launch { repository.setFastMargin(settings.fastMarginSec + 5) } },
                            stepLabel = stringResource(R.string.s12_fast_margin),
                        )
                    }
                    RcSwitchRow(stringResource(R.string.s12_vibrate), settings.vibrate, onCheckedChange = { scope.launch { repository.setVibrate(it) } }, showDivider = false)
                }

                Section(stringResource(R.string.s12_tracking)) {
                    RcSwitchRow(stringResource(R.string.s12_auto_pause), settings.autoPause, onCheckedChange = { scope.launch { repository.setAutoPause(it) } })
                    SettingRow(stringResource(R.string.s12_calibration), showDivider = false) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("%.4f".format(settings.calibrationFactor), style = LocalRcTypography.current.numberXS, color = colors.textPrimary)
                            RcTextButton(stringResource(R.string.s12_calibration_reset), onClick = { scope.launch { repository.setCalibrationFactor(1.0) } }, tone = RcTextButtonTone.Neutral)
                        }
                    }
                }

                Section(stringResource(R.string.s12_data)) {
                    SettingRow(stringResource(R.string.s12_data), showDivider = false) {
                        RcIconButton(RcIcons.ChevronRight, null, onClick = onOpenData, tint = colors.textSecondary)
                    }
                }

                Section(stringResource(R.string.s12_help)) {
                    SettingRow(stringResource(R.string.action_battery_guide)) {
                        RcIconButton(RcIcons.ChevronRight, null, onClick = onOpenBatteryGuide, tint = colors.textSecondary)
                    }
                    SettingRow(stringResource(R.string.s12_version, versionName), showDivider = false) {}
                }

                if (debugBuild) {
                    Section(stringResource(R.string.s12_dev)) {
                        SettingRow(stringResource(R.string.home_simulator)) {
                            RcIconButton(RcIcons.ChevronRight, null, onClick = onOpenSimulator, tint = colors.textSecondary)
                        }
                        SettingRow(stringResource(R.string.rc_catalog), showDivider = false) {
                            RcIconButton(RcIcons.ChevronRight, null, onClick = onOpenCatalog, tint = colors.textSecondary)
                        }
                    }
                }
                androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = spacing.s4))
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        SectionHeader(title)
        RcCard(modifier = Modifier.fillMaxWidth()) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingRow(label: String, showDivider: Boolean = true, control: @Composable () -> Unit) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = type.body, color = colors.textPrimary, modifier = Modifier.weight(1f))
            control()
        }
        if (showDivider) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outline))
    }
}
