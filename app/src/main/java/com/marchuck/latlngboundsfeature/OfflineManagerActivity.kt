package com.marchuck.latlngboundsfeature

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.EditText
import android.widget.Toast

import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.maps.Style.*
import com.mapbox.mapboxsdk.offline.OfflineManager
import com.mapbox.mapboxsdk.offline.OfflineRegion
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition
import com.marchuck.latlngboundsfeature.domain.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.SerialDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_offline_manager.*

import org.json.JSONObject
import timber.log.Timber
import java.nio.charset.Charset

import java.util.ArrayList


/**
 * Download, view, navigate to, and delete an offline region.
 */
class OfflineManagerActivity : AppCompatActivity() {

    // UI elements
    private var map: MapboxMap? = null

    private var isEndNotified: Boolean = false
    private var regionSelected: Int = 0


    var downloadOfflineMapsUseCase: DownloadRegionUseCase? = null

    val deleteRegionUseCase = DeleteRegionUseCase()
    val getRegionsUseCase by lazy { GetRegionsUseCase(OfflineManager.getInstance(this)) }

    var toast: Toast? = null
    val handler by lazy { Handler(Looper.getMainLooper()) }

    val renderDownloadStatusDisposable = SerialDisposable()
    val getRegionsDisposable = SerialDisposable()
    val deleteRegionDisposable = SerialDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        downloadOfflineMapsUseCase = App.getInstance(this).downloadOfflineMapsUseCase

        // This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_offline_manager)

        // Set up the MapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { mapboxMap ->
            map = mapboxMap

            mapboxMap.setStyle(MAPBOX_STREETS) {
                // Assign overlay_view for later use

                // Set up the offlineManager --already setup because of lazy init

                // Bottom navigation bar button clicks are handled here.
                // Download offline button
                download_button.setOnClickListener { downloadRegionDialog() }

                // List offline regions
                list_button.setOnClickListener { renderAlreadyDownloadedRegionsList() }
            }

