package com.example.digitalspiritlevel.level

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitalspiritlevel.DigitalSpiritLevelApplication
import com.example.digitalspiritlevel.decision.DecisionEngine
import com.example.digitalspiritlevel.helpers.ActuatorResult
import com.example.digitalspiritlevel.helpers.BleConnectionState
import com.example.digitalspiritlevel.helpers.IBleConnector
import com.example.digitalspiritlevel.helpers.MockBleConnector
import com.example.digitalspiritlevel.helpers.MockBleScanner
import com.example.digitalspiritlevel.helpers.SensorReader
import com.example.digitalspiritlevel.helpers.SmartActuator
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
import kotlin.time.Duration.Companion.milliseconds

class LevelViewModel(app: Application) : AndroidViewModel(app) {

    private val sensorReader = SensorReader(app.applicationContext)
    private val scanner = MockBleScanner()
    private val connector: IBleConnector = MockBleConnector()
    private val decisionEngine = DecisionEngine()
    private val smartActuator = SmartActuator(app.applicationContext)
    private val sensorRepository =
        (app as DigitalSpiritLevelApplication).sensorRepository
    private val remoteControlRepository =
        (app as DigitalSpiritLevelApplication).remoteControlRepository

    private val _state = MutableStateFlow(
        LevelState(
            hasFlashlight = smartActuator.hasFlashlight,
            hasVibrator = smartActuator.hasVibrator
        )
    )
    val state: StateFlow<LevelState> = _state.asStateFlow()

