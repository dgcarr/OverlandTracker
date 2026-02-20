package com.overlandtracker.app.ui.map

import android.graphics.Color
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.overlandtracker.app.data.huts.HutRepository
import com.overlandtracker.app.data.huts.LatLngPoint
import com.overlandtracker.app.data.huts.RouteBundle
import com.overlandtracker.app.data.local.AppDatabase
import com.overlandtracker.app.data.location.LocationRepository
import com.overlandtracker.app.data.map.OfflineMapRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun MapScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context).trackDao() }
    val hutRepository = remember { HutRepository(context, db) }
    val locationRepository = remember { LocationRepository(context, db) }
    val offlineMapRepository = remember { OfflineMapRepository(context) }

    var routeBundle by remember { mutableStateOf<RouteBundle?>(null) }
    val currentPosition by locationRepository.observeCurrentPosition().collectAsStateWithLifecycle(initialValue = null)
    val breadcrumbs by locationRepository.observeBreadcrumbHistory().collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            offlineMapRepository.prepareOfflineRegion()
        }
        routeBundle = withContext(Dispatchers.IO) { hutRepository.loadRouteBundle() }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            MapView(context).apply {
                offlineMapRepository.configure(this)
                setMultiTouchControls(true)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { mapView ->
            renderMapOverlays(
                mapView = mapView,
                routeBundle = routeBundle,
                currentPosition = currentPosition,
                breadcrumbs = breadcrumbs
            )
        }
    )
}

private fun renderMapOverlays(
    mapView: MapView,
    routeBundle: RouteBundle?,
    currentPosition: LatLngPoint?,
    breadcrumbs: List<LatLngPoint>
) {
    mapView.overlays.clear()
    routeBundle ?: return

    routeBundle.segments.forEach { segment ->
        val routePolyline = Polyline(mapView).apply {
            setPoints(segment.points.map { GeoPoint(it.lat, it.lng) })
            outlinePaint.color = Color.parseColor("#2E7D32")
            outlinePaint.strokeWidth = 7f
        }
        mapView.overlays.add(routePolyline)
    }

    routeBundle.huts.forEach { hut ->
        mapView.overlays.add(
            Marker(mapView).apply {
                position = GeoPoint(hut.lat, hut.lng)
                title = hut.name
                subDescription = hut.metadata.entries.joinToString()
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
        )
    }

    if (breadcrumbs.isNotEmpty()) {
        mapView.overlays.add(
            Polyline(mapView).apply {
                setPoints(breadcrumbs.map { GeoPoint(it.lat, it.lng) })
                outlinePaint.color = Color.parseColor("#2962FF")
                outlinePaint.strokeWidth = 5f
            }
        )
    }

    currentPosition?.let { position ->
        mapView.overlays.add(
            Marker(mapView).apply {
                this.position = GeoPoint(position.lat, position.lng)
                title = "You"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            }
        )
        mapView.controller.setCenter(GeoPoint(position.lat, position.lng))
        mapView.controller.setZoom(12.5)
    }

    if (currentPosition == null && routeBundle.segments.firstOrNull()?.points?.isNotEmpty() == true) {
        val first = routeBundle.segments.first().points.first()
        mapView.controller.setCenter(GeoPoint(first.lat, first.lng))
        mapView.controller.setZoom(10.5)
    }

    mapView.invalidate()
}
