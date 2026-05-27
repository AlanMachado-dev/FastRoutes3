package com.example.fastroutes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun MapRouteScreen(
    routePoints: List<LatLng>,
    locationNames: List<String> = emptyList(),
    onBackClick: () -> Unit,
    onEditLocationsClick: () -> Unit,
    onStartNavigationClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val defaultLocation = LatLng(-34.9011, -56.1645) // Montevideo

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            routePoints.firstOrNull() ?: defaultLocation,
            if (routePoints.isEmpty()) 11f else 13f
        )
    }

    LaunchedEffect(routePoints) {
        when {
            routePoints.size == 1 -> {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(routePoints.first(), 15f)
                )
            }

            routePoints.size > 1 -> {
                val boundsBuilder = LatLngBounds.builder()

                routePoints.forEach { point ->
                    boundsBuilder.include(point)
                }

                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(
                        boundsBuilder.build(),
                        140
                    )
                )
            }
        }
    }

    val mapProperties = MapProperties(
        mapType = MapType.NORMAL,
        isTrafficEnabled = true
    )

    val mapUiSettings = MapUiSettings(
        zoomControlsEnabled = true,
        compassEnabled = true,
        myLocationButtonEnabled = false
    )

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            MapHeader(
                totalStops = routePoints.size,
                onBackClick = onBackClick
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties,
                    uiSettings = mapUiSettings
                ) {
                    routePoints.forEachIndexed { index, point ->
                        Marker(
                            state = MarkerState(position = point),
                            title = getMarkerTitle(index, routePoints.size),
                            snippet = locationNames.getOrNull(index) ?: "Parada ${index + 1}"
                        )
                    }

                    if (routePoints.size >= 2) {
                        Polyline(
                            points = routePoints,
                            color = Color(0xFF1565C0),
                            width = 10f,
                            geodesic = true
                        )
                    }
                }

                if (routePoints.isEmpty()) {
                    EmptyMapMessage(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                }
            }

            RouteBottomPanel(
                routePoints = routePoints,
                locationNames = locationNames,
                onEditLocationsClick = onEditLocationsClick,
                onStartNavigationClick = onStartNavigationClick
            )
        }
    }
}

@Composable
private fun MapHeader(
    totalStops: Int,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(
            onClick = onBackClick
        ) {
            Text(text = "Volver")
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "Ruta optimizada",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "$totalStops paradas",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RouteBottomPanel(
    routePoints: List<LatLng>,
    locationNames: List<String>,
    onEditLocationsClick: () -> Unit,
    onStartNavigationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 24.dp,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.42f)
                .padding(20.dp)
        ) {
            Text(
                text = "Orden de recorrido",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (routePoints.isEmpty()) {
                Text(
                    text = "No hay ubicaciones para mostrar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(routePoints) { index, point ->
                        RouteStopItem(
                            number = index + 1,
                            title = getStopTitle(index, routePoints.size),
                            address = locationNames.getOrNull(index),
                            latitude = point.latitude,
                            longitude = point.longitude
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onStartNavigationClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                enabled = routePoints.size >= 2
            ) {
                Text(text = "Iniciar recorrido")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onEditLocationsClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "Editar ubicaciones")
            }
        }
    }
}

@Composable
private fun RouteStopItem(
    number: Int,
    title: String,
    address: String?,
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "$number.",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.padding(horizontal = 5.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (!address.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = address,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "Lat: ${formatCoordinate(latitude)} | Lng: ${formatCoordinate(longitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyMapMessage(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Sin ruta cargada",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Agregá ubicaciones para poder visualizar el recorrido en el mapa.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getMarkerTitle(
    index: Int,
    totalStops: Int
): String {
    return when (index) {
        0 -> "Inicio"
        totalStops - 1 -> "Destino"
        else -> "Parada ${index + 1}"
    }
}

private fun getStopTitle(
    index: Int,
    totalStops: Int
): String {
    return when (index) {
        0 -> "Punto de inicio"
        totalStops - 1 -> "Destino final"
        else -> "Parada intermedia"
    }
}

private fun formatCoordinate(value: Double): String {
    return "%.5f".format(value)
}

@Preview(showBackground = true)
@Composable
private fun MapRouteScreenPreview() {
    MaterialTheme {
        MapRouteScreen(
            routePoints = listOf(
                LatLng(-34.9011, -56.1645),
                LatLng(-34.9055, -56.1851),
                LatLng(-34.8836, -56.1819)
            ),
            locationNames = listOf(
                "Centro, Montevideo",
                "Parque Rodó, Montevideo",
                "Tres Cruces, Montevideo"
            ),
            onBackClick = {},
            onEditLocationsClick = {},
            onStartNavigationClick = {}
        )
    }
}