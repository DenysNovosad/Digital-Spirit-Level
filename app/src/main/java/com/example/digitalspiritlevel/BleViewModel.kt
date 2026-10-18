package com.example.digitalspiritlevel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BleViewModel : ViewModel() {

    private val scanner = MockBleScanner()
    private val connector: IBleConnector =
        MockBleConnector(viewModelScope, ::appendMessage)

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _devices = MutableStateFlow<List<MockBleDevice>>(emptyList())
    val devices: StateFlow<List<MockBleDevice>> = _devices.asStateFlow()

    private val _terminalMessages = MutableStateFlow<List<String>>(emptyList())
    val terminalMessages: StateFlow<List<String>> =
        _terminalMessages.asStateFlow()

    val connectionState: StateFlow<BleConnectionState> =
        connector.connectionState

    fun searchDevices() {
        if (_isScanning.value) return

        viewModelScope.launch {
            _devices.value = emptyList()
            _isScanning.value = true
            log("Scanning started")

            _devices.value = scanner.scan()
            _isScanning.value = false
            log("Device found: ${_devices.value.first().name}")
        }
    }

    fun connect() = connector.connect()

    fun disconnect() = connector.disconnect()

    fun sendLevelData(angleX: Float, angleY: Float, status: String) {
        val packet = String.format(
            Locale.US,
            "{\"angleX\":%.2f,\"angleY\":%.2f,\"status\":\"%s\"}",
            angleX,
            angleY,
            status
        )
        connector.sendData(packet)
    }

    private fun appendMessage(message: String) {
        _terminalMessages.value = _terminalMessages.value + message
    }

    private fun log(message: String) {
        Log.d(TAG, message)
        appendMessage(message)
    }

    private companion object {
        const val TAG = "BleViewModel"
    }
}
