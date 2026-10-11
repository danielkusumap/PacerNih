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

/** Tabular figures, so pace/time/distance numbers never shift width while they change (DESIGN-RESKIN 1.2). */
private const val TABULAR = "tnum"

// Barlow Condensed (500/700/800) — bundled static weights (OFL). Numbers, titles, labels, buttons.
private fun barlow(resId: Int, weight: Int) = Font(resId, FontWeight(weight), FontStyle.Normal)

val RcCondensed: FontFamily = FontFamily(
    barlow(R.font.barlow_condensed_500, 500),
    barlow(R.font.barlow_condensed_700, 700),
    barlow(R.font.barlow_condensed_800, 800),
)

// Manrope (400/600/700) — bundled variable font (OFL). Body copy.
private fun manrope(weight: Int) = Font(
    resId = R.font.manrope_variable,
    weight = FontWeight(weight),
    style = FontStyle.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val RcSans: FontFamily = FontFamily(manrope(400), manrope(600), manrope(700))

/**
 * Typography tokens (DESIGN-RESKIN section 1.2). Numbers/titles/labels/buttons use Barlow Condensed
 * (labels/buttons/chips/tabs are uppercase), body copy uses Manrope. All numbers carry tabular figures.
 */
@Immutable
data class RcTypography(
    // Barlow Condensed display numbers
    val displayHero: TextStyle,
    val displayL: TextStyle,
    val displayM: TextStyle,
    val displayS: TextStyle,
    val numberXL: TextStyle,
    val numberL: TextStyle,
    val numberM: TextStyle,
    val numberS: TextStyle,
    val numberXS: TextStyle,
    // Barlow Condensed titles / labels / controls
    val titleXL: TextStyle,
    val titleL: TextStyle,
    val titleM: TextStyle,
    val titleDlg: TextStyle,
    val tab: TextStyle,
    val band: TextStyle,
    val button: TextStyle,
    val segmented: TextStyle,
    val chip: TextStyle,
    val label: TextStyle,
    val tabBar: TextStyle,
    // Manrope body copy
    val titleS: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val caption: TextStyle,
)

private fun number(size: Int, weight: Int = 800, spacingEm: Double = -0.01) = TextStyle(
    fontFamily = RcCondensed,
    fontSize = size.sp,
    fontWeight = FontWeight(weight),
    lineHeight = size.sp,
    letterSpacing = spacingEm.em,
    fontFeatureSettings = TABULAR,
)

private fun caps(size: Int, weight: Int, spacingEm: Double, lineHeight: Int) = TextStyle(
    fontFamily = RcCondensed,
    fontSize = size.sp,
    fontWeight = FontWeight(weight),
    lineHeight = lineHeight.sp,
    letterSpacing = spacingEm.em,
)

private fun sans(size: Int, weight: Int, lineHeight: Int) = TextStyle(
    fontFamily = RcSans,
    fontSize = size.sp,
    fontWeight = FontWeight(weight),
    lineHeight = lineHeight.sp,
)

val RcType = RcTypography(
    displayHero = number(150),
    displayL = number(96),
    displayM = number(72),
    displayS = number(56),
    numberXL = number(40),
    numberL = number(34),
    numberM = number(30),
    numberS = number(24),
    numberXS = number(22),
    titleXL = number(44),
    titleL = number(34),
    titleM = number(28),
    titleDlg = number(26),
    tab = caps(18, 800, 0.1, 22),
    band = caps(20, 800, 0.1, 24),
    button = caps(17, 800, 0.12, 20),
    segmented = caps(14, 700, 0.1, 18),
    chip = caps(12, 700, 0.1, 14),
    label = caps(12, 700, 0.14, 14),
    tabBar = caps(11, 700, 0.1, 13),
    titleS = sans(16, 700, 22),
    body = sans(14, 400, 20),
    bodySmall = sans(13, 400, 18),
    caption = sans(12, 400, 16),
)

val LocalRcTypography = staticCompositionLocalOf { RcType }
