package com.example.fastroutes.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.example.fastroutes.data.model.SavedLocation

@Composable
fun LocationFormDialog(
    location: SavedLocation?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        address: String?,
        latitude: Double,
        longitude: Double
    ) -> Unit
) {
    var name by remember(location?.id) {
        mutableStateOf(location?.name.orEmpty())
    }

    var address by remember(location?.id) {
        mutableStateOf(location?.address.orEmpty())
    }

    var latitudeText by remember(location?.id) {
        mutableStateOf(location?.latitude?.toString().orEmpty())
    }

    var longitudeText by remember(location?.id) {
        mutableStateOf(location?.longitude?.toString().orEmpty())
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (location == null) {
                    "Agregar ubicación"
                } else {
                    "Editar ubicación"
                }
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true
                )

                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Dirección / referencia") },
                    singleLine = true
                )

                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))

                OutlinedTextField(
                    value = latitudeText,
                    onValueChange = { latitudeText = it },
                    label = { Text("Latitud") },
                    singleLine = true
                )

                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))

                OutlinedTextField(
                    value = longitudeText,
                    onValueChange = { longitudeText = it },
                    label = { Text("Longitud") },
                    singleLine = true
                )

                if (errorMessage != null) {
                    Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))

                    Text(text = errorMessage ?: "")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val latitude = latitudeText.toDoubleOrNull()
                    val longitude = longitudeText.toDoubleOrNull()

                    when {
                        name.isBlank() -> {
                            errorMessage = "Ingresá un nombre."
                        }

                        latitude == null || latitude !in -90.0..90.0 -> {
                            errorMessage = "Ingresá una latitud válida."
                        }

                        longitude == null || longitude !in -180.0..180.0 -> {
                            errorMessage = "Ingresá una longitud válida."
                        }

                        else -> {
                            onSave(
                                name.trim(),
                                address.trim().ifBlank { null },
                                latitude,
                                longitude
                            )
                        }
                    }
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