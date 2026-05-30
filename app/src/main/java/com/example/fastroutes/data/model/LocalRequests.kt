package com.example.fastroutes.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateLocationRequest(
    val name: String,
    val address: String? = null,
    val latitude: Double,
    val longitude: Double,
    val active: Boolean = true
)

@Serializable
data class UpdateLocationRequest(
    val name: String,
    val address: String? = null,
    val latitude: Double,
    val longitude: Double,
    val active: Boolean = true
)

@Serializable
data class LocationRouteOptionRequest(
    @SerialName("location_id")
    val locationId: String,

    @SerialName("route_option_id")
    val routeOptionId: String,

    val active: Boolean = true
)