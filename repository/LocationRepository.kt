package com.example.trackingtest.repository

import android.util.Log
import com.example.trackingtest.data.LocationDao
import com.example.trackingtest.data.LocationData

class LocationRepository(private val locationDao: LocationDao) {
    suspend fun insertLocation(locationData: LocationData) {
        locationDao.insertLocation(locationData)
    }

    suspend fun deleteAllLocations() {
        locationDao.deleteAllLocations()
    }

    suspend fun getLocationCount(): Int {
        return try {
            val count = locationDao.getLocationCount()
            count
        } catch (e: Exception) {
            0
        }
    }
    suspend fun getAllLocations(): List<LocationData> {
        return try {
            locationDao.getAllLocations()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

