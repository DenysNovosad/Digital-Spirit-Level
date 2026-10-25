package com.example.digitalspiritlevel.helpers

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface IBleConnector {
    val connectionState: StateFlow<BleConnectionState>
    val messages: Flow<String>

    suspend fun connect()

    fun disconnect()

    suspend fun sendData(data: String)
}
