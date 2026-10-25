package com.example.digitalspiritlevel.helpers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SensorReader(context: Context) {

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    fun readings(): Flow<FloatArray> = callbackFlow {
        val sensor = accelerometer
        if (sensor == null) {
            close(IllegalStateException("На пристрої немає акселерометра"))
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(event.values.clone())
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        val registered = sensorManager.registerListener(
            listener,
            sensor,
            SensorManager.SENSOR_DELAY_UI
        )

        if (!registered) {
            close(IllegalStateException("Не вдалося запустити акселерометр"))
            return@callbackFlow
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
