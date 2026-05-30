package com.example.fastroutes.data.repository

import com.example.fastroutes.data.model.RouteOption
import com.example.fastroutes.network.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class RouteOptionsRepository {

    private val supabase = SupabaseClientProvider.client

    suspend fun getActiveRouteOptions(): List<RouteOption> {
        return supabase
            .from("route_options")
            .select {
                filter {
                    eq("active", true)
                }

                order(
                    column = "name",
                    order = Order.ASCENDING
                )
            }
            .decodeList<RouteOption>()
    }
}