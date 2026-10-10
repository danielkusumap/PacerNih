package com.daniel.runcoach.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

private fun rcColorScheme(c: RcColors, dark: Boolean) =
    (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = c.accent,
        onPrimary = c.onAccent,
        primaryContainer = c.accentContainer,
        onPrimaryContainer = c.accentText,
        secondary = c.textSecondary,
        onSecondary = c.bg,
        background = c.bg,
        onBackground = c.textPrimary,
        surface = c.surface,
        onSurface = c.textPrimary,
        surfaceVariant = c.surfaceRaised,
        onSurfaceVariant = c.textSecondary,
        outline = c.outline,
        outlineVariant = c.outlineStrong,
        error = c.fail,
        onError = c.onFail,
        errorContainer = c.failContainer,
        onErrorContainer = c.fail,
        scrim = c.scrim,
    )

private fun rcMaterialTypography(t: RcTypography) = Typography(
    displayLarge = t.displayHero,
    displayMedium = t.displayL,
    headlineSmall = t.titleL,
    titleLarge = t.titleL,
    titleMedium = t.titleM,
    titleSmall = t.titleS,
    bodyLarge = t.body,
    bodyMedium = t.body,
    bodySmall = t.bodySmall,
    labelLarge = t.titleS,
    labelMedium = t.label,
    labelSmall = t.caption,
)

/**
 * Root theme (DESIGN.md section 12.1, U0). Provides the colour, type, spacing, shape and motion tokens and
 * maps them onto `MaterialTheme` so any Material widget we keep does not fall back to the default palette.
 * Dynamic colour stays off.
 */
@Composable
fun RcTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) RcDarkColors else RcLightColors
    val widthDp = LocalConfiguration.current.screenWidthDp
    val spacing = RcSpacing(screenH = if (widthDp < 360) 16.dp else 20.dp)
    CompositionLocalProvider(
        LocalRcColors provides colors,
        LocalRcTypography provides RcType,
        LocalRcSpacing provides spacing,
        LocalRcShapes provides RcShapes(),
        LocalRcMotion provides RcMotion(),
    ) {
        MaterialTheme(
            colorScheme = rcColorScheme(colors, darkTheme),
            typography = rcMaterialTypography(RcType),
            content = content,
        )
    }
}
