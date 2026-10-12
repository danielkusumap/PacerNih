package com.daniel.runcoach.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.daniel.runcoach.R
import com.daniel.runcoach.engine.result.SegmentStatus
import com.daniel.runcoach.ui.theme.RcDarkColors
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/** F3/F4: which pieces of data the user wants on the image. */
enum class ShareItem { NAME, DISTANCE, TIME, PACE, SPLITS, SEGMENTS, ROUTE }

/** F3: the three templates (canvas sizes). */
enum class ShareTemplate(val width: Int, val height: Int) {
    COMPACT(1080, 1080),
    SPLIT(1080, 1350),
    FULL(1080, 1920);

    fun supports(item: ShareItem): Boolean = when (this) {
        COMPACT -> item in setOf(ShareItem.NAME, ShareItem.DISTANCE, ShareItem.TIME, ShareItem.PACE, ShareItem.ROUTE)
        SPLIT -> item in setOf(ShareItem.NAME, ShareItem.DISTANCE, ShareItem.TIME, ShareItem.PACE, ShareItem.SPLITS, ShareItem.SEGMENTS)
        FULL -> true
    }
}

/** F3: text colour. */
enum class ShareTextColor { WHITE, BLACK }

/** One segment row on the share image: uniform bar coloured by its status (R1). */
data class ShareSegment(val index: Int, val label: String, val paceSecPerKm: Double, val status: String)

/** All the data the renderer needs, already formatted. */
data class ShareData(
    val name: String,
    val dateTime: String,
    val distanceKm: Double,
    val movingMs: Long,
    val avgPaceSecPerKm: Double?,
    /** label -> pace seconds per km, for the split-km chart. */
    val splits: List<Pair<String, Double>>,
    /** selected segments (any status), drawn as uniform status-coloured bars (R1). */
    val segments: List<ShareSegment>,
    val track: List<Pair<Double, Double>>,
)

/**
 * F3/F4: renders a transparent PNG (ARGB_8888, alpha 0 background) with the selected blocks for the chosen
 * template. Only blocks the template supports are drawn; the map is a plain polyline (no boundary dots, no
 * start/end markers) per the owner's request.
 */
object ShareImageRenderer {

    private const val PAD = 72f

