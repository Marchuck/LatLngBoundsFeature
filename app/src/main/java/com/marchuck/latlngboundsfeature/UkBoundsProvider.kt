package com.marchuck.latlngboundsfeature

import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds

class UkBoundsProvider : BoundsProvider {
    //fetched from map, but cound be uk-bounding-box
    //https://gist.github.com/UsabilityEtc/6d2059bd4f0181a98d76
    val edgeCoordinates = arrayListOf(
        LatLng(51.103280831899724, 2.617468669353741)
        , LatLng(49.23494608768942, -6.364527174613727)
        , LatLng(58.13123526671316, -9.382138894833531)
        , LatLng(59.43264547840795, 0.11186393346059731)
    )

    val edgeCoordinates2 = arrayListOf(
        LatLng(53.103280831899724, -2.617468669353741)
        , LatLng(55.13123526671316, -4.382138894833531)
    )

    override fun provideBounds(): LatLngBounds {
        return generateBounds(edgeCoordinates)
    }

    private fun generateBounds(edgeCoordinates: Collection<LatLng>): LatLngBounds {
        val latLngBuilder = LatLngBounds.Builder()

        for (latLng in edgeCoordinates) {
            latLngBuilder.include(latLng)
        }

        val bounds = latLngBuilder.build()
        return bounds
    }

}