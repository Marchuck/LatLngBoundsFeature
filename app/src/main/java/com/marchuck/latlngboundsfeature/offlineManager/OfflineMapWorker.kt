package com.marchuck.latlngboundsfeature.offlineManager

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.mapbox.mapboxsdk.offline.OfflineRegion
import com.mapbox.mapboxsdk.offline.OfflineManager
import org.json.JSONObject
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition
import com.mapbox.mapboxsdk.style.expressions.Expression.zoom
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.marchuck.latlngboundsfeature.OfflineManagerActivity.Companion.JSON_FIELD_REGION_NAME


class OfflineMapWorker(val context: Context, parameters: WorkerParameters) : Worker(context, parameters) {


    override fun doWork(): Result {
        return Result.success()
    }
}