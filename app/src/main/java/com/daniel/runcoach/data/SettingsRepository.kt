package com.daniel.runcoach.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.daniel.runcoach.engine.voice.VoiceLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable
enum class AppTheme { SYSTEM, LIGHT, DARK }

@Serializable
enum class CountdownStyleSetting { BEEP, VOICE }

/** All user settings (plan S12). Kept in DataStore, not Room. */
@Serializable
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
    val lastBackupEpochMs: Long? = null,
    val calibrationFactor: Double = 1.0,
    val backupReminderDays: Int = 0,
    val historyMap: Boolean = false,
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
            lastBackupEpochMs = prefs[KEY_LAST_BACKUP],
            calibrationFactor = prefs[KEY_CALIBRATION] ?: 1.0,
            backupReminderDays = prefs[KEY_BACKUP_REMINDER] ?: 0,
            historyMap = prefs[KEY_HISTORY_MAP] ?: false,
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

    /** Restores every setting to its default (used by "delete all data"). */
    suspend fun reset() = dataStore.edit { it.clear() }

    suspend fun setLastBackup(epochMs: Long) = dataStore.edit { it[KEY_LAST_BACKUP] = epochMs }

    suspend fun setCalibrationFactor(factor: Double) = dataStore.edit { it[KEY_CALIBRATION] = factor }

    suspend fun setBackupReminderDays(days: Int) = dataStore.edit { it[KEY_BACKUP_REMINDER] = days }

    suspend fun setHistoryMap(on: Boolean) = dataStore.edit { it[KEY_HISTORY_MAP] = on }

    /** Writes every field at once (used by restore). */
    suspend fun setAll(s: AppSettings) = dataStore.edit { prefs ->
        prefs[KEY_THEME] = s.theme.name
        prefs[KEY_VOICE_LANG] = if (s.voiceLanguage == VoiceLanguage.EN) "en" else "id"
        prefs[KEY_START_COUNTDOWN] = s.startCountdownSec
        prefs[KEY_SEGMENT_COUNTDOWN] = s.segmentCountdownSec
        prefs[KEY_CUE_DISTANCE] = s.cueDistanceM
        prefs[KEY_COUNTDOWN_STYLE] = if (s.countdownStyle == CountdownStyleSetting.VOICE) "voice" else "beep"
        prefs[KEY_FAST_MARGIN] = s.fastMarginSec
        prefs[KEY_VIBRATE] = s.vibrate
        prefs[KEY_AUTO_PAUSE] = s.autoPause
        prefs[KEY_KEEP_SCREEN_ON] = s.keepScreenOn
        prefs[KEY_KM_MARKER] = s.kmMarker
        prefs[KEY_PACE_ALERTS] = s.paceAlerts
        prefs[KEY_LIVE_MAP] = s.liveMap
        prefs[KEY_CALIBRATION] = s.calibrationFactor
        prefs[KEY_BACKUP_REMINDER] = s.backupReminderDays
        prefs[KEY_HISTORY_MAP] = s.historyMap
        s.lastBackupEpochMs?.let { prefs[KEY_LAST_BACKUP] = it }
    }

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
        val KEY_LAST_BACKUP = longPreferencesKey("last_backup")
        val KEY_CALIBRATION = doublePreferencesKey("calibration_factor")
        val KEY_BACKUP_REMINDER = intPreferencesKey("backup_reminder_days")
        val KEY_HISTORY_MAP = booleanPreferencesKey("history_map")
    }
}
