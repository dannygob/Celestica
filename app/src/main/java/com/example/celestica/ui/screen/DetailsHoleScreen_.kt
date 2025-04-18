package com.example.celestica.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.celestica.models.DetectionItem

@Composable
fun DetailsHoleScreen(
    index: Int, // Índice del agujero seleccionado
    detectionItems: List<DetectionItem>, // Lista de elementos detectados
    navController: NavController
) {
    // Obtener el agujero específico basado en el índice
    val detectionItem = detectionItems.getOrNull(index)

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (detectionItem != null) {
                // Mostrar detalles del agujero
                Text(
                    text = "Details of Hole #${index + 1}",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                BasicText("Type: ${detectionItem.type}")
                BasicText("Position: ${detectionItem.position?.toString() ?: "Unknown"}")
                BasicText("Diameter: ${detectionItem.diameter ?: "Unknown"} px")
                BasicText("Width: ${detectionItem.width ?: "N/A"} px")
                BasicText("Height: ${detectionItem.height ?: "N/A"} px")
            } else {
                // Si no se encuentra el elemento
                Text("Hole details not found.", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { navController.navigate("camera") }) {
                Text("Back to Camera")
            }
        }
    }
}