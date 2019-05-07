package com.marchuck.latlngboundsfeature

import android.app.Application
import com.mapbox.mapboxsdk.Mapbox

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        val token = getString(R.string.mapbox_access_token)
        Mapbox.getInstance(this, token)
    }
}
