package com.marchuck.latlngboundsfeature

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.SerialDisposable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class MapBoxObserver(val mapView: MapView?, val boundsProvider: BoundsProvider) : LifecycleObserver {

    val mapEventsDisposable = SerialDisposable()

    private val subject: PublishSubject<Boolean> = PublishSubject.create()
    private val ADJUST_TO_BOUNDS_TIME = 100


    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        mapView?.getMapAsync { mapboxMap ->

            mapboxMap.setStyle(Style.MAPBOX_STREETS) {
            }

            val bounds = boundsProvider.provideBounds()

            mapboxMap.setLatLngBoundsForCameraTarget(bounds)

            scheduleZoomIfNeeded(mapboxMap, bounds)
//
//            //disable zooming out of England/Ireland/France area
            mapboxMap.setMinZoomPreference(4.0)
//            //https://docs.mapbox.com/android/maps/overview/events/
            mapboxMap.addOnCameraMoveListener {
                scheduleZoomIfNeeded(mapboxMap, bounds)
            }
            mapboxMap.addOnCameraIdleListener {
                scheduleZoomIfNeeded(mapboxMap, bounds)
            }
            mapboxMap.addOnCameraMoveListener {
                scheduleZoomIfNeeded(mapboxMap, bounds)
            }
            mapboxMap.addOnCameraMoveStartedListener {
                scheduleZoomIfNeeded(mapboxMap, bounds)
            }

            mapEventsDisposable.set(subject.throttleFirst(ADJUST_TO_BOUNDS_TIME.toLong(), TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .filter { it }
                .subscribe({
                    mapboxMap.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0), ADJUST_TO_BOUNDS_TIME)
                }, {
                })
            )
        }
    }

    private fun scheduleZoomIfNeeded(mapboxMap: MapboxMap, bounds: LatLngBounds) {
        subject.onNext(shouldZoomToBounds(mapboxMap, bounds))
    }

    private fun shouldZoomToBounds(mapboxMap: MapboxMap, bounds: LatLngBounds): Boolean {
        val visibleBounds = mapboxMap.projection.visibleRegion.latLngBounds
        return !bounds.contains(visibleBounds)
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        mapView?.onResume()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        mapView?.onStart()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        mapView?.onStop()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        mapView?.onPause()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        //remove listeners if any added
        mapView?.onDestroy()
    }

}