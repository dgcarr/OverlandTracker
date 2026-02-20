package com.overlandtracker.app.ui.map

import android.graphics.Color
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.overlandtracker.app.R
import com.overlandtracker.app.data.thermal.DeviceThermalState
import com.overlandtracker.app.data.thermal.ThermalLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun MapScreen(
    mapViewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val gpxDataState = remember { mutableStateOf<TrailGpxData?>(null) }
    val thermalState by mapViewModel.thermalState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        gpxDataState.value = withContext(Dispatchers.IO) {
            runCatching { parseTrailGpx(context) }.getOrNull()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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

        ThermalIndicatorChip(
            state = thermalState,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
        )
    }
}

@Composable
private fun ThermalIndicatorChip(
    state: DeviceThermalState,
    modifier: Modifier = Modifier
) {
    var showTooltip by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        ElevatedAssistChip(
            onClick = { showTooltip = true },
            label = { Text(text = state.toDisplayLabel()) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = stringResource(id = R.string.thermal_info_content_description)
                )
            }
        )

        DropdownMenu(
            expanded = showTooltip,
            onDismissRequest = { showTooltip = false }
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.thermal_tooltip)) },
                onClick = { showTooltip = false }
            )
        }
    }
}

private fun DeviceThermalState.toDisplayLabel(): String {
    return batteryTemperatureCelsius?.let { "${it.toInt()}°C" }
        ?: "Thermal: ${qualitativeStatus.toDisplayText()}"
}

private fun ThermalLevel.toDisplayText(): String {
    return when (this) {
        ThermalLevel.NORMAL -> "Normal"
        ThermalLevel.WARM -> "Warm"
        ThermalLevel.HOT,
        ThermalLevel.CRITICAL,
        ThermalLevel.EMERGENCY,
        ThermalLevel.SHUTDOWN -> "Hot"
        ThermalLevel.UNKNOWN -> "Unknown"
    }
}
