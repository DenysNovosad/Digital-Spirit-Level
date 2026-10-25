package com.example.digitalspiritlevel.repository

data class SensorData(
    val deviceId: String,
    val timestamp: Long,
    val value: Float,
    val type: String,
    val x: Float,
    val y: Float,
    val z: Float
)

data class SensorPayload(
    val deviceId: String,
    val sensorType: String,
    val value: Float,
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long
)

fun SensorData.toPayload() = SensorPayload(
    deviceId = deviceId,
    sensorType = type,
    value = value,
    x = x,
    y = y,
    z = z,
    timestamp = timestamp
)
