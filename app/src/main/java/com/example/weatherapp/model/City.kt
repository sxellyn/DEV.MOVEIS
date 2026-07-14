package com.example.weatherapp.model

import com.google.android.gms.maps.model.LatLng

data class City(
    val name: String,
    var location: LatLng? = null,
    val isMonitored: Boolean = false
)
