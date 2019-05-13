package com.marchuck.latlngboundsfeature.domain

import com.mapbox.mapboxsdk.offline.*
import com.marchuck.latlngboundsfeature.domain.exceptions.CreateOfflineRegionException
import com.marchuck.latlngboundsfeature.domain.exceptions.OfflineRegionErrorException
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.subjects.BehaviorSubject
import org.json.JSONObject
import timber.log.Timber
import java.nio.charset.Charset

class DownloadRegionUseCase(val offlineManager: OfflineManager) {

    companion object {
        // JSON encoding/decoding
        val JSON_CHARSET: Charset = Charset.forName("UTF-8")
        val JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME"
    }

    val events: BehaviorSubject<DownloadRegionEvent> = BehaviorSubject.createDefault(DownloadRegionEvent.Idle)

    fun isNotRunning(): Boolean {
        val lastEvent = events.value
        return lastEvent is DownloadRegionEvent.Idle ||
                lastEvent is DownloadRegionEvent.Done
    }

    fun isRunning() = !isNotRunning()

    fun execute(regionName: String,
                definition: OfflineTilePyramidRegionDefinition) {
        executeImpl(regionName, definition).subscribe(events)
    }

    private fun executeImpl(regionName: String,
                            definition: OfflineTilePyramidRegionDefinition): Observable<DownloadRegionEvent> {

        // Build a JSONObject using the user-defined offline region title,
        // convert it into string, and use it to create a metadata variable.
        // The metadata variable will later be passed to createOfflineRegion()

        val metadata: ByteArray
        try {
            metadata = constructMetadata(regionName)
        } catch (exc: java.lang.Exception) {
            val readableMessage = "Metadata error: ${exc.localizedMessage}"
            return Observable.fromCallable { DownloadRegionEvent.Error(regionName, readableMessage, exc) }
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
    private fun constructMetadata(regionName: String): ByteArray {
        val jsonObject = JSONObject()
        jsonObject.put(JSON_FIELD_REGION_NAME, regionName)
        val json = jsonObject.toString()
        return json.toByteArray(JSON_CHARSET)
    }

    private fun emitDownloadStep(regionName: String,
                                 emitter: ObservableEmitter<DownloadRegionEvent>,
                                 status: OfflineRegionStatus) {

        val percentage = if (status.requiredResourceCount >= 0)
            (100.0 * status.completedResourceCount / status.requiredResourceCount)
        else
            0.0

        // Log what is being currently downloaded
        Timber.d(
                "%s/%s resources; %s bytes downloaded.",
                (status.completedResourceCount).toString(),
                (status.requiredResourceCount).toString(),
                (status.completedResourceSize).toString()
        )

        // Switch to determinate state
        emitter.onNext(DownloadRegionEvent.Downloading(regionName, percentage))
    }

    private fun startDownload(emitter: ObservableEmitter<DownloadRegionEvent>,
                              offlineRegion: OfflineRegion,
                              regionName: String) {
        // Set up an observer to handle download progress and
        // notify the user when the region is finished downloading
        offlineRegion.setObserver(object : OfflineRegion.OfflineRegionObserver {
            override fun onStatusChanged(status: OfflineRegionStatus) {
                if (emitter.isDisposed) {
                    //todo: cancel download

                    return
                }

                if (status.isComplete) {
                    // Download complete
                    emitter.onNext(DownloadRegionEvent.Done(regionName))
                    emitter.onComplete()

                    return
                } else if (status.isRequiredResourceCountPrecise) {
                    emitDownloadStep(regionName, emitter, status)
                }
            }

            override fun onError(error: OfflineRegionError) {
                Timber.e("onError reason: %s", error.reason)
                Timber.e("onError message: %s", error.message)
                if (!emitter.isDisposed) {
                    emitter.onError(OfflineRegionErrorException(error))
                }
            }

            override fun mapboxTileCountLimitExceeded(limit: Long) {
                Timber.e("Mapbox tile count limit exceeded: %s", limit)
                if (!emitter.isDisposed) {
                    emitter.onNext(DownloadRegionEvent.TileCountLimitExceeded(limit))
                }
            }
        })

        // Change the region state - this line triggers downloading
        offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
    }
}
