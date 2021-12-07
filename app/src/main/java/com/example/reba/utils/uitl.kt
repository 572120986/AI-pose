package com.example.reba.utils

import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.atan2

class uitl {

    fun getAngle(firstPoint: PoseLandmark, midPoint: PoseLandmark, lastPoint: PoseLandmark): Double {

        var result = Math.toDegrees(
            atan2( lastPoint.getPosition().y.toDouble() - midPoint.getPosition().y,
            lastPoint.getPosition().x.toDouble() - midPoint.getPosition().x)
                - atan2(firstPoint.getPosition().y - midPoint.getPosition().y,
            firstPoint.getPosition().x - midPoint.getPosition().x)
        )
        result = Math.abs(result) // Angle should never be negative
        if (result > 180) {
            result = 360.0 - result // Always get the acute representation of the angle
        }
        return result
    }

    fun getNeckAngle(
        orecchio: PoseLandmark, spalla: PoseLandmark
    ): Double {

        var result = Math.toDegrees(
            atan2( spalla.getPosition().y.toDouble() - spalla.getPosition().y,
            (spalla.getPosition().x + 100 ).toDouble() - spalla.getPosition().x)
                - atan2(orecchio.getPosition().y - spalla.getPosition().y,
            orecchio.getPosition().x - spalla.getPosition().x)
        )

        result = Math.abs(result) // Angle should never be negative

        if (result > 180) {
            result = 360.0 - result // Always get the acute representation of the angle
        }
        return result
    }



}