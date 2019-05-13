package com.marchuck.latlngboundsfeature

import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import org.junit.Assert
import org.junit.Test

class UkBoundsProviderTest {

    val ukBoundsProvider = UkBoundsProvider()

    @Test
    fun provideBounds() {

        val coordinates = arrayListOf(
            LatLng(51.103280831899724, 2.617468669353741)
            , LatLng(49.23494608768942, -6.364527174613727)
            , LatLng(58.13123526671316, -9.382138894833531)
            , LatLng(59.43264547840795, 0.11186393346059731)
        )

        val expectedBounds =
            LatLngBounds.from(
                59.43264547840795, 2.617468669353741,
                49.23494608768942, -9.382138894833531
            )

        val actualBounds = ukBoundsProvider.generateBounds(coordinates)

        Assert.assertEquals(actualBounds, expectedBounds)
    }
}