package com.example.fastroutes.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import com.example.fastroutes.data.model.RouteOption
import com.example.fastroutes.data.model.SavedLocation
import com.example.fastroutes.data.repository.RouteOptionsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AssignLocationDialog(
    location: SavedLocation,
    initialSelectedRouteOptionIds: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val repository = remember {
        RouteOptionsRepository()
    }

    var routeOptions by remember {
        mutableStateOf<List<RouteOption>>(emptyList())
    }

    var selectedIds by remember {
        mutableStateOf(initialSelectedRouteOptionIds)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(Unit) {
        isLoading = true

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Asignar ubicación")
        },
        text = {
            Column {
                Text(location.name)

                Spacer(modifier = Modifier.height(10.dp))

                when {
                    isLoading -> {
                        CircularProgressIndicator()
                    }

                    errorMessage != null -> {
                        Text(errorMessage ?: "")
                    }

                    else -> {
                        LazyColumn {
                            items(routeOptions, key = { it.id }) { option ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selectedIds.contains(option.id),
                                        onCheckedChange = { checked ->
                                            selectedIds = if (checked) {
                                                selectedIds + option.id
                                            } else {
                                                selectedIds.filterNot { it == option.id }
                                            }
                                        }
                                    )

                                    Text(option.name)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(selectedIds)
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancelar")
            }
        }
    )
}