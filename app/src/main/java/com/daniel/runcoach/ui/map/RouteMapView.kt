package com.daniel.runcoach.ui.map

import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.isSystemInDarkTheme
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
 * osmdroid route map (plan P3.1/P3.2, DESIGN S05/S08): OpenStreetMap tiles, no API key. The route is drawn as
 * an accent polyline that fits the view, with distinct start/finish dots. [interactive] off lets the map sit
 * inside a scrolling page without stealing gestures. Rendering pauses with the lifecycle.
 */
@Composable
fun RouteMapView(
    points: List<Pair<Double, Double>>,
    boundaryIndices: List<Int> = emptyList(),
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val darkTheme = isSystemInDarkTheme()
    val colors = LocalRcColors.current
    val routeColor = colors.accent.toArgb()
    val startColor = colors.pass.toArgb()
    val finishColor = colors.textPrimary.toArgb()
    val boundaryColor = colors.warn.toArgb()
    val background = colors.surface.toArgb()

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
            if (points.size >= 2) {
                val geo = points.map { GeoPoint(it.first, it.second) }
                view.overlays.add(
                    Polyline(view, true).apply {
                        setPoints(geo)
                        outlinePaint.color = routeColor
                        outlinePaint.strokeWidth = 12f
                        outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                        outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                    },
                )
                view.overlays.add(dotMarker(view, geo.first(), startColor))
                view.overlays.add(dotMarker(view, geo.last(), finishColor))
                boundaryIndices.forEach { index ->
                    geo.getOrNull(index)?.let { point ->
                        view.overlays.add(dotMarker(view, point, boundaryColor, radius = 8))
                    }
                }
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

private fun dotMarker(view: MapView, point: GeoPoint, color: Int, radius: Int = 14): Marker {
    val size = radius * 2
    val drawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setSize(size, size)
        setStroke(4, android.graphics.Color.WHITE)
    }
    return Marker(view).apply {
        position = point
        icon = drawable
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
    }
}
