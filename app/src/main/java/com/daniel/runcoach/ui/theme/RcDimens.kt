package com.daniel.runcoach.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** Spacing on a 4dp grid (DESIGN.md section 5.1). */
@Immutable
data class RcSpacing(
    val s1: Dp = 4.dp,
    val s2: Dp = 8.dp,
    val s3: Dp = 12.dp,
    val s4: Dp = 16.dp,
    val s5: Dp = 24.dp,
    val s6: Dp = 32.dp,
    val s7: Dp = 48.dp,
    /** Screen horizontal padding: 20dp, or 16dp on widths below 360dp. */
    val screenH: Dp = 20.dp,
    val cardGap: Dp = 12.dp,
    val groupGap: Dp = 24.dp,
    val cardPad: Dp = 16.dp,
    val minTouch: Dp = 48.dp,
)

/** Corner radii (DESIGN.md section 5.2). */
@Immutable
data class RcShapes(
    val radiusS: Dp = 8.dp,
    val radiusM: Dp = 14.dp,
    val radiusL: Dp = 20.dp,
    val radiusXL: Dp = 28.dp,
)

/** Motion tokens (DESIGN.md section 5.5). */
@Immutable
data class RcMotion(
    val standardMs: Int = 180,
    val sheetMs: Int = 240,
    val crossfadeMs: Int = 150,
    val easing: Easing = CubicBezierEasing(0f, 0f, 0.2f, 1f),
) {
    val standard: Duration get() = standardMs.milliseconds
    val sheet: Duration get() = sheetMs.milliseconds
}

val LocalRcSpacing = staticCompositionLocalOf { RcSpacing() }
val LocalRcShapes = staticCompositionLocalOf { RcShapes() }
val LocalRcMotion = staticCompositionLocalOf { RcMotion() }
