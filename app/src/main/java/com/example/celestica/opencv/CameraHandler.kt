package com.example.celestica.opencv

import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.view.View
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.JavaCameraView
import org.opencv.core.Mat

class CameraHandler(context: Context) : JavaCameraView(context, CAMERA_ID_BACK), CameraBridgeViewBase.CvCameraViewListener2 {

    private var frameProcessors: MutableList<(Mat) -> Unit> = mutableListOf()

    init {
        visibility = View.VISIBLE
        setCvCameraViewListener(this)
        enableFpsMeter = true // Mostrar el FPS
    }

    // Agrega un nuevo procesador de frames (soporte para múltiples detectores)
    fun addFrameProcessor(processor: (Mat) -> Unit) {
        frameProcessors.add(processor)
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        // Configuración inicial: aquí podríamos ajustar la resolución
    }

    override fun onCameraViewStopped() {
        // Limpieza de memoria si es necesario
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        val rgba = inputFrame?.rgba() ?: Mat()

        // Procesamiento asíncrono para mejorar rendimiento
        GlobalScope.launch(Dispatchers.Default) {
            frameProcessors.forEach { it.invoke(rgba) }
        }

        return rgba
    }

    // Método para alternar entre cámaras si el dispositivo tiene múltiples opciones
    fun switchCamera(cameraId: Int) {
        disableView()
        setCameraIndex(cameraId)
        enableView()
    }

    // Método para verificar permisos antes de activar la cámara
    fun hasCameraPermission(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
}

