package com.marchuck.latlngboundsfeature.domain

import android.support.annotation.FloatRange

sealed class DownloadRegionEvent {

    data class Downloading(val regionName: String,
                           @FloatRange(from = 0.0, to = 100.0) val progress: Double) : DownloadRegionEvent()

    /**
     * @see DownloadRegionUseCase.startDownload
     */
    data class TileCountLimitExceeded(val limit: Long) : DownloadRegionEvent()

    data class Done(val regionName: String) : DownloadRegionEvent()

    data class Error(val regionName: String,
                     val readableMessage: String,
                     val throwable: Throwable) : DownloadRegionEvent()

    object Idle : DownloadRegionEvent()

}