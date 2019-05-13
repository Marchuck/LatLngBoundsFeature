package com.marchuck.latlngboundsfeature.offlineManager

import android.content.res.Resources
import androidx.work.*
import com.google.gson.Gson
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition


class DownloadOfflineMapsUseCase(val resources: Resources) {

    companion object {
        const val REGION_DEFINITION = "OfflineTilePyramidRegionDefinition"
        const val REGION_NAME = "REGION_NAME"
    }

    fun execute(map: MapboxMap, regionName: String) {

        // Create offline definition using the current
        // style and boundaries of visible map area

        val definition = createDefinitionFromCurrentMap(map)

        val workManager = WorkManager.getInstance()
        workManager.beginWith(
            OneTimeWorkRequest.Builder(OfflineMapWorker::class.java)
                .addTag("OFFLINE_MAP")
                .setInputData(
                    createInputData(definition, regionName)
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresStorageNotLow(true)
                        .build()
                )
                .build()
        ).enqueue()
    }

    private fun createInputData(definition: OfflineTilePyramidRegionDefinition, regionName: String): Data {
        return Data.Builder()
            .putString(REGION_DEFINITION, Gson().toJson(definition))
            .putString(REGION_NAME, regionName)
            .build()
    }

    private fun createDefinitionFromCurrentMap(map: MapboxMap): OfflineTilePyramidRegionDefinition {

        val styleUrl = map.style?.url
        val bounds = map.projection.visibleRegion.latLngBounds
        val minZoom = map.cameraPosition.zoom
        val maxZoom = map.maxZoomLevel
        val pixelRatio = resources.displayMetrics.density

        val definition = OfflineTilePyramidRegionDefinition(
            styleUrl, bounds, minZoom, maxZoom, pixelRatio
        )
        return definition
    }

}