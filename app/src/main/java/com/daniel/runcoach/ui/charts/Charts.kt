package com.daniel.runcoach.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.daniel.runcoach.engine.chart.ChartMath
import com.daniel.runcoach.ui.components.RcStatus
import com.daniel.runcoach.ui.components.onColor
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.LocalRcTypography
import kotlin.math.ceil
import kotlin.math.max

/** One bar: a nullable value (null = missing) with an optional target and status colour. */
data class Bar(
    val label: String,
    val value: Double?,
    val target: Double? = null,
    val status: RcStatus? = null,
    val weight: Float = 1f,
    val partial: Boolean = false,
)

/** One line point: nullable y (null = gap in the line). */
data class LinePoint(val x: Double, val y: Double?)

/** A stair-step target over an x-range (per segment). */
data class TargetStep(val startX: Double, val endX: Double, val target: Double)

private val PlotLeft = 36.dp
private val PlotBottom = 24.dp
private val PlotTop = 8.dp
private val PlotRight = 8.dp

/**
 * Generic bar chart (DESIGN 6.1): transparent background, up to 4 horizontal guides, 60% slots with rounded
 * tops, accent for the highlighted bar and accent at 35% for the rest, a thin outline line for zero values.
 */
@Composable
fun ChartBar(
    bars: List<Bar>,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    highlightIndex: Int? = null,
    minMax: Double = 1.0,
    valueFormatter: (Double) -> String = { "%.1f".format(it) },
    showValues: Boolean = true,
    onTap: (Int) -> Unit = {},
) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    val measurer = rememberTextMeasurer()
    var selected by remember { mutableStateOf(highlightIndex) }
    val statusOn = RcStatus.entries.associateWith { it.onColor() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(bars) {
                detectTapGestures { offset ->
                    if (bars.isEmpty()) return@detectTapGestures
                    val left = PlotLeft.toPx()
                    val right = size.width - PlotRight.toPx()
                    val slot = (right - left) / bars.size
                    val index = ((offset.x - left) / slot).toInt().coerceIn(0, bars.size - 1)
                    selected = index
                    onTap(index)
                }
            },
    ) {
        val left = PlotLeft.toPx()
        val bottom = size.height - PlotBottom.toPx()
        val top = PlotTop.toPx()
        val right = size.width - PlotRight.toPx()
        if (bars.isEmpty() || right <= left) return@Canvas

        val rawMax = (bars.mapNotNull { it.value } + bars.mapNotNull { it.target }).maxOrNull() ?: minMax
        val scaleMax = niceMax(max(rawMax, minMax))
        val ticks = listOf(0.0, scaleMax / 2.0, scaleMax)
        ticks.forEach { tick ->
            val y = bottom - (tick / scaleMax * (bottom - top)).toFloat()
            drawLine(colors.outline, Offset(left, y), Offset(right, y), strokeWidth = 1f)
            drawText(measurer, formatTick(tick), 0f, y - 8f, colors.textTertiary, type)
        }

        val slot = (right - left) / bars.size
        bars.forEachIndexed { index, bar ->
            val cx = left + slot * index + slot / 2f
            val barWidth = slot * 0.6f
            val value = bar.value
            if (value == null || value == 0.0) {
                drawLine(colors.outline, Offset(cx - barWidth / 2f, bottom), Offset(cx + barWidth / 2f, bottom), strokeWidth = 2f)
            } else {
                val h = (value / scaleMax * (bottom - top)).toFloat()
                val active = index == selected
                val color = bar.status?.let { s -> statusOn.getValue(s).let { if (active) it else it.copy(alpha = 0.7f) } }
                    ?: (if (active) colors.accent else colors.accent.copy(alpha = 0.35f))
                drawRoundRect(
                    color = color,
                    topLeft = Offset(cx - barWidth / 2f, bottom - h),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
                if (showValues) {
                    val label = valueFormatter(value)
                    val layout = measurer.measure(label, captionStyle(colors.textTertiary, type))
                    drawText(layout, topLeft = Offset(cx - layout.size.width / 2f, bottom - h - layout.size.height - 2f))
                }
            }
            drawText(measurer, bar.label, cx - 8f, bottom + 4f, colors.textTertiary, type)
        }
    }
}

/**
 * Pace-per-km bar chart (CH2). Bar height is proportional to speed (1/pace), so taller is faster; the Y axis
 * is hidden and each bar carries its pace label. Failed km are red, borderline amber.
 */
