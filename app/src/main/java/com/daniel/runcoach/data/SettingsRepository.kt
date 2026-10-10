package com.daniel.runcoach.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.daniel.runcoach.engine.voice.VoiceLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppTheme { SYSTEM, LIGHT, DARK }

enum class CountdownStyleSetting { BEEP, VOICE }

/** All user settings (plan S12). Kept in DataStore, not Room. */
data class AppSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val voiceLanguage: VoiceLanguage = VoiceLanguage.ID,
    val startCountdownSec: Int = 5,
    val segmentCountdownSec: Int = 3,
    val cueDistanceM: Int = 20,
    val countdownStyle: CountdownStyleSetting = CountdownStyleSetting.BEEP,
    val fastMarginSec: Int = 20,
    val vibrate: Boolean = true,
    val autoPause: Boolean = false,
    val keepScreenOn: Boolean = false,
    val kmMarker: Boolean = true,
    val paceAlerts: Boolean = true,
    val liveMap: Boolean = false,
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Reads and writes settings (plan S12). Theme changes apply immediately through the settings flow. */
class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            theme = prefs[KEY_THEME]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() } ?: AppTheme.SYSTEM,
            voiceLanguage = if (prefs[KEY_VOICE_LANG] == "en") VoiceLanguage.EN else VoiceLanguage.ID,
            startCountdownSec = prefs[KEY_START_COUNTDOWN] ?: 5,
            segmentCountdownSec = prefs[KEY_SEGMENT_COUNTDOWN] ?: 3,
            cueDistanceM = prefs[KEY_CUE_DISTANCE] ?: 20,
            countdownStyle = if (prefs[KEY_COUNTDOWN_STYLE] == "voice") CountdownStyleSetting.VOICE else CountdownStyleSetting.BEEP,
            fastMarginSec = prefs[KEY_FAST_MARGIN] ?: 20,
            vibrate = prefs[KEY_VIBRATE] ?: true,
            autoPause = prefs[KEY_AUTO_PAUSE] ?: false,
            keepScreenOn = prefs[KEY_KEEP_SCREEN_ON] ?: false,
            kmMarker = prefs[KEY_KM_MARKER] ?: true,
            paceAlerts = prefs[KEY_PACE_ALERTS] ?: true,
            liveMap = prefs[KEY_LIVE_MAP] ?: false,
        )
    }

    suspend fun setTheme(theme: AppTheme) = dataStore.edit { it[KEY_THEME] = theme.name }
    suspend fun setVoiceLanguage(language: VoiceLanguage) = dataStore.edit { it[KEY_VOICE_LANG] = if (language == VoiceLanguage.EN) "en" else "id" }
    suspend fun setStartCountdown(sec: Int) = dataStore.edit { it[KEY_START_COUNTDOWN] = sec }
    suspend fun setSegmentCountdown(sec: Int) = dataStore.edit { it[KEY_SEGMENT_COUNTDOWN] = sec }
    suspend fun setCueDistance(m: Int) = dataStore.edit { it[KEY_CUE_DISTANCE] = m }
    suspend fun setCountdownStyle(style: CountdownStyleSetting) = dataStore.edit { it[KEY_COUNTDOWN_STYLE] = if (style == CountdownStyleSetting.VOICE) "voice" else "beep" }
    suspend fun setFastMargin(sec: Int) = dataStore.edit { it[KEY_FAST_MARGIN] = sec }
    suspend fun setVibrate(on: Boolean) = dataStore.edit { it[KEY_VIBRATE] = on }
    suspend fun setAutoPause(on: Boolean) = dataStore.edit { it[KEY_AUTO_PAUSE] = on }
    suspend fun setKeepScreenOn(on: Boolean) = dataStore.edit { it[KEY_KEEP_SCREEN_ON] = on }
    suspend fun setKmMarker(on: Boolean) = dataStore.edit { it[KEY_KM_MARKER] = on }
    suspend fun setPaceAlerts(on: Boolean) = dataStore.edit { it[KEY_PACE_ALERTS] = on }
    suspend fun setLiveMap(on: Boolean) = dataStore.edit { it[KEY_LIVE_MAP] = on }

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_VOICE_LANG = stringPreferencesKey("voice_lang")
        val KEY_START_COUNTDOWN = intPreferencesKey("start_countdown")
        val KEY_SEGMENT_COUNTDOWN = intPreferencesKey("segment_countdown")
        val KEY_CUE_DISTANCE = intPreferencesKey("cue_distance")
        val KEY_COUNTDOWN_STYLE = stringPreferencesKey("countdown_style")
        val KEY_FAST_MARGIN = intPreferencesKey("fast_margin")
        val KEY_VIBRATE = booleanPreferencesKey("vibrate")
        val KEY_AUTO_PAUSE = booleanPreferencesKey("auto_pause")
        val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val KEY_KM_MARKER = booleanPreferencesKey("km_marker")
        val KEY_PACE_ALERTS = booleanPreferencesKey("pace_alerts")
        val KEY_LIVE_MAP = booleanPreferencesKey("live_map")
    }
}
