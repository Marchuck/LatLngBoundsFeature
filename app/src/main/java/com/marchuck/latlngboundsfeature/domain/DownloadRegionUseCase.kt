package com.marchuck.latlngboundsfeature.domain

import com.mapbox.mapboxsdk.offline.*
import com.marchuck.latlngboundsfeature.OfflineManagerActivity
import com.marchuck.latlngboundsfeature.domain.exceptions.CreateOfflineRegionException
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import org.json.JSONObject
import timber.log.Timber

class DownloadRegionUseCase(
    val resourceRepository: ResourceRepository,
    val offlineManager: OfflineManager
) {


    fun execute(regionName: String, definition: OfflineTilePyramidRegionDefinition): Observable<DownloadRegionEvent> {

        // Build a JSONObject using the user-defined offline region title,
        // convert it into string, and use it to create a metadata variable.
        // The metadata variable will later be passed to createOfflineRegion()

        val metadata: ByteArray
        try {
            metadata = constructMetdata(regionName)
        } catch (exc: java.lang.Exception) {
            val readableMessage = "Metadata error: ${exc.localizedMessage}"
            return Observable.fromCallable { DownloadRegionEvent.Error(readableMessage, exc) }
        }

        // Create the offline region and launch the download
        //todo: current offlineManager implementation does not support cancellation
        return Observable.create { emitter ->

            offlineManager.createOfflineRegion(
                definition,
                metadata,
                object : OfflineManager.CreateOfflineRegionCallback {
                    override fun onCreate(offlineRegion: OfflineRegion) {
                        Timber.d("Offline region created: %s", regionName)
                        startDownload(emitter, offlineRegion, regionName)
                    }

                    override fun onError(error: String) {
                        Timber.e("Error: %s", error)
                        emitter.onError(CreateOfflineRegionException(error))
                    }
                })
        }
    }

    @Throws(Exception::class)
    private fun constructMetdata(regionName: String): ByteArray {
        val jsonObject = JSONObject()
        jsonObject.put(OfflineManagerActivity.JSON_FIELD_REGION_NAME, regionName)
        val json = jsonObject.toString()
        return json.toByteArray(charset(OfflineManagerActivity.JSON_CHARSET))
    }

    private fun startDownload(
        emitter: ObservableEmitter<DownloadRegionEvent>,
        offlineRegion: OfflineRegion, regionName: String
    ) {
        // Set up an observer to handle download progress and
        // notify the user when the region is finished downloading
        offlineRegion.setObserver(object : OfflineRegion.OfflineRegionObserver {
            override fun onStatusChanged(status: OfflineRegionStatus) {
                // Compute a percentage
                val percentage = if (status.requiredResourceCount >= 0)
                    (100.0 * status.completedResourceCount / status.requiredResourceCount)
                else
                    0.0

                if (status.isComplete) {
                    // Download complete
                    emitter.onNext(DownloadRegionEvent.Done(regionName))
                    emitter.onComplete()
                    return
                } else if (status.isRequiredResourceCountPrecise) {
                    // Switch to determinate state
                    emitter.onNext(DownloadRegionEvent.Downloading(regionName, percentage))
                }

                // Log what is being currently downloaded
                Timber.d(
                    "%s/%s resources; %s bytes downloaded.",
                    (status.completedResourceCount).toString(),
                    (status.requiredResourceCount).toString(),
                    (status.completedResourceSize).toString()
                )
            }

            override fun onError(error: OfflineRegionError) {
                Timber.e("onError reason: %s", error.reason)
                Timber.e("onError message: %s", error.message)
            }

            override fun mapboxTileCountLimitExceeded(limit: Long) {
                Timber.e("Mapbox tile count limit exceeded: %s", limit)
            }
        })

        // Change the region state
        offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
    }
}