package com.marchuck.latlngboundsfeature.domain.exceptions

import com.mapbox.mapboxsdk.offline.OfflineRegion
import com.mapbox.mapboxsdk.offline.OfflineRegionError
import java.lang.Exception

sealed class RegionDownloadException : Exception() {

    data class RegionNameExists(val region: OfflineRegion) : RegionDownloadException()

    data class RegionError(val error: OfflineRegionError) : RegionDownloadException()

    data class MetadataError(val error: String, val source: Exception) : RegionDownloadException()

    data class CreateRegionError(val error: String) : RegionDownloadException()
}