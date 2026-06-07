package com.example.fastroutes.data.repository

import com.example.fastroutes.data.model.LocationHour
import com.example.fastroutes.data.model.CreateLocationRequest
import com.example.fastroutes.data.model.LocationRouteOptionRequest
import com.example.fastroutes.data.model.SavedLocation
import com.example.fastroutes.data.model.UpdateLocationRequest
import com.example.fastroutes.network.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class LocationsRepository {


    private val supabase = SupabaseClientProvider.client

    suspend fun getActiveLocationsByRouteOption(
        routeOptionId: String
    ): List<SavedLocation> {
        return supabase
            .from("active_locations_by_route_option")
            .select {
                filter {
                    eq("route_option_id", routeOptionId)
                }

                order(
                    column = "name",
                    order = Order.ASCENDING
                )
            }
            .decodeList<SavedLocation>()
    }

    suspend fun getAllActiveLocations(): List<SavedLocation> {
        return supabase
            .from("locations")
            .select {
                filter {
                    eq("active", true)
                }

                order(
                    column = "name",
                    order = Order.ASCENDING
                )
            }
            .decodeList<SavedLocation>()
    }

    suspend fun createLocation(
        name: String,
        address: String?,
        latitude: Double,
        longitude: Double
    ): SavedLocation {
        val request = CreateLocationRequest(
            name = name,
            address = address,
            latitude = latitude,
            longitude = longitude,
            active = true
        )

        return supabase
            .from("locations")
            .insert(request) {
                select()
            }
            .decodeSingle<SavedLocation>()
    }

    suspend fun updateLocation(
        locationId: String,
        name: String,
        address: String?,
        latitude: Double,
        longitude: Double
    ) {
        val request = UpdateLocationRequest(
            name = name,
            address = address,
            latitude = latitude,
            longitude = longitude,
            active = true
        )

        supabase
            .from("locations")
            .update(request) {
                filter {
                    eq("id", locationId)
                }
            }
    }

    suspend fun deactivateLocation(
        locationId: String
    ) {
        supabase
            .from("locations")
            .update(
                mapOf("active" to false)
            ) {
                filter {
                    eq("id", locationId)
                }
            }
    }

    suspend fun replaceLocationAssignments(
        locationId: String,
        routeOptionIds: List<String>
    ) {
        supabase
            .from("location_route_options")
            .delete {
                filter {
                    eq("location_id", locationId)
                }
            }

        if (routeOptionIds.isEmpty()) return

        val assignments = routeOptionIds.map { routeOptionId ->
            LocationRouteOptionRequest(
                locationId = locationId,
                routeOptionId = routeOptionId,
                active = true
            )
        }

        supabase
            .from("location_route_options")
            .insert(assignments)
    }
    suspend fun getLocationHoursByLocationIds(
        locationIds: List<String>
    ): Map<String, List<LocationHour>> {
        if (locationIds.isEmpty()) {
            return emptyMap()
        }

        val locationIdSet = locationIds.toSet()

        val allHours = supabase
            .from("location_hours")
            .select()
            .decodeList<LocationHour>()

        return allHours
            .filter { hour -> hour.locationId in locationIdSet }
            .groupBy { hour -> hour.locationId }
    }
}