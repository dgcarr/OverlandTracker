package com.overlandtracker.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "track_segments")
data class TrackSegmentEntity(
    @PrimaryKey val id: String,
    val pointsJson: String
)

@Entity(tableName = "hut_waypoints")
data class HutWaypointEntity(
    @PrimaryKey val orderIndex: Int,
    val name: String,
    val lat: Double,
    val lng: Double,
    val metadataJson: String
)

@Entity(tableName = "current_location")
data class CurrentLocationEntity(
    @PrimaryKey val id: Int = 1,
    val lat: Double,
    val lng: Double,
    val timestampMillis: Long
)

@Entity(tableName = "breadcrumb_points")
data class BreadcrumbPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lat: Double,
    val lng: Double,
    val timestampMillis: Long
)