    private val _events = Channel<LevelEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var sensorJob: Job? = null
    private var cloudSyncJob: Job? = null
    private var remoteControlJob: Job? = null
    private var remoteCommandIndex = 0

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
            LevelAction.ToggleAutomaticControl -> toggleAutomaticControl()
            LevelAction.ToggleManualAlarm -> toggleManualAlarm()
            LevelAction.TriggerVibration -> triggerVibration()
            is LevelAction.SetFlashlight -> setFlashlight(action.enabled)
            is LevelAction.CameraPermissionResult -> {
                handleCameraPermissionResult(action)
            }
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
        startRemoteControlPolling()
    }

    private fun stopSensor() {
        sensorJob?.cancel()
        sensorJob = null
        stopCloudSync()
        stopRemoteControlPolling()
        smartActuator.stopAlarm()
        if (_state.value.isFlashlightOn) {
            smartActuator.toggleFlashlight(false)
        }
        _state.update {
            it.copy(
                isSensorRunning = false,
                isAlarmActive = false,
                isFlashlightOn = false,
                remoteControlStatus = RemoteControlStatus.WAITING,
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
                delay(CLOUD_SYNC_INTERVAL_MS.milliseconds)
                syncCurrentSensorData()
            }
        }
    }

    private fun stopCloudSync() {
        cloudSyncJob?.cancel()
        cloudSyncJob = null
    }

    private fun startRemoteControlPolling() {
        if (remoteControlJob != null) return

        remoteControlJob = viewModelScope.launch {
            while (isActive) {
                pollRemoteCommand()
                delay(REMOTE_CONTROL_POLL_INTERVAL_MS.milliseconds)
            }
        }
    }

    private fun stopRemoteControlPolling() {
        remoteControlJob?.cancel()
        remoteControlJob = null
    }

    private suspend fun pollRemoteCommand() {
        val commandId = REMOTE_COMMAND_IDS[remoteCommandIndex]
        _state.update {
            it.copy(remoteControlStatus = RemoteControlStatus.POLLING)
        }

        remoteControlRepository.fetchCommand(commandId)
            .onSuccess { command ->
                val previousValue = _state.value.isRemoteLampOn
                _state.update {
                    it.copy(
                        isRemoteLampOn = command.enabled,
                        remoteControlStatus = RemoteControlStatus.RECEIVED,
                        lastRemoteCommand = buildString {
                            append("Команда #${command.id}: ")
                            append(if (command.enabled) "ON" else "OFF")
                        }
                    )
                }
                Log.d(
                    TAG,
                    "Remote lamp command #${command.id}: ${command.enabled}"
                )
                if (previousValue != command.enabled) {
                    _events.send(
                        LevelEvent.ShowMessage(
                            if (command.enabled) {
                                "Хмарна команда ввімкнула лампу"
                            } else {
                                "Хмарна команда вимкнула лампу"
                            }
                        )
                    )
                }
                remoteCommandIndex =
                    (remoteCommandIndex + 1) % REMOTE_COMMAND_IDS.size
            }
            .onFailure { error ->
                _state.update {
                    it.copy(
                        remoteControlStatus = RemoteControlStatus.ERROR,
                        lastRemoteCommand = error.message
                            ?: "Не вдалося отримати команду"
                    )
                }
                Log.w(TAG, "Remote command polling failed", error)
            }
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

        applyAutomaticDecision(angleX, angleY)
    }

    private fun applyAutomaticDecision(angleX: Float, angleY: Float) {
        val currentState = _state.value
        if (!currentState.automaticControlEnabled) return

        val decision = decisionEngine.evaluate(angleX, angleY)
        if (decision.shouldActivateAlarm == currentState.isAlarmActive) return

        if (decision.shouldActivateAlarm) {
            val result = smartActuator.triggerAlarm()
            _state.update { it.copy(isAlarmActive = true) }
            Log.d(
                TAG,
                "Automatic alarm ON at ${decision.maxTiltDegrees} degrees"
            )
            notifyActuatorProblem(result)
        } else {
            smartActuator.stopAlarm()
            _state.update { it.copy(isAlarmActive = false) }
            Log.d(TAG, "Automatic alarm OFF")
        }
    }

    private fun toggleAutomaticControl() {
        val enable = !_state.value.automaticControlEnabled
        if (!enable) {
            smartActuator.stopAlarm()
            _state.update {
                it.copy(
                    automaticControlEnabled = false,
                    isAlarmActive = false
                )
            }
            return
        }

        _state.update { it.copy(automaticControlEnabled = true) }
        applyAutomaticDecision(_state.value.angleX, _state.value.angleY)
    }

    private fun toggleManualAlarm() {
        val enable = !_state.value.isAlarmActive
        _state.update {
            it.copy(
                automaticControlEnabled = false,
                isAlarmActive = enable
            )
        }

        if (enable) {
            val result = smartActuator.triggerAlarm()
            notifyActuatorProblem(result)
        } else {
            smartActuator.stopAlarm()
        }
    }

    private fun triggerVibration() {
        when (val result = smartActuator.triggerAlarm()) {
            ActuatorResult.Success -> sendMessage("Вібровідгук активовано")
            ActuatorResult.Unavailable -> {
                sendMessage("На цьому пристрої немає вібратора")
            }
            ActuatorResult.PermissionRequired -> Unit
            is ActuatorResult.Failure -> sendMessage(result.message)
        }
    }

    private fun setFlashlight(enabled: Boolean) {
        when (val result = smartActuator.toggleFlashlight(enabled)) {
            ActuatorResult.Success -> {
                _state.update { it.copy(isFlashlightOn = enabled) }
            }
            ActuatorResult.Unavailable -> {
                _state.update { it.copy(isFlashlightOn = enabled) }
                sendMessage(
                    "Апаратний ліхтарик недоступний — змінено віртуальний стан"
                )
            }
            ActuatorResult.PermissionRequired -> {
                viewModelScope.launch {
                    _events.send(
                        LevelEvent.RequestCameraPermission(enabled)
                    )
                }
            }
            is ActuatorResult.Failure -> {
                _state.update { it.copy(isFlashlightOn = enabled) }
                sendMessage(
                    "${result.message}. Змінено лише віртуальний стан"
                )
            }
        }
    }

    private fun handleCameraPermissionResult(
        action: LevelAction.CameraPermissionResult
    ) {
        if (action.granted) {
            setFlashlight(action.enableAfterGrant)
        } else {
            sendMessage("Без дозволу Camera доступний лише віртуальний ліхтарик")
            _state.update {
                it.copy(isFlashlightOn = action.enableAfterGrant)
            }
        }
    }

    private fun notifyActuatorProblem(result: ActuatorResult) {
        when (result) {
            ActuatorResult.Success -> Unit
            ActuatorResult.Unavailable -> sendMessage(
                "Вібрація недоступна, активовано віртуальну тривогу"
            )
            ActuatorResult.PermissionRequired -> Unit
            is ActuatorResult.Failure -> sendMessage(result.message)
        }
    }

    private fun sendMessage(message: String) {
        viewModelScope.launch {
            _events.send(LevelEvent.ShowMessage(message))
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
        smartActuator.release()
        connector.disconnect()
        super.onCleared()
    }

    private companion object {
        const val TAG = "LevelViewModel"
        const val CLOUD_SYNC_INTERVAL_MS = 5_000L
        const val REMOTE_CONTROL_POLL_INTERVAL_MS = 5_000L
        val REMOTE_COMMAND_IDS = intArrayOf(1, 4)
    }
}