    fun render(context: Context, data: ShareData, selected: Set<ShareItem>, template: ShareTemplate, textColor: ShareTextColor): Bitmap {
        val bitmap = Bitmap.createBitmap(template.width, template.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap) // untouched -> fully transparent
        val ink = if (textColor == ShareTextColor.WHITE) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        val outline = if (textColor == ShareTextColor.WHITE) 0x73000000 else 0x73FFFFFF
        val number = ResourcesCompat.getFont(context, R.font.barlow_condensed_800) ?: Typeface.DEFAULT_BOLD
        val label = ResourcesCompat.getFont(context, R.font.barlow_condensed_700) ?: Typeface.DEFAULT_BOLD

        val items = ShareItem.entries.filter { it in selected && template.supports(it) }
        val w = template.width.toFloat()
        var y = PAD
        val contentBottom = template.height - PAD

        fun gap(v: Float) { y += v }

        items.forEach { item ->
            when (item) {
                ShareItem.NAME -> {
                    drawText(canvas, data.name.uppercase(Locale.getDefault()), PAD, y + 40f, 40f, label, ink, outline, Paint.Align.LEFT, letterSpacing = 0.14f)
                    y += 40f + 8f
                    drawText(canvas, data.dateTime, PAD, y + 64f, 64f, number, ink, outline, Paint.Align.LEFT)
                    y += 64f + 24f
                }
                ShareItem.DISTANCE -> {
                    drawText(canvas, "%.2f".format(Locale.getDefault(), data.distanceKm), PAD, y + 220f, 220f, number, ink, outline, Paint.Align.LEFT)
                    drawText(canvas, "km", PAD + measure(data.distanceKm, 220f, number) + 24f, y + 220f, 64f, number, ink, outline, Paint.Align.LEFT)
                    y += 220f + 24f
                }
                ShareItem.TIME -> {
                    drawText(canvas, "WAKTU", PAD, y + 36f, 36f, label, ink, outline, Paint.Align.LEFT, letterSpacing = 0.14f)
                    drawText(canvas, duration(data.movingMs), PAD, y + 36f + 96f, 96f, number, ink, outline, Paint.Align.LEFT)
                    y += 36f + 96f + 24f
                }
                ShareItem.PACE -> {
                    drawText(canvas, "PACE", PAD, y + 36f, 36f, label, ink, outline, Paint.Align.LEFT, letterSpacing = 0.14f)
                    drawText(canvas, pace(data.avgPaceSecPerKm) + " /km", PAD, y + 36f + 96f, 96f, number, ink, outline, Paint.Align.LEFT)
                    y += 36f + 96f + 24f
                }
                ShareItem.SPLITS -> {
                    if (data.splits.isNotEmpty()) {
                        drawText(canvas, "SPLIT KM", PAD, y + 36f, 36f, label, ink, outline, Paint.Align.LEFT, letterSpacing = 0.14f)
                        y += 36f + 28f
                        val h = (data.splits.size * 44f).coerceIn(120f, 360f)
                        drawSplitRows(canvas, data.splits, PAD, y, w - 2 * PAD, h, ink, outline, number)
                        y += h + 24f
                    }
                }
                ShareItem.SEGMENTS -> {
                    if (data.segments.isNotEmpty()) {
                        drawText(canvas, "SPLIT SEGMENT", PAD, y + 36f, 36f, label, ink, outline, Paint.Align.LEFT, letterSpacing = 0.14f)
                        y += 36f + 28f
                        val h = 300f
                        drawSegmentBars(canvas, data.segments, PAD, y, w - 2 * PAD, h, ink, outline, number)
                        y += h + 24f
                    }
                }
                ShareItem.ROUTE -> {
                    val remaining = (contentBottom - y).coerceAtLeast(0f)
                    val boxH = min(remaining, if (template == ShareTemplate.COMPACT) remaining else 520f).coerceAtLeast(0f)
                    if (data.track.size >= 2 && boxH > 0f) {
                        drawRoute(canvas, data.track, PAD, y, w - 2 * PAD, boxH)
                        y += boxH + 24f
                    }
                }
            }
        }
        return bitmap
    }

