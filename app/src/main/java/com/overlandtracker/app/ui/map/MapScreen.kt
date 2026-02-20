package com.overlandtracker.app.ui.map

import android.graphics.Color
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun MapScreen() {
    val context = LocalContext.current
    val gpxDataState = remember { mutableStateOf<TrailGpxData?>(null) }

    LaunchedEffect(Unit) {
        gpxDataState.value = withContext(Dispatchers.IO) {
            runCatching { parseTrailGpx(context) }.getOrNull()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            Configuration.getInstance().userAgentValue = context.packageName
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { mapView ->
            val gpxData = gpxDataState.value ?: return@AndroidView
            if (mapView.overlays.isNotEmpty()) return@AndroidView

            if (gpxData.routePoints.isNotEmpty()) {
                val route = Polyline(mapView).apply {
                    setPoints(gpxData.routePoints)
                    outlinePaint.color = Color.parseColor("#2E7D32")
                    outlinePaint.strokeWidth = 7f
                }
                mapView.overlays.add(route)
                mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                mapView.controller.setCenter(gpxData.routePoints.first())
                mapView.controller.setZoom(10.0)
            }

            gpxData.waypoints.forEach { waypoint ->
                mapView.overlays.add(
                    Marker(mapView).apply {
                        position = waypoint.location
                        title = waypoint.name
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                )
            }

            mapView.invalidate()
        }
    )
}
