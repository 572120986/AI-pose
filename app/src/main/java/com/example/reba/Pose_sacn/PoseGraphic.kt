package github.hongbeomi.macgyver.mlkit.vision.barcode_scan

import android.graphics.*
import com.example.reba.GraphicOverlay

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark



class PoseGraphic(
    overlay: GraphicOverlay,
    private val pose: Pose,
    private val imageRect: Rect
) : GraphicOverlay.Graphic(overlay) {

    private var rectPaint = Paint().apply {
        color = TEXT_COLOR
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH
    }



    override fun draw(canvas: Canvas?) {

        val rect = calculateRect(
            imageRect.height().toFloat(),
            imageRect.width().toFloat(),
            imageRect

        )
        canvas?.drawRect(rect, rectPaint)
        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
        val lefyEyeInner = pose.getPoseLandmark(PoseLandmark.LEFT_EYE_INNER)
        val lefyEye = pose.getPoseLandmark(PoseLandmark.LEFT_EYE)
        val leftEyeOuter = pose.getPoseLandmark(PoseLandmark.LEFT_EYE_OUTER)
        val rightEyeInner = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE_INNER)
        val rightEye = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE)
        val rightEyeOuter = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE_OUTER)
        val leftEar = pose.getPoseLandmark(PoseLandmark.LEFT_EAR)
        val rightEar = pose.getPoseLandmark(PoseLandmark.RIGHT_EAR)
        val leftMouth = pose.getPoseLandmark(PoseLandmark.LEFT_MOUTH)
        val rightMouth = pose.getPoseLandmark(PoseLandmark.RIGHT_MOUTH)

        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        val leftPinky = pose.getPoseLandmark(PoseLandmark.LEFT_PINKY)
        val rightPinky = pose.getPoseLandmark(PoseLandmark.RIGHT_PINKY)
        val leftIndex = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX)
        val rightIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX)
        val leftThumb = pose.getPoseLandmark(PoseLandmark.LEFT_THUMB)
        val rightThumb = pose.getPoseLandmark(PoseLandmark.RIGHT_THUMB)
        val leftHeel = pose.getPoseLandmark(PoseLandmark.LEFT_HEEL)
        val rightHeel = pose.getPoseLandmark(PoseLandmark.RIGHT_HEEL)
        val leftFootIndex = pose.getPoseLandmark(PoseLandmark.LEFT_FOOT_INDEX)
        val rightFootIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_FOOT_INDEX)



//        if (pose.allPoseLandmarks.isNotEmpty()) {
//            if (canvas != null) {
//                drawLine(canvas, nose, lefyEyeInner, rectPaint)
//                drawLine(canvas, lefyEyeInner, lefyEye, rectPaint)
//                drawLine(canvas, lefyEye, leftEyeOuter, rectPaint)
//                drawLine(canvas, leftEyeOuter, leftEar, rectPaint)
//                drawLine(canvas, nose, rightEyeInner, rectPaint)
//                drawLine(canvas, rightEyeInner, rightEye, rectPaint)
//                drawLine(canvas, rightEye, rightEyeOuter, rectPaint)
//                drawLine(canvas, rightEyeOuter, rightEar, rectPaint)
//                drawLine(canvas, leftMouth, rightMouth, rectPaint)
//
//                drawLine(canvas, leftShoulder, rightShoulder, rectPaint)
//                drawLine(canvas, leftHip, rightHip, rectPaint)
//
//                // Left body
//                drawLine(canvas, leftShoulder, leftElbow, rectPaint)
//                drawLine(canvas, leftElbow, leftWrist, rectPaint)
//                drawLine(canvas, leftShoulder, leftHip, rectPaint)
//                drawLine(canvas, leftHip, leftKnee, rectPaint)
//                drawLine(canvas, leftKnee, leftAnkle, rectPaint)
//                drawLine(canvas, leftWrist, leftThumb, rectPaint)
//                drawLine(canvas, leftWrist, leftPinky, rectPaint)
//                drawLine(canvas, leftWrist, leftIndex, rectPaint)
//                drawLine(canvas, leftIndex, leftPinky, rectPaint)
//                drawLine(canvas, leftAnkle, leftHeel, rectPaint)
//                drawLine(canvas, leftHeel, leftFootIndex, rectPaint)
//
//                // Right body
//                drawLine(canvas, rightShoulder, rightElbow, rectPaint)
//                drawLine(canvas, rightElbow, rightWrist, rectPaint)
//                drawLine(canvas, rightShoulder, rightHip, rectPaint)
//                drawLine(canvas, rightHip, rightKnee, rectPaint)
//                drawLine(canvas, rightKnee, rightAnkle, rectPaint)
//                drawLine(canvas, rightWrist, rightThumb, rectPaint)
//                drawLine(canvas, rightWrist, rightPinky, rectPaint)
//                drawLine(canvas, rightWrist, rightIndex, rectPaint)
//                drawLine(canvas, rightIndex, rightPinky, rectPaint)
//                drawLine(canvas, rightAnkle, rightHeel, rectPaint)
//                drawLine(canvas, rightHeel, rightFootIndex, rectPaint)
//            }
//        }


    }


    internal fun drawLine(
        canvas: Canvas,
        startLandmark: PoseLandmark?,
        endLandmark: PoseLandmark?,
        paint: Paint
    ) {
        val start = startLandmark!!.position3D
        val end = endLandmark!!.position3D

        // Gets average z for the current body line
        val avgZInImagePixel = (start.z + end.z) / 2
//        maybeUpdatePaintColor(paint, canvas, avgZInImagePixel)

        canvas.drawLine(
            translateX(start.x),
            translateY(start.y),
            translateX(end.x),
            translateY(end.y),
            paint
        )
    }



    companion object {
        private const val TEXT_COLOR = Color.WHITE
        private const val TEXT_SIZE = 54.0f
        private const val STROKE_WIDTH = 4.0f
        private const val ROUND_RECT_CORNER = 8f
    }




}