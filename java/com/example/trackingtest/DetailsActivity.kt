package com.example.trackingtest

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.trackingtest.data.LocationData
import com.google.gson.Gson
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback

class DetailsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mapView: MapView
    val mapId = "streets-v2"
    val key = BuildConfig.MAPTILER_API_KEY

    val styleUrl = "https://api.maptiler.com/maps/$mapId/style.json?key=$key"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        mapView = findViewById(R.id.detailsMapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    override fun onMapReady(maplibreMap: MapLibreMap) {
        maplibreMap.setStyle(styleUrl) { style ->
            val locations =
                Gson().fromJson(intent.getStringExtra("locations"), Array<LocationData>::class.java)
                    ?.toList()
            if (locations != null && locations.size > 1) {
                val latLngs = locations.map { LatLng(it.latitude, it.longitude) }
                val polylineOptions = PolylineOptions()
                    .addAll(latLngs)
                    .color(Color.RED)
                    .width(14.0f)
                maplibreMap.addPolyline(polylineOptions)

                val boundsBuilder = LatLngBounds.Builder()
                latLngs.forEach { boundsBuilder.include(it) }
                val bounds = boundsBuilder.build()

                val padding = 534
                val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
                maplibreMap.animateCamera(cameraUpdate)
            }
        }
    }

}