package com.example.trackingtest.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LocationDao {
    @Insert
    suspend fun insertLocation(locationData: LocationData)

    @Query("DELETE FROM location_data")
    suspend fun deleteAllLocations()

    @Query("SELECT COUNT(*) FROM location_data")
    suspend fun getLocationCount(): Int

    @Query("SELECT * FROM location_data")
    suspend fun getAllLocations(): List<LocationData>
}