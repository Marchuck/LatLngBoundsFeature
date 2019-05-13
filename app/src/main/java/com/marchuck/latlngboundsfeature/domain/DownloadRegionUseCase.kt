package com.marchuck.latlngboundsfeature.domain

import com.mapbox.mapboxsdk.offline.OfflineRegion
import com.mapbox.mapboxsdk.offline.OfflineManager
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus
import com.mapbox.mapboxsdk.offline.OfflineRegionError
import com.marchuck.latlngboundsfeature.domain.exceptions.RegionDownloadException
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.subjects.BehaviorSubject
import org.json.JSONObject
import timber.log.Timber
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

/**
 * todo: check if it works with process kill and introduce additional states when internet connectivity lost
 */
class DownloadRegionUseCase(private val offlineManager: OfflineManager,
                            private val getRegionsUseCase: GetRegionsUseCase) {

    companion object {
        val JSON_CHARSET: Charset = Charset.forName("UTF-8")
        const val JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME"
    }

    private val events: BehaviorSubject<DownloadRegionEvent> = BehaviorSubject.createDefault(DownloadRegionEvent.Idle)

    fun observeDownloadEvents(): Observable<DownloadRegionEvent> = events

    fun isRunning() = events.value !is DownloadRegionEvent.Idle

    fun execute(regionName: String,
                definition: OfflineTilePyramidRegionDefinition) {
        val unmanagableDisposable = executeImpl(regionName, definition)
                .subscribe({
                    events.onNext(it)
                }, {
                    if (it is RegionDownloadException) {
                        events.onNext(DownloadRegionEvent.Error(regionName, it))
                    } else {
                        events.onNext(DownloadRegionEvent.Error(regionName, RegionDownloadException.UnrecognizedError(it)))
                    }
                })
    }

    private fun executeImpl(regionName: String,
                            definition: OfflineTilePyramidRegionDefinition): Observable<DownloadRegionEvent> {

        val metadata: ByteArray
        try {
            metadata = constructMetadata(regionName)
        } catch (exc: java.lang.Exception) {
            return Observable.error(RegionDownloadException.MetadataError(regionName, exc))
        }

        return getRegionsUseCase.execute()
                .toObservable()
                .flatMap { existingRegions ->
                    for (region in existingRegions) {
                        val regionNamed = getRegionName(region)
                        println("region $regionName, region iterated $regionNamed")
                        if (regionName == regionNamed) {
                            val exception = RegionDownloadException.RegionNameExists(region)
                            return@flatMap Observable.error<DownloadRegionEvent>(exception)
                        }
                    }
                    return@flatMap download(regionName, metadata, definition)
                }
    }

    fun getRegionName(offlineRegion: OfflineRegion): String {
        return try {
            val metadata = offlineRegion.metadata
            val json = String(metadata, JSON_CHARSET)
            val jsonObject = JSONObject(json)
            jsonObject.getString(JSON_FIELD_REGION_NAME)
        } catch (exception: Exception) {
            Timber.e("Failed to decode metadata: %s", exception.message)
            String.format("region id: %d", offlineRegion.id)
        }
    }

    private fun download(regionName: String,
                         metadata: ByteArray,
                         definition: OfflineTilePyramidRegionDefinition): Observable<DownloadRegionEvent> {
        return Observable.create { emitter ->

            offlineManager.createOfflineRegion(definition, metadata, object : OfflineManager.CreateOfflineRegionCallback {
                override fun onCreate(offlineRegion: OfflineRegion) {
                    Timber.d("Offline region created: %s", regionName)
                    startDownload(emitter, offlineRegion, regionName)
                }

                override fun onError(error: String) {
                    Timber.e("Error: %s", error)
                    emitter.onError(RegionDownloadException.CreateRegionError(error))
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

        val percentage = calculateProgress(status)

        //avoid duplicate progresses during casting
        if (percentage != lastPercentageSpotted) {
            Timber.d("%s/%s resources; %s bytes downloaded.",
                    (status.completedResourceCount).toString(),
                    (status.requiredResourceCount).toString(),
                    (status.completedResourceSize).toString())
            Timber.d("percentage downloaded $percentage")
            lastPercentageSpotted = percentage

            emitter.onNext(DownloadRegionEvent.Downloading(regionName, lastPercentageSpotted))
        } else {
            val currentTime = System.currentTimeMillis()
            val difference = currentTime - lastPercentageTime
            val seconds = TimeUnit.MILLISECONDS.toSeconds(difference)
            if (seconds >= 30) {
                emitter.onError(RegionDownloadException.Timeout)
            }
        }
    }

    private fun calculateProgress(status: OfflineRegionStatus): Int {
        return if (status.requiredResourceCount >= 0)
            (100.0 * status.completedResourceCount / status.requiredResourceCount).toInt()
        else
            0
    }

    @Volatile
    private var lastPercentageSpotted: Int = 0
        set(value) {
            field = value
            lastPercentageTime = System.currentTimeMillis()
        }

    @Volatile
    private var lastPercentageTime: Long = 0L

    private fun startDownload(emitter: ObservableEmitter<DownloadRegionEvent>,
                              offlineRegion: OfflineRegion,
                              regionName: String) {

        lastPercentageSpotted = 0

        offlineRegion.setObserver(object : OfflineRegion.OfflineRegionObserver {
            override fun onStatusChanged(status: OfflineRegionStatus) {
                if (emitter.isDisposed) {
                    //todo: cancel download, but current implementation of mapBox
                    // can only pause it

                    return
                }

                if (status.isComplete) {
                    //reset last seen percentage
                    lastPercentageSpotted = 0
                    emitter.onNext(DownloadRegionEvent.Done(regionName, System.currentTimeMillis()))
                    emitter.onComplete()
                } else if (status.isRequiredResourceCountPrecise) {
                    emitDownloadStep(regionName, emitter, status)
                }
            }

            override fun onError(error: OfflineRegionError) {
                Timber.e("onError reason: %s", error.reason)
                Timber.e("onError message: %s", error.message)
                if (!emitter.isDisposed) {
                    emitter.onError(RegionDownloadException.RegionError(error))
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
        //state INACITVE pauses it
        offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
    }
}
