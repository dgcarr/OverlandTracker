package com.overlandtracker.app.domain.navigation

import com.overlandtracker.app.data.huts.HutWaypoint
import com.overlandtracker.app.data.huts.LatLngPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TargetHutSelectorTest {

    private val route = listOf(
        LatLngPoint(lat = -41.00, lng = 146.00),
        LatLngPoint(lat = -41.00, lng = 146.02),
        LatLngPoint(lat = -41.00, lng = 146.04)
    )

    private val huts = listOf(
        HutWaypoint("South Hut", -41.00, 146.005, 0),
        HutWaypoint("Mid Hut", -41.00, 146.020, 1),
        HutWaypoint("North Hut", -41.00, 146.035, 2)
    )

    @Test
    fun leavingHutInMorning_selectsForwardHutEvenIfNearestIsBehind() {
        val selector = TargetHutSelector()
        val current = LatLngPoint(-41.00, 146.010)
        val recentPoints = listOf(
            LatLngPoint(-41.00, 146.007),
            LatLngPoint(-41.00, 146.0085),
            LatLngPoint(-41.00, 146.010)
        )

        val selection = selector.selectTarget(
            currentPoint = current,
            recentPoints = recentPoints,
            orderedHuts = huts,
            routePolyline = route
        )

        assertEquals("Mid Hut", selection.targetHut?.name)
        assertEquals(TravelDirection.FORWARD, selection.travelDirection)
    }

    @Test
    fun approachingHutRapidly_shortensPingInterval() {
        val policy = PingPolicy()

        val decision = policy.evaluate(
            nowMillis = 10_000L,
            distanceToTargetMeters = 200.0,
            etaMillis = 5 * 60_000L,
            movingAwayFromTarget = false,
            adaptiveEnabled = true
        )

        assertEquals(60_000L, decision.intervalMillis)
        assertEquals(70_000L, decision.nextPingAtMillis)
    }

    @Test
    fun stationaryOrNoisyGps_keepsDirectionUnknownAndAvoidsMovingAwayPenalty() {
        val selector = TargetHutSelector()
        val current = LatLngPoint(-41.00, 146.01001)
        val noisyRecent = listOf(
            LatLngPoint(-41.00, 146.01000),
            LatLngPoint(-41.00, 146.01001),
            LatLngPoint(-41.00, 146.01000),
            LatLngPoint(-41.00, 146.01001)
        )

        val selection = selector.selectTarget(
            currentPoint = current,
            recentPoints = noisyRecent,
            orderedHuts = huts,
            routePolyline = route
        )

        assertEquals(TravelDirection.UNKNOWN, selection.travelDirection)

        val policy = PingPolicy()
        val decision = policy.evaluate(
            nowMillis = 0L,
            distanceToTargetMeters = selection.distanceToTargetMeters,
            etaMillis = null,
            movingAwayFromTarget = selection.movingAwayFromTarget,
            adaptiveEnabled = true
        )
        assertTrue(decision.intervalMillis <= 5 * 60_000L)
    }
}
