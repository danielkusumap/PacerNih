package com.daniel.runcoach.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.daniel.runcoach.R
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.LocalRcTypography
import com.daniel.runcoach.ui.theme.RcIcons
import kotlinx.coroutines.launch

/** EmptyState: centred neutral circle + title + one line + optional action (DESIGN.md section 6). */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: Int = RcIcons.DirectionsRun,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(colors.neutralContainer), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(32.dp))
        }
        Text(title, style = type.titleM, color = colors.textPrimary, textAlign = TextAlign.Center)
        Text(message, style = type.body, color = colors.textSecondary, textAlign = TextAlign.Center)
        action?.invoke()
    }
}

/** ErrorState: like EmptyState with an error icon and a retry action (DESIGN.md section 6). */
@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(colors.failContainer), contentAlignment = Alignment.Center) {
            Icon(painterResource(RcIcons.Error), contentDescription = null, tint = colors.fail, modifier = Modifier.size(32.dp))
        }
        Text(message, style = type.body, color = colors.textSecondary, textAlign = TextAlign.Center)
        if (onRetry != null) {
            RcButton(text = stringResource(R.string.rc_retry), onClick = onRetry, variant = RcButtonVariant.Secondary)
        }
    }
}

/** Skeleton: shimmering surfaceRaised block matching the element it replaces (DESIGN.md section 6). */
@Composable
fun Skeleton(modifier: Modifier = Modifier, radius: androidx.compose.ui.unit.Dp = 20.dp) {
    val colors = LocalRcColors.current
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    Box(modifier = modifier.background(colors.surfaceRaised.copy(alpha = alpha), RoundedCornerShape(radius)))
}

/** Skeleton for a list of cards. */
@Composable
fun SkeletonCards(count: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count) { Skeleton(modifier = Modifier.fillMaxWidth().height(88.dp)) }
    }
}

/**
 * ProgressRing: hold-to-confirm ring around a control. The ring fills clockwise in [holdMs]; releasing before
 * it is full cancels (DESIGN.md section 6, D05 stop).
 */
@Composable
fun ProgressRing(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    holdMs: Int = 2000,
    content: @Composable () -> Unit,
) {
    val colors = LocalRcColors.current
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    var confirmed = false
                    val job = scope.launch {
                        progress.animateTo(1f, tween(holdMs, easing = LinearEasing))
                        if (!confirmed) {
                            confirmed = true
                            onConfirm()
                        }
                    }
                    val released = tryAwaitRelease()
                    if (!confirmed && released && progress.value >= 1f) {
                        confirmed = true
                        onConfirm()
                    }
                    job.cancel()
                    scope.launch { progress.animateTo(0f, tween(150)) }
                },
            )
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(72.dp)) {
            val stroke = 3.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = colors.fail,
                startAngle = -90f,
                sweepAngle = 360f * progress.value,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        content()
    }
}
