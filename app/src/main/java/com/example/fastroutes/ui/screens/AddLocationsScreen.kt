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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastroutes.data.model.SavedLocation
import com.example.fastroutes.data.repository.LocationsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch

@Composable
fun AddLocationsScreen(
    routeOptionId: String,
    routeTitle: String,
    autoSelectAll: Boolean = false,
    onBackClick: () -> Unit,
    onCalculateRouteClick: (List<SavedLocation>) -> Unit,
    modifier: Modifier = Modifier
) {
    val locationsRepository = remember {
        LocationsRepository()
    }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val showScrollToTopButton by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 2
        }
    }

    var availableLocations by remember {
        mutableStateOf<List<SavedLocation>>(emptyList())
    }

    var selectedLocationIds by remember {
        mutableStateOf<List<String>>(emptyList())
    }

    var searchText by remember {
        mutableStateOf("")
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(routeOptionId, autoSelectAll) {
        isLoading = true
        errorMessage = null

        try {
            val result = withContext(Dispatchers.IO) {
                locationsRepository.getActiveLocationsByRouteOption(routeOptionId)
            }

            availableLocations = result

            selectedLocationIds = if (autoSelectAll) {
                result.map { it.id }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "No se pudieron cargar las ubicaciones."
        } finally {
            isLoading = false
        }
    }

    val filteredLocations = availableLocations.filter { location ->
        location.name.contains(searchText, ignoreCase = true) ||
                location.address.orEmpty().contains(searchText, ignoreCase = true)
    }

    val selectedLocations = selectedLocationIds.mapNotNull { selectedId ->
        availableLocations.firstOrNull { it.id == selectedId }
    }

    fun toggleLocation(location: SavedLocation) {
        selectedLocationIds = if (selectedLocationIds.contains(location.id)) {
            selectedLocationIds.filterNot { it == location.id }
        } else {
            selectedLocationIds + location.id
        }
    }

    fun calculateRoute() {
        if (selectedLocations.isEmpty()) {
            errorMessage = "Seleccioná al menos 1 ubicación para calcular una ruta."
            return
        }

        errorMessage = null
        onCalculateRouteClick(selectedLocations)
    }

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
        FastRoutesAddLocationsBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 18.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    AddLocationsHeader(
                        title = routeTitle,
                        onBackClick = onBackClick
                    )
                }

                item {
                    SearchLocationField(
                        value = searchText,
                        onValueChange = {
                            searchText = it
                            errorMessage = null
                        }
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ubicaciones disponibles",
                            color = Color.White,
                            fontSize = 21.sp,
                            lineHeight = 25.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "${selectedLocations.size} seleccionadas",
                            color = Color(0xFF35B8FF),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End
                        )
                    }
                }

                errorMessage?.let { message ->
                    item {
                        ErrorCard(message = message)
                    }
                }

                if (autoSelectAll && availableLocations.isNotEmpty()) {
                    item {
                        val allSelected = selectedLocationIds.size == availableLocations.size

                        OutlinedButton(
                            onClick = {
                                selectedLocationIds = if (allSelected) {
                                    emptyList()
                                } else {
                                    availableLocations.map { it.id }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF35B8FF)
                            )
                        ) {
                            Text(
                                text = if (allSelected) {
                                    "Deseleccionar todas"
                                } else {
                                    "Seleccionar todas"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                when {
                    isLoading -> {
                        item {
                            LoadingLocations(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            )
                        }
                    }

                    availableLocations.isEmpty() -> {
                        item {
                            EmptyLocations(
                                message = "No hay ubicaciones cargadas en Supabase.",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            )
                        }
                    }

                    filteredLocations.isEmpty() -> {
                        item {
                            EmptyLocations(
                                message = "No se encontraron ubicaciones con esa búsqueda.",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            )
                        }
                    }

                    else -> {
                        items(
                            items = filteredLocations,
                            key = { it.id }
                        ) { location ->
                            LocationSelectableItem(
                                location = location,
                                isSelected = selectedLocationIds.contains(location.id),
                                onClick = {
                                    toggleLocation(location)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            GradientCalculateButton(
                enabled = selectedLocations.isNotEmpty(),
                onClick = {
                    calculateRoute()
                }
            )

            if (!autoSelectAll) {
                Spacer(modifier = Modifier.height(10.dp))

                ClearSelectionButton(
                    enabled = selectedLocations.isNotEmpty(),
                    onClick = {
                        selectedLocationIds = emptyList()
                        errorMessage = null
                    }
                )
            }
        }

        if (showScrollToTopButton) {
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 82.dp, end = 20.dp)
                    .size(48.dp),
                shape = RoundedCornerShape(16.dp),
                containerColor = Color(0xFF0078FF),
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowUp,
                    contentDescription = "Volver arriba",
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}
@Composable
private fun AddLocationsHeader(
    title: String,
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
                modifier = Modifier.size(26.dp)
            )

            Spacer(modifier = Modifier.size(6.dp))

            Text(
                text = "Volver",
                color = Color(0xFF35B8FF),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }


        Text(
            text = title,
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SearchLocationField(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(18.dp),
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = Color(0xFFB7C7DD),
                modifier = Modifier.size(28.dp)
            )
        },
        placeholder = {
            Text(
                text = "Buscar ubicación",
                color = Color(0xFFB7C7DD),
                fontSize = 18.sp
            )
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF0078FF),
            unfocusedBorderColor = Color(0xFF1E5FAF),
            cursorColor = Color(0xFF35B8FF),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = Color(0xFF061B3A).copy(alpha = 0.58f),
            unfocusedContainerColor = Color(0xFF061B3A).copy(alpha = 0.58f)
        )
    )
}

@Composable
private fun LocationSelectableItem(
    location: SavedLocation,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(118.dp)
            .shadow(
                elevation = if (isSelected) 18.dp else 10.dp,
                shape = RoundedCornerShape(26.dp),
                clip = false
            ),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                Color(0xFF072B5F).copy(alpha = 0.97f)
            } else {
                Color(0xFF061B3A).copy(alpha = 0.88f)
            }
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
                    .size(46.dp)
                    .background(
                        brush = if (isSelected) {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF005BFF),
                                    Color(0xFF00D9E8)
                                )
                            )
                        } else {
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF073B7A),
                                    Color(0xFF061B3A)
                                )
                            )
                        },
                        shape = RoundedCornerShape(13.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Seleccionada",
                        tint = Color.White,
                        modifier = Modifier.size(29.dp)
                    )
                } else {
                    Checkbox(
                        checked = false,
                        onCheckedChange = {
                            onClick()
                        },
                        colors = CheckboxDefaults.colors(
                            uncheckedColor = Color(0xFF8EA7C7),
                            checkedColor = Color(0xFF0078FF),
                            checkmarkColor = Color.White
                        )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, end = 12.dp)
            ) {
                Text(
                    text = location.name,
                    color = Color.White,
                    fontSize = 20.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )

                if (!location.address.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = location.address,
                        color = Color(0xFFB7C7DD),
                        fontSize = 16.sp,
                        maxLines = 1
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = Color(0xFF031225).copy(alpha = 0.38f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Place,
                    contentDescription = null,
                    tint = Color(0xFF35B8FF),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
@Composable
private fun GradientCalculateButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color(0xFF233A59)
        ),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (enabled) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF005BFF),
                                Color(0xFF00D9E8)
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF233A59),
                                Color(0xFF334A6B)
                            )
                        )
                    },
                    shape = RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Calcular ruta",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ClearSelectionButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color(0xFF35B8FF),
            disabledContentColor = Color(0xFF64748B)
        )
    ) {
        Text(
            text = "Limpiar selección",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LoadingLocations(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF061B3A).copy(alpha = 0.90f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = Color(0xFF35B8FF)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Cargando ubicaciones...",
                color = Color(0xFFB7C7DD),
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun EmptyLocations(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF061B3A).copy(alpha = 0.90f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                color = Color(0xFFB7C7DD),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 18.dp)
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
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF331C25).copy(alpha = 0.95f)
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(18.dp),
            color = Color(0xFFFF8A8A),
            fontSize = 14.sp
        )
    }
}

@Composable
private fun FastRoutesAddLocationsBackground() {
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
            center = Offset(width * 0.58f, height * 0.08f),
            color = Color(0xFF00D9E8)
        )

        drawLocationPin(
            center = Offset(width * 0.93f, height * 0.20f),
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