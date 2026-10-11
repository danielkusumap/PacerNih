package com.daniel.runcoach.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.daniel.runcoach.ui.theme.LocalRcColors
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * osmdroid route map (plan P3.1/P3.2, DESIGN S05/S08, P2/B4/F6): OpenStreetMap tiles, no API key. The route is
 * drawn as an orange (`route`) polyline per segment (a single continuous route visually), with a halo, start
 * (green) and end markers and small boundary dots. [interactive] off lets the map sit inside a scrolling page.
 */
@Composable
fun RouteMapView(
    points: List<Pair<Double, Double>>,
    segments: List<List<Pair<Double, Double>>> = emptyList(),
    boundaryIndices: List<Int> = emptyList(),
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = LocalRcColors.current
    val routeColor = colors.route.toArgb()
    val haloColor = colors.routeHalo.toArgb()
    val startColor = colors.markerStart.toArgb()
    val endColor = colors.markerEnd.toArgb()
    val endCoreColor = colors.markerEndCore.toArgb()
    val boundaryColor = colors.boundaryDot.toArgb()
    val background = colors.surface.toArgb()
    val density = context.resources.displayMetrics.density

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

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            view.setBackgroundColor(background)
            view.setMultiTouchControls(interactive)
            view.overlays.clear()
            val geo = points.map { GeoPoint(it.first, it.second) }
            if (geo.size >= 2) {
                // Route: one polyline per segment so the joins are explicit, drawn as a single continuous route.
                val slices: List<List<GeoPoint>> = if (segments.isNotEmpty()) {
                    segments.map { seg -> seg.map { GeoPoint(it.first, it.second) } }.filter { it.size >= 2 }
                } else {
                    listOf(geo)
                }
                slices.forEach { slice ->
                    view.overlays.add(routePolyline(view, slice, haloColor, 16f * density))
                    view.overlays.add(routePolyline(view, slice, routeColor, 8f * density))
                }
                // Boundary dots at the joins between segments (shared endpoints, so no gap).
                slices.drop(1).forEach { slice ->
                    view.overlays.add(dotMarker(view, slice.first(), boundaryColor, routeColor, radiusPx = 4.5f * density, ringPx = 2f * density))
                }
                // Fallback boundary dots from explicit indices (older callers).
                if (segments.isEmpty()) {
                    boundaryIndices.forEach { index ->
                        geo.getOrNull(index)?.let { view.overlays.add(dotMarker(view, it, boundaryColor, routeColor, radiusPx = 4.5f * density, ringPx = 2f * density)) }
                    }
                }
                // End first, then start on top (so a circular route still shows both).
                view.overlays.add(dotMarker(view, geo.last(), endColor, null, radiusPx = 10f * density, ringPx = 0f))
                view.overlays.add(dotMarker(view, geo.last(), endCoreColor, null, radiusPx = 4f * density, ringPx = 0f))
                view.overlays.add(dotMarker(view, geo.first(), startColor, android.graphics.Color.WHITE, radiusPx = 9f * density, ringPx = 2.5f * density))
                view.overlays.add(labelMarker(view, geo.first(), "Start", density))
                view.overlays.add(labelMarker(view, geo.last(), "Finish", density))

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
}

private fun routePolyline(view: MapView, geo: List<GeoPoint>, color: Int, widthPx: Float): Polyline =
    Polyline(view, true).apply {
        setPoints(geo)
        outlinePaint.color = color
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
