package com.overlandtracker.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM track_segments")
    suspend fun getSegments(): List<TrackSegmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSegments(segments: List<TrackSegmentEntity>)

    @Query("SELECT * FROM hut_waypoints ORDER BY orderIndex ASC")
    suspend fun getHuts(): List<HutWaypointEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHuts(huts: List<HutWaypointEntity>)

    @Query("SELECT * FROM current_location WHERE id = 1")
    fun observeCurrentLocation(): Flow<CurrentLocationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCurrentLocation(location: CurrentLocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreadcrumbPoint(point: BreadcrumbPointEntity)

    @Query("SELECT * FROM breadcrumb_points ORDER BY timestampMillis ASC")
    fun observeBreadcrumbs(): Flow<List<BreadcrumbPointEntity>>
}
