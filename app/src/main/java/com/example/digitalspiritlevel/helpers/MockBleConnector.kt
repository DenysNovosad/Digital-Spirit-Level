package com.example.digitalspiritlevel.helpers

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class MockBleConnector : IBleConnector {

    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<BleConnectionState> =
        _connectionState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    override val messages: Flow<String> = _messages.asSharedFlow()

    override suspend fun connect() {
        if (_connectionState.value != BleConnectionState.DISCONNECTED) return

        _connectionState.value = BleConnectionState.CONNECTING
        log("Connecting to Digital Spirit Level Hub")
        delay(700)

        if (_connectionState.value != BleConnectionState.CONNECTING) return

        _connectionState.value = BleConnectionState.CONNECTED
        log("Connected")
        log("READ status: READY")
    }

    override fun disconnect() {
        if (_connectionState.value == BleConnectionState.DISCONNECTED) return

        _connectionState.value = BleConnectionState.DISCONNECTED
        log("Disconnected")
    }

    override suspend fun sendData(data: String) {
        if (_connectionState.value != BleConnectionState.CONNECTED) return

        _connectionState.value = BleConnectionState.DATA_TRANSFER
        log("WRITE ${BleProfile.COMMAND_CHARACTERISTIC_UUID}: $data")
        delay(350)

        if (_connectionState.value != BleConnectionState.DATA_TRANSFER) return

        log("NOTIFY ${BleProfile.STATUS_CHARACTERISTIC_UUID}: ACK")
        _connectionState.value = BleConnectionState.CONNECTED
    }

    private fun log(message: String) {
        Log.d(TAG, message)
        _messages.tryEmit(message)
    }

    private companion object {
        const val TAG = "MockBleConnector"
    }
}
