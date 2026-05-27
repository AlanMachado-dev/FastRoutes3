package com.example.fastroutes.data.model


import com.google.android.gms.maps.model.LatLng

data class LocationPoint(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val latitude: Double,
    val longitude: Double,
    val order: Int = 0
) {
    fun toLatLng(): LatLng {
        return LatLng(latitude, longitude)
    }

    fun hasValidCoordinates(): Boolean {
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }
}

fun List<LocationPoint>.toLatLngList(): List<LatLng> {
    return this.map { it.toLatLng() }
}