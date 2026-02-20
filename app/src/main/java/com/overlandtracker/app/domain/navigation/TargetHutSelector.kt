package com.overlandtracker.app.domain.navigation

import com.overlandtracker.app.data.huts.HutWaypoint
import com.overlandtracker.app.data.huts.LatLngPoint
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

enum class TravelDirection {
    FORWARD,
    BACKWARD,
    UNKNOWN
}

data class TargetHutSelection(
    val targetHut: HutWaypoint?,
    val travelDirection: TravelDirection,
    val projectedDistanceMeters: Double?,
    val distanceToTargetMeters: Double?,
    val movingAwayFromTarget: Boolean
)

class TargetHutSelector(
    private val recentWindowSize: Int = 6,
    private val snapThresholdMeters: Double = 200.0
) {
    fun selectTarget(
        currentPoint: LatLngPoint,
        recentPoints: List<LatLngPoint>,
        orderedHuts: List<HutWaypoint>,
        routePolyline: List<LatLngPoint>
    ): TargetHutSelection {
        if (orderedHuts.isEmpty() || routePolyline.size < 2) {
            return TargetHutSelection(null, TravelDirection.UNKNOWN, null, null, false)
        }

        val routeProjection = RouteProjector(routePolyline)
        val currentProjection = routeProjection.project(currentPoint)
        val progressAtCurrent = if (currentProjection.distanceMeters <= snapThresholdMeters) {
            currentProjection.progressMeters
        } else {
            null
        }

        val sampledRecent = (recentPoints + currentPoint).takeLast(recentWindowSize)
        val direction = determineDirection(sampledRecent, routeProjection, progressAtCurrent)

        val hutsByProgress = orderedHuts.sortedBy { it.orderIndex }.map { hut ->
            HutProgress(hut, routeProjection.project(LatLngPoint(hut.lat, hut.lng)).progressMeters)
        }

        val target = chooseTarget(progressAtCurrent, hutsByProgress, direction)
        val distanceToTarget = target?.hut?.let {
            haversineMeters(currentPoint, LatLngPoint(it.lat, it.lng))
        }

        val movingAway = if (target != null && sampledRecent.size >= 2) {
            val previous = sampledRecent[sampledRecent.lastIndex - 1]
            val prevDistance = haversineMeters(previous, LatLngPoint(target.hut.lat, target.hut.lng))
            distanceToTarget != null && distanceToTarget > prevDistance + 5.0
        } else {
            false
        }

        return TargetHutSelection(
            targetHut = target?.hut,
            travelDirection = direction,
            projectedDistanceMeters = progressAtCurrent,
            distanceToTargetMeters = distanceToTarget,
            movingAwayFromTarget = movingAway
        )
    }

    private fun chooseTarget(
        progressAtCurrent: Double?,
        hutsByProgress: List<HutProgress>,
        direction: TravelDirection
    ): HutProgress? {
        if (progressAtCurrent == null) {
            return hutsByProgress.firstOrNull()
        }

        return when (direction) {
            TravelDirection.FORWARD -> hutsByProgress.firstOrNull { it.progressMeters > progressAtCurrent }
                ?: hutsByProgress.lastOrNull()

            TravelDirection.BACKWARD -> hutsByProgress.lastOrNull { it.progressMeters < progressAtCurrent }
                ?: hutsByProgress.firstOrNull()

            TravelDirection.UNKNOWN -> {
                hutsByProgress.firstOrNull { it.progressMeters > progressAtCurrent }
                    ?: hutsByProgress.minByOrNull { abs(it.progressMeters - progressAtCurrent) }
            }
        }
    }

    private fun determineDirection(
        recentPoints: List<LatLngPoint>,
        routeProjector: RouteProjector,
        progressAtCurrent: Double?
    ): TravelDirection {
        if (recentPoints.size < 2 || progressAtCurrent == null) return TravelDirection.UNKNOWN

        val start = recentPoints.first()
        val end = recentPoints.last()
        val startProjection = routeProjector.project(start)
        val endProjection = routeProjector.project(end)
        val progressDelta = endProjection.progressMeters - startProjection.progressMeters

        val movement = localVector(start, end)
        val tangent = routeProjector.tangentAt(progressAtCurrent)
        val dot = movement.first * tangent.first + movement.second * tangent.second

        return when {
            abs(progressDelta) >= 10.0 -> if (progressDelta > 0) TravelDirection.FORWARD else TravelDirection.BACKWARD
            abs(dot) >= 1.0 -> if (dot > 0) TravelDirection.FORWARD else TravelDirection.BACKWARD
            else -> TravelDirection.UNKNOWN
        }
    }

    private data class HutProgress(
        val hut: HutWaypoint,
        val progressMeters: Double
    )
}

