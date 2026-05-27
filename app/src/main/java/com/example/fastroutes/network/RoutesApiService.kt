package com.example.fastroutes.network

import com.google.android.gms.maps.model.LatLng
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface RoutesApiService {

    @POST("directions/v2:computeRoutes")
    suspend fun computeRoutes(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Header("X-Goog-FieldMask") fieldMask: String = DEFAULT_FIELD_MASK,
        @Body request: ComputeRoutesRequest
    ): Response<ComputeRoutesResponse>

    companion object {
        private const val BASE_URL = "https://routes.googleapis.com/"
        const val DEFAULT_FIELD_MASK =
            "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline,routes.optimizedIntermediateWaypointIndex"

        fun create(): RoutesApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(RoutesApiService::class.java)
        }
    }
}

data class ComputeRoutesRequest(
    val origin: RouteWaypoint,
    val destination: RouteWaypoint,
    val intermediates: List<RouteWaypoint> = emptyList(),
    val travelMode: String = "DRIVE",
    val routingPreference: String = "TRAFFIC_AWARE",
    val computeAlternativeRoutes: Boolean = false,
    val optimizeWaypointOrder: Boolean = true,
    val languageCode: String = "es-419",
    val units: String = "METRIC"
)

data class RouteWaypoint(
    val location: RouteLocation
)

data class RouteLocation(
    val latLng: RouteLatLng
)

data class RouteLatLng(
    val latitude: Double,
    val longitude: Double
)

data class ComputeRoutesResponse(
    val routes: List<RouteData> = emptyList()
)

data class RouteData(
    val distanceMeters: Int? = null,
    val duration: String? = null,
    val polyline: RoutePolyline? = null,

    @SerializedName("optimizedIntermediateWaypointIndex")
    val optimizedIntermediateWaypointIndex: List<Int> = emptyList()
)

data class RoutePolyline(
    val encodedPolyline: String? = null
)

fun LatLng.toRouteWaypoint(): RouteWaypoint {
    return RouteWaypoint(
        location = RouteLocation(
            latLng = RouteLatLng(
                latitude = latitude,
                longitude = longitude
            )
        )
    )
}

fun buildComputeRoutesRequest(
    points: List<LatLng>,
    optimizeWaypointOrder: Boolean = true
): ComputeRoutesRequest {
    require(points.size >= 2) {
        "Se necesitan al menos 2 puntos para calcular una ruta."
    }

    val origin = points.first().toRouteWaypoint()
    val destination = points.last().toRouteWaypoint()
    val intermediates = points
        .drop(1)
        .dropLast(1)
        .map { it.toRouteWaypoint() }

    return ComputeRoutesRequest(
        origin = origin,
        destination = destination,
        intermediates = intermediates,
        optimizeWaypointOrder = optimizeWaypointOrder
    )
}