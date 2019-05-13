package com.marchuck.latlngboundsfeature

import android.app.AlertDialog
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.maps.Style.*
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition
import com.marchuck.latlngboundsfeature.domain.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.SerialDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_offline_manager.*
import java.util.*


/**
 * Download, view, navigate to, and delete an offline region.
 */
class OfflineManagerActivity : AppCompatActivity() {

    private var map: MapboxMap? = null
    var toast: Toast? = null
    private var regionSelected: Int = 0

    private val downloadOfflineMapsUseCase: DownloadRegionUseCase by lazy { App.getInstance(this).downloadOfflineMapsUseCase }
    private val deleteRegionUseCase = DeleteRegionUseCase()
    private val stylesProvider by lazy { StylesProvider(AndroidResourceRepository(resources)) }
    private val getRegionsUseCase by lazy { App.getInstance(this).getRegionsUseCase }

    private val renderDownloadStatusDisposable = SerialDisposable()
    private val getRegionsDisposable = SerialDisposable()
    private val deleteRegionDisposable = SerialDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline_manager)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { mapboxMap ->
            map = mapboxMap

            mapboxMap.setStyle(MAPBOX_STREETS) {
                download_button.setOnClickListener { downloadRegionDialog() }

                list_button.setOnClickListener { renderAlreadyDownloadedRegionsList() }
            }

            toggle_layers_button.setOnClickListener {
                toggleLayer(mapboxMap, it)
            }
        }

        if (downloadOfflineMapsUseCase.isRunning()) {
            observeDownloadEvents()
        }
    }

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

    private fun observeDownloadEvents() {
        renderDownloadStatusDisposable.set(downloadOfflineMapsUseCase
                .observeDownloadEvents()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({

                    when (it) {
                        is DownloadRegionEvent.Downloading -> {
                            overlay_view.visibility = View.VISIBLE

                            val formattedProgress = String.format("%.2f", it.progress.toFloat())

                            showToast("${it.regionName} download in progress...\n $formattedProgress %")
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
                            it.throwable.printStackTrace()
                        }

                        is DownloadRegionEvent.TileCountLimitExceeded -> {
                            overlay_view.visibility = View.GONE
                            showToast("exceeded limit of downloaded tiles. please contact sales")
                        }
                    }

                }, {
                    overlay_view.visibility = View.GONE
                    showToast("unrecognized error: ${it.message}")
                    it.printStackTrace()

                })
        )
    }

    private fun toggleLayer(mapboxMap: MapboxMap, button: View) {
        var index = button.tag as? Int ?: 0

        val stylesAndNames = stylesProvider.provideStyles()
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
        regionNameEdit.hint = "specify region name"

        // Build the dialog box
        builder.setTitle("Download region")
                .setView(regionNameEdit)
                .setMessage("Whole screen bounds will be downloaded")
                .setPositiveButton(
                        "Download"
                ) { dialog, which ->
                    val regionName = regionNameEdit.text.toString()
                    if (regionName.isEmpty()) {
                        showToast("Please enter non-empty value")
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

        downloadOfflineMapsUseCase.execute(regionName, definition)
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
            offlineRegionsNames.add(downloadOfflineMapsUseCase.getRegionName(offlineRegion))
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


    private fun showToast(message: String) {
        toast?.cancel()
        toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        toast?.show()
    }
}
