package com.overlandtracker.app.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.overlandtracker.app.data.huts.LatLngPoint
import com.overlandtracker.app.data.local.BreadcrumbPointEntity
import com.overlandtracker.app.data.local.CurrentLocationEntity
import com.overlandtracker.app.data.local.TrackDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class LocationRepository(
    context: Context,
    private val trackDao: TrackDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext)

    private var locationCallback: LocationCallback? = null

    fun observeCurrentPosition(): Flow<LatLngPoint?> =
        trackDao.observeCurrentLocation().map { location ->
            location?.let { LatLngPoint(lat = it.lat, lng = it.lng) }
        }

    fun observeBreadcrumbHistory(): Flow<List<LatLngPoint>> =
        trackDao.observeBreadcrumbs().map { breadcrumbs ->
            breadcrumbs.map { LatLngPoint(lat = it.lat, lng = it.lng) }
        }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (locationCallback != null) return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
            .setMinUpdateIntervalMillis(2_500L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val timestamp = System.currentTimeMillis()
                    scope.launch {
                        trackDao.upsertCurrentLocation(
                            CurrentLocationEntity(
                                lat = location.latitude,
                                lng = location.longitude,
                                timestampMillis = timestamp
                            )
                        )
                        trackDao.insertBreadcrumbPoint(
                            BreadcrumbPointEntity(
                                lat = location.latitude,
                                lng = location.longitude,
                                timestampMillis = timestamp
                            )
                        )
                    }
                }
            }
        }

        runCatching {
            fusedLocationClient.requestLocationUpdates(
                request,
                checkNotNull(locationCallback),
                Looper.getMainLooper()
            )
        }
    }

    fun stopTracking() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
        }
        locationCallback = null
    }
}
