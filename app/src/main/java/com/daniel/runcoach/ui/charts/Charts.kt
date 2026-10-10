package com.daniel.runcoach.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

/** One bar: a nullable value (null = missing) with an optional target line. */
data class Bar(val label: String, val value: Double?, val target: Double? = null)

/** One line point: nullable y (null = gap in the line). */
data class LinePoint(val x: Double, val y: Double?)

/** A stair-step target over an x-range (per segment). */
data class TargetStep(val startX: Double, val endX: Double, val target: Double)

/**
 * Simple reusable bar chart on Canvas (plan P2.7). No chart dependency. Tap highlights and reports the index.
 * Light/dark friendly (colors from the theme). Null values are skipped.
 */
@Composable
fun BarChart(
    bars: List<Bar>,
    modifier: Modifier = Modifier,
    highlightIndex: Int? = null,
    valueFormatter: (Double) -> String = { it.toInt().toString() },
    onTap: (Int) -> Unit = {},
) {
    val measurer = rememberTextMeasurer()
    val barColor = MaterialTheme.colorScheme.primary
    val highlightColor = MaterialTheme.colorScheme.tertiary
    val targetColor = MaterialTheme.colorScheme.error
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    var selected by remember { mutableStateOf(highlightIndex) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .pointerInput(bars) {
                detectTapGestures { offset ->
                    if (bars.isEmpty()) return@detectTapGestures
                    val slot = size.width.toFloat() / bars.size
                    val index = (offset.x / slot).toInt().coerceIn(0, bars.size - 1)
                    selected = index
                    onTap(index)
                }
            },
    ) {
        val maxValue = (bars.mapNotNull { it.value } + bars.mapNotNull { it.target }).maxOrNull() ?: 1.0
        val scaleMax = max(maxValue, 1.0)
        val bottom = size.height - 24f
        val top = 12f
        val slot = size.width / bars.size.coerceAtLeast(1)
        bars.forEachIndexed { index, bar ->
            val value = bar.value
            if (value != null) {
                val height = ((value / scaleMax) * (bottom - top)).toFloat()
                val left = index * slot + slot * 0.2f
                val width = slot * 0.6f
                drawRect(
                    color = if (selected == index) highlightColor else barColor,
                    topLeft = Offset(left, bottom - height),
                    size = androidx.compose.ui.geometry.Size(width, height),
                )
            }
            bar.target?.let { target ->
                val y = bottom - ((target / scaleMax) * (bottom - top)).toFloat()
                drawLine(targetColor, Offset(index * slot, y), Offset((index + 1) * slot, y), strokeWidth = 3f)
            }
            drawLabel(measurer, bar.label, index * slot + slot / 2f - 10f, bottom + 4f, labelColor)
        }
    }
    selected?.let { index ->
        bars.getOrNull(index)?.value?.let { value ->
            androidx.compose.material3.Text(
                text = "${bars[index].label}: ${valueFormatter(value)}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * Reusable line chart (plan P2.7) with an optional stair-step target and dashed vertical boundaries. Null
 * y-values break the line. Tap reports the nearest point.
 */
@Composable
fun LineChart(
    points: List<LinePoint>,
    modifier: Modifier = Modifier,
    steps: List<TargetStep> = emptyList(),
    boundaries: List<Double> = emptyList(),
    valueFormatter: (Double) -> String = { it.toInt().toString() },
    onTap: (LinePoint) -> Unit = {},
) {
    val measurer = rememberTextMeasurer()
    val lineColor = MaterialTheme.colorScheme.primary
    val stepColor = MaterialTheme.colorScheme.error
    val boundaryColor = MaterialTheme.colorScheme.outline
    var tapped by remember { mutableStateOf<LinePoint?>(null) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .pointerInput(points) {
                detectTapGestures { offset ->
                    if (points.isEmpty()) return@detectTapGestures
                    val minX = points.first().x
                    val maxX = points.last().x
                    val x = minX + (offset.x / size.width) * (maxX - minX)
                    val nearest = points.minByOrNull { kotlin.math.abs(it.x - x) }
                    if (nearest != null) {
                        tapped = nearest
                        onTap(nearest)
                    }
                }
            },
    ) {
        val values = (points.mapNotNull { it.y } + steps.map { it.target })
        val maxY = (values.maxOrNull() ?: 1.0)
        val minY = (values.minOrNull() ?: 0.0)
        val spanY = (maxY - minY).coerceAtLeast(1.0)
        val minX = points.firstOrNull()?.x ?: 0.0
        val maxX = points.lastOrNull()?.x ?: 1.0
        val spanX = (maxX - minX).coerceAtLeast(1.0)
        val top = 12f
        val bottom = size.height - 12f

        fun px(x: Double) = ((x - minX) / spanX * size.width).toFloat()
        fun py(y: Double) = (bottom - ((y - minY) / spanY) * (bottom - top)).toFloat()

        boundaries.forEach { x ->
            drawLine(
                color = boundaryColor,
                start = Offset(px(x), top),
                end = Offset(px(x), bottom),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
            )
        }
        steps.forEach { step ->
            drawLine(stepColor, Offset(px(step.startX), py(step.target)), Offset(px(step.endX), py(step.target)), strokeWidth = 3f)
        }

        val path = Path()
        var started = false
        points.forEach { point ->
            val y = point.y
            if (y == null) {
                started = false
            } else {
                val x = px(point.x)
                val yy = py(y)
                if (!started) {
                    path.moveTo(x, yy)
                    started = true
                } else {
                    path.lineTo(x, yy)
                }
            }
        }
        drawPath(path, color = lineColor, style = Stroke(width = 4f))

        tapped?.let { point -> point.y?.let { y -> drawCircle(lineColor, radius = 8f, center = Offset(px(point.x), py(y))) } }
    }
    tapped?.let { point ->
        point.y?.let { y ->
            androidx.compose.material3.Text(
                text = "${point.x.toInt()} m: ${valueFormatter(y)}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLabel(
    measurer: TextMeasurer,
    text: String,
    x: Float,
    y: Float,
    color: Color,
) {
    val layout = measurer.measure(text, TextStyle(fontSize = 10.sp, color = color))
    drawText(layout, topLeft = Offset(x, y))
}

// --- Previews -------------------------------------------------------------------

@androidx.compose.ui.tooling.preview.Preview(name = "Bars light")
@androidx.compose.ui.tooling.preview.Preview(name = "Bars dark", uiMode = 0x20)
@Composable
private fun BarChartPreview() {
    com.daniel.runcoach.ui.theme.RunCoachTheme {
        BarChart(
            bars = listOf(
                Bar("Mo", 3.2), Bar("Tu", 0.0), Bar("We", 5.1, target = 4.0), Bar("Th", null), Bar("Fr", 7.4),
            ),
            highlightIndex = 2,
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Empty bars")
@Composable
private fun BarChartEmptyPreview() {
    com.daniel.runcoach.ui.theme.RunCoachTheme { BarChart(bars = emptyList()) }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Line light")
@androidx.compose.ui.tooling.preview.Preview(name = "Line dark", uiMode = 0x20)
@Composable
private fun LineChartPreview() {
    com.daniel.runcoach.ui.theme.RunCoachTheme {
        LineChart(
            points = (0..50).map { LinePoint(it * 100.0, 360.0 + (it % 7) * 5.0) },
            steps = listOf(TargetStep(0.0, 2500.0, 400.0), TargetStep(2500.0, 5000.0, 360.0)),
            boundaries = listOf(2500.0),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Line with gap")
@Composable
private fun LineChartGapPreview() {
    com.daniel.runcoach.ui.theme.RunCoachTheme {
        LineChart(points = listOf(LinePoint(0.0, 360.0), LinePoint(100.0, 365.0), LinePoint(200.0, null), LinePoint(300.0, 350.0)))
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Empty line")
@Composable
private fun LineChartEmptyPreview() {
    com.daniel.runcoach.ui.theme.RunCoachTheme { LineChart(points = emptyList()) }
}
