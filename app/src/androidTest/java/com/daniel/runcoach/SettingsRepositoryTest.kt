package com.daniel.runcoach

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.daniel.runcoach.data.AppTheme
import com.daniel.runcoach.data.SettingsRepository
import com.daniel.runcoach.engine.voice.VoiceLanguage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** P2.11 AC: settings persist (theme, voice language, countdown, cue distance). */
@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest {

    @Test
    fun settingsPersistAndReload() {
        runBlocking {
            val repository = SettingsRepository(InstrumentationRegistry.getInstrumentation().targetContext)
            repository.setTheme(AppTheme.DARK)
            repository.setVoiceLanguage(VoiceLanguage.EN)
            repository.setCueDistance(30)
            repository.setStartCountdown(10)

            val loaded = repository.settings.first()
            assertEquals(AppTheme.DARK, loaded.theme)
            assertEquals(VoiceLanguage.EN, loaded.voiceLanguage)
            assertEquals(30, loaded.cueDistanceM)
            assertEquals(10, loaded.startCountdownSec)

            // Restore defaults so other tests are unaffected.
            repository.setTheme(AppTheme.SYSTEM)
            repository.setVoiceLanguage(VoiceLanguage.ID)
            repository.setCueDistance(20)
            repository.setStartCountdown(5)
        }
    }

    /** P3.7 AC: the calibration factor persists and reset brings it back to 1.0. */
    @Test
    fun calibrationFactorPersistsAndResetReturnsOne() {
        runBlocking {
            val repository = SettingsRepository(InstrumentationRegistry.getInstrumentation().targetContext)
            repository.setCalibrationFactor(1.03)
            assertEquals(1.03, repository.settings.first().calibrationFactor, 1e-9)

            repository.reset()
            assertEquals(1.0, repository.settings.first().calibrationFactor, 1e-9)
        }
    }

    /** P3.8: the backup reminder interval persists. */
    @Test
    fun backupReminderIntervalPersists() {
        runBlocking {
            val repository = SettingsRepository(InstrumentationRegistry.getInstrumentation().targetContext)
            repository.setBackupReminderDays(14)
            assertEquals(14, repository.settings.first().backupReminderDays)
            repository.setBackupReminderDays(0)
            assertEquals(0, repository.settings.first().backupReminderDays)
        }
    }

    /** Onboarding is remembered so it does not reappear after the first run. */
    @Test
    fun onboardingDonePersists() {
        runBlocking {
            val repository = SettingsRepository(InstrumentationRegistry.getInstrumentation().targetContext)
            repository.setOnboardingDone(true)
            assertEquals(true, repository.settings.first().onboardingDone)
            repository.setOnboardingDone(false)
            assertEquals(false, repository.settings.first().onboardingDone)
        }
    }
}
