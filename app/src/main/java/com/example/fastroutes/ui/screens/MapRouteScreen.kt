package com.example.fastroutes.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF031225),
                        Color(0xFF06366F),
                        Color(0xFF031225)
                    )
                )
            )
    ) {
        FastRoutesMapBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 8.dp)
        ) {
            RouteHeader(
                totalStops = stopPoints.size,
                totalSegments = navigationSegments.size,
                clickedSegments = clickedSegmentIndexes.size,
                onBackClick = onBackClick
            )

            Spacer(modifier = Modifier.height(10.dp))

            RouteMapCard(
                stopPoints = stopPoints,
                routePolylinePoints = routePolylinePoints,
                locationNames = locationNames,
                cameraPositionState = cameraPositionState,
                mapProperties = mapProperties,
                mapUiSettings = mapUiSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            RouteBottomPanel(
                stopPoints = stopPoints,
                locationNames = locationNames,
                navigationSegments = navigationSegments,
                clickedSegmentIndexes = clickedSegmentIndexes,
                onEditLocationsClick = onEditLocationsClick,
                onOpenNavigationSegmentClick = onOpenNavigationSegmentClick,
                modifier = Modifier.weight(1f)
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
            .height(58.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onBackClick,
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = "Volver",
                tint = Color(0xFF35B8FF),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.size(5.dp))

            Text(
                text = "Volver",
                color = Color(0xFF35B8FF),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "Ruta por calles",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "$totalStops paradas • $clickedSegments/$totalSegments tramos",
                color = Color(0xFFB7C7DD),
                fontSize = 14.sp,
                textAlign = TextAlign.End,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RouteMapCard(
    stopPoints: List<LatLng>,
    routePolylinePoints: List<LatLng>,
    locationNames: List<String>,
    cameraPositionState: com.google.maps.android.compose.CameraPositionState,
    mapProperties: MapProperties,
    mapUiSettings: MapUiSettings,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.shadow(
            elevation = 18.dp,
            shape = RoundedCornerShape(26.dp),
            clip = false
        ),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF061B3A).copy(alpha = 0.92f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
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
                        color = Color(0xFF00B8FF),
                        width = 9f
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
    onOpenNavigationSegmentClick: (Int) -> Unit,
    modifier: Modifier = Modifier
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

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            CollapsibleSectionHeader(
                title = "Tramos del recorrido",
                subtitle = "${clickedSegmentIndexes.size}/$totalSegments tramos abiertos",
                icon = Icons.Outlined.List,
                isExpanded = expandedSection == RouteExpandedSection.Segments,
                onClick = {
                    expandedSection = RouteExpandedSection.Segments
                }
            )
        }

        if (expandedSection == RouteExpandedSection.Segments) {
            item {
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
                }
            }

            if (selectedSegment != null && totalSegments > 0) {
                item {
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
        }

        item {
            CollapsibleSectionHeader(
                title = "Orden completo de paradas",
                subtitle = "${stopPoints.size} paradas en total",
                icon = Icons.Outlined.Place,
                isExpanded = expandedSection == RouteExpandedSection.Stops,
                onClick = {
                    expandedSection = RouteExpandedSection.Stops
                }
            )
        }

        if (expandedSection == RouteExpandedSection.Stops) {
            itemsIndexed(stopPoints) { index, point ->
                RouteStopItem(
                    number = index + 1,
                    name = locationNames.getOrNull(index) ?: "Parada ${index + 1}",
                    point = point
                )
            }
        }

        item {
            EditLocationsButton(
                onClick = onEditLocationsClick
            )
        }
    }
}

@Composable
private fun CollapsibleSectionHeader(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(22.dp),
                clip = false
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF061B3A).copy(alpha = 0.90f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = subtitle,
                    color = Color(0xFFB7C7DD),
                    fontSize = 14.sp,
                    maxLines = 1
                )
            }

            Icon(
                imageVector = if (isExpanded) {
                    Icons.Outlined.KeyboardArrowDown
                } else {
                    Icons.Outlined.ChevronRight
                },
                contentDescription = null,
                tint = Color(0xFF35B8FF),
                modifier = Modifier.size(30.dp)
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
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF061B3A).copy(alpha = 0.90f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onPreviousClick,
                enabled = totalSegments > 1,
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(46.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF35B8FF),
                    disabledContentColor = Color(0xFF64748B)
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChevronLeft,
                    contentDescription = "Tramo anterior",
                    modifier = Modifier.size(30.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Tramo ${selectedSegmentIndex + 1} de $totalSegments",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (wasClicked) {
                        "Ya fue abierto"
                    } else {
                        "Pendiente de abrir"
                    },
                    color = if (wasClicked) {
                        Color(0xFF35B8FF)
                    } else {
                        Color(0xFFB7C7DD)
                    },
                    fontSize = 13.sp,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(totalSegments.coerceAtMost(7)) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == selectedSegmentIndex) 9.dp else 7.dp)
                                .background(
                                    color = if (index == selectedSegmentIndex) {
                                        Color(0xFF35B8FF)
                                    } else {
                                        Color(0xFF1E5FAF).copy(alpha = 0.45f)
                                    },
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onNextClick,
                enabled = totalSegments > 1,
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF35B8FF),
                    disabledContentColor = Color(0xFF64748B)
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "Siguiente tramo",
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptySegmentMessage() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF061B3A).copy(alpha = 0.90f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Sin tramos disponibles",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Calculá una ruta para ver los tramos.",
                color = Color(0xFFB7C7DD),
                fontSize = 15.sp
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(26.dp),
                clip = false
            ),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (wasClicked) {
                Color(0xFF072B5F).copy(alpha = 0.96f)
            } else {
                Color(0xFF061B3A).copy(alpha = 0.90f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF073B7A),
                                    Color(0xFF061B3A)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Place,
                        contentDescription = null,
                        tint = Color(0xFF35B8FF),
                        modifier = Modifier.size(31.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    Text(
                        text = "Tramo ${segmentIndex + 1}",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = "$startName → $endName",
                        color = Color(0xFFB7C7DD),
                        fontSize = 16.sp,
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${segmentPoints.size} puntos/paradas en este tramo",
                color = Color(0xFF8EA7C7),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (wasClicked) {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF35B8FF)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(modifier = Modifier.size(10.dp))

                    Text(
                        text = "Abrir de nuevo",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF005BFF),
                                    Color(0xFF00D9E8)
                                )
                            ),
                            shape = RoundedCornerShape(18.dp)
                        ),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF005BFF),
                                        Color(0xFF00D9E8)
                                    )
                                ),
                                shape = RoundedCornerShape(18.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Navigation,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(23.dp)
                            )

                            Spacer(modifier = Modifier.size(12.dp))

                            Text(
                                text = "Abrir tramo en Maps",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
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
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF061B3A).copy(alpha = 0.88f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF073B7A),
                                Color(0xFF061B3A)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number.toString(),
                    color = Color(0xFF35B8FF),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = name,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                lineHeight = 21.sp
            )
        }
    }
}
@Composable
private fun EditLocationsButton(
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color(0xFF35B8FF)
        )
    ) {
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
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
            containerColor = Color(0xFF061B3A).copy(alpha = 0.92f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Sin ruta cargada",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Seleccioná ubicaciones para calcular el recorrido.",
                color = Color(0xFFB7C7DD),
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FastRoutesMapBackground() {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val width = size.width
        val height = size.height

        val mapLineColor = Color(0xFF1B6FD1).copy(alpha = 0.13f)
        val routeColor = Color(0xFF00B8FF).copy(alpha = 0.46f)

        drawLine(
            color = mapLineColor,
            start = Offset(-80f, height * 0.18f),
            end = Offset(width * 0.58f, height * 0.04f),
            strokeWidth = 5f
        )

        drawLine(
            color = mapLineColor,
            start = Offset(-40f, height * 0.68f),
            end = Offset(width * 0.58f, height * 0.45f),
            strokeWidth = 4f
        )

        drawLine(
            color = mapLineColor,
            start = Offset(width * 0.06f, height * 0.90f),
            end = Offset(width * 0.88f, height * 0.68f),
            strokeWidth = 4f
        )

        drawLine(
            color = mapLineColor,
            start = Offset(width * 0.18f, height),
            end = Offset(width * 0.95f, height * 0.78f),
            strokeWidth = 4f
        )

        val rightRoutePath = Path().apply {
            moveTo(width * 0.88f, height * 0.02f)
            cubicTo(
                width * 0.76f,
                height * 0.16f,
                width * 1.08f,
                height * 0.18f,
                width * 0.90f,
                height * 0.36f
            )
            cubicTo(
                width * 0.76f,
                height * 0.50f,
                width * 0.58f,
                height * 0.62f,
                width * 0.88f,
                height * 0.86f
            )
        }

        drawPath(
            path = rightRoutePath,
            color = routeColor,
            style = Stroke(
                width = 4f,
                pathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(22f, 18f),
                    phase = 0f
                )
            )
        )

        val bottomRoutePath = Path().apply {
            moveTo(width * 0.12f, height * 0.97f)
            cubicTo(
                width * 0.28f,
                height * 0.88f,
                width * 0.48f,
                height * 1.02f,
                width * 0.63f,
                height * 0.90f
            )
        }

        drawPath(
            path = bottomRoutePath,
            color = routeColor.copy(alpha = 0.42f),
            style = Stroke(
                width = 4f,
                pathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(22f, 18f),
                    phase = 0f
                )
            )
        )

        drawLocationPin(
            center = Offset(width * 0.78f, height * 0.07f),
            color = Color(0xFF0078FF)
        )

        drawLocationPin(
            center = Offset(width * 0.93f, height * 0.23f),
            color = Color(0xFF00D9E8)
        )

        drawLocationPin(
            center = Offset(width * 0.16f, height * 0.92f),
            color = Color(0xFF00D9E8)
        )
    }
}

private fun DrawScope.drawLocationPin(
    center: Offset,
    color: Color
) {
    drawCircle(
        color = color.copy(alpha = 0.24f),
        radius = 25f,
        center = center
    )

    drawCircle(
        color = color,
        radius = 13f,
        center = center
    )

    drawCircle(
        color = Color(0xFF031225),
        radius = 5f,
        center = center
    )

    drawLine(
        color = color,
        start = Offset(center.x, center.y + 12f),
        end = Offset(center.x, center.y + 40f),
        strokeWidth = 8f,
        cap = StrokeCap.Round
    )
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