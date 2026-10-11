package com.daniel.runcoach

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.RcDarkColors
import com.daniel.runcoach.ui.theme.RcLightColors
import com.daniel.runcoach.ui.theme.RcSans
import com.daniel.runcoach.ui.theme.RcTheme
import com.daniel.runcoach.ui.theme.RcType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** U0 AC: RcTheme provides the right tokens in both themes, maps Material's palette to the tokens (no purple)
 * and the Manrope family is the one bundled in res/font (DESIGN-RESKIN 1.2). */
@RunWith(AndroidJUnit4::class)
class RcThemeTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun darkThemeProvidesDarkTokens() = assertTokens(dark = true, expected = RcDarkColors)

    @Test
    fun lightThemeProvidesLightTokens() = assertTokens(dark = false, expected = RcLightColors)

    private fun assertTokens(dark: Boolean, expected: com.daniel.runcoach.ui.theme.RcColors) {
        var captured: com.daniel.runcoach.ui.theme.RcColors? = null
        var materialPrimary = expected.accent
        rule.setContent { Probe(dark) { colors, primary -> captured = colors; materialPrimary = primary } }
        rule.waitForIdle()
        assertEquals(expected, captured)
        assertEquals("Material primary must be the token accent", expected.accent, materialPrimary)
        assertNotEquals("no default purple", androidx.compose.ui.graphics.Color(0xFF6650a4), materialPrimary)
        assertEquals(RcSans, RcType.body.fontFamily)
    }

    @Composable
    private fun Probe(
        dark: Boolean,
        onRead: (com.daniel.runcoach.ui.theme.RcColors, androidx.compose.ui.graphics.Color) -> Unit,
    ) {
        RcTheme(darkTheme = dark) {
            onRead(LocalRcColors.current, MaterialTheme.colorScheme.primary)
        }
    }
}
