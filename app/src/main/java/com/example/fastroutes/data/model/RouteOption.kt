package com.example.fastroutes.data.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RouteOption(
    val id: String,
    val type: String,
    val name: String,
    val active: Boolean = true,

    @SerialName("created_at")
    val createdAt: String? = null
)