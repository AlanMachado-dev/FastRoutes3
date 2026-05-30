package com.example.fastroutes.ui.screens

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fastroutes.data.model.SavedLocation
import com.example.fastroutes.data.repository.LocationsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

@Composable
fun LocationsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locationsRepository = remember {
        LocationsRepository()
    }

    val coroutineScope = rememberCoroutineScope()

    var locations by remember {
        mutableStateOf<List<SavedLocation>>(emptyList())
    }

    var searchText by remember {
        mutableStateOf("")
    }
    var selectedLocation by remember {
        mutableStateOf<SavedLocation?>(null)
    }

    var showLocationForm by remember { mutableStateOf(false) }

    var editingLocation by remember { mutableStateOf<SavedLocation?>(null) }

    var locationToDeactivate by remember { mutableStateOf<SavedLocation?>(null) }

    var assigningLocation by remember { mutableStateOf<SavedLocation?>(null) }

    var showAssignDialog by remember { mutableStateOf(false) }

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

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp)
        ) {
            TextButton(
                onClick = onBackClick
            ) {
                Text(text = "Volver")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Ubicaciones",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))


            OutlinedTextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(text = "Buscar ubicación")
                },
                placeholder = {
                    Text(text = "Buscar por nombre o dirección")
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    editingLocation = null
                    showLocationForm = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Agregar ubicación")
            }

            Spacer(modifier = Modifier.height(14.dp))

            selectedLocation?.let { location ->
                SelectedLocationMap(
                    location = location,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))


                Button(
                    onClick = {
                        assigningLocation = location
                        showAssignDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Asignar a Reparto/Vendedor")
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Listado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${filteredLocations.size} de ${locations.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            when {
                isLoading -> {
                    LoadingLocationsList(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }

                errorMessage != null -> {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                locations.isEmpty() -> {
                    EmptyLocationsList(
                        message = "No hay ubicaciones cargadas.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }

                filteredLocations.isEmpty() -> {
                    EmptyLocationsList(
                        message = "No se encontraron ubicaciones con esa búsqueda.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = filteredLocations,
                            key = { it.id }
                        ) { location ->
                            LocationListItem(
                                location = location,
                                isSelected = selectedLocation?.id == location.id,
                                onClick = {
                                    selectedLocation = location
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
                Text("Desactivar ubicación")
            },
            text = {
                Text("¿Seguro que querés desactivar ${location.name}?")
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

                                selectedLocation = null
                                locationToDeactivate = null
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "No se pudo desactivar la ubicación."
                            }
                        }
                    }
                ) {
                    Text("Desactivar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        locationToDeactivate = null
                    }
                ) {
                    Text("Cancelar")
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
private fun LocationListItem(
    location: SavedLocation,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeactivateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.padding(end = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar ubicación",
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }

                IconButton(
                    onClick = onDeactivateClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Desactivar ubicación",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                if (!location.address.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = location.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isSelected) {
                        "Ubicación seleccionada en el mapa"
                    } else {
                        "Tocar para ver en el mapa"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
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
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = location.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (!location.address.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = location.address,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    Text(text = "Ver en Maps")
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
        }
    }
}
@Composable
private fun LoadingLocationsList(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Cargando ubicaciones...",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyLocationsList(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(0.45f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(0.45f)
        )
    }
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