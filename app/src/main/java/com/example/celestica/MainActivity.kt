package com.example.celestica


import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.JavaCameraView
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

class MainActivity : ComponentActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private lateinit var mOpenCvCameraView: JavaCameraView
    private val detectionItems = mutableListOf<DetectionItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 1.0.0 Crea un NavController
            val navController = rememberNavController()

            // 1.1.0 Define el NavHost con rutas
            NavHost(navController = navController, startDestination = "camera") {
                // 1.1.1 Pantalla principal (CameraView)
                composable("camera") {
                    CameraView(navController = navController) // Pasa el navController aquí
                }

                // 1.1.2 Pantalla de detalle del agujero
                composable("detalleAgujero/{index}") { backStackEntry ->
                    // 1.1.3 Recuperar el índice pasado como parámetro
                    val index = backStackEntry.arguments?.getString("index")?.toIntOrNull() ?: 0
                    DetalleAgujeroScreen(index = index, detectionItems = detectionItems)
                }
            }
        }

        // 1.2.0 Inicialización de OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Error al inicializar OpenCV.")
            Toast.makeText(this, "Error al inicializar OpenCV", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("OpenCV", "OpenCV inicializado correctamente.")
        }
    }

    // 2.0.0 Modelo de datos para lámina, agujeros y avellanados
    data class DetectionItem(
        val type: String, // "lamina", "agujero", "avellanado"
        val position: Point? = null,
        val width: Int? = null,
        val height: Int? = null,
        val diameter: Int? = null
    )

    @Composable
    fun CameraView(navController: NavController) {
        AndroidView(
            factory = { context ->
                JavaCameraView(context).apply {
                    setCvCameraViewListener(this@MainActivity)
                    visibility = View.VISIBLE
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        inputFrame?.let {
            val rgba = inputFrame.rgba()
            detectionItems.clear() // Limpiar detecciones por cada frame
            detectSteelSheet(rgba)
            return rgba
        }
        return Mat()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {}
    override fun onCameraViewStopped() {}

    // 3.0.0 Detección de la lámina
    private fun detectSteelSheet(frame: Mat) {
        val gray = Mat()
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.bilateralFilter(gray, gray, 9, 75.0, 75.0)

        val edges = Mat()
        Imgproc.Canny(gray, edges, 50.0, 150.0)

        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(
            edges,
            contours,
            Mat(),
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        var steelSheetRect: Rect? = null
        for (contour in contours) {
            val epsilon = 0.04 * Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true)
            val approx = MatOfPoint()
            Imgproc.approxPolyDP(
                MatOfPoint2f(*contour.toArray()),
                MatOfPoint2f(),
                epsilon,
                true
            ).toArray().let {
                approx.fromArray(*it)
            }

            if (approx.total() == 4L) {
                val rect = Imgproc.boundingRect(approx)
                if (steelSheetRect == null || rect.area() > steelSheetRect!!.area()) {
                    steelSheetRect = rect
                }
            }
        }

        steelSheetRect?.let {
            Imgproc.rectangle(frame, it.tl(), it.br(), Scalar(255.0, 0.0, 0.0), 3)
            val width = it.width
            val height = it.height

            Imgproc.putText(
                frame, "Lámina: $width x $height px",
                Point(it.x + 10, it.y + 10),
                Imgproc.FONT_HERSHEY_SIMPLEX, 1.0,
                Scalar(0.0, 255.0, 0.0), 2
            )

            detectionItems.add(
                DetectionItem(type = "lamina", width = width, height = height)
            )

            Log.d("Lámina", "Ancho: $width px, Alto: $height px")
        }

        detectHoles(frame, gray)
    }

    // 4.0.0 Detección de agujeros
    private fun detectHoles(frame: Mat, gray: Mat) {
        val circles = Mat()

        Imgproc.HoughCircles(
            gray, circles, Imgproc.CV_HOUGH_GRADIENT, 1.0, gray.rows() / 4.0,
            100.0, 30.0, 10, 50
        )

        if (circles.cols() > 0) {
            val detectedCircles =
                mutableListOf<Triple<Point, Int, Int>>() // centro, radio, index

            for (i in 0 until circles.cols()) {
                val data = circles.get(0, i)
                val center = Point(data[0], data[1])
                val radius = data[2].toInt()

                detectedCircles.add(Triple(center, radius, i))

                // 4.1.0 Dibujar círculo principal
                Imgproc.circle(frame, center, radius, Scalar(0.0, 255.0, 0.0), 2)
                Imgproc.circle(frame, center, 3, Scalar(0.0, 0.0, 255.0), 2)

                val diameter = radius * 2
                detectionItems.add(
                    DetectionItem(type = "agujero", position = center, diameter = diameter)
                )

                Imgproc.putText(
                    frame, "Ø $diameter px",
                    Point(center.x + 10, center.y + 10),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.7,
                    Scalar(0.0, 255.0, 0.0), 2
                )
            }

            // 4.2.0 Detectar avellanado y anodizado
            detectCounters(detectedCircles, frame)
        }
    }

    // 5.0.0 Detectar avellanado y anodizado
    private fun detectCounters(circles: List<Triple<Point, Int, Int>>, frame: Mat) {
        // Lógica de detección de avellanado y anodizado
        for ((i, triple) in circles.withIndex()) {
            val (center, radius, _) = triple

            // 5.1.0 Determinar el tipo de agujero (anodizado, avellanado o normal)
            val holeTypeColor = determineHoleType(center, radius, frame)

            // 5.2.0 Usar el color determinado para el agujero (negro, verde, rojo)
            Imgproc.circle(frame, center, radius, holeTypeColor, 2)

            // 5.3.0 Etiquetar el agujero con su tipo y número
            val holeLabel = when (holeTypeColor) {
                Scalar(0.0, 0.0, 255.0) -> "Z" // Anodizado
                Scalar(0.0, 255.0, 0.0) -> "A" // Avellanado
                else -> "H" // Agujero normal
            }

            Imgproc.putText(
                frame, "$holeLabel${i + 1}", // Etiqueta alfa numérica
                Point(center.x + 10, center.y + 10),
                Imgproc.FONT_HERSHEY_SIMPLEX, 0.7,
                holeTypeColor, 2
            )
        }
    }

    // 5.4.0 Determinar el tipo del agujero usando color y bordes
    private fun determineHoleType(center: Point, radius: Int, frame: Mat): Scalar {
        val colorSample = getColorAtPoint(center, frame) // 5.4.1 Obtener el color del centro del agujero

        return when {
            isAnodized(colorSample) -> Scalar(0.0, 0.0, 255.0) // 5.4.2 Si es anodizado (gris claro o blanco), usar rojo
            isCountersink(center, radius, frame) -> Scalar(0.0, 255.0, 0.0) // 5.4.3 Si tiene bordes suaves, es avellanado (verde)
            else -> Scalar(0.0, 0.0, 0.0) // 5.4.4 Agujero normal (negro)
        }
    }

    // 5.4.1 Obtener el color en el centro del agujero
    private fun getColorAtPoint(center: Point, frame: Mat): Scalar {
        val pixelColor = frame.get(center.y.toInt(), center.x.toInt())
        return Scalar(pixelColor[0], pixelColor[1], pixelColor[2]) // Devuelve color en formato BGR
    }

    // 5.4.2 Verificar si el color representa un anodizado (gris claro o blanco)
    private fun isAnodized(color: Scalar): Boolean {
        // Umbral simple para tonos claros (puedes afinar estos valores)
        val b = color.`val`[0]
        val g = color.`val`[1]
        val r = color.`val`[2]

        return b > 100 && g > 100 && r > 100 &&
                Math.abs(b - g) < 15 && Math.abs(g - r) < 15 && Math.abs(b - r) < 15 // tonalidad grisácea
    }

    // 5.4.3 Verificar si el borde representa un avellanado
    private fun isCountersink(center: Point, radius: Int, frame: Mat): Boolean {
        // Define una región alrededor del agujero
        val safeX = Math.max((center.x - radius).toInt(), 0)
        val safeY = Math.max((center.y - radius).toInt(), 0)
        val width = if (safeX + radius * 2 < frame.cols()) radius * 2 else frame.cols() - safeX
        val height = if (safeY + radius * 2 < frame.rows()) radius * 2 else frame.rows() - safeY

        val region = frame.submat(Rect(safeX, safeY, width, height))

        // Aplicar detección de bordes a la región del agujero
        val gray = Mat()
        Imgproc.cvtColor(region, gray, Imgproc.COLOR_BGR2GRAY)

        val edges = Mat()
        Imgproc.Canny(gray, edges, 100.0, 200.0)

        // Contar cuántos bordes hay: si son muchos, puede indicar transición suave → avellanado
        val nonZeroEdges = Core.countNonZero(edges)

        return nonZeroEdges > 2000 // Umbral ajustable según pruebas
    }

