package com.daniel.runcoach.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colour tokens (DESIGN.md section 3). Dynamic colour is off; every component reads these through
 * [LocalRcColors]. Material's `colorScheme` is only mapped from them so the few Material widgets we keep do
 * not fall back to the default purple.
 */
@Immutable
data class RcColors(
    val bg: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val outline: Color,
    val outlineStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val onAccent: Color,
    val accentText: Color,
    val accentContainer: Color,
    val scrim: Color,
    // Status tokens: only ever used for status, never for buttons.
    val pass: Color,
    val passContainer: Color,
    val fail: Color,
    val failContainer: Color,
    val onFail: Color,
    val warn: Color,
    val warnContainer: Color,
    val fast: Color,
    val fastContainer: Color,
    val neutral: Color,
    val neutralContainer: Color,
    // Segment kind tokens: strip and markers only, always with an icon + label.
    val kindRun: Color,
    val kindWalk: Color,
    val kindRecovery: Color,
    val kindWarmCool: Color,
)

val RcDarkColors = RcColors(
    bg = Color(0xFF0B0D0F),
    surface = Color(0xFF14181C),
    surfaceRaised = Color(0xFF1C2127),
    outline = Color(0xFF2A3139),
    outlineStrong = Color(0xFF3A444E),
    textPrimary = Color(0xFFF2F5F7),
    textSecondary = Color(0xFFA3ADB7),
    textTertiary = Color(0xFF7C8791),
    accent = Color(0xFFC6F432),
    onAccent = Color(0xFF0B0D0F),
    accentText = Color(0xFFC6F432),
    accentContainer = Color(0xFF2D371F),
    scrim = Color(0x99000000),
    pass = Color(0xFF34D399),
    passContainer = Color(0xFF19342F),
    fail = Color(0xFFFF5D5D),
    failContainer = Color(0xFF372222),
    onFail = Color(0xFF0B0D0F),
    warn = Color(0xFFFFB547),
    warnContainer = Color(0xFF373022),
    fast = Color(0xFF5BB5FF),
    fastContainer = Color(0xFF1F303E),
    neutral = Color(0xFFA3ADB7),
    neutralContainer = Color(0xFF232A31),
    kindRun = Color(0xFFC6F432),
    kindWalk = Color(0xFF8FA3B8),
    kindRecovery = Color(0xFF6FB7C4),
    kindWarmCool = Color(0xFFB8A6E8),
)

val RcLightColors = RcColors(
    bg = Color(0xFFF4F5F1),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFFFFFFF),
    outline = Color(0xFFE1E4DC),
    outlineStrong = Color(0xFFC5CABF),
    textPrimary = Color(0xFF0E1114),
    textSecondary = Color(0xFF4A545D),
    textTertiary = Color(0xFF66707A),
    accent = Color(0xFFB7E41C),
    onAccent = Color(0xFF0E1114),
    accentText = Color(0xFF4F6F00),
    accentContainer = Color(0xFFF1FAD2),
    scrim = Color(0x66000000),
    pass = Color(0xFF0B7A55),
    passContainer = Color(0xFFDDF4EA),
    fail = Color(0xFFC42B2B),
    failContainer = Color(0xFFFBE2E2),
    onFail = Color(0xFFFFFFFF),
    warn = Color(0xFF9A5B00),
    warnContainer = Color(0xFFFFEFD0),
    fast = Color(0xFF135FA8),
    fastContainer = Color(0xFFDCEBFA),
    neutral = Color(0xFF4A545D),
    neutralContainer = Color(0xFFECEEE8),
    kindRun = Color(0xFFA4D10F),
    kindWalk = Color(0xFF6F8499),
    kindRecovery = Color(0xFF3E93A3),
    kindWarmCool = Color(0xFF8C77D4),
)

val LocalRcColors = staticCompositionLocalOf { RcDarkColors }
