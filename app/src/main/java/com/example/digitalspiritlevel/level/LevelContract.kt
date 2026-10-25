package com.example.digitalspiritlevel.level

import com.example.digitalspiritlevel.helpers.BleConnectionState
import com.example.digitalspiritlevel.helpers.MockBleDevice

data class LevelState(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val angleX: Float = 0f,
    val angleY: Float = 0f,
    val levelStatus: LevelStatus = LevelStatus.LEVEL,
    val isSensorRunning: Boolean = false,
    val isScanning: Boolean = false,
    val devices: List<MockBleDevice> = emptyList(),
    val connectionState: BleConnectionState = BleConnectionState.DISCONNECTED,
    val terminalMessages: List<String> = emptyList(),
    val syncStatus: SyncStatus = SyncStatus.IDLE
)

enum class LevelStatus(val label: String) {
    LEVEL("РІВНО"),
    SLIGHT_TILT("НЕВЕЛИКИЙ НАХИЛ"),
    SIGNIFICANT_TILT("ЗНАЧНИЙ НАХИЛ")
}

enum class SyncStatus(val label: String) {
    IDLE("Очікування"),
    SYNCING("Синхронізація..."),
    SYNCED("Синхронізовано"),
    PENDING("Pending")
}

sealed interface LevelAction {
    data object Start : LevelAction
    data object Stop : LevelAction
    data object SearchDevices : LevelAction
    data object Connect : LevelAction
    data object Disconnect : LevelAction
    data object SendLevelData : LevelAction
}

sealed interface LevelEvent {
    data class ShowMessage(val text: String) : LevelEvent
}
