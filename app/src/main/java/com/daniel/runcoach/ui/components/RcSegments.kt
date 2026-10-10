package com.daniel.runcoach.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.daniel.runcoach.engine.model.SegmentKind
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.RcIcons

@Composable
fun segmentKindColor(kind: SegmentKind): Color = LocalRcColors.current.let {
    when (kind) {
        SegmentKind.RUN -> it.kindRun
        SegmentKind.WALK -> it.kindWalk
        SegmentKind.RECOVERY -> it.kindRecovery
        SegmentKind.WARMUP, SegmentKind.COOLDOWN -> it.kindWarmCool
    }
}

fun segmentKindIcon(kind: SegmentKind): Int = when (kind) {
    SegmentKind.RUN -> RcIcons.DirectionsRun
    SegmentKind.WALK -> RcIcons.DirectionsWalk
    SegmentKind.RECOVERY -> RcIcons.SelfImprovement
    SegmentKind.WARMUP -> RcIcons.Whatshot
    SegmentKind.COOLDOWN -> RcIcons.AcUnit
}

/** SegmentProgressBar: 10dp track, fill in the segment kind colour (DESIGN.md section 6). */
@Composable
fun SegmentProgressBar(progress: Float, kind: SegmentKind, modifier: Modifier = Modifier) {
    val colors = LocalRcColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .background(colors.outline, RoundedCornerShape(5.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(10.dp)
                .background(segmentKindColor(kind), RoundedCornerShape(5.dp)),
        )
    }
}

/** SegmentStrip: thin proportional blocks coloured by segment kind (DESIGN.md section 6). */
@Composable
fun SegmentStrip(
    kinds: List<SegmentKind>,
    modifier: Modifier = Modifier,
    weights: List<Float>? = null,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        kinds.forEachIndexed { index, kind ->
            val weight = weights?.getOrNull(index)?.coerceAtLeast(0.05f) ?: 1f
            Box(
                modifier = Modifier
                    .weight(weight)
                    .height(8.dp)
                    .background(segmentKindColor(kind), RoundedCornerShape(3.dp)),
            )
        }
    }
}

/** ResultStrip: thin proportional blocks coloured by segment result status (DESIGN.md section 6). */
@Composable
fun ResultStrip(
    statuses: List<RcStatus>,
    modifier: Modifier = Modifier,
    weights: List<Float>? = null,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        statuses.forEachIndexed { index, status ->
            val weight = weights?.getOrNull(index)?.coerceAtLeast(0.05f) ?: 1f
            Box(
                modifier = Modifier
                    .weight(weight)
                    .height(8.dp)
                    .background(status.onColor(), RoundedCornerShape(3.dp)),
            )
        }
    }
}

/** Fixed-width helper for the minimum block width rule. */
@Composable
fun SegmentStripMinSpacer(modifier: Modifier = Modifier) {
    Box(modifier.width(6.dp).height(8.dp))
}
