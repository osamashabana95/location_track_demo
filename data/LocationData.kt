package com.example.trackingtest.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_data")
data class LocationData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)