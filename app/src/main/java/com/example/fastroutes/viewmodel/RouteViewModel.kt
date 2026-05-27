package com.example.fastroutes.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.example.fastroutes.data.model.LocationPoint
import com.example.fastroutes.data.repository.OptimizedRoute
import com.example.fastroutes.data.repository.RouteRepository
import com.example.fastroutes.data.repository.RouteRepositoryResult
import com.example.fastroutes.network.RoutesApiService
import com.example.fastroutes.network.buildComputeRoutesRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class RouteViewModel(
    private val routeRepository: RouteRepository = RouteRepository(),
    private val routesApiService: RoutesApiService = RoutesApiService.create()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RouteUiState())
    val uiState: StateFlow<RouteUiState> = _uiState.asStateFlow()

    fun addLocation(
        name: String,
        address: String,
        latitude: Double,
        longitude: Double
    ) {
        val cleanName = name.trim()
        val cleanAddress = address.trim()

        if (cleanAddress.isBlank()) {
            setError("Ingresá una dirección válida.")
            return
        }

        val newLocation = LocationPoint(
            id = UUID.randomUUID().toString(),
            name = cleanName.ifBlank { "Parada ${_uiState.value.locations.size + 1}" },
            address = cleanAddress,
            latitude = latitude,
            longitude = longitude,
            order = _uiState.value.locations.size + 1
        )

        if (!newLocation.hasValidCoordinates()) {
            setError("Las coordenadas ingresadas no son válidas.")
            return
        }

        val alreadyExists = _uiState.value.locations.any {
            it.latitude == latitude && it.longitude == longitude
        }

        if (alreadyExists) {
            setError("Esa ubicación ya fue agregada.")
            return
        }

        _uiState.update { currentState ->
            currentState.copy(
                locations = currentState.locations + newLocation,
                errorMessage = null
            )
        }
    }

    fun removeLocation(locationId: String) {
        _uiState.update { currentState ->
            val updatedLocations = currentState.locations
                .filterNot { it.id == locationId }
                .mapIndexed { index, location ->
                    location.copy(order = index + 1)
                }

            currentState.copy(
                locations = updatedLocations,
                optimizedRoute = null,
                routePolylinePoints = emptyList(),
                errorMessage = null
            )
        }
    }

    fun clearLocations() {
        _uiState.update {
            RouteUiState()
        }
    }

    fun calculateLocalOptimizedRoute() {
        val locations = _uiState.value.locations

        if (locations.size < 2) {
            setError("Agregá al menos 2 ubicaciones para calcular una ruta.")
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null
            )
        }

        val result = routeRepository.calculateOptimizedRoute(locations)

        when (result) {
            is RouteRepositoryResult.Success -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        optimizedRoute = result.route,
                        routePolylinePoints = result.route.routePoints,
                        errorMessage = null
                    )
                }
            }

            is RouteRepositoryResult.Error -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun calculateGoogleRoute(apiKey: String) {
        val locations = _uiState.value.locations

        if (locations.size < 2) {
            setError("Agregá al menos 2 ubicaciones para calcular una ruta.")
            return
        }

        if (apiKey.isBlank()) {
            setError("Falta configurar la API Key de Google Routes.")
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            try {
                val points = locations.map { location ->
                    LatLng(location.latitude, location.longitude)
                }

                val request = buildComputeRoutesRequest(
                    points = points,
                    optimizeWaypointOrder = true
                )

                val response = routesApiService.computeRoutes(
                    apiKey = apiKey,
                    request = request
                )

                if (!response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Error al calcular la ruta: ${response.code()}"
                        )
                    }
                    return@launch
                }

                val route = response.body()?.routes?.firstOrNull()

                if (route == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "No se encontró una ruta disponible."
                        )
                    }
                    return@launch
                }

                val orderedLocations = applyOptimizedWaypointOrder(
                    locations = locations,
                    optimizedIntermediateIndexes = route.optimizedIntermediateWaypointIndex
                )

                val decodedPolyline = route.polyline
                    ?.encodedPolyline
                    ?.let { decodePolyline(it) }
                    .orEmpty()

                val fallbackPolyline = orderedLocations.map {
                    LatLng(it.latitude, it.longitude)
                }

                val optimizedRoute = OptimizedRoute(
                    orderedLocations = orderedLocations,
                    routePoints = if (decodedPolyline.isNotEmpty()) decodedPolyline else fallbackPolyline,
                    totalDistanceKm = (route.distanceMeters ?: 0) / 1000.0,
                    totalStops = orderedLocations.size
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        optimizedRoute = optimizedRoute,
                        routePolylinePoints = optimizedRoute.routePoints,
                        distanceMeters = route.distanceMeters,
                        duration = route.duration,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Ocurrió un error al calcular la ruta."
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }

    private fun setError(message: String) {
        _uiState.update {
            it.copy(errorMessage = message)
        }
    }

    private fun applyOptimizedWaypointOrder(
        locations: List<LocationPoint>,
        optimizedIntermediateIndexes: List<Int>
    ): List<LocationPoint> {
        if (locations.size <= 2 || optimizedIntermediateIndexes.isEmpty()) {
            return locations.mapIndexed { index, location ->
                location.copy(order = index + 1)
            }
        }

        val origin = locations.first()
        val destination = locations.last()
        val intermediates = locations.drop(1).dropLast(1)

        val optimizedIntermediates = optimizedIntermediateIndexes.mapNotNull { index ->
            intermediates.getOrNull(index)
        }

        return (listOf(origin) + optimizedIntermediates + listOf(destination))
            .mapIndexed { index, location ->
                location.copy(order = index + 1)
            }
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val polyline = mutableListOf<LatLng>()
        var index = 0
        val length = encoded.length
        var latitude = 0
        var longitude = 0

        while (index < length) {
            var result = 0
            var shift = 0
            var byte: Int

            do {
                byte = encoded[index++].code - 63
                result = result or ((byte and 0x1f) shl shift)
                shift += 5
            } while (byte >= 0x20)

            val deltaLatitude = if ((result and 1) != 0) {
                (result shr 1).inv()
            } else {
                result shr 1
            }

            latitude += deltaLatitude

            result = 0
            shift = 0

            do {
                byte = encoded[index++].code - 63
                result = result or ((byte and 0x1f) shl shift)
                shift += 5
            } while (byte >= 0x20)

            val deltaLongitude = if ((result and 1) != 0) {
                (result shr 1).inv()
            } else {
                result shr 1
            }

            longitude += deltaLongitude

            polyline.add(
                LatLng(
                    latitude / 100000.0,
                    longitude / 100000.0
                )
            )
        }

        return polyline
    }
}

data class RouteUiState(
    val locations: List<LocationPoint> = emptyList(),
    val optimizedRoute: OptimizedRoute? = null,
    val routePolylinePoints: List<LatLng> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val distanceMeters: Int? = null,
    val duration: String? = null
)