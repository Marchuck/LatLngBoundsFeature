package com.marchuck.latlngboundsfeature

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.mapbox.mapboxsdk.maps.MapView


/**
 * @author Lukasz Marczak
 *
 * based on
 * https://github.com/mapbox/mapbox-android-demo/blob/master/MapboxAndroidDemo/src/main/java/com/mapbox/mapboxandroiddemo/examples/camera/BoundingBoxCameraActivity.java
 */
class MainActivity : AppCompatActivity() {

    private var mapView: MapView? = null

    private val boundsProvider: BoundsProvider = UkBoundsProvider()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        val mapBoxObserver = MapBoxObserver(mapView, boundsProvider)
        lifecycle.addObserver(mapBoxObserver)
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

}