package com.example.trackingtest


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.trackingtest.data.LocationDatabase
import com.example.trackingtest.model.LocationViewModel
import com.example.trackingtest.repository.LocationRepository
import com.example.trackingtest.service.LocationTrackingService
import com.google.gson.Gson
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.location.LocationComponent
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.permissions.PermissionsListener
import org.maplibre.android.location.permissions.PermissionsManager
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.maps.Style

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private var lastLocation: Location? = null
    private lateinit var mapView: MapView
    private var permissionsManager: PermissionsManager? = null
    private var locationComponent: LocationComponent? = null
    private lateinit var maplibreMap: MapLibreMap
    val key = BuildConfig.MAPTILER_API_KEY
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var locationViewModel: LocationViewModel
    val mapId = "streets-v2"

    val styleUrl = "https://api.maptiler.com/maps/$mapId/style.json?key=$key"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapLibre.getInstance(this, key, WellKnownTileServer.MapLibre)

        setContentView(R.layout.activity_main)
        if (intent?.action == "STOP_TRACKING") {
            stopTracking()
        }
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)

        sharedPreferences = getSharedPreferences("my_prefs", Context.MODE_PRIVATE)

        val database = LocationDatabase.getDatabase(this)
        val locationDao = database.locationDao()

        val locationRepository = LocationRepository(locationDao)

        val viewModelFactory = LocationViewModelFactory(locationRepository)


        locationViewModel = ViewModelProvider(this, viewModelFactory)[LocationViewModel::class.java]
        val trackButton = findViewById<Button>(R.id.trackButton)
        trackButton.text =
            if (sharedPreferences.getBoolean("isTracking", false)) "Stop Tracking" else "Track"
        trackButton.setOnClickListener {

            if (sharedPreferences.getBoolean("isTracking", false)) {
                stopTracking()
                trackButton.text = "Track"
            } else {
                locationViewModel.startTracking()
                startTracking()
                trackButton.text = "Stop Tracking"
            }
        }
        val showButton = findViewById<Button>(R.id.showTrackButton)

        showButton.setOnClickListener {
            locationViewModel.getLocationCount().observe(this) { count ->
                if (count <= 1) {
                    Toast.makeText(
                        this,
                        "Not enough location points to show and create track line click track button",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    locationViewModel.getAllLocations().observe(this) { locations ->
                        val intent = Intent(this, DetailsActivity::class.java)
                        val locationsJson = Gson().toJson(locations)

                        intent.putExtra("locations", locationsJson)
                        startActivity(intent)
                    }
                }
            }

        }
        //  checkPermissions()
    }

    private fun checkPermissions() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            if (!isGpsEnabled()) {
                showGpsEnablePrompt()
            } else {
                mapView.getMapAsync(this)
            }
        } else {
            permissionsManager = PermissionsManager(object : PermissionsListener {
                override fun onExplanationNeeded(permissionsToExplain: List<String>) {
                    Toast.makeText(
                        this@MainActivity,
                        "You need to accept location permissions.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onPermissionResult(granted: Boolean) {
                    if (granted) {
                        if (!isGpsEnabled()) {
                            showGpsEnablePrompt()
                        } else {
                            mapView.getMapAsync(this@MainActivity)
                        }
                    } else {
                        finish()
                    }
                }
            })
            permissionsManager!!.requestLocationPermissions(this)
        }
    }

    private fun isGpsEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun showGpsEnablePrompt() {
        AlertDialog.Builder(this).setTitle("Enable GPS")
            .setMessage("GPS is required for this app to function properly. Please enable GPS in your device settings.")
            .setPositiveButton("Settings") { dialog, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                dialog.dismiss()
            }.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                finish() // Or handle the case where the user cancels
            }.setCancelable(false).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager!!.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(maplibreMap: MapLibreMap) {
        this.maplibreMap = maplibreMap
        maplibreMap.setStyle(styleUrl) { style: Style ->
            val currentCameraPosition = maplibreMap.cameraPosition
            val newCameraPosition = CameraPosition.Builder(currentCameraPosition)
                .zoom(14.0) // Set your desired zoom level here
                .build()
            maplibreMap.cameraPosition = newCameraPosition//flyTo(newCameraPosition)

            locationComponent = maplibreMap.locationComponent
            val locationComponentOptions =
                LocationComponentOptions.builder(this@MainActivity).pulseEnabled(true).build()
            val locationComponentActivationOptions =
                buildLocationComponentActivationOptions(style, locationComponentOptions)
            locationComponent!!.activateLocationComponent(locationComponentActivationOptions)
            locationComponent!!.isLocationComponentEnabled = true
            locationComponent!!.cameraMode = CameraMode.TRACKING
            locationComponent!!.forceLocationUpdate(lastLocation)
        }
    }

    private fun buildLocationComponentActivationOptions(
        style: Style, locationComponentOptions: LocationComponentOptions
    ): LocationComponentActivationOptions {
        return LocationComponentActivationOptions.builder(this, style)
            .locationComponentOptions(locationComponentOptions).useDefaultLocationEngine(true)
            .locationEngineRequest(
                LocationEngineRequest.Builder(750).setFastestInterval(750)
                    .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)

                    .build()
            ).build()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        checkPermissions()
        if (intent?.action == "STOP_TRACKING") {
            stopTracking()
        }

    }

    override fun onResume() {
        super.onResume()
        //mapView.onCreate(Bundle())
        mapView.onResume()

    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    private fun startTracking() {
        sharedPreferences.edit().putBoolean("isTracking", true).apply()
        val serviceIntent = Intent(this, LocationTrackingService::class.java)
        startForegroundService(serviceIntent)
    }

    private fun stopTracking() {
        sharedPreferences = getSharedPreferences("my_prefs", Context.MODE_PRIVATE)

        sharedPreferences.edit().putBoolean("isTracking", false).apply()

        val serviceIntent = Intent(this, LocationTrackingService::class.java)
        stopService(serviceIntent)
    }

}

class LocationViewModelFactory(private val locationRepository: LocationRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LocationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return LocationViewModel(locationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}