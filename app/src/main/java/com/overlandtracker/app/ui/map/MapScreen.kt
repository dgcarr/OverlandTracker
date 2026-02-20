package com.overlandtracker.app.ui.map

import android.graphics.Color
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.overlandtracker.app.data.huts.HutRepository
import com.overlandtracker.app.data.huts.HutWaypoint
import com.overlandtracker.app.data.huts.LatLngPoint
import com.overlandtracker.app.data.huts.RouteBundle
import com.overlandtracker.app.data.local.AppDatabase
import com.overlandtracker.app.data.location.LocationRepository
import com.overlandtracker.app.data.map.OfflineMapRepository
import com.overlandtracker.app.domain.navigation.PingPolicy
import com.overlandtracker.app.domain.navigation.TargetHutSelector
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
    val targetHutSelector = remember { TargetHutSelector() }
    val pingPolicy = remember { PingPolicy() }

    var routeBundle by remember { mutableStateOf<RouteBundle?>(null) }
    var adaptivePingEnabled by remember { mutableStateOf(true) }

    val currentPosition by locationRepository.observeCurrentPosition().collectAsStateWithLifecycle(initialValue = null)
    val breadcrumbs by locationRepository.observeBreadcrumbHistory().collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            offlineMapRepository.prepareOfflineRegion()
        }
        routeBundle = withContext(Dispatchers.IO) { hutRepository.loadRouteBundle() }
    }

    val selection = remember(currentPosition, breadcrumbs, routeBundle) {
        val route = routeBundle?.segments?.flatMap { it.points }.orEmpty()
        val huts = routeBundle?.huts?.sortedBy(HutWaypoint::orderIndex).orEmpty()
        if (currentPosition == null || route.isEmpty() || huts.isEmpty()) {
            null
        } else {
            targetHutSelector.selectTarget(
                currentPoint = currentPosition!!,
                recentPoints = breadcrumbs.takeLast(6),
                orderedHuts = huts,
                routePolyline = route
            )
        }
    }

    val etaMillis = remember(selection, breadcrumbs) {
        val distance = selection?.distanceToTargetMeters ?: return@remember null
        if (breadcrumbs.size < 2) return@remember null

        val recent = breadcrumbs.takeLast(6)
        val totalDistance = recent.zipWithNext { a, b ->
            val p1 = GeoPoint(a.lat, a.lng)
            val p2 = GeoPoint(b.lat, b.lng)
            p1.distanceToAsDouble(p2)
        }.sum()
        val speedMps = totalDistance / (recent.size - 1).coerceAtLeast(1)
        if (speedMps <= 0.8) null else ((distance / speedMps) * 1_000).toLong()
    }

    val pingDecision = remember(selection, adaptivePingEnabled, etaMillis) {
        pingPolicy.evaluate(
            nowMillis = System.currentTimeMillis(),
            distanceToTargetMeters = selection?.distanceToTargetMeters,
            etaMillis = etaMillis,
            movingAwayFromTarget = selection?.movingAwayFromTarget ?: false,
            adaptiveEnabled = adaptivePingEnabled
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
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

        Card(modifier = Modifier.padding(12.dp)) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Adaptive hut ping", style = MaterialTheme.typography.titleSmall)
                Switch(
                    checked = adaptivePingEnabled,
                    onCheckedChange = { adaptivePingEnabled = it }
                )
                Text(text = "Target hut: ${selection?.targetHut?.name ?: "Unknown"}")
                Text(text = "Next ping ETA: ${formatDuration(pingDecision.nextPingAtMillis - System.currentTimeMillis())}")
            }
        }
    }
}

private fun formatDuration(durationMillis: Long): String {
    val totalSeconds = (durationMillis / 1_000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
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
