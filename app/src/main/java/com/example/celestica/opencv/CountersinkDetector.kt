package com.example.celestica.opencv

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import com.example.celestica.models.DetectionItem

object CountersinkDetector {

    fun classifyHoles(frame: Mat, holes: List<DetectionItem>): List<DetectionItem> {
        val classifiedHoles = mutableListOf<DetectionItem>()

        for (hole in holes) {
            hole.position?.let { position ->
                val radius = hole.diameter?.div(2) ?: 0

                // Determinar el tipo de agujero basado en color y bordes
                val holeType = determineHoleType(position, radius, frame)

                // Clasificar el agujero y agregarlo a la lista
                classifiedHoles.add(
                    hole.copy(type = holeType)
                )

                // Dibujar el agujero clasificado con su color correspondiente
                val color = when (holeType) {
                    "anodizado" -> Scalar(0.0, 0.0, 255.0) // Rojo
                    "avellanado" -> Scalar(0.0, 255.0, 0.0) // Verde
                    else -> Scalar(0.0, 0.0, 0.0) // Negro
                }
                Imgproc.circle(frame, position, radius, color, 2)
                Imgproc.putText(
                    frame, "$holeType", Point(position.x + 10, position.y + 10),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.7, color, 2
                )
            }
        }

        return classifiedHoles
    }

    private fun determineHoleType(center: Point, radius: Int, frame: Mat): String {
        val color = getColorAtPoint(center, frame)

        return when {
            isAnodized(color) -> "anodizado"
            isCountersink(center, radius, frame) -> "avellanado"
            else -> "normal"
        }
    }

    private fun getColorAtPoint(center: Point, frame: Mat): Scalar {
        val pixelColor = frame.get(center.y.toInt(), center.x.toInt())
        return Scalar(pixelColor[0], pixelColor[1], pixelColor[2]) // BGR
    }

    private fun isAnodized(color: Scalar): Boolean {
        val (b, g, r) = color.`val`
        return b > 100 && g > 100 && r > 100 &&
                Math.abs(b - g) < 15 && Math.abs(g - r) < 15 && Math.abs(b - r) < 15
    }

    private fun isCountersink(center: Point, radius: Int, frame: Mat): Boolean {
        val safeX = Math.max((center.x - radius).toInt(), 0)
        val safeY = Math.max((center.y - radius).toInt(), 0)
        val width = if (safeX + radius * 2 < frame.cols()) radius * 2 else frame.cols() - safeX
        val height = if (safeY + radius * 2 < frame.rows()) radius * 2 else frame.rows() - safeY

        val region = frame.submat(Rect(safeX, safeY, width, height))
        val gray = Mat()
        Imgproc.cvtColor(region, gray, Imgproc.COLOR_BGR2GRAY)

        val edges = Mat()
        Imgproc.Canny(gray, edges, 100.0, 200.0)

        val nonZeroEdges = Core.countNonZero(edges)
        return nonZeroEdges > 2000 // Umbral ajustable
    }
}

