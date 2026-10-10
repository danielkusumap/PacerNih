package com.daniel.runcoach.ui.map

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
import androidx.compose.material3.MaterialTheme
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * osmdroid route map (plan P3.1/P3.2): OpenStreetMap tiles, no API key. Draws the track polyline with
 * start/finish and segment-boundary markers. Works offline — tiles are blank but the route still draws.
 * Rendering pauses with the lifecycle so it stops when the screen goes off.
 */
@Composable
fun RouteMapView(
    points: List<Pair<Double, Double>>,
    boundaryIndices: List<Int> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val darkTheme = isSystemInDarkTheme()
    val routeColor = MaterialTheme.colorScheme.primary.toArgb()
    val background = MaterialTheme.colorScheme.surface.toArgb()

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setUseDataConnection(true)
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
            view.overlays.clear()
            if (points.size >= 2) {
                val geo = points.map { GeoPoint(it.first, it.second) }
                view.overlays.add(
                    Polyline(view, true).apply {
                        setPoints(geo)
                        outlinePaint.color = routeColor
                        outlinePaint.strokeWidth = 8f
                    },
                )
                view.overlays.add(Marker(view).apply { position = geo.first(); title = "Start" })
                view.overlays.add(Marker(view).apply { position = geo.last(); title = "Finish" })
                boundaryIndices.forEach { index ->
                    geo.getOrNull(index)?.let { point ->
                        view.overlays.add(Marker(view).apply { position = point; title = "Segment" })
                    }
                }
                view.controller.setZoom(16.0)
                view.controller.setCenter(geo.first())
            } else {
                // Still show a map (blank offline) centered on a default position.
                view.controller.setZoom(4.0)
                view.controller.setCenter(GeoPoint(-8.58, 116.10))
            }
            view.invalidate()
        },
    )
}
