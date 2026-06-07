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
import androidx.compose.material3.HorizontalDivider
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
import kotlin.math.abs
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun MapRouteScreen(
    stopPoints: List<LatLng>,
    routePolylinePoints: List<LatLng>,
    locationNames: List<String> = emptyList(),
    navigationSegments: List<List<LatLng>> = emptyList(),
    clickedSegmentIndexes: Set<Int> = emptySet(),
    onBackClick: () -> Unit,
    onEditLocationsClick: () -> Unit,
    onOpenNavigationSegmentClick: (Int) -> Unit = {},
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
                totalSegments = navigationSegments.size,
                clickedSegments = clickedSegmentIndexes.size,
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
                navigationSegments = navigationSegments,
                clickedSegmentIndexes = clickedSegmentIndexes,
                onEditLocationsClick = onEditLocationsClick,
                onOpenNavigationSegmentClick = onOpenNavigationSegmentClick
            )
        }
    }
}

@Composable
private fun RouteHeader(
    totalStops: Int,
    totalSegments: Int,
    clickedSegments: Int,
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
                text = "$totalStops paradas • $clickedSegments/$totalSegments tramos abiertos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private enum class RouteExpandedSection {
    Segments,
    Stops
}

@Composable
private fun RouteBottomPanel(
    stopPoints: List<LatLng>,
    locationNames: List<String>,
    navigationSegments: List<List<LatLng>>,
    clickedSegmentIndexes: Set<Int>,
    onEditLocationsClick: () -> Unit,
    onOpenNavigationSegmentClick: (Int) -> Unit
) {
    var selectedSegmentIndex by remember(navigationSegments.size) {
        mutableStateOf(0)
    }

    var expandedSection by remember {
        mutableStateOf(RouteExpandedSection.Segments)
    }

    val totalSegments = navigationSegments.size

    if (selectedSegmentIndex > totalSegments - 1) {
        selectedSegmentIndex = 0
    }

    val selectedSegment = navigationSegments.getOrNull(selectedSegmentIndex)
    val wasClicked = clickedSegmentIndexes.contains(selectedSegmentIndex)

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
            CollapsibleSectionHeader(
                title = "Tramos del recorrido",
                subtitle = "${clickedSegmentIndexes.size}/$totalSegments tramos abiertos",
                isExpanded = expandedSection == RouteExpandedSection.Segments,
                onClick = {
                    expandedSection = RouteExpandedSection.Segments
                }
            )

            if (expandedSection == RouteExpandedSection.Segments) {
                Spacer(modifier = Modifier.height(12.dp))

                if (selectedSegment == null || totalSegments == 0) {
                    EmptySegmentMessage()
                } else {
                    SegmentNavigator(
                        selectedSegmentIndex = selectedSegmentIndex,
                        totalSegments = totalSegments,
                        wasClicked = wasClicked,
                        onPreviousClick = {
                            selectedSegmentIndex =
                                if (selectedSegmentIndex > 0) {
                                    selectedSegmentIndex - 1
                                } else {
                                    totalSegments - 1
                                }
                        },
                        onNextClick = {
                            selectedSegmentIndex =
                                if (selectedSegmentIndex < totalSegments - 1) {
                                    selectedSegmentIndex + 1
                                } else {
                                    0
                                }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    RouteSegmentCard(
                        segmentIndex = selectedSegmentIndex,
                        segmentPoints = selectedSegment,
                        stopPoints = stopPoints,
                        locationNames = locationNames,
                        wasClicked = wasClicked,
                        onClick = {
                            onOpenNavigationSegmentClick(selectedSegmentIndex)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            CollapsibleSectionHeader(
                title = "Orden completo de paradas",
                subtitle = "${stopPoints.size} paradas en total",
                isExpanded = expandedSection == RouteExpandedSection.Stops,
                onClick = {
                    expandedSection = RouteExpandedSection.Stops
                }
            )

            if (expandedSection == RouteExpandedSection.Stops) {
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
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
            }

            Spacer(modifier = Modifier.height(14.dp))

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
private fun CollapsibleSectionHeader(
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = if (isExpanded) "▾" else "▸",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SegmentNavigator(
    selectedSegmentIndex: Int,
    totalSegments: Int,
    wasClicked: Boolean,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onPreviousClick,
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                enabled = totalSegments > 1
            ) {
                Text(
                    text = "‹",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (wasClicked) {
                        "✓ Tramo ${selectedSegmentIndex + 1} de $totalSegments"
                    } else {
                        "○ Tramo ${selectedSegmentIndex + 1} de $totalSegments"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (wasClicked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                Text(
                    text = if (wasClicked) {
                        "Ya fue abierto"
                    } else {
                        "Pendiente de abrir"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = onNextClick,
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                enabled = totalSegments > 1
            ) {
                Text(
                    text = "›",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptySegmentMessage() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Sin tramos disponibles",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Calculá una ruta para ver los tramos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RouteSegmentCard(
    segmentIndex: Int,
    segmentPoints: List<LatLng>,
    stopPoints: List<LatLng>,
    locationNames: List<String>,
    wasClicked: Boolean,
    onClick: () -> Unit
) {
    val startIndex = segmentPoints.firstOrNull()
        ?.let { point -> findStopIndexForPoint(stopPoints, point) }
        ?: -1

    val endIndex = segmentPoints.lastOrNull()
        ?.let { point -> findStopIndexForPoint(stopPoints, point) }
        ?: -1

    val startName = locationNames.getOrNull(startIndex)
        ?: if (startIndex >= 0) "Parada ${startIndex + 1}" else "Inicio"

    val endName = locationNames.getOrNull(endIndex)
        ?: if (endIndex >= 0) "Parada ${endIndex + 1}" else "Destino"

    val titlePrefix = if (wasClicked) {
        "✓"
    } else {
        "○"
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (wasClicked) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = "$titlePrefix Tramo ${segmentIndex + 1}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$startName → $endName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${segmentPoints.size} puntos/paradas en este tramo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (wasClicked) {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text = "Abrir de nuevo")
                }
            } else {
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text = "Abrir en Google Maps")
                }
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

private fun findStopIndexForPoint(
    stopPoints: List<LatLng>,
    point: LatLng
): Int {
    return stopPoints.indexOfFirst { stopPoint ->
        areSamePoint(stopPoint, point)
    }
}

private fun areSamePoint(
    first: LatLng,
    second: LatLng
): Boolean {
    return abs(first.latitude - second.latitude) < 0.000001 &&
            abs(first.longitude - second.longitude) < 0.000001
}

private fun formatCoordinate(value: Double): String {
    return "%.5f".format(value)
}