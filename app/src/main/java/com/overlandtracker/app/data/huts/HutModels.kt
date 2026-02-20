package com.overlandtracker.app.data.huts

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrackPolylineSegment(
    @SerialName("id") val id: String,
    @SerialName("points") val points: List<LatLngPoint>
)

@Serializable
data class HutWaypoint(
    @SerialName("name") val name: String,
    @SerialName("lat") val lat: Double,
    @SerialName("lng") val lng: Double,
    @SerialName("orderIndex") val orderIndex: Int,
    @SerialName("metadata") val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class LatLngPoint(
    @SerialName("lat") val lat: Double,
    @SerialName("lng") val lng: Double
)

@Serializable
data class RouteBundle(
    @SerialName("segments") val segments: List<TrackPolylineSegment>,
    @SerialName("huts") val huts: List<HutWaypoint>
)
