package com.example.trackingtest.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.trackingtest.MainActivity
import com.example.trackingtest.data.LocationData
import com.example.trackingtest.data.LocationDatabase
import com.example.trackingtest.repository.LocationRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LocationTrackingService : Service() {
    lateinit var locationRepository: LocationRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
        .build()
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            for (location in locationResult.locations) {
                // Save location data to database
                Log.d(
                    "Location",
                    "Latitude: ${location.latitude}, Longitude: ${location.longitude}"
                )
                val locationData =
                    LocationData(latitude = location.latitude, longitude = location.longitude)
                serviceScope.launch {
                    locationRepository.insertLocation(locationData)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val database = LocationDatabase.getDatabase(this)
        val locationDao = database.locationDao()
        locationRepository = LocationRepository(locationDao)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {


        val stopIntent = Intent(this, MainActivity::class.java)
        stopIntent.action = "STOP_TRACKING"

        val stopPendingIntent = PendingIntent.getActivity(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val channel = NotificationChannel(
            "location_tracking_channel",
            "Location Tracking Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, "location_tracking_channel")
            .setContentTitle("Location Tracking")
            .setContentText("Tracking your location...")
            .setSmallIcon(org.maplibre.android.R.drawable.maplibre_mylocation_icon_bearing) // Replace with your icon
            .addAction(
                com.google.android.material.R.drawable.ic_mtrl_chip_close_circle,
                "Stop Tracking",
                stopPendingIntent
            )
            .build()


        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            startForeground(1, notification)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

}