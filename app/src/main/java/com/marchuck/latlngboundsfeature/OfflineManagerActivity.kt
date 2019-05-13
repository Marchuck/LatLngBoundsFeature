package com.marchuck.latlngboundsfeature

import android.app.AlertDialog
import android.content.DialogInterface
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
import com.mapbox.mapboxsdk.offline.OfflineRegionError
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition
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


    var toast: Toast? = null
    val handler by lazy { Handler(Looper.getMainLooper()) }

    // Offline objects
    private val offlineManager: OfflineManager by lazy { OfflineManager.getInstance(this) }
    private var offlineRegion: OfflineRegion? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_offline_manager)

        // Set up the MapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { mapboxMap ->
            map = mapboxMap

            mapboxMap.setStyle(Style.MAPBOX_STREETS) {
                // Assign overlay_view for later use

                // Set up the offlineManager --already setup because of lazy init

                // Bottom navigation bar button clicks are handled here.
                // Download offline button
                download_button.setOnClickListener { downloadRegionDialog() }

                // List offline regions
                list_button.setOnClickListener { downloadedRegionList() }
            }

            toggle_layers_button.setOnClickListener {
                toggleLayer(mapboxMap, it)

            }
        }
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
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    private fun downloadRegionDialog() {
        // Set up download interaction. Display a dialog
        // when the user clicks download button and require
        // a user-provided region name
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
                // Require a region name to begin the download.
                // If the user-provided string is empty, display
                // a toast message and do not begin download.
                if (regionName.length == 0) {
                    Toast.makeText(this@OfflineManagerActivity, getString(R.string.dialog_toast), Toast.LENGTH_SHORT)
                        .show()
                } else {
                    // Begin download process
                    downloadRegion(regionName)
                }
            }
            .setNegativeButton(getString(android.R.string.cancel), object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which: Int) {
                    dialog.cancel()
                }
            })

        // Display the dialog
        builder.show()
    }

    private fun downloadRegion(regionName: String) {
        // Define offline region parameters, including bounds,
        // min/max zoom, and metadata

        // Start the overlay_view
        startProgress()

        // Create offline definition using the current
        // style and boundaries of visible map area
        val styleUrl = map!!.style!!.url
        val bounds = map!!.projection.visibleRegion.latLngBounds
        val minZoom = map!!.cameraPosition.zoom
        val maxZoom = map!!.maxZoomLevel
        val pixelRatio = this.resources.displayMetrics.density
        val definition = OfflineTilePyramidRegionDefinition(
            styleUrl, bounds, minZoom, maxZoom, pixelRatio
        )

        // Build a JSONObject using the user-defined offline region title,
        // convert it into string, and use it to create a metadata variable.
        // The metadata variable will later be passed to createOfflineRegion()
        var metadata: ByteArray? = null
        try {
            val jsonObject = JSONObject()
            jsonObject.put(JSON_FIELD_REGION_NAME, regionName)
            val json = jsonObject.toString()
            metadata = json.toByteArray(charset(JSON_CHARSET))
        } catch (exception: Exception) {
            Timber.e("Failed to encode metadata: %s", exception.message)
            metadata = null
        }

        // Create the offline region and launch the download
        offlineManager.createOfflineRegion(
            definition,
            metadata!!,
            object : OfflineManager.CreateOfflineRegionCallback {
                override fun onCreate(offlineRegion: OfflineRegion) {
                    Timber.d("Offline region created: %s", regionName)
                    this@OfflineManagerActivity.offlineRegion = offlineRegion
                    launchDownload()
                }

                override fun onError(error: String) {
                    Timber.e("Error: %s", error)
                }
            })
    }

    private fun launchDownload() {
        // Set up an observer to handle download progress and
        // notify the user when the region is finished downloading
        offlineRegion?.setObserver(object : OfflineRegion.OfflineRegionObserver {
            override fun onStatusChanged(status: OfflineRegionStatus) {
                // Compute a percentage
                val percentage = if (status.requiredResourceCount >= 0)
                    (100.0 * status.completedResourceCount / status.requiredResourceCount)
                else
                    0.0

                if (status.isComplete) {
                    // Download complete
                    endProgress("download complete")
                    return
                } else if (status.isRequiredResourceCountPrecise) {
                    // Switch to determinate state
                    setPercentage(Math.round(percentage).toInt())
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
        offlineRegion?.setDownloadState(OfflineRegion.STATE_ACTIVE)
    }

    private fun downloadedRegionList() {
        // Build a region list when the user clicks the list button

        // Reset the region selected int to 0
        regionSelected = 0

        // Query the DB asynchronously
        offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>?) {
                // Check result. If no regions have been
                // downloaded yet, notify user and return
                if (offlineRegions.isNullOrEmpty()) {
                    Toast.makeText(applicationContext, "no regions yet", Toast.LENGTH_SHORT)
                        .show()
                    return
                }

                // Add all of the region names to a list
                val offlineRegionsNames = ArrayList<String>()
                for (offlineRegion in offlineRegions) {
                    offlineRegionsNames.add(getRegionName(offlineRegion))
                }
                val items = offlineRegionsNames.toTypedArray<CharSequence>()

                // Build a dialog containing the list of regions
                val dialog = AlertDialog.Builder(this@OfflineManagerActivity)
                    .setTitle("Choose offline map")
                    .setSingleChoiceItems(items, 0, object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface, which: Int) {
                            // Track which region the user selects
                            regionSelected = which
                        }
                    })
                    .setPositiveButton("NAVIGATE",
                        object : DialogInterface.OnClickListener {
                            override fun onClick(dialog: DialogInterface, id: Int) {

                                Toast.makeText(this@OfflineManagerActivity, items[regionSelected], Toast.LENGTH_LONG)
                                    .show()

                                // Get the region bounds and zoom
                                val bounds =
                                    (offlineRegions[regionSelected].definition as OfflineTilePyramidRegionDefinition).bounds
                                val regionZoom =
                                    (offlineRegions[regionSelected].definition as OfflineTilePyramidRegionDefinition).minZoom

                                // Create new camera position
                                val cameraPosition = CameraPosition.Builder()
                                    .target(bounds.center)
                                    .zoom(regionZoom)
                                    .build()

                                // Move camera to new position
                                map!!.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

                            }
                        })
                    .setNeutralButton(
                        "DELETE",
                        object : DialogInterface.OnClickListener {
                            override fun onClick(dialog: DialogInterface, id: Int) {
                                // Make overlay_view indeterminate and
                                // set it to visible to signal that
                                // the deletion process has begun
                                overlay_view.visibility = View.VISIBLE

                                // Begin the deletion process
                                offlineRegions[regionSelected].delete(object :
                                    OfflineRegion.OfflineRegionDeleteCallback {
                                    override fun onDelete() {
                                        // Once the region is deleted, remove the
                                        // overlay_view and display a toast
                                        overlay_view.visibility = View.INVISIBLE
                                        Toast.makeText(
                                            applicationContext, "region deleted",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }

                                    override fun onError(error: String) {
                                        overlay_view.visibility = View.INVISIBLE
                                        Timber.e("Error: %s", error)
                                    }
                                })
                            }
                        })
                    .setNegativeButton(
                        "CANCEL",
                        object : DialogInterface.OnClickListener {
                            override fun onClick(dialog: DialogInterface, id: Int) {
                                // When the user cancels, don't do anything.
                                // The dialog will automatically close
                            }
                        }).create()
                dialog.show()

            }

            override fun onError(error: String) {
                Timber.e("Error: %s", error)
            }
        })
    }

    private fun getRegionName(offlineRegion: OfflineRegion): String {
        // Get the region name from the offline region metadata

        try {
            val metadata = offlineRegion.metadata
            val json = String(metadata, Charset.forName(JSON_CHARSET))
            val jsonObject = JSONObject(json)
            return jsonObject.getString(JSON_FIELD_REGION_NAME)
        } catch (exception: Exception) {
            Timber.e("Failed to decode metadata: %s", exception.message)
            return String.format("region id: %d", offlineRegion.id)
        }

    }

    // Progress bar methods
    private fun startProgress() {
        // Disable buttons
        download_button.isEnabled = false
        list_button.isEnabled = false

        // Start and show the progress bar
        isEndNotified = false
        overlay_view.visibility = View.VISIBLE
    }

    private fun setPercentage(percentage: Int) {
        handler.post {
            showToast("Downloading ($percentage %)")
        }
    }

    private fun showToast(message: String) {
        toast?.cancel()
        toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        toast?.show()
    }

    private fun endProgress(message: String) {
        // Don't notify more than once
        if (isEndNotified) {
            return
        }

        // Enable buttons
        download_button.isEnabled = true
        list_button.isEnabled = true

        // Stop and hide the progress bar
        isEndNotified = true
        overlay_view.visibility = View.GONE

        // Show a toast
        Toast.makeText(this@OfflineManagerActivity, message, Toast.LENGTH_LONG).show()
    }

    companion object {

        private val TAG = "OffManActivity"

        // JSON encoding/decoding
        val JSON_CHARSET = "UTF-8"
        val JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME"
    }


}
