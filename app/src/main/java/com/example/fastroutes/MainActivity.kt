package com.example.fastroutes

import com.example.fastroutes.data.model.LocationHour
import com.example.fastroutes.data.repository.LocationsRepository
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import com.example.fastroutes.data.repository.AuthRepository
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.fastroutes.data.model.RouteOption
import com.example.fastroutes.data.model.SavedLocation
import com.google.firebase.FirebaseApp
import com.example.fastroutes.location.CurrentLocationProvider
import com.example.fastroutes.network.RoutesApiService
import com.example.fastroutes.network.buildComputeRoutesRequest
import com.example.fastroutes.ui.screens.AddLocationsScreen
import com.example.fastroutes.ui.screens.HomeScreen
import com.example.fastroutes.ui.screens.LocationsScreen
import com.example.fastroutes.ui.screens.LoginScreen
import com.example.fastroutes.ui.screens.MapRouteScreen
import com.example.fastroutes.utils.PolylineDecoder
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        setContent {
            MaterialTheme {
                Surface {
                    FastRoutesApp()
                }
            }
        }
    }
}

@Composable
private fun FastRoutesApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val routesApiService = remember {
        RoutesApiService.create()
    }

    val authRepository = remember {
        AuthRepository()
    }
    val locationsRepository = remember {
        LocationsRepository()
    }

    val fixedDestination = LatLng(
        -34.761394496810844,
        -55.59526743441916
    )

    var currentScreen by remember {
        mutableStateOf(AppScreen.CheckingLogin)
    }

    var selectedRouteOption by remember {
        mutableStateOf<RouteOption?>(null)
    }

    var isAdmin by remember {
        mutableStateOf(false)
    }

    var stopPoints by remember {
        mutableStateOf<List<LatLng>>(emptyList())
    }

    var routePolylinePoints by remember {
        mutableStateOf<List<LatLng>>(emptyList())
    }

    var locationNames by remember {
        mutableStateOf<List<String>>(emptyList())
    }

    var navigationSegments by remember {
        mutableStateOf<List<List<LatLng>>>(emptyList())
    }

    var currentNavigationSegmentIndex by remember {
        mutableStateOf(0)
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var pendingSelectedLocations by remember {
        mutableStateOf<List<SavedLocation>>(emptyList())
    }

    LaunchedEffect(Unit) {
        val result = authRepository.checkCurrentUser()

        result.onSuccess { appUser ->
            isAdmin = appUser.role == "admin"
            currentScreen = AppScreen.Home
        }

        result.onFailure {
            authRepository.logout()
            isAdmin = false
            currentScreen = AppScreen.LoginApp
        }
    }

    fun goBack() {
        when (currentScreen) {
            AppScreen.CheckingLogin -> {
                // No hacemos nada mientras verifica acceso.
            }

            AppScreen.LoginApp -> {
                // Login obligatorio: no permitimos volver a la app sin iniciar sesión.
            }

            AppScreen.Home -> {
                // En Home no hacemos nada.
            }

            AppScreen.Locations -> {
                currentScreen = AppScreen.Home
            }

            AppScreen.AddLocations -> {
                currentScreen = AppScreen.Home
            }

            AppScreen.MapRoute -> {
                currentScreen = AppScreen.AddLocations
            }
        }
    }

    BackHandler(
        enabled = currentScreen != AppScreen.Home &&
                currentScreen != AppScreen.LoginApp &&
                currentScreen != AppScreen.CheckingLogin
    ) {
        goBack()
    }

    fun calculateRouteFromCurrentLocation(
        selectedLocations: List<SavedLocation>
    ) {
        if (selectedLocations.isEmpty()) {
            errorMessage = "Seleccioná al menos 1 ubicación."
            return
        }

        coroutineScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val apiKey = BuildConfig.ROUTES_API_KEY

                if (apiKey.isBlank()) {
                    errorMessage = "Falta configurar ROUTES_API_KEY en local.properties."
                    return@launch
                }

                val currentLocation = CurrentLocationProvider.getCurrentLatLng(context)

                if (currentLocation == null) {
                    errorMessage = "No se pudo obtener tu ubicación actual. Revisá permisos y GPS."
                    return@launch
                }

                val hoursByLocationId = locationsRepository.getLocationHoursByLocationIds(
                    locationIds = selectedLocations.map { location -> location.id }
                )

                val optimizationResult = optimizeStopsWithTimeWindows(
                    currentLocation = currentLocation,
                    selectedLocations = selectedLocations,
                    hoursByLocationId = hoursByLocationId,
                    startTime = ZonedDateTime.now(ZoneId.of("America/Montevideo"))
                )

                val optimizedLocations = optimizationResult.orderedLocations

                if (optimizedLocations.isEmpty()) {
                    errorMessage = "No se pudo armar una ruta: ningún cliente seleccionado está disponible dentro de su horario."
                    return@launch
                }

                if (optimizationResult.skippedLocations.isNotEmpty()) {
                    errorMessage = buildString {
                        append("No se pudieron incluir algunas ubicaciones por horario:\n\n")

                        optimizationResult.skippedLocations.forEach { skipped ->
                            append("- ${skipped.location.name}: ${skipped.reason}\n")
                        }
                    }
                }

                val pointsForRoute = listOf(currentLocation) +
                        optimizedLocations.map { location -> location.toLatLng() } +
                        listOf(fixedDestination)

                val decodedPolyline = computeRoutePolylineInChunks(
                    routesApiService = routesApiService,
                    apiKey = apiKey,
                    orderedPoints = pointsForRoute
                )

                if (decodedPolyline.isEmpty()) {
                    errorMessage = "No se pudo calcular la ruta completa por calles."
                    return@launch
                }

                stopPoints = pointsForRoute
                routePolylinePoints = decodedPolyline

                locationNames = listOf("Mi ubicación actual") +
                        optimizedLocations.map { location ->
                            location.address ?: location.name
                        } +
                        listOf("Destino final")

                navigationSegments = splitRoutePoints(
                    points = pointsForRoute,
                    maxIntermediateWaypoints = 8
                )

                currentNavigationSegmentIndex = 0
                currentScreen = AppScreen.MapRoute
            } catch (e: Exception) {
                errorMessage = e.message ?: "Ocurrió un error al calcular la ruta."
            } finally {
                isLoading = false
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            calculateRouteFromCurrentLocation(pendingSelectedLocations)
        } else {
            errorMessage = "Necesitás permitir la ubicación para usarla como punto de partida."
        }
    }

    fun startRouteCalculation(
        selectedLocations: List<SavedLocation>
    ) {
        pendingSelectedLocations = selectedLocations

        if (CurrentLocationProvider.hasLocationPermission(context)) {
            calculateRouteFromCurrentLocation(selectedLocations)
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (currentScreen) {
            AppScreen.CheckingLogin -> {
                LoadingAccessScreen()
            }

            AppScreen.LoginApp -> {
                LoginScreen(
                    onBackClick = {
                        // Login obligatorio: no vuelve a ninguna pantalla.
                    },
                    onLoginSuccess = {
                        coroutineScope.launch {
                            val result = authRepository.checkCurrentUser()

                            result.onSuccess { appUser ->
                                isAdmin = appUser.role == "admin"
                                currentScreen = AppScreen.Home
                            }

                            result.onFailure { error ->
                                authRepository.logout()
                                isAdmin = false
                                errorMessage = error.message ?: "No tenés acceso a FastRoutes."
                                currentScreen = AppScreen.LoginApp
                            }
                        }
                    }
                )
            }

            AppScreen.Home -> {
                HomeScreen(
                    onRouteOptionClick = { routeOption ->
                        selectedRouteOption = routeOption
                        currentScreen = AppScreen.AddLocations
                    },
                    onLocationsClick = {
                        currentScreen = AppScreen.Locations
                    }
                )
            }

            AppScreen.Locations -> {
                LocationsScreen(
                    isAdmin = isAdmin,
                    onBackClick = {
                        goBack()
                    },
                    onLoginAdminClick = {
                        errorMessage = "Tu usuario no tiene permisos de administrador."
                    },
                    onLogoutAdminClick = {
                        coroutineScope.launch {
                            authRepository.logout()
                            isAdmin = false
                            currentScreen = AppScreen.LoginApp
                        }
                    }
                )
            }

            AppScreen.AddLocations -> {
                val routeOption = selectedRouteOption

                if (routeOption == null) {
                    LaunchedEffect(Unit) {
                        currentScreen = AppScreen.Home
                    }
                } else {
                    AddLocationsScreen(
                        routeOptionId = routeOption.id,
                        routeTitle = routeOption.name,
                        autoSelectAll = routeOption.type == "REPARTO_LECHE",
                        onBackClick = {
                            goBack()
                        },
                        onCalculateRouteClick = { selectedLocations ->
                            startRouteCalculation(selectedLocations)
                        }
                    )
                }
            }

            AppScreen.MapRoute -> {
                MapRouteScreen(
                    stopPoints = stopPoints,
                    routePolylinePoints = routePolylinePoints,
                    locationNames = locationNames,
                    currentSegmentIndex = currentNavigationSegmentIndex,
                    totalSegments = navigationSegments.size,
                    onBackClick = {
                        goBack()
                    },
                    onEditLocationsClick = {
                        currentScreen = AppScreen.AddLocations
                    },
                    onStartNavigationClick = {
                        val currentSegment = navigationSegments.getOrNull(
                            currentNavigationSegmentIndex
                        )

                        if (currentSegment == null) {
                            errorMessage = "No hay más tramos disponibles."
                        } else {
                            openGoogleMapsRoute(
                                context = context,
                                points = currentSegment
                            )

                            currentNavigationSegmentIndex += 1
                        }
                    }
                )
            }
        }

        if (isLoading) {
            LoadingRouteOverlay()
        }

        errorMessage?.let { message ->
            AlertDialog(
                onDismissRequest = {
                    errorMessage = null
                },
                title = {
                    Text(text = "Error")
                },
                text = {
                    Text(text = message)
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            errorMessage = null
                        }
                    ) {
                        Text(text = "Aceptar")
                    }
                }
            )
        }
    }
}

