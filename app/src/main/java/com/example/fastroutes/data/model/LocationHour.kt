package com.example.fastroutes.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocationHour(
    val id: String? = null,

    @SerialName("location_id")
    val locationId: String,

    @SerialName("day_of_week")
    val dayOfWeek: Int,

    @SerialName("open_time")
    val openTime: String? = null,

    @SerialName("close_time")
    val closeTime: String? = null,

    @SerialName("is_closed")
    val isClosed: Boolean = false
)