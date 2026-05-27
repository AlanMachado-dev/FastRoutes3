package com.example.fastroutes.data.repository


import com.google.android.gms.maps.model.LatLng
import com.example.fastroutes.data.model.LocationPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class RouteRepository {

    fun calculateOptimizedRoute(
        locations: List<LocationPoint>
    ): RouteRepositoryResult {
        if (locations.size < 2) {
            return RouteRepositoryResult.Error(
                message = "Se necesitan al menos 2 ubicaciones para calcular una ruta."
            )
        }

        val invalidLocations = locations.filterNot { it.hasValidCoordinates() }

        if (invalidLocations.isNotEmpty()) {
            return RouteRepositoryResult.Error(
                message = "Hay ubicaciones con coordenadas inválidas."
            )
        }

        val optimizedLocations = optimizeByNearestNeighbor(locations)

        val totalDistanceKm = calculateTotalDistanceKm(optimizedLocations)

        val route = OptimizedRoute(
            orderedLocations = optimizedLocations,
            routePoints = optimizedLocations.map { it.toLatLng() },
            totalDistanceKm = totalDistanceKm,
            totalStops = optimizedLocations.size
        )

        return RouteRepositoryResult.Success(route)
    }

    private fun optimizeByNearestNeighbor(
        locations: List<LocationPoint>
    ): List<LocationPoint> {
        if (locations.size <= 2) {
            return locations.mapIndexed { index, location ->
                location.copy(order = index + 1)
            }
        }

        val pendingLocations = locations.toMutableList()
        val optimizedRoute = mutableListOf<LocationPoint>()

        var currentLocation = pendingLocations.removeAt(0)
        optimizedRoute.add(currentLocation)

        while (pendingLocations.isNotEmpty()) {
            val nearestLocation = pendingLocations.minBy { candidate ->
                calculateDistanceKm(
                    from = currentLocation,
                    to = candidate
                )
            }

            pendingLocations.remove(nearestLocation)
            optimizedRoute.add(nearestLocation)
            currentLocation = nearestLocation
        }

        return optimizedRoute.mapIndexed { index, location ->
            location.copy(order = index + 1)
        }
    }

    private fun calculateTotalDistanceKm(
        locations: List<LocationPoint>
    ): Double {
        if (locations.size < 2) return 0.0

        return locations
            .zipWithNext()
            .sumOf { pair ->
                calculateDistanceKm(
                    from = pair.first,
                    to = pair.second
                )
            }
    }

    private fun calculateDistanceKm(
        from: LocationPoint,
        to: LocationPoint
    ): Double {
        return calculateDistanceKm(
            fromLatitude = from.latitude,
            fromLongitude = from.longitude,
            toLatitude = to.latitude,
            toLongitude = to.longitude
        )
    }

    private fun calculateDistanceKm(
        fromLatitude: Double,
        fromLongitude: Double,
        toLatitude: Double,
        toLongitude: Double
    ): Double {
        val earthRadiusKm = 6371.0

        val latitudeDistance = Math.toRadians(toLatitude - fromLatitude)
        val longitudeDistance = Math.toRadians(toLongitude - fromLongitude)

        val fromLatitudeRadians = Math.toRadians(fromLatitude)
        val toLatitudeRadians = Math.toRadians(toLatitude)

        val a = sin(latitudeDistance / 2).pow(2) +
                cos(fromLatitudeRadians) *
                cos(toLatitudeRadians) *
                sin(longitudeDistance / 2).pow(2)

        val c = 2 * atan2(
            sqrt(a),
            sqrt(1 - a)
        )

        return earthRadiusKm * c
    }
}

data class OptimizedRoute(
    val orderedLocations: List<LocationPoint>,
    val routePoints: List<LatLng>,
    val totalDistanceKm: Double,
    val totalStops: Int
)

sealed class RouteRepositoryResult {
    data class Success(
        val route: OptimizedRoute
    ) : RouteRepositoryResult()

    data class Error(
        val message: String
    ) : RouteRepositoryResult()
}