@Composable
fun ChartBarPace(
    bars: List<Bar>,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    highlightIndex: Int? = null,
    valueFormatter: (Double) -> String = { "" },
    onTap: (Int) -> Unit = {},
) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    val measurer = rememberTextMeasurer()
    var selected by remember { mutableStateOf(highlightIndex) }
    val statusOn = RcStatus.entries.associateWith { it.onColor() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(bars) {
                detectTapGestures { offset ->
                    if (bars.isEmpty()) return@detectTapGestures
                    val left = PlotLeft.toPx()
                    val right = size.width - PlotRight.toPx()
                    val slot = (right - left) / bars.size
                    val index = ((offset.x - left) / slot).toInt().coerceIn(0, bars.size - 1)
                    selected = index
                    onTap(index)
                }
            },
    ) {
        val left = PlotLeft.toPx()
        val bottom = size.height - PlotBottom.toPx()
        val top = PlotTop.toPx()
        val right = size.width - PlotRight.toPx()
        if (bars.isEmpty() || right <= left) return@Canvas

        val speeds = bars.mapNotNull { it.value?.takeIf { v -> v > 0 } }.map { 1.0 / it }
        val maxSpeed = speeds.maxOrNull() ?: 1.0
        val slot = (right - left) / bars.size
        val totalWeight = bars.sumOf { it.weight.toDouble() }.coerceAtLeast(1.0)
        var cursor = left

        bars.forEachIndexed { index, bar ->
            val w = (slot * (bar.weight / totalWeight) * bars.size).toFloat().coerceAtLeast(12f)
            val cx = cursor + w / 2f
            val pace = bar.value
            if (pace != null && pace > 0) {
                val h = ((1.0 / pace) / maxSpeed * (bottom - top)).toFloat()
                val active = index == selected
                val base = bar.status?.let { statusOn.getValue(it) } ?: colors.accent
                val color = if (bar.partial) colors.accent.copy(alpha = 0.35f) else if (active || bar.status != null) base else colors.accent
                drawRoundRect(
                    color = color,
                    topLeft = Offset(cursor + (w - w * 0.6f) / 2f, bottom - h),
                    size = Size(w * 0.6f, h),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
                if (bar.partial) {
                    drawRoundRect(
                        color = colors.accent,
                        topLeft = Offset(cursor + (w - w * 0.6f) / 2f, bottom - h),
                        size = Size(w * 0.6f, h),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                        style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))),
                    )
                }
                val label = valueFormatter(pace)
                val layout = measurer.measure(label, captionStyle(colors.textSecondary, type))
                drawText(layout, topLeft = Offset(cx - layout.size.width / 2f, bottom - h - layout.size.height - 2f))
            }
            val xLabel = measurer.measure(bar.label, captionStyle(colors.textTertiary, type))
            drawText(xLabel, topLeft = Offset(cx - xLabel.size.width / 2f, bottom + 4f))
            cursor += w
        }
    }
}

/**
 * Line chart (CH1/CH5/CH6) with a reversed pace axis (faster up), gradient area, dashed stair-step targets and
 * dashed segment boundaries. Null y-values break the line. A tooltip card appears at the touched point.
 */
