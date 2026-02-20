package com.overlandtracker.app.data.huts

import android.content.Context
import com.overlandtracker.app.data.local.HutWaypointEntity
import com.overlandtracker.app.data.local.TrackSegmentEntity
import com.overlandtracker.app.data.local.TrackDao
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val ROUTE_BUNDLE_ASSET = "route_bundle.json"

class HutRepository(
    private val context: Context,
    private val trackDao: TrackDao,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    suspend fun loadRouteBundle(): RouteBundle {
        val storedSegments = trackDao.getSegments()
        val storedHuts = trackDao.getHuts()
        if (storedSegments.isNotEmpty() && storedHuts.isNotEmpty()) {
            return RouteBundle(
                segments = storedSegments.map { entity ->
                    TrackPolylineSegment(
                        id = entity.id,
                        points = json.decodeFromString(entity.pointsJson)
                    )
                },
                huts = storedHuts.map { entity ->
                    HutWaypoint(
                        name = entity.name,
                        lat = entity.lat,
                        lng = entity.lng,
                        orderIndex = entity.orderIndex,
                        metadata = json.decodeFromString(entity.metadataJson)
                    )
                }
            )
        }

        val fromAssets = context.assets.open(ROUTE_BUNDLE_ASSET).bufferedReader().use { reader ->
            json.decodeFromString<RouteBundle>(reader.readText())
        }
        persistRouteBundle(fromAssets)
        return fromAssets
    }

    private suspend fun persistRouteBundle(bundle: RouteBundle) {
        trackDao.upsertSegments(bundle.segments.map { segment ->
            TrackSegmentEntity(
                id = segment.id,
                pointsJson = json.encodeToString(segment.points)
            )
        })
        trackDao.upsertHuts(bundle.huts.map { hut ->
            HutWaypointEntity(
                orderIndex = hut.orderIndex,
                name = hut.name,
                lat = hut.lat,
                lng = hut.lng,
                metadataJson = json.encodeToString(hut.metadata)
            )
        })
    }
}
