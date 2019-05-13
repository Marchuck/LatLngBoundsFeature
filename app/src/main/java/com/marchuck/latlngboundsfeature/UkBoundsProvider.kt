package com.marchuck.latlngboundsfeature

import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds


class UkBoundsProvider : BoundsProvider {

    private val UK_BOUNDS = LatLngBounds.from(61.464083, 3.633984, 49.766133, -7.555904)

    override fun provideBounds(): LatLngBounds {
        return UK_BOUNDS
    }

    fun generateBounds(edgeCoordinates: Collection<LatLng>): LatLngBounds {
        val latLngBuilder = LatLngBounds.Builder()
        for (latLng in edgeCoordinates) {
            latLngBuilder.include(latLng)
        }
        val bounds = latLngBuilder.build()
        return bounds
    }

}