@Composable
fun ChartLine(
    points: List<LinePoint>,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
    steps: List<TargetStep> = emptyList(),
    boundaries: List<Double> = emptyList(),
    reversedPace: Boolean = true,
    valueFormatter: (Double) -> String = { "" },
    onTap: (LinePoint) -> Unit = {},
) {
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    val measurer = rememberTextMeasurer()
    var tapped by remember { mutableStateOf<LinePoint?>(null) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(points) {
                detectTapGestures { offset ->
                    if (points.isEmpty()) return@detectTapGestures
                    val minX = points.first().x
                    val maxX = points.last().x
                    val left = PlotLeft.toPx()
                    val right = size.width - PlotRight.toPx()
                    val x = minX + ((offset.x - left) / (right - left)).coerceIn(0f, 1f) * (maxX - minX)
                    val nearest = points.filter { it.y != null }.minByOrNull { kotlin.math.abs(it.x - x) }
                    if (nearest != null) {
                        tapped = nearest
                        onTap(nearest)
                    }
                }
            },
    ) {
        val left = PlotLeft.toPx()
        val bottom = size.height - PlotBottom.toPx()
        val top = PlotTop.toPx()
        val right = size.width - PlotRight.toPx()
        if (points.isEmpty() || right <= left) return@Canvas

        val values = (points.mapNotNull { it.y } + steps.map { it.target })
        val (minV, maxV) = if (reversedPace) {
            ChartMath.paceAxisBounds(values)
        } else {
            val lo = (values.minOrNull() ?: 0.0)
            val hi = (values.maxOrNull() ?: 1.0)
            lo to (if (hi - lo < 1e-6) lo + 1.0 else hi)
        }
        val minX = points.first().x
        val maxX = points.last().x
        val spanX = (maxX - minX).takeIf { kotlin.math.abs(it) > 1e-9 } ?: 1.0

        fun px(x: Double) = (left + ((x - minX) / spanX).toFloat() * (right - left))
        fun py(v: Double) = if (reversedPace) {
            bottom - (ChartMath.invertPaceFraction(v, minV, maxV).toFloat() * (bottom - top))
        } else {
            bottom - (((v - minV) / (maxV - minV)).toFloat() * (bottom - top))
        }

        val ticks = if (reversedPace) ChartMath.paceTicks(minV, maxV) else listOf(minV, (minV + maxV) / 2, maxV)
        ticks.forEach { tick ->
            val y = py(tick)
            drawLine(colors.outline, Offset(left, y), Offset(right, y), strokeWidth = 1f)
            drawText(measurer, formatTick(tick), 0f, y - 8f, colors.textTertiary, type)
        }
        drawText(measurer, "lebih cepat", left, top - 6f, colors.textTertiary, type)

        boundaries.forEach { x ->
            drawLine(
                color = colors.outlineStrong,
                start = Offset(px(x), top),
                end = Offset(px(x), bottom),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
            )
        }
        steps.forEach { step ->
            drawLine(
                color = colors.textSecondary,
                start = Offset(px(step.startX), py(step.target)),
                end = Offset(px(step.endX), py(step.target)),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
            )
        }

        val line = Path()
        val area = Path()
        var started = false
        points.forEach { point ->
            val y = point.y
            if (y == null) {
                started = false
            } else {
                val x = px(point.x)
                val yy = py(y)
                if (!started) {
                    line.moveTo(x, yy)
                    area.moveTo(x, bottom)
                    area.lineTo(x, yy)
                    started = true
                } else {
                    line.lineTo(x, yy)
                    area.lineTo(x, yy)
                }
            }
        }
        area.lineTo(px(maxX), bottom)
        area.close()
        drawPath(area, brush = Brush.verticalGradient(listOf(colors.accent.copy(alpha = 0.18f), Color.Transparent)))
        drawPath(line, color = colors.accent, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

        tapped?.let { point ->
            point.y?.let { y ->
                val cx = px(point.x)
                val cy = py(y)
                drawCircle(colors.bg, radius = 5.dp.toPx() + 2.dp.toPx(), center = Offset(cx, cy))
                drawCircle(colors.accent, radius = 5.dp.toPx(), center = Offset(cx, cy))
                drawTooltip(measurer, colors, type, "${point.x.toInt()} m · ${valueFormatter(y)}", cx, cy, top)
            }
        }
    }
}

private fun DrawScope.drawTooltip(
    measurer: TextMeasurer,
    colors: com.daniel.runcoach.ui.theme.RcColors,
    type: com.daniel.runcoach.ui.theme.RcTypography,
    text: String,
    cx: Float,
    cy: Float,
    top: Float,
) {
    val layout = measurer.measure(text, captionStyle(colors.textPrimary, type))
    val padX = 12.dp.toPx()
    val padY = 8.dp.toPx()
    val w = layout.size.width + padX * 2
    val h = layout.size.height + padY * 2
    val x = (cx - w / 2).coerceIn(0f, size.width - w)
    val y = (cy - h - 8.dp.toPx()).coerceAtLeast(top)
    drawRoundRect(color = colors.surfaceRaised, topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(12.dp.toPx()))
    drawText(layout, topLeft = Offset(x + padX, y + padY))
}

private fun niceMax(value: Double): Double {
    if (value <= 0) return 1.0
    val step = when {
        value <= 5 -> 1.0
        value <= 20 -> 5.0
        value <= 50 -> 10.0
        value <= 200 -> 25.0
        else -> 100.0
    }
    return ceil(value / step) * step
}

private fun formatTick(value: Double): String =
    if (value >= 100) value.toInt().toString() else "%.1f".format(value)

private fun captionStyle(color: Color, type: com.daniel.runcoach.ui.theme.RcTypography): TextStyle =
    type.caption.copy(color = color)

private fun DrawScope.drawText(
    measurer: TextMeasurer,
    text: String,
    x: Float,
    y: Float,
    color: Color,
    type: com.daniel.runcoach.ui.theme.RcTypography,
) {
    val layout: TextLayoutResult = measurer.measure(text, captionStyle(color, type))
    drawText(layout, topLeft = Offset(x, y))
}

// --- Backwards-compatible wrappers -------------------------------------------------

/** Legacy alias for [ChartBar]. */
@Composable
fun BarChart(
    bars: List<Bar>,
    modifier: Modifier = Modifier,
    highlightIndex: Int? = null,
    valueFormatter: (Double) -> String = { it.toInt().toString() },
    onTap: (Int) -> Unit = {},
) = ChartBar(bars = bars, modifier = modifier, highlightIndex = highlightIndex, valueFormatter = valueFormatter, onTap = onTap)

/** Legacy alias for [ChartLine]. */
@Composable
fun LineChart(
    points: List<LinePoint>,
    modifier: Modifier = Modifier,
    steps: List<TargetStep> = emptyList(),
    boundaries: List<Double> = emptyList(),
    valueFormatter: (Double) -> String = { it.toInt().toString() },
    onTap: (LinePoint) -> Unit = {},
) = ChartLine(points = points, modifier = modifier, steps = steps, boundaries = boundaries, valueFormatter = valueFormatter, onTap = onTap)

// --- Previews -------------------------------------------------------------------

@androidx.compose.ui.tooling.preview.Preview(name = "ChartBar light", showBackground = true)
@Composable
private fun ChartBarLightPreview() = com.daniel.runcoach.ui.theme.RcTheme(darkTheme = false) {
    ChartBar(bars = listOf(Bar("Mo", 3.2), Bar("Tu", 0.0), Bar("We", 5.1, target = 4.0), Bar("Th", null), Bar("Fr", 7.4)), highlightIndex = 4)
}

@androidx.compose.ui.tooling.preview.Preview(name = "ChartBar dark", showBackground = true)
@Composable
private fun ChartBarDarkPreview() = com.daniel.runcoach.ui.theme.RcTheme(darkTheme = true) {
    ChartBar(bars = listOf(Bar("Mo", 3.2), Bar("Tu", 0.0), Bar("We", 5.1, target = 4.0), Bar("Th", null), Bar("Fr", 7.4)), highlightIndex = 4)
}

@androidx.compose.ui.tooling.preview.Preview(name = "ChartBar empty", showBackground = true)
@Composable
private fun ChartBarEmptyPreview() = com.daniel.runcoach.ui.theme.RcTheme(darkTheme = true) { ChartBar(bars = emptyList()) }

@androidx.compose.ui.tooling.preview.Preview(name = "ChartBarPace light", showBackground = true)
@Composable
private fun ChartBarPaceLightPreview() = com.daniel.runcoach.ui.theme.RcTheme(darkTheme = false) {
    ChartBarPace(
        bars = listOf(
            Bar("1", 340.0), Bar("2", 355.0, status = RcStatus.Pass), Bar("3", 372.0, status = RcStatus.Warn),
            Bar("4", 400.0, status = RcStatus.Fail), Bar("0,10", 420.0, weight = 0.1f, partial = true),
        ),
        valueFormatter = { "%d:%02d".format(it.toInt() / 60, it.toInt() % 60) },
    )
}

@androidx.compose.ui.tooling.preview.Preview(name = "ChartLine light", showBackground = true)
@Composable
private fun ChartLineLightPreview() = com.daniel.runcoach.ui.theme.RcTheme(darkTheme = false) {
    ChartLine(
        points = (0..50).map { LinePoint(it * 100.0, 360.0 + (it % 7) * 5.0) },
        steps = listOf(TargetStep(0.0, 2500.0, 400.0), TargetStep(2500.0, 5000.0, 360.0)),
        boundaries = listOf(2500.0),
        valueFormatter = { "%d:%02d".format(it.toInt() / 60, it.toInt() % 60) },
    )
}

@androidx.compose.ui.tooling.preview.Preview(name = "ChartLine dark gap", showBackground = true)
@Composable
private fun ChartLineGapPreview() = com.daniel.runcoach.ui.theme.RcTheme(darkTheme = true) {
    ChartLine(points = listOf(LinePoint(0.0, 360.0), LinePoint(100.0, 365.0), LinePoint(200.0, null), LinePoint(300.0, 350.0)))
}

@androidx.compose.ui.tooling.preview.Preview(name = "ChartLine single point", showBackground = true)
@Composable
private fun ChartLineSinglePreview() = com.daniel.runcoach.ui.theme.RcTheme(darkTheme = true) {
    ChartLine(points = listOf(LinePoint(0.0, 360.0)))
}

@androidx.compose.ui.tooling.preview.Preview(name = "ChartLine empty", showBackground = true)
@Composable
private fun ChartLineEmptyPreview() = com.daniel.runcoach.ui.theme.RcTheme(darkTheme = true) { ChartLine(points = emptyList()) }
