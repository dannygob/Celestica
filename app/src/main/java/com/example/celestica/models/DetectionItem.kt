package com.example.celestica.models

import org.opencv.core.Point

data class DetectionItem(
    val type: String, // Tipo: "lamina", "agujero", "avellanado", etc.
    val position: Point? = null, // Posición del elemento en la imagen
    val width: Int? = null, // Anchura (solo aplicable a láminas)
    val height: Int? = null, // Altura (solo aplicable a láminas)
    val diameter: Int? = null // Diámetro (solo aplicable a agujeros)
)
