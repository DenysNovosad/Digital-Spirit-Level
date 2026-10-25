package com.example.digitalspiritlevel.level

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitalspiritlevel.DigitalSpiritLevelApplication
import com.example.digitalspiritlevel.helpers.BleConnectionState
import com.example.digitalspiritlevel.helpers.IBleConnector
import com.example.digitalspiritlevel.helpers.MockBleConnector
import com.example.digitalspiritlevel.helpers.MockBleScanner
import com.example.digitalspiritlevel.helpers.SensorReader
import com.example.digitalspiritlevel.repository.SensorData
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LevelViewModel(app: Application) : AndroidViewModel(app) {

    private val sensorReader = SensorReader(app.applicationContext)
    private val scanner = MockBleScanner()
    private val connector: IBleConnector = MockBleConnector()
    private val sensorRepository =
        (app as DigitalSpiritLevelApplication).sensorRepository

    private val _state = MutableStateFlow(LevelState())
    val state: StateFlow<LevelState> = _state.asStateFlow()

    private val _events = Channel<LevelEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var sensorJob: Job? = null
    private var cloudSyncJob: Job? = null

    init {
        viewModelScope.launch {
            connector.connectionState.collect { connectionState ->
                _state.update { it.copy(connectionState = connectionState) }
            }
        }

        viewModelScope.launch {
            connector.messages.collect(::appendTerminalMessage)
        }
    }

    fun onAction(action: LevelAction) {
        when (action) {
            LevelAction.Start -> startSensor()
            LevelAction.Stop -> stopSensor()
            LevelAction.SearchDevices -> searchDevices()
            LevelAction.Connect -> connect()
            LevelAction.Disconnect -> connector.disconnect()
            LevelAction.SendLevelData -> sendLevelData()
        }
    }

    private fun startSensor() {
        if (sensorJob != null) return

        _state.update { it.copy(isSensorRunning = true) }
        sensorJob = viewModelScope.launch {
            sensorReader.readings()
                .catch { error ->
                    _state.update { it.copy(isSensorRunning = false) }
                    stopCloudSync()
                    _events.send(
                        LevelEvent.ShowMessage(
                            error.message ?: "Не вдалося зчитати акселерометр"
                        )
                    )
                }
                .onCompletion {
                    sensorJob = null
                }
                .collect(::updateSensorReading)
        }
        startCloudSync()
    }

    private fun stopSensor() {
        sensorJob?.cancel()
        sensorJob = null
        stopCloudSync()
        _state.update {
            it.copy(
                isSensorRunning = false,
                syncStatus = if (it.syncStatus == SyncStatus.SYNCING) {
                    SyncStatus.IDLE
                } else {
                    it.syncStatus
                }
            )
        }
    }

    private fun startCloudSync() {
        if (cloudSyncJob != null) return

        cloudSyncJob = viewModelScope.launch {
            while (isActive) {
                delay(CLOUD_SYNC_INTERVAL_MS)
                syncCurrentSensorData()
            }
        }
    }

    private fun stopCloudSync() {
        cloudSyncJob?.cancel()
        cloudSyncJob = null
    }

    private suspend fun syncCurrentSensorData() {
        val currentState = _state.value
        if (!currentState.isSensorRunning) return

        val wasPending = currentState.syncStatus == SyncStatus.PENDING
        _state.update { it.copy(syncStatus = SyncStatus.SYNCING) }

        val accelerationMagnitude = sqrt(
            (
                currentState.x * currentState.x +
                    currentState.y * currentState.y +
                    currentState.z * currentState.z
                ).toDouble()
        ).toFloat()

        val data = SensorData(
            deviceId = "digital-spirit-level-${Build.MODEL}",
            timestamp = System.currentTimeMillis(),
            value = accelerationMagnitude,
            type = "accelerometer",
            x = currentState.x,
            y = currentState.y,
            z = currentState.z
        )

        sensorRepository.save(data)
            .onSuccess {
                _state.update { it.copy(syncStatus = SyncStatus.SYNCED) }
                Log.d(TAG, "Cloud synchronization completed")
            }
            .onFailure { error ->
                _state.update { it.copy(syncStatus = SyncStatus.PENDING) }
                Log.w(TAG, "Cloud synchronization pending", error)
                if (!wasPending) {
                    _events.send(
                        LevelEvent.ShowMessage(
                            error.message ?: "Не вдалося синхронізувати дані"
                        )
                    )
                }
            }
    }

    private fun updateSensorReading(values: FloatArray) {
        val x = values[0]
        val y = values[1]
        val z = values[2]

        val angleX = Math.toDegrees(
            atan2(x.toDouble(), sqrt((y * y + z * z).toDouble()))
        ).toFloat()
        val angleY = Math.toDegrees(
            atan2(y.toDouble(), sqrt((x * x + z * z).toDouble()))
        ).toFloat()

        val maxAngle = maxOf(abs(angleX), abs(angleY))
        val levelStatus = when {
            maxAngle <= 2f -> LevelStatus.LEVEL
            maxAngle <= 5f -> LevelStatus.SLIGHT_TILT
            else -> LevelStatus.SIGNIFICANT_TILT
        }

        _state.update {
            it.copy(
                x = x,
                y = y,
                z = z,
                angleX = angleX,
                angleY = angleY,
                levelStatus = levelStatus
            )
        }
    }

    private fun searchDevices() {
        if (_state.value.isScanning) return

        viewModelScope.launch {
            _state.update { it.copy(isScanning = true, devices = emptyList()) }
            log("Scanning started")

            try {
                val devices = scanner.scan()
                _state.update { it.copy(devices = devices) }
                log("Device found: ${devices.first().name}")
                _events.send(LevelEvent.ShowMessage("BLE-пристрій знайдено"))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                log("Scanning failed: ${error.message}")
                _events.send(LevelEvent.ShowMessage("Помилка пошуку пристроїв"))
            } finally {
                _state.update { it.copy(isScanning = false) }
            }
        }
    }

    private fun connect() {
        viewModelScope.launch {
            connector.connect()
        }
    }

    private fun sendLevelData() {
        if (_state.value.connectionState != BleConnectionState.CONNECTED) {
            viewModelScope.launch {
                _events.send(LevelEvent.ShowMessage("Спочатку підключіть пристрій"))
            }
            return
        }

        val currentState = _state.value
        val packet = String.format(
            Locale.US,
            "{\"angleX\":%.2f,\"angleY\":%.2f,\"status\":\"%s\"}",
            currentState.angleX,
            currentState.angleY,
            currentState.levelStatus.label
        )

        viewModelScope.launch {
            connector.sendData(packet)
        }
    }

    private fun appendTerminalMessage(message: String) {
        _state.update {
            it.copy(terminalMessages = it.terminalMessages + message)
        }
    }

    private fun log(message: String) {
        Log.d(TAG, message)
        appendTerminalMessage(message)
    }

    override fun onCleared() {
        connector.disconnect()
        super.onCleared()
    }

    private companion object {
        const val TAG = "LevelViewModel"
        const val CLOUD_SYNC_INTERVAL_MS = 5_000L
    }
}
