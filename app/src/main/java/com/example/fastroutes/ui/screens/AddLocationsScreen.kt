package com.example.fastroutes.ui.screens

import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fastroutes.data.model.SavedLocation
import com.example.fastroutes.data.repository.LocationsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                TextButton(
                    onClick = onBackClick
                ) {
                    Text(text = "Volver")
                }

                Text(
                    text = routeTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(text = "Buscar ubicación")
                },
                placeholder = {
                    Text(text = "Ej: Cliente Centro, San Luis, Pando...")
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ubicaciones disponibles",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${selectedLocations.size} seleccionadas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            if (autoSelectAll && availableLocations.isNotEmpty()) {
                val allSelected = selectedLocationIds.size == availableLocations.size

                OutlinedButton(
                    onClick = {
                        selectedLocationIds = if (allSelected) {
                            emptyList()
                        } else {
                            availableLocations.map { it.id }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (allSelected) {
                            "Deseleccionar todas"
                        } else {
                            "Seleccionar todas"
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            when {
                isLoading -> {
                    LoadingLocations(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }

                availableLocations.isEmpty() -> {
                    EmptyLocations(
                        message = "No hay ubicaciones cargadas en Supabase.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }

                filteredLocations.isEmpty() -> {
                    EmptyLocations(
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

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    calculateRoute()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                enabled = selectedLocations.isNotEmpty()
            ) {
                Text(text = "Calcular ruta")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    selectedLocationIds = emptyList()
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                enabled = selectedLocations.isNotEmpty()
            ) {
                Text(text = "Limpiar selección")
            }
        }
    }
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
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = {
                    onClick()
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
            ) {
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (!location.address.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = location.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Lat: ${location.latitude} | Lng: ${location.longitude}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LoadingLocations(
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyLocations(
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

@Preview(showBackground = true)
@Composable
private fun AddLocationsScreenPreview() {
    val demoLocations = listOf(
        SavedLocation(
            id = "1",
            name = "Cliente Centro",
            address = "Centro, Montevideo",
            latitude = -34.9011,
            longitude = -56.1645
        ),
        SavedLocation(
            id = "2",
            name = "Cliente San Luis",
            address = "San Luis, Canelones",
            latitude = -34.7748,
            longitude = -55.5847
        )
    )

    MaterialTheme {
        LazyColumn {
            items(demoLocations) { location ->
                LocationSelectableItem(
                    location = location,
                    isSelected = false,
                    onClick = {}
                )
            }
        }
    }
}