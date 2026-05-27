package com.example.fastroutes


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.android.gms.maps.model.LatLng
import com.example.fastroutes.ui.screens.AddLocationsScreen
import com.example.fastroutes.ui.screens.HomeScreen
import com.example.fastroutes.ui.screens.MapRouteScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                Surface {
                    RutaOptimaApp()
                }
            }
        }
    }
}

@Composable
private fun RutaOptimaApp() {
    var currentScreen by remember {
        mutableStateOf(AppScreen.Home)
    }

    var routePoints by remember {
        mutableStateOf<List<LatLng>>(emptyList())
    }

    var locationNames by remember {
        mutableStateOf<List<String>>(emptyList())
    }

    when (currentScreen) {
        AppScreen.Home -> {
            HomeScreen(
                onAddLocationsClick = {
                    currentScreen = AppScreen.AddLocations
                },
                onMapClick = {
                    currentScreen = AppScreen.MapRoute
                },
                onHistoryClick = {
                    // Más adelante podés crear HistoryScreen
                }
            )
        }

        AppScreen.AddLocations -> {
            AddLocationsScreen(
                onBackClick = {
                    currentScreen = AppScreen.Home
                },
                onCalculateRouteClick = { locations ->
                    locationNames = locations

                    /*
                     * Temporal:
                     * AddLocationsScreen por ahora devuelve textos, no coordenadas reales.
                     * Para poder probar el mapa, generamos coordenadas demo.
                     *
                     * Después esto se reemplaza por:
                     * - Google Geocoding API
                     * - Places API
                     * - Autocomplete de Google Maps
                     * - Backend propio
                     */
                    routePoints = locations.mapIndexed { index, location ->
                        demoCoordinatesFor(
                            index = index,
                            location = location
                        )
                    }

                    currentScreen = AppScreen.MapRoute
                }
            )
        }

        AppScreen.MapRoute -> {
            MapRouteScreen(
                routePoints = routePoints,
                locationNames = locationNames,
                onBackClick = {
                    currentScreen = AppScreen.Home
                },
                onEditLocationsClick = {
                    currentScreen = AppScreen.AddLocations
                },
                onStartNavigationClick = {
                    // Más adelante: abrir Google Maps, Waze o navegación interna
                }
            )
        }
    }
}

private enum class AppScreen {
    Home,
    AddLocations,
    MapRoute
}

private fun demoCoordinatesFor(
    index: Int,
    location: String
): LatLng {
    val normalizedLocation = location.lowercase()

    return when {
        "montevideo" in normalizedLocation || "centro" in normalizedLocation -> {
            LatLng(-34.9011, -56.1645)
        }

        "tres cruces" in normalizedLocation -> {
            LatLng(-34.8937, -56.1666)
        }

        "parque rodó" in normalizedLocation || "parque rodo" in normalizedLocation -> {
            LatLng(-34.9127, -56.1646)
        }

        "punta carretas" in normalizedLocation -> {
            LatLng(-34.9227, -56.1594)
        }

        "pocitos" in normalizedLocation -> {
            LatLng(-34.9087, -56.1507)
        }

        "malvín" in normalizedLocation || "malvin" in normalizedLocation -> {
            LatLng(-34.8948, -56.1056)
        }

        "ciudad de la costa" in normalizedLocation -> {
            LatLng(-34.8167, -55.9500)
        }

        "san luis" in normalizedLocation -> {
            LatLng(-34.7748, -55.5847)
        }

        "canelones" in normalizedLocation -> {
            LatLng(-34.5228, -56.2778)
        }

        else -> {
            /*
             * Coordenadas demo cerca de Montevideo.
             * Esto evita que todos los puntos queden exactamente encima.
             */
            LatLng(
                -34.9011 + index * 0.012,
                -56.1645 + index * 0.015
            )
        }
    }
}