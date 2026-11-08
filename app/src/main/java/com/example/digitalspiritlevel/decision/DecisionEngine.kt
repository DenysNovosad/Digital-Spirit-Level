package com.example.digitalspiritlevel.decision

import kotlin.math.abs

data class ActuatorDecision(
    val shouldActivateAlarm: Boolean,
    val maxTiltDegrees: Float
)

class DecisionEngine(
    private val criticalTiltDegrees: Float = DEFAULT_CRITICAL_TILT_DEGREES
) {

    init {
        require(criticalTiltDegrees > 0f) {
            "Порогове значення має бути більшим за нуль"
        }
    }

    fun evaluate(angleX: Float, angleY: Float): ActuatorDecision {
        val maxTilt = maxOf(abs(angleX), abs(angleY))
        return ActuatorDecision(
            shouldActivateAlarm = maxTilt > criticalTiltDegrees,
            maxTiltDegrees = maxTilt
        )
    }

    companion object {
        const val DEFAULT_CRITICAL_TILT_DEGREES = 5f
    }
}
