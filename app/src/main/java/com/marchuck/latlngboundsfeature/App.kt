package com.marchuck.latlngboundsfeature

import android.app.Application
import android.content.Context
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.offline.OfflineManager
import com.marchuck.latlngboundsfeature.domain.DownloadRegionUseCase

class App : Application() {

    val downloadOfflineMapsUseCase by lazy { DownloadRegionUseCase(OfflineManager.getInstance(this)) }

    override fun onCreate() {
        super.onCreate()

        val token = getString(R.string.mapbox_access_token)
        Mapbox.getInstance(this, token)
    }

    companion object {
        fun getInstance(context: Context) = context.applicationContext as App
    }
}
