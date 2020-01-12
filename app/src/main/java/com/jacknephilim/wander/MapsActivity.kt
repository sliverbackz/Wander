package com.jacknephilim.wander


import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.os.Looper
import android.os.PersistableBundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.activity_maps.*
import org.jetbrains.anko.alert

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private val TAG = MapsActivity::class.java.simpleName
    private val REQUEST_LOCATION_PERMISSION = 1
    private val REQUEST_BACKGROUND_PERMISSION = 2
    private val REQUESTING_LOCATION_UPDATES_KEY = "is_location_update"
    private val REQUEST_CHECK_SETTINGS = 2
    private var requestingLocationUpdates = false


    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onResume() {
        super.onResume()
        if (requestingLocationUpdates) {
            startLocationUpdates()
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, requestingLocationUpdates)
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create()?.apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun updateValuesFromBundle(savedInstanceState: Bundle?) {
        savedInstanceState ?: return
        if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
            requestingLocationUpdates =
                savedInstanceState.getBoolean(REQUESTING_LOCATION_UPDATES_KEY)
        }
        // Update UI to match restored state
        //updateUI()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        alert {
            message = "Do you want to allow location update?"
            positiveButton("Yes") {
                requestingLocationUpdates = true
                it.dismiss()
            }
            negativeButton("No") {
                requestingLocationUpdates = false
                it.dismiss()
            }
        }.show()

        updateValuesFromBundle(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        //getting lastLocation from google location services
        fusedLocationClient.lastLocation.addOnCompleteListener { location ->
            if (location.isSuccessful) {
                Log.i("LatLog", "${location.result?.latitude} ${location.result?.longitude}")
                tv_lat.text = getString(R.string.str_latitude, location.result?.latitude)
                tv_lng.text = getString(R.string.str_longitude, location.result?.longitude)
            }

        }

        createLocationRequest()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    tv_lat.text = getString(R.string.str_latitude, location.latitude)
                    tv_lng.text = getString(R.string.str_longitude, location.longitude)
                }
            }
        }


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun isPermissionGranted(): Boolean {
        return PermissonUtils.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun isBackgroundLocationPermissionGranted(): Boolean {
        return PermissonUtils.hasPermissions(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    private fun enableMyLocation() {
        if (isPermissionGranted()) {
            map.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    private fun enableBackgroundLocation() {
        if (!isBackgroundLocationPermissionGranted()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                REQUEST_BACKGROUND_PERMISSION
            )
        } else {
            //I do what I want to do.
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        val latitude = 16.809712
        val longitude = 96.131717
        val zoomLevel = 15f
        val overlaySize = 100f

        val homeLatLng = LatLng(latitude, longitude)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(homeLatLng, zoomLevel))
        val androidOverlay = GroundOverlayOptions()
            .image(BitmapDescriptorFactory.fromResource(R.drawable.android))
            .position(homeLatLng, overlaySize)
        //   map.addGroundOverlay(androidOverlay)
        map.addMarker(MarkerOptions().position(homeLatLng))
        googleMap.apply {
            setMapLongClick(this)
            setPoiClick(this)
            setMapStyle(this)
        }
        enableMyLocation()
        enableBackgroundLocation()

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.map_options, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }

        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }

        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                if (grantResults.contains(PackageManager.PERMISSION_GRANTED)) {
                    enableMyLocation()
                }
            }

            REQUEST_BACKGROUND_PERMISSION -> {
                if (grantResults.contains(PackageManager.PERMISSION_GRANTED)) {
                    Log.i("BackPermisson", "granted")

                } else {

                    Log.i("BackPermisson", "denied")

                }
            }

        }

    }

    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapClickListener { latLng ->

            val snippet = String.format(
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )
            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )
            poiMarker.showInfoWindow()
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this,
                    R.raw.map_style
                )
            )
            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }

        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    private fun createLocationRequest() {
        val locationRequest = LocationRequest.create()?.apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val reqBuilder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest!!)

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(reqBuilder.build())

        task.addOnCompleteListener { locationSettingsResponse ->
            // All location settings are satisfied. The client can initialize
            // location requests here.
            Log.i("LocationResponse", locationSettingsResponse.isSuccessful.toString())

        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                Log.i("LocationReqFailure", exception.toString())
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                    Log.i("RequestEx", "Ok")

                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            Log.i("RequestResult", "Ok")
        }
    }
}
