package com.example.fastroutes

data class AppUser(
    val uid: String = "",
    val email: String = "",
    val active: Boolean = false,
    val role: String = "client",
    val businessName: String = ""
)