package com.marchuck.latlngboundsfeature.domain

import com.mapbox.mapboxsdk.offline.OfflineManager
import com.mapbox.mapboxsdk.offline.OfflineRegion
import com.marchuck.latlngboundsfeature.domain.exceptions.OfflineRegionsFetchException
import io.reactivex.Single

typealias OfflineRegions = ArrayList<OfflineRegion>

class GetRegionsUseCase(val offlineManager: OfflineManager) {

    fun execute(): Single<OfflineRegions> {
        return Single.create { emitter ->

            offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
                override fun onList(offlineRegions: Array<out OfflineRegion>?) {
                    if (!emitter.isDisposed) {
                        if (offlineRegions.isNullOrEmpty()) {
                            //no regions yet
                            emitter.onSuccess(OfflineRegions())
                        } else {
                            val regions = OfflineRegions()
                            regions.addAll(offlineRegions)
                            emitter.onSuccess(regions)
                        }
                    }
                }

                override fun onError(error: String?) {
                    emitter.onError(
                        OfflineRegionsFetchException(
                            error
                        )
                    )
                }
            })
        }
    }
}