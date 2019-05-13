package com.marchuck.latlngboundsfeature.domain

import android.support.annotation.IntRange

sealed class DownloadRegionEvent {

    data class Downloading(val regionName: String,
                           @IntRange(from = 0, to = 100) val progress: Int) : DownloadRegionEvent()

    /**
     * @see DownloadRegionUseCase.startDownload
     */
    data class TileCountLimitExceeded(val limit: Long) : DownloadRegionEvent()

    data class Done(val regionName: String, val timeOfLastDownload: Long) : DownloadRegionEvent()

    data class Error(val regionName: String,
                     val readableMessage: String,
                     val throwable: Throwable) : DownloadRegionEvent()

    object Idle : DownloadRegionEvent()

}