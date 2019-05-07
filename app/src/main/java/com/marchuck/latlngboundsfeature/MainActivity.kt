package com.marchuck.latlngboundsfeature

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style

/**
 * @author Lukasz Marczak
 *
 * based on
 * https://github.com/mapbox/mapbox-android-demo/blob/master/MapboxAndroidDemo/src/main/java/com/mapbox/mapboxandroiddemo/examples/camera/BoundingBoxCameraActivity.java
 */
class MainActivity : AppCompatActivity() {

    private var mapView: MapView? = null
    private var mapboxMap: MapboxMap? = null


    private val mapClickListener = MapboxMap.OnMapClickListener {
        System.err.println("clicked (${it.latitude},${it.longitude})")
        false
    }

    //fetched from map, but cound be uk-bounding-box
    //https://gist.github.com/UsabilityEtc/6d2059bd4f0181a98d76
    val edgeCoordinates = arrayListOf(
        LatLng(51.103280831899724, 2.617468669353741)
        , LatLng(49.23494608768942, -6.364527174613727)
        , LatLng(58.13123526671316, -9.382138894833531)
        , LatLng(59.43264547840795, 0.11186393346059731)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync { mapboxMap ->

            mapboxMap.setStyle(Style.MAPBOX_STREETS) {
            }

            val bounds = generateUkBounds(edgeCoordinates)

            mapboxMap.setLatLngBoundsForCameraTarget(bounds)
            zoomToUkIfNotZoomed(mapboxMap,bounds)

            mapboxMap.addOnMapClickListener(mapClickListener)

            //disable zooming out of England/Ireland/France area
            mapboxMap.setMinZoomPreference(4.0)
            //https://docs.mapbox.com/android/maps/overview/events/
            mapboxMap.addOnCameraMoveListener {
                zoomToUkIfNotZoomed(mapboxMap, bounds)
            }
            mapboxMap.addOnCameraIdleListener {
                zoomToUkIfNotZoomed(mapboxMap, bounds)
            }
            mapboxMap.addOnCameraMoveListener {
                zoomToUkIfNotZoomed(mapboxMap, bounds)
            }
            mapboxMap.addOnCameraMoveStartedListener {
                zoomToUkIfNotZoomed(mapboxMap, bounds)
            }
        }
    }

    private fun zoomToUkIfNotZoomed(mapboxMap: MapboxMap, bounds: LatLngBounds) {
        val centerLatLng = mapboxMap.projection.visibleRegion.latLngBounds.center
        System.err.println("camera moved to ${centerLatLng.latitude}/${centerLatLng.longitude}")
        if (!bounds.contains(centerLatLng)) {
            mapboxMap.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0), 100)
        }
    }


    private fun generateUkBounds(edgeCoordinates: Collection<LatLng>): LatLngBounds {
        val latLngBuilder = LatLngBounds.Builder()

        for (latLng in edgeCoordinates) {
            latLngBuilder.include(latLng)
        }

        val bounds = latLngBuilder.build()
        return bounds
    }

    public override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    public override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mapboxMap != null) {
            mapboxMap?.removeOnMapClickListener(mapClickListener)
        }
        mapView?.onDestroy()
    }
}