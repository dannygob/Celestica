package com.example.celestica

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.celestica.models.DetectionItem
import com.example.celestica.ui.DetailsHoleScreen
import com.example.celestica.ui.screen.CameraView
import com.example.celestica.utils.OpenCVInitializer

class MainActivity : ComponentActivity() {

    private val detectionItems = mutableListOf<DetectionItem>() // Almacena elementos detectados

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()

            // Definición de las rutas de la aplicación
            NavHost(navController = navController, startDestination = "camera") {
                composable("camera") { CameraView(navController = navController) }
                composable("detailsHole/{index}") { backStackEntry ->
                    val index = backStackEntry.arguments?.getString("index")?.toIntOrNull() ?: 0
                    DetailsHoleScreen(
                        index = index,
                        detectionItems = detectionItems,
                        navController = navController
                    )
                }
            }
        }

        // Verificar y cargar OpenCV
        if (!OpenCVInitializer.initOpenCV(this)) {
            Log.e("OpenCV", "Error initializing OpenCV.")
            Toast.makeText(this, "Error initializing OpenCV", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("OpenCV", "OpenCV initialized successfully.")
        }
    }
}