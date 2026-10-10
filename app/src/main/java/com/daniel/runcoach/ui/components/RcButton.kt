package com.daniel.runcoach.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.LocalRcShapes
import com.daniel.runcoach.ui.theme.LocalRcSpacing
import com.daniel.runcoach.ui.theme.LocalRcTypography

/** RcButton variants (DESIGN.md section 6). */
enum class RcButtonVariant { Primary, Secondary, Tertiary, Destructive, DestructiveFilled }

enum class RcButtonSize { Regular, Small }

/** Reusable button. Height 56dp (Small 44dp), radius 14, no Material pill. */
@Composable
fun RcButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: RcButtonVariant = RcButtonVariant.Primary,
    size: RcButtonSize = RcButtonSize.Regular,
    icon: Int? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    fillWidth: Boolean = false,
) {
    val colors = LocalRcColors.current
    val shapes = LocalRcShapes.current
    val type = LocalRcTypography.current
    val spacing = LocalRcSpacing.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val height = if (size == RcButtonSize.Small) 44.dp else 56.dp
    val (container, border, content) = when (variant) {
        RcButtonVariant.Primary -> Triple(colors.accent, null, colors.onAccent)
        RcButtonVariant.Secondary -> Triple(Color.Transparent, BorderStroke(1.5.dp, colors.outlineStrong), colors.textPrimary)
        RcButtonVariant.Tertiary -> Triple(Color.Transparent, null, colors.accentText)
        RcButtonVariant.Destructive -> Triple(Color.Transparent, null, colors.fail)
        RcButtonVariant.DestructiveFilled -> Triple(colors.fail, null, colors.onFail)
    }
    val pressOverlay = if (pressed) 0.10f else 0f

    Box(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .height(height)
            .alpha(if (enabled) 1f else 0.38f)
            .background(container, RoundedCornerShape(shapes.radiusM))
            .then(if (border != null) Modifier.border(border, RoundedCornerShape(shapes.radiusM)) else Modifier)
            .then(if (pressed && container != Color.Transparent) Modifier.background(Color.Black.copy(alpha = pressOverlay), RoundedCornerShape(shapes.radiusM)) else Modifier)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = spacing.s4),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = content, strokeWidth = 2.dp)
            } else if (icon != null) {
                Icon(painter = painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
            }
            Text(
                text = text,
                color = content,
                style = if (variant == RcButtonVariant.Primary) type.titleS.copy(fontWeight = FontWeight.Bold) else type.titleS,
            )
        }
    }
}

/** RcIconButton: 48dp touch area, 24dp icon (DESIGN.md section 6). */
enum class RcIconButtonVariant { Plain, Filled, Accent }

@Composable
fun RcIconButton(
    icon: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: RcIconButtonVariant = RcIconButtonVariant.Plain,
    tint: Color? = null,
    iconSize: Int = 24,
    enabled: Boolean = true,
) {
    val colors = LocalRcColors.current
    val container = when (variant) {
        RcIconButtonVariant.Plain -> Color.Transparent
        RcIconButtonVariant.Filled -> colors.surfaceRaised
        RcIconButtonVariant.Accent -> colors.accent
    }
    val iconTint = tint ?: when (variant) {
        RcIconButtonVariant.Plain -> colors.textSecondary
        RcIconButtonVariant.Filled -> colors.textPrimary
        RcIconButtonVariant.Accent -> colors.onAccent
    }
    Box(
        modifier = modifier
            .size(if (variant == RcIconButtonVariant.Plain) 48.dp else 56.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .background(container, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(iconSize.dp),
        )
    }
}
