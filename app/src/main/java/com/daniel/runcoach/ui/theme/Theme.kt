package com.daniel.runcoach.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

/**
 * Backwards-compatible alias for [RcTheme]. New code should call [RcTheme] directly; this stays so existing
 * previews and tests keep working while pages are re-skinned (DESIGN.md U0).
 */
@Composable
fun RunCoachTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    RcTheme(darkTheme = darkTheme, content = content)
}
