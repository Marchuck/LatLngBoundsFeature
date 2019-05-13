package com.marchuck.latlngboundsfeature

import android.app.Application
import android.content.Context
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.offline.OfflineManager
import com.marchuck.latlngboundsfeature.domain.DownloadRegionUseCase
import com.marchuck.latlngboundsfeature.domain.GetRegionsUseCase
import timber.log.Timber

class App : Application() {

    //dependencies
    val offlineManager by lazy { OfflineManager.getInstance(this) }
    val getRegionsUseCase by lazy { GetRegionsUseCase(offlineManager) }
    val downloadOfflineMapsUseCase by lazy { DownloadRegionUseCase(offlineManager, getRegionsUseCase) }

    override fun onCreate() {
        super.onCreate()

        val token = getString(R.string.mapbox_access_token)
        Mapbox.getInstance(this, token)
        Timber.plant(Timber.DebugTree())
    }

    companion object {
        fun getInstance(context: Context) = context.applicationContext as App
    }
}
