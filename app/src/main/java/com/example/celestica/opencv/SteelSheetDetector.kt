package com.example.celestica.opencv

import org.opencv.core.*
import org.opencv.imgproc.Imgproc

object SteelSheetDetector {

    fun detectSteelSheet(frame: Mat): DetectionItem? {
        val gray = Mat()
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.bilateralFilter(gray, gray, 9, 75.0, 75.0)

        val edges = Mat()
        Imgproc.Canny(gray, edges, 50.0, 150.0)

        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(edges, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        var steelSheetRect: Rect? = null
        for (contour in contours) {
            val epsilon = 0.04 * Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true)
            val approx = MatOfPoint()
            Imgproc.approxPolyDP(MatOfPoint2f(*contour.toArray()), MatOfPoint2f(), epsilon, true).toArray().let {
                approx.fromArray(*it)
            }

            if (approx.total() == 4L) { // Identificación de posibles láminas rectangulares
                val rect = Imgproc.boundingRect(approx)
                if (steelSheetRect == null || rect.area() > steelSheetRect!!.area()) {
                    steelSheetRect = rect
                }
            }
        }

        return steelSheetRect?.let {
            Imgproc.rectangle(frame, it.tl(), it.br(), Scalar(255.0, 0.0, 0.0), 3)
            DetectionItem(type = "lamina", width = it.width, height = it.height)
        }
    }
}

