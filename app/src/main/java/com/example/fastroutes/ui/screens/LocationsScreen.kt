package com.example.fastroutes.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastroutes.data.model.SavedLocation
import com.example.fastroutes.data.repository.LocationsRepository
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.lazy.rememberLazyListState

@Composable
fun LocationsScreen(
    isAdmin: Boolean,
    onBackClick: () -> Unit,
    onLoginAdminClick: () -> Unit,
    onLogoutAdminClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locationsRepository = remember {
        LocationsRepository()
    }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var locations by remember {
        mutableStateOf<List<SavedLocation>>(emptyList())
    }

    var searchText by remember {
        mutableStateOf("")
    }

    var selectedLocation by remember {
        mutableStateOf<SavedLocation?>(null)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var showLocationForm by remember {
        mutableStateOf(false)
    }

    var editingLocation by remember {
        mutableStateOf<SavedLocation?>(null)
    }

    var locationToDeactivate by remember {
        mutableStateOf<SavedLocation?>(null)
    }

    var assigningLocation by remember {
        mutableStateOf<SavedLocation?>(null)
    }

    var showAssignDialog by remember {
        mutableStateOf(false)
    }

    fun reloadLocations() {
        coroutineScope.launch {
            try {
                locations = withContext(Dispatchers.IO) {
                    locationsRepository.getAllActiveLocations()
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "No se pudieron cargar las ubicaciones."
            }
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null

        try {
            locations = withContext(Dispatchers.IO) {
                locationsRepository.getAllActiveLocations()
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "No se pudieron cargar las ubicaciones."
        } finally {
            isLoading = false
        }
    }

    val filteredLocations = locations.filter { location ->
        location.name.contains(searchText, ignoreCase = true) ||
                location.address.orEmpty().contains(searchText, ignoreCase = true)
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
        FastRoutesLocationsBackground()

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(
                top = 18.dp,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                LocationsHeader(
                    title = "Ubicaciones",
                    onBackClick = onBackClick
                )
            }

            item {
                SearchLocationField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                    }
                )
            }

            if (isAdmin) {
                item {
                    AdminActionsRow(
                        isAdmin = isAdmin,
                        onLoginAdminClick = onLoginAdminClick,
                        onLogoutAdminClick = onLogoutAdminClick,
                        onAddLocationClick = {
                            editingLocation = null
                            showLocationForm = true
                        }
                    )
                }
            }

            selectedLocation?.let { location ->
                item {
                    SelectedLocationMap(
                        location = location,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(330.dp)
                    )
                }

                if (isAdmin) {
                    item {
                        Button(
                            onClick = {
                                assigningLocation = location
                                showAssignDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0078FF)
                            )
                        ) {
                            Text(
                                text = "Asignar a Reparto/Vendedor",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            errorMessage?.let { message ->
                item {
                    ErrorCard(message = message)
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Listado",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "${filteredLocations.size} de ${locations.size}",
                        color = Color(0xFF35B8FF),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                }
            }

            when {
                isLoading -> {
                    item {
                        LoadingLocationsList()
                    }
                }

                locations.isEmpty() -> {
                    item {
                        EmptyLocationsList(
                            message = "No hay ubicaciones cargadas."
                        )
                    }
                }

                filteredLocations.isEmpty() -> {
                    item {
                        EmptyLocationsList(
                            message = "No se encontraron ubicaciones con esa búsqueda."
                        )
                    }
                }

                else -> {
                    items(
                        items = filteredLocations,
                        key = { it.id }
                    ) { location ->
                        LocationListItem(
                            location = location,
                            isSelected = selectedLocation?.id == location.id,
                            isAdmin = isAdmin,
                            onClick = {
                                selectedLocation = location

                                coroutineScope.launch {
                                    listState.animateScrollToItem(index = 1)
                                }
                            },
                            onEditClick = {
                                editingLocation = location
                                showLocationForm = true
                            },
                            onDeactivateClick = {
                                locationToDeactivate = location
                            }
                        )
                    }
                }
            }
        }
    }

    if (showLocationForm) {
        LocationFormDialog(
            location = editingLocation,
            onDismiss = {
                showLocationForm = false
                editingLocation = null
            },
            onSave = { name, address, latitude, longitude ->
                coroutineScope.launch {
                    try {
                        if (editingLocation == null) {
                            locationsRepository.createLocation(
                                name = name,
                                address = address,
                                latitude = latitude,
                                longitude = longitude
                            )
                        } else {
                            locationsRepository.updateLocation(
                                locationId = editingLocation!!.id,
                                name = name,
                                address = address,
                                latitude = latitude,
                                longitude = longitude
                            )
                        }

                        locations = withContext(Dispatchers.IO) {
                            locationsRepository.getAllActiveLocations()
                        }

                        showLocationForm = false
                        editingLocation = null
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "No se pudo guardar la ubicación."
                    }
                }
            }
        )
    }

    locationToDeactivate?.let { location ->
        AlertDialog(
            onDismissRequest = {
                locationToDeactivate = null
            },
            title = {
                Text(text = "Desactivar ubicación")
            },
            text = {
                Text(text = "¿Seguro que querés desactivar ${location.name}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                locationsRepository.deactivateLocation(location.id)

                                locations = withContext(Dispatchers.IO) {
                                    locationsRepository.getAllActiveLocations()
                                }

                                if (selectedLocation?.id == location.id) {
                                    selectedLocation = null
                                }

                                locationToDeactivate = null
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "No se pudo desactivar la ubicación."
                            }
                        }
                    }
                ) {
                    Text(text = "Desactivar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        locationToDeactivate = null
                    }
                ) {
                    Text(text = "Cancelar")
                }
            }
        )
    }

    assigningLocation?.let { location ->
        if (showAssignDialog) {
            AssignLocationDialog(
                location = location,
                onDismiss = {
                    showAssignDialog = false
                    assigningLocation = null
                },
                onSave = { selectedRouteOptionIds ->
                    coroutineScope.launch {
                        try {
                            locationsRepository.replaceLocationAssignments(
                                locationId = location.id,
                                routeOptionIds = selectedRouteOptionIds
                            )

                            showAssignDialog = false
                            assigningLocation = null

                            reloadLocations()
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "No se pudieron guardar las asignaciones."
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun LocationsHeader(
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
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.size(6.dp))

            Text(
                text = "Volver",
                color = Color(0xFF35B8FF),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = title,
            color = Color.White,
            fontSize = 31.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            maxLines = 1
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
            .height(62.dp),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = Color(0xFFB7C7DD),
                modifier = Modifier.size(27.dp)
            )
        },
        placeholder = {
            Text(
                text = "Buscar ubicación",
                color = Color(0xFFB7C7DD),
                fontSize = 17.sp
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
            focusedContainerColor = Color(0xFF061B3A).copy(alpha = 0.55f),
            unfocusedContainerColor = Color(0xFF061B3A).copy(alpha = 0.55f)
        )
    )
}

@Composable
private fun AdminActionsRow(
    isAdmin: Boolean,
    onLoginAdminClick: () -> Unit,
    onLogoutAdminClick: () -> Unit,
    onAddLocationClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onAddLocationClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0078FF)
            )
        ) {
            Text(
                text = "Agregar ubicación",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        TextButton(
            onClick = if (isAdmin) onLogoutAdminClick else onLoginAdminClick,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(
                text = if (isAdmin) "Cerrar admin" else "Modo admin",
                color = Color(0xFF35B8FF),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LocationListItem(
    location: SavedLocation,
    isSelected: Boolean,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeactivateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(if (isAdmin) 116.dp else 96.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                Color(0xFF072B5F).copy(alpha = 0.96f)
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
            if (isAdmin) {
                Column(
                    modifier = Modifier.padding(end = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar ubicación",
                            tint = Color(0xFF35B8FF)
                        )
                    }

                    IconButton(
                        onClick = onDeactivateClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Desactivar ubicación",
                            tint = Color(0xFFFF6B6B)
                        )
                    }
                }
            }

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
                    .padding(start = 16.dp, end = 8.dp)
            ) {
                Text(
                    text = location.name,
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                if (!location.address.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = location.address,
                        color = Color(0xFFB7C7DD),
                        fontSize = 15.sp,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(7.dp))

                Text(
                    text = if (isSelected) {
                        "Ubicación seleccionada en el mapa"
                    } else {
                        "Tocar para ver en el mapa"
                    },
                    color = if (isSelected) {
                        Color(0xFF35B8FF)
                    } else {
                        Color(0xFF8EA7C7)
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Abrir",
                tint = Color(0xFF35B8FF),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun SelectedLocationMap(
    location: SavedLocation,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val point = LatLng(
        location.latitude,
        location.longitude
    )

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(point, 16f)
    }

    LaunchedEffect(location.id) {
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngZoom(point, 16f)
        )
    }

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
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .padding(horizontal = 18.dp),
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
                        text = location.name,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    if (!location.address.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = location.address,
                            color = Color(0xFFB7C7DD),
                            fontSize = 15.sp,
                            maxLines = 1
                        )
                    }
                }

                TextButton(
                    onClick = {
                        openLocationInGoogleMaps(
                            context = context,
                            location = location
                        )
                    }
                ) {
                    Text(
                        text = "Ver en Maps",
                        color = Color(0xFF35B8FF),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowUp,
                        contentDescription = null,
                        tint = Color(0xFF35B8FF),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                cameraPositionState = cameraPositionState
            ) {
                Marker(
                    state = MarkerState(position = point),
                    title = location.name,
                    snippet = location.address ?: "Ubicación cargada"
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(Color(0xFF061B3A).copy(alpha = 0.96f))
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Place,
                    contentDescription = null,
                    tint = Color(0xFF35B8FF),
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.size(10.dp))

                Text(
                    text = "Ubicación seleccionada en el mapa",
                    color = Color(0xFFB7C7DD),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun LoadingLocationsList() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        shape = RoundedCornerShape(24.dp),
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
private fun EmptyLocationsList(
    message: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        shape = RoundedCornerShape(24.dp),
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
private fun FastRoutesLocationsBackground() {
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
            center = Offset(width * 0.93f, height * 0.23f),
            color = Color(0xFF00D9E8)
        )

        drawLocationPin(
            center = Offset(width * 0.86f, height * 0.88f),
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

private fun openLocationInGoogleMaps(
    context: Context,
    location: SavedLocation
) {
    val latitude = location.latitude
    val longitude = location.longitude
    val label = Uri.encode(location.name)

    val geoUri = Uri.parse(
        "geo:$latitude,$longitude?q=$latitude,$longitude($label)"
    )

    val googleMapsIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
        setPackage("com.google.android.apps.maps")
    }

    try {
        context.startActivity(googleMapsIntent)
    } catch (e: ActivityNotFoundException) {
        val browserUri = Uri.Builder()
            .scheme("https")
            .authority("www.google.com")
            .path("maps/search/")
            .appendQueryParameter("api", "1")
            .appendQueryParameter("query", "$latitude,$longitude")
            .build()

        val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
        context.startActivity(browserIntent)
    }
}