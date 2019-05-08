package com.marchuck.latlngboundsfeature

import com.mapbox.mapboxsdk.geometry.LatLngBounds

interface BoundsProvider {
    fun provideBounds(): LatLngBounds
}