@Composable
private fun LoadingRouteOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.large
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()

            Text(
                text = "Calculando ruta desde tu ubicación actual...",
                modifier = Modifier.padding(top = 14.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun LoadingAccessScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.large
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()

            Text(
                text = "Verificando acceso a FastRoutes...",
                modifier = Modifier.padding(top = 14.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private suspend fun computeRoutePolylineInChunks(
    routesApiService: RoutesApiService,
    apiKey: String,
    orderedPoints: List<LatLng>
): List<LatLng> {
    val routeChunks = splitRoutePoints(
        points = orderedPoints,
        maxIntermediateWaypoints = 25
    )

    val fullPolyline = mutableListOf<LatLng>()

    routeChunks.forEachIndexed { index, chunkPoints ->
        val request = buildComputeRoutesRequest(
            points = chunkPoints,
            optimizeWaypointOrder = false
        )

        val response = routesApiService.computeRoutes(
            apiKey = apiKey,
            request = request
        )

        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string()
            throw Exception("Error de Routes API: ${response.code()} ${errorBody.orEmpty()}")
        }

        val route = response.body()
            ?.routes
            ?.firstOrNull()
            ?: throw Exception("Google Routes API no devolvió ruta en uno de los tramos.")

        val encodedPolyline = route.polyline
            ?.encodedPolyline
            ?: throw Exception("Google Routes API no devolvió polyline en uno de los tramos.")

        val decodedChunk = PolylineDecoder.decode(encodedPolyline)

        if (decodedChunk.isNotEmpty()) {
            if (index == 0) {
                fullPolyline.addAll(decodedChunk)
            } else {
                fullPolyline.addAll(decodedChunk.drop(1))
            }
        }
    }

    return fullPolyline
}

private fun splitRoutePoints(
    points: List<LatLng>,
    maxIntermediateWaypoints: Int
): List<List<LatLng>> {
    if (points.size < 2) return emptyList()

    val maxPointsPerRequest = maxIntermediateWaypoints + 2

    if (points.size <= maxPointsPerRequest) {
        return listOf(points)
    }

    val chunks = mutableListOf<List<LatLng>>()
    var startIndex = 0

    while (startIndex < points.lastIndex) {
        val endIndex = minOf(
            startIndex + maxPointsPerRequest - 1,
            points.lastIndex
        )

        chunks.add(points.subList(startIndex, endIndex + 1))

        startIndex = endIndex
    }

    return chunks
}

private data class TimeWindowOptimizationResult(
    val orderedLocations: List<SavedLocation>,
    val skippedLocations: List<SkippedRouteLocation>
)

private data class SkippedRouteLocation(
    val location: SavedLocation,
    val reason: String
)

private data class VisitCandidate(
    val location: SavedLocation,
    val travelMinutes: Long,
    val waitMinutes: Long,
    val arrivalTime: ZonedDateTime,
    val serviceStartTime: ZonedDateTime,
    val finishTime: ZonedDateTime,
    val score: Double
)

private data class OpeningWindow(
    val openAt: ZonedDateTime,
    val closeAt: ZonedDateTime
)

private fun optimizeStopsWithTimeWindows(
    currentLocation: LatLng,
    selectedLocations: List<SavedLocation>,
    hoursByLocationId: Map<String, List<LocationHour>>,
    startTime: ZonedDateTime,
    averageSpeedKmH: Double = 30.0
): TimeWindowOptimizationResult {
    val pendingLocations = selectedLocations.toMutableList()
    val orderedLocations = mutableListOf<SavedLocation>()
    val skippedLocations = mutableListOf<SkippedRouteLocation>()

    var currentPoint = currentLocation
    var currentTime = startTime

    while (pendingLocations.isNotEmpty()) {
        val candidates = pendingLocations.mapNotNull { location ->
            buildVisitCandidate(
                currentPoint = currentPoint,
                currentTime = currentTime,
                location = location,
                hours = hoursByLocationId[location.id].orEmpty(),
                averageSpeedKmH = averageSpeedKmH
            )
        }

        if (candidates.isEmpty()) {
            pendingLocations.forEach { location ->
                skippedLocations.add(
                    SkippedRouteLocation(
                        location = location,
                        reason = "no se llega dentro del horario de hoy"
                    )
                )
            }

            pendingLocations.clear()
            break
        }

        val chosenCandidate = candidates.minBy { candidate ->
            val futureRisk = calculateFutureClosingRisk(
                currentPointAfterCandidate = candidate.location.toLatLng(),
                currentTimeAfterCandidate = candidate.finishTime,
                pendingLocations = pendingLocations.filterNot { it.id == candidate.location.id },
                hoursByLocationId = hoursByLocationId,
                averageSpeedKmH = averageSpeedKmH
            )

            candidate.score + futureRisk
        }

        orderedLocations.add(chosenCandidate.location)
        pendingLocations.remove(chosenCandidate.location)

        currentPoint = chosenCandidate.location.toLatLng()
        currentTime = chosenCandidate.finishTime
    }

    return TimeWindowOptimizationResult(
        orderedLocations = orderedLocations,
        skippedLocations = skippedLocations
    )
}

private fun buildVisitCandidate(
    currentPoint: LatLng,
    currentTime: ZonedDateTime,
    location: SavedLocation,
    hours: List<LocationHour>,
    averageSpeedKmH: Double
): VisitCandidate? {
    val travelMinutes = estimateTravelMinutes(
        distanceKm = distanceKm(
            from = currentPoint,
            to = location.toLatLng()
        ),
        averageSpeedKmH = averageSpeedKmH
    )

    val arrivalTime = currentTime.plusMinutes(travelMinutes)

    val openingWindow = getOpeningWindowForDate(
        hours = hours,
        date = arrivalTime.toLocalDate(),
        zoneId = arrivalTime.zone
    )

    val serviceMinutes = location.serviceMinutes.coerceAtLeast(0).toLong()

    if (openingWindow == null) {
        val finishTime = arrivalTime.plusMinutes(serviceMinutes)

        return VisitCandidate(
            location = location,
            travelMinutes = travelMinutes,
            waitMinutes = 0,
            arrivalTime = arrivalTime,
            serviceStartTime = arrivalTime,
            finishTime = finishTime,
            score = travelMinutes.toDouble()
        )
    }

    val serviceStartTime = if (arrivalTime.isBefore(openingWindow.openAt)) {
        openingWindow.openAt
    } else {
        arrivalTime
    }

    val finishTime = serviceStartTime.plusMinutes(serviceMinutes)

    if (finishTime.isAfter(openingWindow.closeAt)) {
        return null
    }

    val waitMinutes = Duration.between(arrivalTime, serviceStartTime)
        .toMinutes()
        .coerceAtLeast(0)

    val slackUntilClose = Duration.between(finishTime, openingWindow.closeAt)
        .toMinutes()
        .coerceAtLeast(0)

    /*
     * Score bajo = mejor candidato.
     *
     * travelMinutes: distancia/tiempo de viaje.
     * waitMinutes: castiga ir demasiado temprano a un cliente cerrado.
     * slackUntilClose: cuanto menos margen queda antes del cierre, más prioridad.
     */
    val urgencyPenalty = when {
        slackUntilClose <= 15 -> -500.0
        slackUntilClose <= 30 -> -250.0
        slackUntilClose <= 60 -> -100.0
        slackUntilClose <= 120 -> -30.0
        else -> 0.0
    }

    val score =
        travelMinutes * 1.0 +
                waitMinutes * 1.5 +
                slackUntilClose.coerceAtMost(240) * 0.08 +
                urgencyPenalty

    return VisitCandidate(
        location = location,
        travelMinutes = travelMinutes,
        waitMinutes = waitMinutes,
        arrivalTime = arrivalTime,
        serviceStartTime = serviceStartTime,
        finishTime = finishTime,
        score = score
    )
}
private fun calculateFutureClosingRisk(
    currentPointAfterCandidate: LatLng,
    currentTimeAfterCandidate: ZonedDateTime,
    pendingLocations: List<SavedLocation>,
    hoursByLocationId: Map<String, List<LocationHour>>,
    averageSpeedKmH: Double
): Double {
    var riskScore = 0.0

    pendingLocations.forEach { location ->
        val travelMinutes = estimateTravelMinutes(
            distanceKm = distanceKm(
                from = currentPointAfterCandidate,
                to = location.toLatLng()
            ),
            averageSpeedKmH = averageSpeedKmH
        )

        val estimatedArrival = currentTimeAfterCandidate.plusMinutes(travelMinutes)

        val openingWindow = getOpeningWindowForDate(
            hours = hoursByLocationId[location.id].orEmpty(),
            date = estimatedArrival.toLocalDate(),
            zoneId = estimatedArrival.zone
        )

        if (openingWindow != null) {
            val serviceMinutes = location.serviceMinutes.coerceAtLeast(0).toLong()
            val estimatedFinish = estimatedArrival.plusMinutes(serviceMinutes)

            val minutesUntilClose = Duration
                .between(estimatedFinish, openingWindow.closeAt)
                .toMinutes()

            riskScore += when {
                minutesUntilClose < 0 -> 10_000.0
                minutesUntilClose <= 15 -> 1_000.0
                minutesUntilClose <= 30 -> 500.0
                minutesUntilClose <= 60 -> 150.0
                else -> 0.0
            }
        }
    }

    return riskScore
}

private fun getOpeningWindowForDate(
    hours: List<LocationHour>,
    date: LocalDate,
    zoneId: ZoneId
): OpeningWindow? {
    if (hours.isEmpty()) {
        return null
    }

    val dayOfWeek = date.dayOfWeek.value

    val dayHours = hours.firstOrNull { hour ->
        hour.dayOfWeek == dayOfWeek
    } ?: return null

    if (dayHours.isClosed) {
        return null
    }

    val openTimeText = dayHours.openTime ?: return null
    val closeTimeText = dayHours.closeTime ?: return null

    val openTime = parseSupabaseTime(openTimeText)
    val closeTime = parseSupabaseTime(closeTimeText)

    val openAt = ZonedDateTime.of(date, openTime, zoneId)

    var closeAt = ZonedDateTime.of(date, closeTime, zoneId)

    if (!closeTime.isAfter(openTime)) {
        closeAt = closeAt.plusDays(1)
    }

    return OpeningWindow(
        openAt = openAt,
        closeAt = closeAt
    )
}

private fun parseSupabaseTime(value: String): LocalTime {
    val cleanValue = value.trim()

    return if (cleanValue.length >= 8) {
        LocalTime.parse(cleanValue.take(8))
    } else {
        LocalTime.parse(cleanValue)
    }
}

private fun estimateTravelMinutes(
    distanceKm: Double,
    averageSpeedKmH: Double
): Long {
    if (distanceKm <= 0.0) {
        return 1
    }

    /*
     * routeFactor compensa que la distancia en línea recta casi siempre
     * es menor que el recorrido real por calles.
     *
     * 1.35 = suma aprox. 35% extra al recorrido.
     */
    val routeFactor = 1.35

    val estimatedRoadDistanceKm = distanceKm * routeFactor

    return ((estimatedRoadDistanceKm / averageSpeedKmH) * 60)
        .toLong()
        .coerceAtLeast(2)
}

private fun distanceKm(
    from: LatLng,
    to: LatLng
): Double {
    val earthRadiusKm = 6371.0

    val latitudeDistance = Math.toRadians(to.latitude - from.latitude)
    val longitudeDistance = Math.toRadians(to.longitude - from.longitude)

    val fromLatitudeRadians = Math.toRadians(from.latitude)
    val toLatitudeRadians = Math.toRadians(to.latitude)

    val a = sin(latitudeDistance / 2) * sin(latitudeDistance / 2) +
            cos(fromLatitudeRadians) *
            cos(toLatitudeRadians) *
            sin(longitudeDistance / 2) *
            sin(longitudeDistance / 2)

    val c = 2 * atan2(
        sqrt(a),
        sqrt(1 - a)
    )

    return earthRadiusKm * c
}

private fun openGoogleMapsRoute(
    context: Context,
    points: List<LatLng>
) {
    if (points.size < 2) return

    val origin = points.first()
    val destination = points.last()
    val waypoints = points.drop(1).dropLast(1)

    val originText = "${origin.latitude},${origin.longitude}"
    val destinationText = "${destination.latitude},${destination.longitude}"

    val uriBuilder = Uri.Builder()
        .scheme("https")
        .authority("www.google.com")
        .path("maps/dir/")
        .appendQueryParameter("api", "1")
        .appendQueryParameter("origin", originText)
        .appendQueryParameter("destination", destinationText)
        .appendQueryParameter("travelmode", "driving")

    if (waypoints.isNotEmpty()) {
        val waypointsText = waypoints.joinToString("|") { point ->
            "${point.latitude},${point.longitude}"
        }

        uriBuilder.appendQueryParameter("waypoints", waypointsText)
    }

    val mapsUri = uriBuilder.build()

    val googleMapsIntent = Intent(Intent.ACTION_VIEW, mapsUri).apply {
        setPackage("com.google.android.apps.maps")
    }

    try {
        context.startActivity(googleMapsIntent)
    } catch (e: ActivityNotFoundException) {
        val browserIntent = Intent(Intent.ACTION_VIEW, mapsUri)
        context.startActivity(browserIntent)
    }
}

private enum class AppScreen {
    CheckingLogin,
    LoginApp,
    Home,
    AddLocations,
    Locations,
    MapRoute
}