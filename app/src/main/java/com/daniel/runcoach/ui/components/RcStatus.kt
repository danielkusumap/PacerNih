package com.daniel.runcoach.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.LocalRcShapes
import com.daniel.runcoach.ui.theme.LocalRcTypography
import com.daniel.runcoach.ui.theme.RcIcons

/** The five status meanings (DESIGN.md section 3.2). Never used for buttons. */
enum class RcStatus { Pass, Fail, Warn, Fast, Neutral }

@Composable
fun RcStatus.onColor(): Color = LocalRcColors.current.let {
    when (this) {
        RcStatus.Pass -> it.pass
        RcStatus.Fail -> it.fail
        RcStatus.Warn -> it.warn
        RcStatus.Fast -> it.fast
        RcStatus.Neutral -> it.neutral
    }
}

@Composable
fun RcStatus.containerColor(): Color = LocalRcColors.current.let {
    when (this) {
        RcStatus.Pass -> it.passContainer
        RcStatus.Fail -> it.failContainer
        RcStatus.Warn -> it.warnContainer
        RcStatus.Fast -> it.fastContainer
        RcStatus.Neutral -> it.neutralContainer
    }
}

fun RcStatus.icon(): Int? = when (this) {
    RcStatus.Pass -> RcIcons.CheckCircle
    RcStatus.Fail -> RcIcons.Cancel
    RcStatus.Warn -> RcIcons.Warning
    RcStatus.Fast -> RcIcons.TrendingUp
    RcStatus.Neutral -> null
}

/** StatusBadge: live status pill, 44dp, icon + label + optional tabular delta (DESIGN.md section 6). */
@Composable
fun StatusBadge(
    status: RcStatus,
    label: String,
    modifier: Modifier = Modifier,
    delta: String? = null,
) {
    val type = LocalRcTypography.current
    val shapes = LocalRcShapes.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp)
            .background(status.containerColor(), RoundedCornerShape(shapes.radiusM))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        status.icon()?.let {
            Icon(painterResource(it), contentDescription = null, tint = status.onColor(), modifier = Modifier.size(24.dp))
        }
        Text(
            label.uppercase(),
            style = type.titleS.copy(fontWeight = FontWeight.Bold),
            color = status.onColor(),
            modifier = Modifier.weight(1f),
        )
        if (delta != null) Text(delta, style = type.titleS, color = status.onColor())
    }
}

/** ResultChip: segment/km result, 28dp (DESIGN.md section 6). */
@Composable
fun ResultChip(status: RcStatus, label: String, modifier: Modifier = Modifier) {
    val type = LocalRcTypography.current
    val shapes = LocalRcShapes.current
    val dashed = status == RcStatus.Neutral && label.equals("DATA KURANG", ignoreCase = true)
    Row(
        modifier = modifier
            .background(status.containerColor(), RoundedCornerShape(shapes.radiusS))
            .then(
                if (status == RcStatus.Neutral) {
                    Modifier.border(1.dp, LocalRcColors.current.outlineStrong, RoundedCornerShape(shapes.radiusS))
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (!dashed) {
            status.icon()?.let {
                Icon(painterResource(it), contentDescription = null, tint = status.onColor(), modifier = Modifier.size(16.dp))
            }
        }
        Text(label.uppercase(), style = type.label, color = status.onColor())
    }
}

/** Banner variants (DESIGN.md section 6). */
enum class RcBannerVariant { Info, Warning, Success, Accent }

@Composable
fun Banner(
    text: String,
    modifier: Modifier = Modifier,
    variant: RcBannerVariant = RcBannerVariant.Info,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    val shapes = LocalRcShapes.current
    val (container, content, icon) = when (variant) {
        RcBannerVariant.Info -> Triple(colors.fastContainer, colors.fast, RcIcons.Info)
        RcBannerVariant.Warning -> Triple(colors.warnContainer, colors.warn, RcIcons.Warning)
        RcBannerVariant.Success -> Triple(colors.passContainer, colors.pass, RcIcons.CheckCircle)
        RcBannerVariant.Accent -> Triple(colors.accentContainer, colors.accentText, RcIcons.Info)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .background(container, RoundedCornerShape(shapes.radiusM))
            .then(if (variant == RcBannerVariant.Accent) Modifier.border(1.dp, colors.accent, RoundedCornerShape(shapes.radiusM)) else Modifier)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(24.dp))
        Text(text, style = type.bodySmall.copy(fontWeight = FontWeight.Bold), color = content, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(action, style = type.titleS, color = content, modifier = Modifier.clickable(onClick = onAction))
        }
    }
}
