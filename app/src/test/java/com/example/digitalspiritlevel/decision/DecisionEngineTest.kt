package com.example.digitalspiritlevel.decision

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DecisionEngineTest {

    private val engine = DecisionEngine(criticalTiltDegrees = 5f)

    @Test
    fun tiltAtThresholdDoesNotActivateAlarm() {
        val decision = engine.evaluate(angleX = 5f, angleY = 1f)

        assertFalse(decision.shouldActivateAlarm)
    }

    @Test
    fun tiltAboveThresholdActivatesAlarm() {
        val decision = engine.evaluate(angleX = -5.1f, angleY = 0f)

        assertTrue(decision.shouldActivateAlarm)
    }
}
