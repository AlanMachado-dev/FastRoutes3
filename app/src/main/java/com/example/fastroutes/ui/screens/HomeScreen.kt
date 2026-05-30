package com.example.fastroutes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.MaterialTheme
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
            Text(
                text = "FastRoutes",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Seleccioná el tipo de recorrido que querés preparar.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            when {
                isLoading -> {
                    LoadingOptions(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }

                errorMessage != null -> {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Card(
                                onClick = onLocationsClick,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp)
                                ) {
                                    Text(
                                        text = "Ubicaciones",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "Ver todas las ubicaciones cargadas",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                        item {
                            RouteGroupCard(
                                title = "Reparto Leche",
                                subtitle = "${repartoLeche.size} opciones",
                                isExpanded = expandedType == "REPARTO_LECHE",
                                onClick = {
                                    expandedType = if (expandedType == "REPARTO_LECHE") {
                                        null
                                    } else {
                                        "REPARTO_LECHE"
                                    }
                                }
                            )

                            if (expandedType == "REPARTO_LECHE") {
                                RouteOptionsList(
                                    options = repartoLeche,
                                    onRouteOptionClick = onRouteOptionClick
                                )
                            }
                        }

                        item {
                            RouteGroupCard(
                                title = "Vendedores Colonial",
                                subtitle = "${vendedoresColonial.size} vendedores",
                                isExpanded = expandedType == "VENDEDORES_COLONIAL",
                                onClick = {
                                    expandedType = if (expandedType == "VENDEDORES_COLONIAL") {
                                        null
                                    } else {
                                        "VENDEDORES_COLONIAL"
                                    }
                                }
                            )

                            if (expandedType == "VENDEDORES_COLONIAL") {
                                RouteOptionsList(
                                    options = vendedoresColonial,
                                    onRouteOptionClick = onRouteOptionClick
                                )
                            }
                        }

                        item {
                            RouteGroupCard(
                                title = "Vendedores Claldy",
                                subtitle = "${vendedoresClaldy.size} vendedores",
                                isExpanded = expandedType == "VENDEDORES_CLALDY",
                                onClick = {
                                    expandedType = if (expandedType == "VENDEDORES_CLALDY") {
                                        null
                                    } else {
                                        "VENDEDORES_CLALDY"
                                    }
                                }
                            )

                            if (expandedType == "VENDEDORES_CLALDY") {
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
}

@Composable
private fun RouteGroupCard(
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isExpanded) {
                    "$subtitle • Tocá para cerrar"
                } else {
                    "$subtitle • Tocá para abrir"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun RouteOptionsList(
    options: List<RouteOption>,
    onRouteOptionClick: (RouteOption) -> Unit
) {
    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (options.isEmpty()) {
            Text(
                text = "No hay opciones cargadas.",
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            .padding(start = 14.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = option.name,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun LoadingOptions(
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
            text = "Cargando opciones...",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}