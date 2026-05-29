package com.example.fastroutes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
    stopPoints: List<LatLng>,
    routePolylinePoints: List<LatLng>,
    locationNames: List<String> = emptyList(),
    currentSegmentIndex: Int = 0,
    totalSegments: Int = 0,
    onBackClick: () -> Unit,
    onEditLocationsClick: () -> Unit,
    onStartNavigationClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val defaultLocation = LatLng(-34.9011, -56.1645)
    val cameraPoints = routePolylinePoints.ifEmpty { stopPoints }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            cameraPoints.firstOrNull() ?: defaultLocation,
            if (cameraPoints.isEmpty()) 11f else 13f
        )
    }

    LaunchedEffect(cameraPoints) {
        when {
            cameraPoints.size == 1 -> {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(cameraPoints.first(), 15f)
                )
            }

            cameraPoints.size > 1 -> {
                val boundsBuilder = LatLngBounds.builder()

                cameraPoints.forEach { point ->
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
            RouteHeader(
                totalStops = stopPoints.size,
                totalSegments = totalSegments,
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
                    stopPoints.forEachIndexed { index, point ->
                        Marker(
                            state = MarkerState(position = point),
                            title = getMarkerTitle(index, stopPoints.size),
                            snippet = locationNames.getOrNull(index) ?: "Parada ${index + 1}"
                        )
                    }

                    if (routePolylinePoints.size >= 2) {
                        Polyline(
                            points = routePolylinePoints,
                            color = Color(0xFF1565C0),
                            width = 10f
                        )
                    }
                }

                if (stopPoints.isEmpty()) {
                    EmptyRouteMessage(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                }
            }

            RouteBottomPanel(
                stopPoints = stopPoints,
                locationNames = locationNames,
                currentSegmentIndex = currentSegmentIndex,
                totalSegments = totalSegments,
                onEditLocationsClick = onEditLocationsClick,
                onStartNavigationClick = onStartNavigationClick
            )
        }
    }
}

@Composable
private fun RouteHeader(
    totalStops: Int,
    totalSegments: Int,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
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
                text = "Ruta por calles",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "$totalStops paradas • $totalSegments tramos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
@Composable
private fun RouteBottomPanel(
    stopPoints: List<LatLng>,
    locationNames: List<String>,
    currentSegmentIndex: Int,
    totalSegments: Int,
    onEditLocationsClick: () -> Unit,
    onStartNavigationClick: () -> Unit
) {
    val currentSegmentNumber = currentSegmentIndex + 1
    val hasPendingSegment = totalSegments > 0 && currentSegmentIndex < totalSegments

    val buttonText = when {
        totalSegments == 0 -> "Sin tramos disponibles"
        hasPendingSegment -> "Iniciar tramo $currentSegmentNumber/$totalSegments"
        else -> "Recorrido completo"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 24.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Orden del recorrido",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (hasPendingSegment) {
                    "Próximo tramo: $currentSegmentNumber de $totalSegments"
                } else if (totalSegments > 0) {
                    "Todos los tramos fueron iniciados"
                } else {
                    "No hay tramos cargados"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentPadding = PaddingValues(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(stopPoints) { index, point ->
                    RouteStopItem(
                        number = index + 1,
                        name = locationNames.getOrNull(index) ?: "Parada ${index + 1}",
                        point = point
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onStartNavigationClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                enabled = stopPoints.size >= 2 && hasPendingSegment
            ) {
                Text(text = buttonText)
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
    name: String,
    point: LatLng
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = "$number. $name",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Lat: ${formatCoordinate(point.latitude)} | Lng: ${formatCoordinate(point.longitude)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyRouteMessage(
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
                text = "Seleccioná ubicaciones para calcular el recorrido.",
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

private fun formatCoordinate(value: Double): String {
    return "%.5f".format(value)
}