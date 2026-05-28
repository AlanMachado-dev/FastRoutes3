package com.example.fastroutes.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@SuppressLint("UnrememberedMutableState")
@Composable
fun TestMapScreen() {
    val montevideo = LatLng(-34.9011, -56.1645)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(montevideo, 12f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        Marker(
            state = MarkerState(position = montevideo),
            title = "Montevideo",
            snippet = "Punto de prueba"
        )
    }
}