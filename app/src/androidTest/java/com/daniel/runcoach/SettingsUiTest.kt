package com.daniel.runcoach

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.daniel.runcoach.ui.settings.SettingsScreen
import com.daniel.runcoach.ui.theme.RunCoachTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** P2.11 AC: S12 renders its sections and the voice pack status line. */
@RunWith(AndroidJUnit4::class)
class SettingsUiTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun showsSectionsAndVoicePackStatus() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        rule.setContent {
            RunCoachTheme {
                SettingsScreen(debugBuild = false, onOpenBatteryGuide = {}, onOpenData = {}, onOpenSimulator = {}, onSelectTab = {})
            }
        }
        rule.onNodeWithText(context.getString(R.string.s12_display).uppercase()).assertExists()
        rule.onNodeWithText(context.getString(R.string.s12_countdown).uppercase()).assertExists()
        rule.onNodeWithText(context.getString(R.string.s12_voice).uppercase()).assertExists()
    }
}

