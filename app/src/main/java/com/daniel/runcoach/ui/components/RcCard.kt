package com.daniel.runcoach.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.LocalRcShapes
import com.daniel.runcoach.ui.theme.LocalRcSpacing
import com.daniel.runcoach.ui.theme.LocalRcTypography
import com.daniel.runcoach.ui.theme.RcIcons

/** RcCard: surface, radius 20, padding 16, 1dp outline border (DESIGN.md section 6). */
@Composable
fun RcCard(
    modifier: Modifier = Modifier,
    interactive: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = LocalRcColors.current
    val shapes = LocalRcShapes.current
    val spacing = LocalRcSpacing.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val clickable = onClick != null
    val shape = RoundedCornerShape(shapes.radiusL)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, shape)
            .border(1.dp, colors.outline, shape)
            .then(
                if (pressed && (interactive || clickable)) {
                    Modifier.background(Color.White.copy(alpha = 0.06f), shape)
                } else {
                    Modifier
                },
            )
            .then(
                if (clickable) {
                    Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick!!)
                } else {
                    Modifier
                },
            )
            .padding(spacing.cardPad),
    ) {
        content()
    }
}

/** SectionHeader: uppercase label with optional trailing text action (DESIGN.md section 6). */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    val spacing = LocalRcSpacing.current
    Row(
        modifier = modifier.fillMaxWidth().padding(top = spacing.s5, bottom = spacing.s2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title.uppercase(), style = type.label, color = colors.textSecondary)
        if (action != null && onAction != null) {
            Text(action, style = type.titleS, color = colors.accentText, modifier = Modifier.clickable(onClick = onAction))
        }
    }
}

/** ListRow: min 56dp, content left, trailing slot right (DESIGN.md section 6). */
@Composable
fun ListRow(
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    val spacing = LocalRcSpacing.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = spacing.s4, vertical = spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s3),
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = type.titleS, color = colors.textPrimary)
            if (subtitle != null) Text(subtitle, style = type.bodySmall, color = colors.textSecondary)
        }
        trailing?.invoke()
    }
}

/** Thin inset divider for lists (DESIGN.md section 6). */
@Composable
fun RcDivider(modifier: Modifier = Modifier, inset: androidx.compose.ui.unit.Dp = 20.dp) {
    val colors = LocalRcColors.current
    Box(modifier = modifier.fillMaxWidth().padding(start = inset).height(1.dp).background(colors.outline))
}

/** InfoIcon (ⓘ): 20dp info icon, 48dp touch area (DESIGN.md section 6). */
@Composable
fun InfoIcon(topic: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalRcColors.current
    Box(modifier = modifier.size(48.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(
            painter = painterResource(RcIcons.Info),
            contentDescription = "Info $topic",
            tint = colors.textSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}
