package com.example.digitalspiritlevel

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MockBleConnector(
    private val scope: CoroutineScope,
    private val onMessage: (String) -> Unit
) : IBleConnector {

    private val _connectionState =
        MutableStateFlow(BleConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<BleConnectionState> =
        _connectionState.asStateFlow()

    private var activeJob: Job? = null

    override fun connect() {
        if (_connectionState.value != BleConnectionState.DISCONNECTED) return

        activeJob = scope.launch {
            _connectionState.value = BleConnectionState.CONNECTING
            log("Connecting to Digital Spirit Level Hub")
            delay(700)

            _connectionState.value = BleConnectionState.CONNECTED
            log("Connected")
            log("READ status: READY")
        }
    }

    override fun disconnect() {
        if (_connectionState.value == BleConnectionState.DISCONNECTED) return

        activeJob?.cancel()
        _connectionState.value = BleConnectionState.DISCONNECTED
        log("Disconnected")
    }

    override fun sendData(data: String) {
        if (_connectionState.value != BleConnectionState.CONNECTED) return

        activeJob = scope.launch {
            _connectionState.value = BleConnectionState.DATA_TRANSFER
            log("WRITE ${BleProfile.COMMAND_CHARACTERISTIC_UUID}: $data")
            delay(350)
            log("NOTIFY ${BleProfile.STATUS_CHARACTERISTIC_UUID}: ACK")
            _connectionState.value = BleConnectionState.CONNECTED
        }
    }

    private fun log(message: String) {
        Log.d(TAG, message)
        onMessage(message)
    }

    private companion object {
        const val TAG = "MockBleConnector"
    }
}
