package com.daniel.runcoach.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.daniel.runcoach.ui.theme.LocalRcColors
import com.daniel.runcoach.ui.theme.LocalRcTypography
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/** One drawn segment: its polyline plus the info shown in the F7 tooltip. */
data class RouteSegment(
    val points: List<Pair<Double, Double>>,
    val index: Int,
    val label: String,
    val distanceM: Double,
    val durationMs: Long,
)

/**
 * osmdroid route map (plan P3.1/P3.2, DESIGN S05/S08, P2/B4/F6/F7): orange polyline per segment (a single
 * continuous route visually), start/end markers with labels, boundary dots, and an F7 tooltip when a segment
 * is tapped (highlight + dim the others). [interactive] off lets the map sit inside a scrolling page.
 */
@Composable
fun RouteMapView(
    points: List<Pair<Double, Double>>,
    segments: List<RouteSegment> = emptyList(),
    boundaryIndices: List<Int> = emptyList(),
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = LocalRcColors.current
    val type = LocalRcTypography.current
    val routeColor = colors.route.toArgb()
    val haloColor = colors.routeHalo.toArgb()
    val startColor = colors.markerStart.toArgb()
    val endColor = colors.markerEnd.toArgb()
    val endCoreColor = colors.markerEndCore.toArgb()
    val boundaryColor = colors.boundaryDot.toArgb()
    val background = colors.surface.toArgb()
    val density = context.resources.displayMetrics.density

    var selected by remember { mutableStateOf<RouteSegment?>(null) }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setUseDataConnection(true)
            setTilesScaledToDpi(true)
            setBuiltInZoomControls(false)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.matchParentSize(),
            update = { view ->
                view.setBackgroundColor(background)
                view.setMultiTouchControls(interactive)
                view.overlays.clear()
                val geo = points.map { GeoPoint(it.first, it.second) }
                if (geo.size >= 2) {
                    val slices: List<Pair<RouteSegment?, List<GeoPoint>>> = if (segments.isNotEmpty()) {
                        segments.map { seg -> seg to seg.points.map { GeoPoint(it.first, it.second) } }.filter { it.second.size >= 2 }
                    } else {
                        listOf(null to geo)
                    }
                    slices.forEach { (seg, slice) ->
                        val dim = selected != null && selected != seg
                        view.overlays.add(routePolyline(view, slice, haloColor, 16f * density, if (dim) 0.4f else 1f))
                        val line = routePolyline(view, slice, routeColor, (if (selected == seg) 12f else 8f) * density, if (dim) 0.4f else 1f)
                        if (seg != null) {
                            line.setOnClickListener { _, _, _ ->
                                selected = if (selected == seg) null else seg
                                true
                            }
                        }
                        view.overlays.add(line)
                    }
                    slices.drop(1).forEach { (_, slice) ->
                        view.overlays.add(dotMarker(view, slice.first(), boundaryColor, routeColor, radiusPx = 4.5f * density, ringPx = 2f * density))
                    }
                    if (segments.isEmpty()) {
                        boundaryIndices.forEach { index ->
                            geo.getOrNull(index)?.let { view.overlays.add(dotMarker(view, it, boundaryColor, routeColor, radiusPx = 4.5f * density, ringPx = 2f * density)) }
                        }
                    }
                    view.overlays.add(dotMarker(view, geo.last(), endColor, null, radiusPx = 10f * density, ringPx = 0f))
                    view.overlays.add(dotMarker(view, geo.last(), endCoreColor, null, radiusPx = 4f * density, ringPx = 0f))
                    view.overlays.add(dotMarker(view, geo.first(), startColor, android.graphics.Color.WHITE, radiusPx = 9f * density, ringPx = 2.5f * density))
                    view.overlays.add(labelMarker(view, geo.first(), "Start", density))
                    view.overlays.add(labelMarker(view, geo.last(), "Finish", density))

                    // Tap on empty map area clears the highlight/tooltip.
                    view.overlays.add(
                        MapEventsOverlay(object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean { selected = null; return false }
                            override fun longPressHelper(p: GeoPoint?): Boolean = false
                        }),
                    )

                    val minLat = geo.minOf { it.latitude }
                    val maxLat = geo.maxOf { it.latitude }
                    val minLon = geo.minOf { it.longitude }
                    val maxLon = geo.maxOf { it.longitude }
                    val span = maxOf(maxLat - minLat, maxLon - minLon)
                    val zoom = when {
                        span > 0.05 -> 13.0
                        span > 0.02 -> 14.0
                        span > 0.005 -> 15.0
                        else -> 16.0
                    }
                    view.controller.setZoom(zoom)
                    view.controller.setCenter(GeoPoint((minLat + maxLat) / 2.0, (minLon + maxLon) / 2.0))
                } else {
                    view.controller.setZoom(4.0)
                    view.controller.setCenter(GeoPoint(-8.58, 116.10))
                }
                view.invalidate()
            },
        )

        selected?.let { seg ->
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(colors.surfaceRaised, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text("#${seg.index} ${seg.label}", style = type.titleS, color = colors.textPrimary)
                Text(
                    "Jarak ${segDistance(seg.distanceM)} · Waktu ${segTime(seg.durationMs)}",
                    style = type.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

private fun segDistance(meters: Double): String = if (meters < 1000.0) "${meters.toInt()} m" else "%.2f km".format(meters / 1000.0)

private fun segTime(ms: Long): String {
    val total = ms / 1000
    return "%02d:%02d".format(total / 60, total % 60)
}

private fun routePolyline(view: MapView, geo: List<GeoPoint>, color: Int, widthPx: Float, alpha: Float): Polyline =
    Polyline(view, true).apply {
        setPoints(geo)
        outlinePaint.color = color
        outlinePaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        outlinePaint.strokeWidth = widthPx
        outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
        outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
    }

private fun dotMarker(view: MapView, point: GeoPoint, color: Int, ringColor: Int?, radiusPx: Float, ringPx: Float): Marker {
    val size = (radiusPx * 2).toInt()
    val drawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setSize(size, size)
        if (ringColor != null) setStroke(ringPx.toInt().coerceAtLeast(1), ringColor)
    }
    return Marker(view).apply {
        position = point
        icon = drawable
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
    }
}

/** Small static text label (12 px) near a marker. */
private fun labelMarker(view: MapView, point: GeoPoint, text: String, density: Float): Marker {
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
        textSize = 12f * density
        setShadowLayer(3f, 0f, 0f, android.graphics.Color.BLACK)
        textAlign = android.graphics.Paint.Align.CENTER
    }
    val pad = 2f * density
    val width = (paint.measureText(text) + pad * 2).toInt().coerceAtLeast(1)
    val metrics = paint.fontMetrics
    val height = (metrics.descent - metrics.ascent + pad * 2).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    Canvas(bitmap).drawText(text, width / 2f, pad - metrics.ascent, paint)
    val drawable: Drawable = BitmapDrawable(view.resources, bitmap)
    return Marker(view).apply {
        position = point
        icon = drawable
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    }
}
