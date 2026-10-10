package com.daniel.runcoach.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * U0 acceptance: every text/background and status-icon/background pair meets the contrast gate from
 * DESIGN.md section 3.4 (normal text >= 4.5:1, large text and icons >= 3:1). Decorative segment-kind colours
 * are exempt because they are never the only cue (always accompanied by an icon and a label).
 */
class ContrastTest {

    private data class Pair(val label: String, val fg: Color, val bg: Color, val min: Double)

    private fun textPairs(c: RcColors): List<Pair> = listOf(
        Pair("textPrimary/bg", c.textPrimary, c.bg, 4.5),
        Pair("textPrimary/surface", c.textPrimary, c.surface, 4.5),
        Pair("textPrimary/surfaceRaised", c.textPrimary, c.surfaceRaised, 4.5),
        Pair("textSecondary/bg", c.textSecondary, c.bg, 4.5),
        Pair("textSecondary/surface", c.textSecondary, c.surface, 4.5),
        Pair("textTertiary/bg", c.textTertiary, c.bg, 4.5),
        Pair("textTertiary/surface", c.textTertiary, c.surface, 4.5),
        Pair("accentText/bg", c.accentText, c.bg, 4.5),
        Pair("accentText/surface", c.accentText, c.surface, 4.5),
        Pair("onAccent/accent", c.onAccent, c.accent, 4.5),
        Pair("onFail/fail", c.onFail, c.fail, 4.5),
        Pair("pass/passContainer", c.pass, c.passContainer, 4.5),
        Pair("fail/failContainer", c.fail, c.failContainer, 4.5),
        Pair("warn/warnContainer", c.warn, c.warnContainer, 4.5),
        Pair("fast/fastContainer", c.fast, c.fastContainer, 4.5),
        Pair("neutral/neutralContainer", c.neutral, c.neutralContainer, 4.5),
        Pair("accentText/accentContainer", c.accentText, c.accentContainer, 4.5),
    )

    private fun iconPairs(c: RcColors): List<Pair> = listOf(
        Pair("pass/bg", c.pass, c.bg, 3.0),
        Pair("fail/bg", c.fail, c.bg, 3.0),
        Pair("warn/bg", c.warn, c.bg, 3.0),
        Pair("fast/bg", c.fast, c.bg, 3.0),
        Pair("accentText/surface", c.accentText, c.surface, 3.0),
    )

    @Test
    fun darkThemeMeetsContrast() = assertAll(RcDarkColors)

    @Test
    fun lightThemeMeetsContrast() = assertAll(RcLightColors)

    private fun assertAll(c: RcColors) {
        val failures = (textPairs(c) + iconPairs(c)).mapNotNull { pair ->
            val r = ratio(pair.fg, pair.bg)
            if (r + 1e-6 < pair.min) "%.2f < %.1f  %s".format(r, pair.min, pair.label) else null
        }
        assertTrue("contrast failures:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    private fun ratio(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun luminance(c: Color): Double {
        fun channel(v: Float): Double {
            val x = v.toDouble()
            return if (x <= 0.03928) x / 12.92 else ((x + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
    }
}
