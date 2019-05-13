package com.marchuck.latlngboundsfeature.domain

import com.mapbox.mapboxsdk.offline.OfflineRegion
import com.marchuck.latlngboundsfeature.domain.exceptions.DeleteRegionException
import io.reactivex.Completable

class DeleteRegionUseCase() {

    fun execute(offlineRegion: OfflineRegion): Completable {
        return Completable.create { emitter ->
            offlineRegion.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                override fun onDelete() {
                    emitter.onComplete()
                }

                override fun onError(error: String) {
                    emitter.onError(DeleteRegionException(error))
                }
            })
        }
    }
}