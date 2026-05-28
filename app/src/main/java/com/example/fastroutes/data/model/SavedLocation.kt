package com.example.fastroutes.data.model

import com.google.android.gms.maps.model.LatLng
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SavedLocation(
    val id: String,
    val name: String,
    val address: String? = null,
    val latitude: Double,
    val longitude: Double,
    val active: Boolean = true,

    @SerialName("created_at")
    val createdAt: String? = null
) {
    fun toLatLng(): LatLng {
        return LatLng(latitude, longitude)
    }
}