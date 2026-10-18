package com.example.digitalspiritlevel

import kotlinx.coroutines.flow.StateFlow

interface IBleConnector {
    val connectionState: StateFlow<BleConnectionState>

    fun connect()

    fun disconnect()

    fun sendData(data: String)
}
