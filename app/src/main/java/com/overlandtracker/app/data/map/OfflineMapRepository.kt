package com.overlandtracker.app.data.map

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import java.io.File

class OfflineMapRepository(private val context: Context) {
    private val offlineTileDir = File(context.filesDir, "offline_tiles")

    fun configure(mapView: MapView) {
        Configuration.getInstance().userAgentValue = context.packageName
        Configuration.getInstance().osmdroidBasePath = context.filesDir
        Configuration.getInstance().osmdroidTileCache = File(context.cacheDir, "osmdroid")

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setUseDataConnection(!offlineTileDir.exists())
    }

    suspend fun prepareOfflineRegion() = withContext(Dispatchers.IO) {
        if (!offlineTileDir.exists()) {
            offlineTileDir.mkdirs()
        }
        // Reserved directory for pre-downloaded vector/raster tiles.
        // App can populate this folder during trip prep from a selected map package.
    }
}
