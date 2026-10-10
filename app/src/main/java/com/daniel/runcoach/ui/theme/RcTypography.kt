package com.daniel.runcoach.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.daniel.runcoach.R

/** Tabular figures, so pace/time/distance numbers never shift width while they change (DESIGN.md section 4). */
private const val TABULAR = "tnum"

private fun inter(weight: Int): Font = Font(
    resId = R.font.inter_variable,
    weight = FontWeight(weight),
    style = FontStyle.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Inter, bundled in `res/font` (OFL). No runtime download. */
val RcFontFamily: FontFamily = FontFamily(
    inter(400),
    inter(500),
    inter(600),
    inter(700),
    inter(800),
)

/** Typography tokens (DESIGN.md section 4). All styles carry tabular figures. */
@Immutable
data class RcTypography(
    val displayHero: TextStyle,
    val displayL: TextStyle,
    val numberXL: TextStyle,
    val numberL: TextStyle,
    val numberM: TextStyle,
    val titleL: TextStyle,
    val titleM: TextStyle,
    val titleS: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
)

private fun style(size: Int, weight: Int, lineHeight: Int, spacingEm: Double = 0.0) = TextStyle(
    fontFamily = RcFontFamily,
    fontSize = size.sp,
    fontWeight = FontWeight(weight),
    lineHeight = lineHeight.sp,
    letterSpacing = spacingEm.em,
    fontFeatureSettings = TABULAR,
)

val RcType = RcTypography(
    displayHero = style(96, 800, 100, -0.02),
    displayL = style(56, 700, 60),
    numberXL = style(40, 700, 44),
    numberL = style(32, 700, 36),
    numberM = style(24, 600, 28),
    titleL = style(28, 700, 34),
    titleM = style(20, 600, 26),
    titleS = style(16, 600, 22),
    body = style(16, 400, 24),
    bodySmall = style(14, 400, 20),
    label = style(12, 600, 16, 0.06),
    caption = style(12, 400, 16),
)

val LocalRcTypography = staticCompositionLocalOf { RcType }