private class RouteProjector(routePolyline: List<LatLngPoint>) {
    private val localPoints = routePolyline.toLocalCoordinates()
    private val segmentLengths = mutableListOf<Double>()
    private val cumulative = mutableListOf(0.0)

    init {
        for (i in 0 until localPoints.lastIndex) {
            val a = localPoints[i]
            val b = localPoints[i + 1]
            val length = distance(a, b)
            segmentLengths += length
            cumulative += cumulative.last() + length
        }
    }

    fun project(point: LatLngPoint): Projection {
        val localPoint = toLocalCoordinates(listOf(point), point.lat).first()
        var bestDistance = Double.MAX_VALUE
        var bestProgress = 0.0

        for (i in 0 until localPoints.lastIndex) {
            val a = localPoints[i]
            val b = localPoints[i + 1]
            val ab = b.first - a.first to b.second - a.second
            val ap = localPoint.first - a.first to localPoint.second - a.second
            val denom = (ab.first * ab.first + ab.second * ab.second).coerceAtLeast(1e-6)
            val t = ((ap.first * ab.first + ap.second * ab.second) / denom).coerceIn(0.0, 1.0)
            val projected = a.first + ab.first * t to a.second + ab.second * t
            val distance = distance(localPoint, projected)
            if (distance < bestDistance) {
                bestDistance = distance
                bestProgress = cumulative[i] + segmentLengths[i] * t
            }
        }

        return Projection(bestProgress, bestDistance)
    }

    fun tangentAt(progressMeters: Double): Pair<Double, Double> {
        val idx = cumulative.indexOfLast { it <= progressMeters }.coerceIn(0, localPoints.lastIndex - 1)
        val a = localPoints[idx]
        val b = localPoints[idx + 1]
        val dx = b.first - a.first
        val dy = b.second - a.second
        val mag = sqrt(dx * dx + dy * dy).coerceAtLeast(1e-6)
        return dx / mag to dy / mag
    }
}

private data class Projection(
    val progressMeters: Double,
    val distanceMeters: Double
)

private fun List<LatLngPoint>.toLocalCoordinates(): List<Pair<Double, Double>> {
    val referenceLat = firstOrNull()?.lat ?: 0.0
    return toLocalCoordinates(this, referenceLat)
}

private fun toLocalCoordinates(points: List<LatLngPoint>, referenceLat: Double): List<Pair<Double, Double>> {
    val latRad = Math.toRadians(referenceLat)
    val metersPerDegreeLat = 111_320.0
    val metersPerDegreeLng = 111_320.0 * cos(latRad)
    return points.map { point ->
        point.lng * metersPerDegreeLng to point.lat * metersPerDegreeLat
    }
}

private fun localVector(from: LatLngPoint, to: LatLngPoint): Pair<Double, Double> {
    val points = toLocalCoordinates(listOf(from, to), from.lat)
    val a = points[0]
    val b = points[1]
    return b.first - a.first to b.second - a.second
}

private fun distance(a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
    val dx = b.first - a.first
    val dy = b.second - a.second
    return sqrt(dx * dx + dy * dy)
}

private fun haversineMeters(a: LatLngPoint, b: LatLngPoint): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(b.lat - a.lat)
    val dLng = Math.toRadians(b.lng - a.lng)
    val lat1 = Math.toRadians(a.lat)
    val lat2 = Math.toRadians(b.lat)

    val h = sin(dLat / 2) * sin(dLat / 2) +
        cos(lat1) * cos(lat2) * sin(dLng / 2) * sin(dLng / 2)
    return 2 * r * atan2(sqrt(h), sqrt(max(0.0, 1 - h)))
}