    /** Split km: one thin horizontal bar per kilometre (owner request), length proportional to speed. */
    private fun drawSplitRows(canvas: Canvas, splits: List<Pair<String, Double>>, x: Float, y: Float, width: Float, height: Float, ink: Int, outline: Int, font: Typeface) {
        if (splits.isEmpty()) return
        val maxSpeed = splits.map { if (it.second > 0) 1.0 / it.second else 0.0 }.maxOrNull()?.takeIf { it > 0 } ?: 1.0
        val rowH = height / splits.size
        val barH = min(12f, rowH * 0.5f)
        val labelW = 96f
        val paceW = 150f
        val barMax = (width - labelW - paceW - 24f).coerceAtLeast(20f)
        val accent = RcDarkColors.accent.toArgbInt()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; style = Paint.Style.FILL }
        splits.forEachIndexed { index, (labelText, paceSec) ->
            val cy = y + rowH * index + rowH / 2f
            drawText(canvas, labelText, x, cy + 10f, 28f, font, ink, outline, Paint.Align.LEFT)
            val len = ((if (paceSec > 0) 1.0 / paceSec else 0.0) / maxSpeed * barMax).toFloat().coerceAtLeast(4f)
            canvas.drawRoundRect(RectF(x + labelW, cy - barH / 2f, x + labelW + len, cy + barH / 2f), barH / 2f, barH / 2f, paint)
            drawText(canvas, pace(paceSec), x + width, cy + 10f, 28f, font, ink, outline, Paint.Align.RIGHT)
        }
    }

    /** R1: uniform segment bars, one per selected segment, coloured by status. */
    private fun drawSegmentBars(canvas: Canvas, segments: List<ShareSegment>, x: Float, y: Float, width: Float, height: Float, ink: Int, outline: Int, font: Typeface) {
        if (segments.isEmpty()) return
        val slot = width / segments.size
        val barW = slot * 0.6f
        val barH = 160f
        val baseY = y + height - 40f
        segments.forEachIndexed { index, seg ->
            val cx = x + slot * index + slot / 2f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = statusColor(seg.status); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(cx - barW / 2f, baseY - barH, cx + barW / 2f, baseY), 8f, 8f, paint)
            drawText(canvas, pace(seg.paceSecPerKm), cx, baseY - barH - 8f, 28f, font, ink, outline, Paint.Align.CENTER)
            drawText(canvas, "#${seg.index}", cx, y + height - 8f, 28f, font, ink, outline, Paint.Align.CENTER)
        }
    }

    private fun statusColor(status: String): Int = when (status) {
        "PASS" -> RcDarkColors.pass.toArgbInt()
        "FAIL" -> RcDarkColors.fail.toArgbInt()
        "BORDERLINE" -> RcDarkColors.warn.toArgbInt()
        else -> RcDarkColors.neutral.toArgbInt()
    }

    private fun drawBars(canvas: Canvas, bars: List<Pair<String, Double>>, x: Float, y: Float, width: Float, height: Float, ink: Int, outline: Int, font: Typeface) {
        if (bars.isEmpty()) return
        val speeds = bars.map { if (it.second > 0) 1.0 / it.second else 0.0 }
        val maxSpeed = speeds.maxOrNull()?.takeIf { it > 0 } ?: 1.0
        val slot = width / bars.size
        val barW = slot * 0.6f
        val accent = RcDarkColors.accent.toArgbInt()
        bars.forEachIndexed { index, (labelText, paceSec) ->
            val speed = if (paceSec > 0) 1.0 / paceSec else 0.0
            val h = (speed / maxSpeed * (height - 60f)).toFloat().coerceAtLeast(2f)
            val cx = x + slot * index + slot / 2f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(cx - barW / 2f, y + height - 40f - h, cx + barW / 2f, y + height - 40f), 8f, 8f, paint)
            drawText(canvas, pace(paceSec), cx, y + height - 40f - h - 8f, 28f, font, ink, outline, Paint.Align.CENTER)
            drawText(canvas, labelText, cx, y + height - 8f, 28f, font, ink, outline, Paint.Align.CENTER)
        }
    }

    private fun drawRoute(canvas: Canvas, track: List<Pair<Double, Double>>, x: Float, y: Float, width: Float, height: Float) {
        val minLat = track.minOf { it.first }; val maxLat = track.maxOf { it.first }
        val minLon = track.minOf { it.second }; val maxLon = track.maxOf { it.second }
        val spanLat = (maxLat - minLat).takeIf { it > 1e-9 } ?: 1.0
        val spanLon = (maxLon - minLon).takeIf { it > 1e-9 } ?: 1.0
        val scale = min(width / spanLon.toFloat(), height / spanLat.toFloat())
        val offX = x + (width - spanLon.toFloat() * scale) / 2f
        val offY = y + (height - spanLat.toFloat() * scale) / 2f
        val path = Path()
        track.forEachIndexed { i, (lat, lon) ->
            val px = offX + ((lon - minLon) * scale).toFloat()
            val py = offY + ((maxLat - lat) * scale).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = RcDarkColors.route.toArgbInt()
            style = Paint.Style.STROKE
            strokeWidth = 8f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(path, paint)
    }

    private fun drawText(canvas: Canvas, text: String, x: Float, baseline: Float, size: Float, font: Typeface, ink: Int, outline: Int, align: Paint.Align, letterSpacing: Float = 0f) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = font
            textSize = size
            textAlign = align
            this.letterSpacing = letterSpacing
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        paint.color = outline
        canvas.drawText(text, x, baseline, paint)
        paint.style = Paint.Style.FILL
        paint.color = ink
        canvas.drawText(text, x, baseline, paint)
    }

    private fun measure(value: Double, size: Float, font: Typeface): Float {
        val paint = Paint().apply { typeface = font; textSize = size }
        return paint.measureText("%.2f".format(Locale.getDefault(), value))
    }

    private fun pace(secPerKm: Double?): String {
        if (secPerKm == null || secPerKm <= 0) return "--:--"
        val s = kotlin.math.round(secPerKm).toInt()
        return "%d:%02d".format(s / 60, s % 60)
    }

    private fun duration(ms: Long): String {
        val total = ms / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    private fun androidx.compose.ui.graphics.Color.toArgbInt(): Int = android.graphics.Color.argb(
        (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt(),
    )
}
