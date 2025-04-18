package com.example.celestica.ui.screen


import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import org.opencv.android.JavaCameraView

@Composable
fun CameraView(navController: NavController) {
    // Implementación de AndroidView para incorporar JavaCameraView
    AndroidView(
        factory = { context ->
            JavaCameraView(context).apply {
                visibility = View.VISIBLE
                // Configuración adicional del JavaCameraView puede ir aquí
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
