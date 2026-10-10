package com.daniel.runcoach

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.daniel.runcoach.data.SettingsRepository
import com.daniel.runcoach.ui.settings.BackupScreen
import com.daniel.runcoach.ui.theme.RunCoachTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** P3.8 AC: the reminder banner appears once the interval elapsed and stays hidden when the reminder is off. */
@RunWith(AndroidJUnit4::class)
class BackupUiTest {

    @get:Rule
    val rule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val day = 24L * 60 * 60 * 1000

    @Test
    fun bannerShownWhenIntervalElapsed() {
        val settings = SettingsRepository(context)
        runBlocking {
            settings.setBackupReminderDays(14)
            settings.setLastBackup(System.currentTimeMillis() - 15 * day)
        }
        rule.setContent { RunCoachTheme { BackupScreen(onBack = {}) } }

        val due = context.getString(R.string.s13_backup_due)
        rule.waitUntil(5_000) { rule.onAllNodesWithText(due).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText(due).assertExists()

        runBlocking { settings.setBackupReminderDays(0) }
    }

    @Test
    fun bannerHiddenWhenReminderOff() {
        val settings = SettingsRepository(context)
        runBlocking {
            settings.setBackupReminderDays(0)
            settings.setLastBackup(System.currentTimeMillis() - 100 * day)
        }
        rule.setContent { RunCoachTheme { BackupScreen(onBack = {}) } }

        // Wait for the screen to load, then confirm the banner is absent.
        val title = context.getString(R.string.s13_title)
        rule.waitUntil(5_000) { rule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText(context.getString(R.string.s13_backup_due)).assertDoesNotExist()
    }
}
