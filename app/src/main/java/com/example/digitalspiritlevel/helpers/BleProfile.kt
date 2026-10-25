package com.example.digitalspiritlevel.helpers

object BleProfile {
    const val SERVICE_UUID = "4f1a0000-6b7c-4d8e-9f10-112233445566"
    const val STATUS_CHARACTERISTIC_UUID = "4f1a0001-6b7c-4d8e-9f10-112233445566"
    const val COMMAND_CHARACTERISTIC_UUID = "4f1a0002-6b7c-4d8e-9f10-112233445566"
}

data class MockBleDevice(
    val name: String,
    val address: String
)

enum class BleConnectionState(val label: String) {
    DISCONNECTED("Disconnected"),
    CONNECTING("Connecting"),
    CONNECTED("Connected"),
    DATA_TRANSFER("Data Transfer")
}
