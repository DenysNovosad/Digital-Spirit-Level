package com.example.digitalspiritlevel.helpers

import kotlinx.coroutines.delay

class MockBleScanner {
    suspend fun scan(): List<MockBleDevice> {
        delay(2_000)

        return listOf(
            MockBleDevice(
                name = "Digital Spirit Level Hub",
                address = "00:11:22:33:44:55"
            )
        )
    }
}