            toggle_layers_button.setOnClickListener {
                toggleLayer(mapboxMap, it)
            }
        }

        if (downloadOfflineMapsUseCase?.isRunning() == true) {
            observeDownloadEvents()
        }
    }

    private fun observeDownloadEvents() {
        renderDownloadStatusDisposable.set(downloadOfflineMapsUseCase?.events
                ?.observeOn(AndroidSchedulers.mainThread())?.subscribe({

                    when (it) {
                        is DownloadRegionEvent.Downloading -> {
                            overlay_view.visibility = View.VISIBLE
                            showToast("${it.regionName} download in progress...\n${it.progress} %")
                        }
                        is DownloadRegionEvent.Done -> {
                            overlay_view.visibility = View.GONE
                            showToast("${it.regionName} download done")
                        }
                        is DownloadRegionEvent.Idle -> {
                            overlay_view.visibility = View.GONE

                        }
                        is DownloadRegionEvent.Error -> {
                            overlay_view.visibility = View.GONE
                            showToast("${it.regionName} download error: ${it.readableMessage}")
                        }

                        is DownloadRegionEvent.TileCountLimitExceeded -> {
                            overlay_view.visibility = View.GONE
                            showToast("exceeded limit of downloaded tiles. please contact sales")
                        }
                    }

                }, {
                    showToast("unrecognized error")

                })
        )
    }

    // Override Activity lifecycle methods
    public override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    public override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        renderDownloadStatusDisposable.dispose()
        getRegionsDisposable.dispose()
        deleteRegionDisposable.dispose()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    private fun toggleLayer(mapboxMap: MapboxMap, button: View) {
        var index = button.tag as? Int ?: 0

        val stylesAndNames = arrayListOf(
                MAPBOX_STREETS to "streets",
                OUTDOORS to "outdoors",
                LIGHT to "light",
                DARK to "dark",
                getString(R.string.style_satellite) to "satellite",
                SATELLITE_STREETS to "satellite streets",
                TRAFFIC_DAY to "traffic day",
                TRAFFIC_NIGHT to "traffic night",
                getString(R.string.style_greenspace) to "greenspace",
                getString(R.string.style_aerial) to "aerial",
                getString(R.string.style_night) to "night"
        )
        index = (index + 1) % stylesAndNames.size
        button.tag = index

        val styleToUse = stylesAndNames[index]
        val style = Style.Builder().fromUrl(styleToUse.first)
        showToast(styleToUse.second)
        mapboxMap.setStyle(style)
    }


    private fun downloadRegionDialog() {
        val builder = AlertDialog.Builder(this@OfflineManagerActivity)

        val regionNameEdit = EditText(this@OfflineManagerActivity)
        regionNameEdit.hint = getString(R.string.set_region_name_hint)

        // Build the dialog box
        builder.setTitle("Download region")
                .setView(regionNameEdit)
                .setMessage("Enter region name")
                .setPositiveButton(
                        getString(R.string.dialog_positive_button)
                ) { dialog, which ->
                    val regionName = regionNameEdit.text.toString()
                    if (regionName.isEmpty()) {
                        showToast(getString(R.string.dialog_toast))
                    } else {
                        downloadRegion(regionName)
                    }
                }
                .setNegativeButton(getString(android.R.string.cancel)) { dialog, which -> dialog.cancel() }

        // Display the dialog
        builder.show()
    }

    private fun downloadRegion(regionName: String) {

        val styleUrl = map!!.style!!.url
        val bounds = map!!.projection.visibleRegion.latLngBounds
        val minZoom = map!!.cameraPosition.zoom
        val maxZoom = map!!.maxZoomLevel
        val pixelRatio = this.resources.displayMetrics.density
        val definition = OfflineTilePyramidRegionDefinition(styleUrl, bounds, minZoom, maxZoom, pixelRatio)

        overlay_view.visibility = View.VISIBLE

        downloadOfflineMapsUseCase?.execute(regionName, definition)
        observeDownloadEvents()
    }

    private fun renderAlreadyDownloadedRegionsList() {
        regionSelected = 0
        getRegionsDisposable.set(getRegionsUseCase
                .execute()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    onListReceived(it)
                }, {
                }))
    }

    private fun onListReceived(offlineRegions: OfflineRegions) {
        if (offlineRegions.isNullOrEmpty()) {
            showToast("no regions yet")
            return
        }

        val offlineRegionsNames = ArrayList<String>()
        for (offlineRegion in offlineRegions) {
            offlineRegionsNames.add(getRegionName(offlineRegion))
        }
        val items = offlineRegionsNames.toTypedArray<CharSequence>()

        val dialog = AlertDialog.Builder(this@OfflineManagerActivity)
                .setTitle("Choose offline map")
                .setSingleChoiceItems(items, 0) { dialog, which ->
                    // Track which region the user selects
                    regionSelected = which
                }
                .setPositiveButton("NAVIGATE TO") { dialog, id ->
                    showToast(items[regionSelected].toString())

                    val offlineRegionSelected = offlineRegions[regionSelected]
                    val bounds = offlineRegionSelected.definition.bounds
                    val regionZoom = offlineRegionSelected.definition.minZoom
                    val cameraPosition = CameraPosition.Builder()
                            .target(bounds.center)
                            .zoom(regionZoom)
                            .build()
                    map?.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                }
                .setNeutralButton("DELETE") { dialog, id ->
                    overlay_view.visibility = View.VISIBLE

                    deleteRegionDisposable.set(deleteRegionUseCase
                            .execute(offlineRegions[regionSelected]).subscribe({
                                overlay_view.visibility = View.GONE
                                showToast("region deleted")

                            }, {
                                overlay_view.visibility = View.GONE
                                showToast("failed to delete region: ${it.message}")
                            }))
                }
                .setNegativeButton(
                        "CANCEL"
                ) { dialog, id ->

                }.create()
        dialog.show()
    }

    private fun getRegionName(offlineRegion: OfflineRegion): String {
        return try {
            val metadata = offlineRegion.metadata
            val json = String(metadata, Charset.forName(JSON_CHARSET))
            val jsonObject = JSONObject(json)
            jsonObject.getString(JSON_FIELD_REGION_NAME)
        } catch (exception: Exception) {
            Timber.e("Failed to decode metadata: %s", exception.message)
            String.format("region id: %d", offlineRegion.id)
        }
    }

    private fun showToast(message: String) {
        toast?.cancel()
        toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        toast?.show()
    }

    companion object {

        private val TAG = "OffManActivity"

        val JSON_CHARSET = "UTF-8"
        val JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME"
    }


}
