package com.example.fastroutes.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AdminUser(
    @SerialName("user_id")
    val userId: String
)