package com.marchuck.latlngboundsfeature.domain

import com.mapbox.mapboxsdk.maps.Style
import com.marchuck.latlngboundsfeature.R
import java.util.*

class StylesProvider(val resourceRepository: ResourceRepository) {

    fun provideStyles(): ArrayList<Pair<String, String>> {
        val stylesAndNames = arrayListOf(
                Style.MAPBOX_STREETS to "streets",
                Style.OUTDOORS to "outdoors",
                Style.LIGHT to "light",
                Style.DARK to "dark",
                resourceRepository.getString(R.string.style_satellite) to "satellite",
                Style.SATELLITE_STREETS to "satellite streets",
                Style.TRAFFIC_DAY to "traffic day",
                Style.TRAFFIC_NIGHT to "traffic night",
                resourceRepository.getString(R.string.style_greenspace) to "greenspace",
                resourceRepository.getString(R.string.style_aerial) to "aerial",
                resourceRepository.getString(R.string.style_night) to "night"
        )
        return stylesAndNames
    }
}