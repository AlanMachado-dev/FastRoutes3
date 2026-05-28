package com.example.fastroutes.data.repository

import com.example.fastroutes.data.model.SavedLocation
import com.example.fastroutes.network.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from

class LocationsRepository {

    private val supabase = SupabaseClientProvider.client

    suspend fun getActiveLocations(): List<SavedLocation> {
        return supabase
            .from("locations")
            .select {
                filter {
                    eq("active", true)
                }

                order(
                    column = "name",
                    order = io.github.jan.supabase.postgrest.query.Order.ASCENDING
                )
            }
            .decodeList<SavedLocation>()
    }
}