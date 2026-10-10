package com.daniel.runcoach.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.LocalRcTypography
import com.daniel.runcoach.ui.Formatters

/** MetricBlock: small label above, big value below, optional unit beside the value (DESIGN.md section 6). */
@Composable
fun MetricBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    valueStyle: TextStyle? = null,
    valueColor: androidx.compose.ui.graphics.Color? = null,
) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (label.isNotEmpty()) Text(label.uppercase(), style = type.label, color = colors.textSecondary)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                value,
                style = valueStyle ?: type.numberM,
                color = valueColor ?: colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (unit != null) Text(unit, style = type.bodySmall, color = colors.textSecondary)
        }
    }
}

/**
 * HeroPace: the live pace hero (DESIGN.md section 6). Centred, `displayHero`, autosized down to 72sp so a wide
 * value never wraps; `--:--` in textTertiary when there is no pace.
 */
@Composable
fun HeroPace(paceSecPerKm: Double?, modifier: Modifier = Modifier) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val text = Formatters.pace(paceSecPerKm)
    val missing = paceSecPerKm == null || paceSecPerKm <= 0.0

    BoxWithConstraints(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val availablePx = with(density) { maxWidth.toPx() }
        val sizeSp = remember(text, availablePx) {
            listOf(96, 90, 84, 78, 72).firstOrNull { sp ->
                measurer.measure(text, style = type.displayHero.copy(fontSize = sp.sp)).size.width <= availablePx
            } ?: 72
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text,
                style = type.displayHero.copy(fontSize = sizeSp.sp),
                color = if (missing) colors.textTertiary else colors.textPrimary,
                maxLines = 1,
            )
            Text("/km", style = type.titleS, color = colors.textSecondary)
        }
    }
}
