package com.marchuck.latlngboundsfeature.domain

import android.support.annotation.FloatRange

sealed class DownloadRegionEvent {

    data class Downloading(val regionName: String, @FloatRange(from = 0.0, to = 100.0) val progress: Double) :
        DownloadRegionEvent()

    data class Done(val regionName: String) : DownloadRegionEvent()

    data class Error(val readableMessage: String, val throwable: Throwable) : DownloadRegionEvent()

}