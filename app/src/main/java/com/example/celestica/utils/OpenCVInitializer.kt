package com.example.celestica.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import org.opencv.android.OpenCVLoader

object OpenCVInitializer {

    fun initOpenCV(context: Context): Boolean {
        return if (!OpenCVLoader.initDebug()) {
            // Error al inicializar OpenCV
            Log.e("OpenCV", "Error al inicializar OpenCV.")
            Toast.makeText(context, "Error al inicializar OpenCV", Toast.LENGTH_SHORT).show()
            false
        } else {
            // OpenCV inicializado correctamente
            Log.d("OpenCV", "OpenCV inicializado correctamente.")
            Toast.makeText(context, "OpenCV inicializado correctamente", Toast.LENGTH_SHORT).show()
            true
        }
    }
}

