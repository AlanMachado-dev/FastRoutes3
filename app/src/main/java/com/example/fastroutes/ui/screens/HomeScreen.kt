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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastroutes.data.model.RouteOption
import com.example.fastroutes.data.repository.RouteOptionsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    onRouteOptionClick: (RouteOption) -> Unit,
    onLocationsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val repository = remember {
        RouteOptionsRepository()
    }

    var routeOptions by remember {
        mutableStateOf<List<RouteOption>>(emptyList())
    }

    var expandedType by remember {
        mutableStateOf<String?>(null)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null

        try {
            routeOptions = withContext(Dispatchers.IO) {
                repository.getActiveRouteOptions()
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "No se pudieron cargar las opciones."
        } finally {
            isLoading = false
        }
    }

    val repartoLeche = routeOptions.filter { it.type == "REPARTO_LECHE" }
    val vendedoresColonial = routeOptions.filter { it.type == "VENDEDORES_COLONIAL" }
    val vendedoresClaldy = routeOptions.filter { it.type == "VENDEDORES_CLALDY" }

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
        FastRoutesHomeBackground()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(
                top = 22.dp,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Spacer(modifier = Modifier.height(15.dp))
                    Text(
                        text = "FastRoutes",
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )

                    Spacer(modifier = Modifier.height(46.dp))

                }
            }

            when {
                isLoading -> {
                    item {
                        LoadingOptions()
                    }
                }

                errorMessage != null -> {
                    item {
                        ErrorCard(
                            message = errorMessage ?: "No se pudieron cargar las opciones."
                        )
                    }
                }

                else -> {
                    item {
                        HomeOptionCard(
                            title = "Ubicaciones",
                            subtitle = "Ver todas",
                            icon = Icons.Outlined.Place,
                            onClick = onLocationsClick
                        )
                    }

                    item {
                        RouteGroupCard(
                            title = "Leche",
                            subtitle = "${repartoLeche.size} opciones",
                            icon = Icons.Outlined.LocalShipping,
                            isExpanded = expandedType == "REPARTO_LECHE",
                            onClick = {
                                expandedType = if (expandedType == "REPARTO_LECHE") {
                                    null
                                } else {
                                    "REPARTO_LECHE"
                                }
                            }
                        )
                    }

                    if (expandedType == "REPARTO_LECHE") {
                        item {
                            RouteOptionsList(
                                options = repartoLeche,
                                onRouteOptionClick = onRouteOptionClick
                            )
                        }
                    }

                    item {
                        RouteGroupCard(
                            title = "Colonial",
                            subtitle = "${vendedoresColonial.size} vendedores",
                            icon = Icons.Outlined.Person,
                            isExpanded = expandedType == "VENDEDORES_COLONIAL",
                            onClick = {
                                expandedType = if (expandedType == "VENDEDORES_COLONIAL") {
                                    null
                                } else {
                                    "VENDEDORES_COLONIAL"
                                }
                            }
                        )
                    }

                    if (expandedType == "VENDEDORES_COLONIAL") {
                        item {
                            RouteOptionsList(
                                options = vendedoresColonial,
                                onRouteOptionClick = onRouteOptionClick
                            )
                        }
                    }

                    item {
                        RouteGroupCard(
                            title = "Claldy",
                            subtitle = "${vendedoresClaldy.size} vendedores",
                            icon = Icons.Outlined.Person,
                            isExpanded = expandedType == "VENDEDORES_CLALDY",
                            onClick = {
                                expandedType = if (expandedType == "VENDEDORES_CLALDY") {
                                    null
                                } else {
                                    "VENDEDORES_CLALDY"
                                }
                            }
                        )
                    }

                    if (expandedType == "VENDEDORES_CLALDY") {
                        item {
                            RouteOptionsList(
                                options = vendedoresClaldy,
                                onRouteOptionClick = onRouteOptionClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(26.dp),
                clip = false
            ),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        HomeCardContent(
            title = title,
            subtitle = subtitle,
            icon = icon,
            trailingIcon = Icons.Outlined.ChevronRight
        )
    }
}

@Composable
private fun RouteGroupCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(26.dp),
                clip = false
            ),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        HomeCardContent(
            title = title,
            subtitle = subtitle,
            icon = icon,
            trailingIcon = if (isExpanded) {
                Icons.Outlined.ExpandLess
            } else {
                Icons.Outlined.ExpandMore
            }
        )
    }
}

@Composable
private fun HomeCardContent(
    title: String,
    subtitle: String,
    icon: ImageVector,
    trailingIcon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFEAF4FF),
                            Color(0xFFF8FBFF)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF0078FF),
                modifier = Modifier.size(28.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 18.dp, end = 8.dp)
        ) {
            Text(
                text = title,
                color = Color(0xFF061B3A),
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                color = Color(0xFF334A6B),
                fontSize = 14.sp,
                maxLines = 1
            )
        }

        Icon(
            imageVector = trailingIcon,
            contentDescription = null,
            tint = Color(0xFF0078FF),
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
private fun RouteOptionsList(
    options: List<RouteOption>,
    onRouteOptionClick: (RouteOption) -> Unit
) {
    Column(
        modifier = Modifier.padding(top = 2.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (options.isEmpty()) {
            Text(
                text = "No hay opciones cargadas.",
                modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
                color = Color(0xFFB7C7DD),
                fontSize = 15.sp
            )
        } else {
            options.forEach { option ->
                RouteOptionItem(
                    option = option,
                    onClick = {
                        onRouteOptionClick(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun RouteOptionItem(
    option: RouteOption,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE9EDF5)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = option.name,
                modifier = Modifier.weight(1f),
                color = Color(0xFF24324A),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Abrir",
                tint = Color(0xFF0078FF),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun LoadingOptions() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = Color(0xFF0078FF)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Cargando opciones...",
                color = Color(0xFF334A6B),
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun ErrorCard(
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(18.dp),
            color = Color(0xFFE53935),
            fontSize = 15.sp
        )
    }
}

@Composable
private fun FastRoutesHomeBackground() {
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
            moveTo(width * 0.78f, height * 0.06f)
            cubicTo(
                width * 0.68f,
                height * 0.18f,
                width * 1.05f,
                height * 0.16f,
                width * 0.88f,
                height * 0.34f
            )
            cubicTo(
                width * 0.76f,
                height * 0.46f,
                width * 0.60f,
                height * 0.58f,
                width * 0.82f,
                height * 0.74f
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

        val leftRoutePath = Path().apply {
            moveTo(-40f, height * 0.57f)
            cubicTo(
                width * 0.12f,
                height * 0.48f,
                width * 0.22f,
                height * 0.62f,
                width * 0.02f,
                height * 0.68f
            )
        }

        drawPath(
            path = leftRoutePath,
            color = routeColor.copy(alpha = 0.32f),
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
            center = Offset(width * 0.93f, height * 0.17f),
            color = Color(0xFF00D9E8)
        )

        drawLocationPin(
            center = Offset(width * 0.86f, height * 0.87f),
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