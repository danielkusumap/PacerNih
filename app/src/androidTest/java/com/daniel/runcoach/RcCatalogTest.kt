package com.daniel.runcoach

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.daniel.runcoach.ui.components.RcCatalogScreen
import com.daniel.runcoach.ui.theme.RcTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** U1 AC: the debug catalogue renders the reusable components. */
@RunWith(AndroidJUnit4::class)
class RcCatalogTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun catalogShowsComponents() {
        rule.setContent { RcTheme { RcCatalogScreen(onBack = {}) } }
        rule.onNodeWithText("Catalog").assertExists()
        rule.onNodeWithText("BUTTONS").assertExists()
        rule.onNodeWithText("PRIMARY").assertExists()
        rule.onNodeWithText("STEADY").assertExists()
        rule.onNodeWithText("METRICS").assertExists()
        rule.onNodeWithText("CONTROLS").assertExists()
    }
}
