package com.example.trackingtest.model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackingtest.data.LocationData
import com.example.trackingtest.repository.LocationRepository
import kotlinx.coroutines.launch

class LocationViewModel(private val locationRepository: LocationRepository) : ViewModel() {
    fun getAllLocations(): LiveData<List<LocationData>> {
        val locationsLiveData = MutableLiveData<List<LocationData>>()
        viewModelScope.launch {
            try {
                locationsLiveData.value = locationRepository.getAllLocations()
            } catch (e: Exception) {
                locationsLiveData.value = emptyList()
            }
        }
        return locationsLiveData
    }

    fun getLocationCount(): LiveData<Int> {
        val countLiveData = MutableLiveData<Int>()
        viewModelScope.launch {
            try {
                val count = locationRepository.getLocationCount()
                Log.d("LocationViewModel", "Location count: $count")
                countLiveData.value = count
            } catch (e: Exception) {
                Log.e("LocationViewModel", "Error getting location count", e)
                countLiveData.value = 0
            }

        }
        return countLiveData
    }
    fun startTracking() {
        viewModelScope.launch {
            locationRepository.deleteAllLocations() // Clear previous data
        }
    }
}