package com.daniel.runcoach.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import kotlinx.coroutines.launch

/** S12 Settings (plan section 8). Theme applies immediately through the settings flow. */
@Composable
fun SettingsScreen(
    debugBuild: Boolean,
    onOpenBatteryGuide: () -> Unit,
    onOpenData: () -> Unit,
    onOpenSimulator: () -> Unit,
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
    val offLabel = stringResource(R.string.s12_off)

    Scaffold(bottomBar = { MainBottomBar(MainTab.SETTINGS, onSelectTab) }) { padding ->
        Surface(modifier = Modifier.fillMaxSize().padding(padding), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(stringResource(R.string.s12_title), style = MaterialTheme.typography.headlineSmall)

                // Display
                Section(stringResource(R.string.s12_display)) {
                    Text(stringResource(R.string.s12_theme))
                    OptionRow(
                        options = listOf(
                            AppTheme.SYSTEM to stringResource(R.string.s12_theme_system),
                            AppTheme.LIGHT to stringResource(R.string.s12_theme_light),
                            AppTheme.DARK to stringResource(R.string.s12_theme_dark),
                        ),
                        selected = settings.theme,
                        onSelect = { scope.launch { repository.setTheme(it) } },
                    )
                    ToggleRow(stringResource(R.string.s12_keep_screen), settings.keepScreenOn) { scope.launch { repository.setKeepScreenOn(it) } }
                }

                // Voice
                Section(stringResource(R.string.s12_voice)) {
                    Text(stringResource(R.string.s12_voice_language))
                    OptionRow(
                        options = listOf(VoiceLanguage.ID to "ID", VoiceLanguage.EN to "EN"),
                        selected = settings.voiceLanguage,
                        onSelect = { scope.launch { repository.setVoiceLanguage(it) } },
                    )
                    Text(stringResource(R.string.s12_voice_pack, stringResource(if (tts.isLanguageAvailable(settings.voiceLanguage)) R.string.s12_available else R.string.s12_missing)))
                    TextButton(onClick = { tts.setLanguage(settings.voiceLanguage); tts.speak(if (settings.voiceLanguage == VoiceLanguage.ID) "Pace stabil. Sekarang 7 menit 2, target 7 menit." else "Pace is steady. Now 7:02, target 7:00.", flush = true) }) {
                        Text(stringResource(R.string.s12_test_voice))
                    }
                    ToggleRow(stringResource(R.string.s12_km_marker), settings.kmMarker) { scope.launch { repository.setKmMarker(it) } }
                    ToggleRow(stringResource(R.string.s12_pace_alerts), settings.paceAlerts) { scope.launch { repository.setPaceAlerts(it) } }
                }

                // Countdown
                Section(stringResource(R.string.s12_countdown)) {
                    IntOptionRow(stringResource(R.string.s12_countdown_start), listOf(0, 3, 5, 10), settings.startCountdownSec, offLabel) { scope.launch { repository.setStartCountdown(it) } }
                    IntOptionRow(stringResource(R.string.s12_countdown_segment), listOf(0, 3, 5), settings.segmentCountdownSec, offLabel) { scope.launch { repository.setSegmentCountdown(it) } }
                    IntOptionRow(stringResource(R.string.s12_cue_distance), listOf(0, 10, 20, 30, 50), settings.cueDistanceM, offLabel) { scope.launch { repository.setCueDistance(it) } }
                    Text(stringResource(R.string.s12_countdown_style))
                    OptionRow(
                        options = listOf(
                            CountdownStyleSetting.BEEP to stringResource(R.string.s12_style_beep),
                            CountdownStyleSetting.VOICE to stringResource(R.string.s12_style_voice),
                        ),
                        selected = settings.countdownStyle,
                        onSelect = { scope.launch { repository.setCountdownStyle(it) } },
                    )
                }

                // Alerts
                Section(stringResource(R.string.s12_alert)) {
                    IntOptionRow(stringResource(R.string.s12_fast_margin), listOf(0, 10, 20, 30), settings.fastMarginSec, offLabel) { scope.launch { repository.setFastMargin(it) } }
                    ToggleRow(stringResource(R.string.s12_vibrate), settings.vibrate) { scope.launch { repository.setVibrate(it) } }
                }

                // Tracking
                Section(stringResource(R.string.s12_tracking)) {
                    ToggleRow(stringResource(R.string.s12_auto_pause), settings.autoPause) { scope.launch { repository.setAutoPause(it) } }
                    Text(stringResource(R.string.s12_calibration_value, "%.4f".format(settings.calibrationFactor)))
                    TextButton(onClick = { scope.launch { repository.setCalibrationFactor(1.0) } }) {
                        Text(stringResource(R.string.s12_calibration_reset))
                    }
                }

                // Data & help
                TextButton(onClick = onOpenData) { Text(stringResource(R.string.s12_data)) }
                Section(stringResource(R.string.s12_help)) {
                    TextButton(onClick = onOpenBatteryGuide) { Text(stringResource(R.string.action_battery_guide)) }
                    Text(stringResource(R.string.s12_version, versionName))
                }
                if (debugBuild) {
                    Section(stringResource(R.string.s12_dev)) {
                        TextButton(onClick = onOpenSimulator) { Text(stringResource(R.string.home_simulator)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun <T> OptionRow(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { (value, label) ->
            TextButton(onClick = { onSelect(value) }) { Text(label + if (value == selected) " ✓" else "") }
        }
    }
}

@Composable
private fun IntOptionRow(label: String, options: List<Int>, selected: Int, offLabel: String, onSelect: (Int) -> Unit) {
    Text(label)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { value ->
            val text = if (value == 0) offLabel else value.toString()
            TextButton(onClick = { onSelect(value) }) { Text(text + if (value == selected) " ✓" else "") }
        }
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        TextButton(onClick = { onChange(!value) }) { Text(if (value) "ON" else "OFF") }
    }
}
