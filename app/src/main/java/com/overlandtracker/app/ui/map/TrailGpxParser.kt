package com.overlandtracker.app.ui.map

import android.content.Context
import org.osmdroid.util.GeoPoint
import org.xmlpull.v1.XmlPullParser

private const val GPX_FILE_NAME = "Overland-Track-BNA-2025.gpx"

data class TrailGpxData(
    val routePoints: List<GeoPoint>,
    val waypoints: List<TrailWaypoint>
)

data class TrailWaypoint(
    val name: String,
    val location: GeoPoint
)

fun parseTrailGpx(context: Context): TrailGpxData {
    val routePoints = mutableListOf<GeoPoint>()
    val waypoints = mutableListOf<TrailWaypoint>()

    context.assets.open(GPX_FILE_NAME).use { inputStream ->
        val parser = android.util.Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(inputStream, "UTF-8")
        }

        var eventType = parser.eventType
        var inWaypoint = false
        var inWaypointName = false
        var waypointName = ""
        var waypointLat: Double? = null
        var waypointLon: Double? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "wpt" -> {
                        inWaypoint = true
                        waypointName = ""
                        waypointLat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                        waypointLon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                    }

                    "trkpt", "rtept" -> {
                        val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                        val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                        if (lat != null && lon != null) {
                            routePoints += GeoPoint(lat, lon)
                        }
                    }

                    "name" -> {
                        if (inWaypoint) {
                            inWaypointName = true
                        }
                    }
                }

                XmlPullParser.TEXT -> {
                    if (inWaypointName) {
                        waypointName = parser.text.orEmpty().trim()
                    }
                }

                XmlPullParser.END_TAG -> when (parser.name) {
                    "name" -> inWaypointName = false
                    "wpt" -> {
                        val lat = waypointLat
                        val lon = waypointLon
                        if (lat != null && lon != null) {
                            waypoints += TrailWaypoint(
                                name = waypointName.ifBlank { "Waypoint" },
                                location = GeoPoint(lat, lon)
                            )
                        }
                        inWaypoint = false
                    }
                }
            }

            eventType = parser.next()
        }
    }

    return TrailGpxData(routePoints = routePoints, waypoints = waypoints)
}
