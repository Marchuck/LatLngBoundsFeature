package com.marchuck.latlngboundsfeature.domain.exceptions

import com.mapbox.mapboxsdk.offline.OfflineRegionError

class OfflineRegionErrorException(val regionError: OfflineRegionError) : Exception()
