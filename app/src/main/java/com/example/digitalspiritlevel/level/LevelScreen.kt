package com.example.digitalspiritlevel.level

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.digitalspiritlevel.helpers.BleConnectionState

@Composable
fun LevelScreen(viewModel: LevelViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingFlashlightState by remember { mutableStateOf(false) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onAction(
            LevelAction.CameraPermissionResult(
                granted = granted,
                enableAfterGrant = pendingFlashlightState
            )
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is LevelEvent.ShowMessage -> {
                    Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
                }
                is LevelEvent.RequestCameraPermission -> {
                    pendingFlashlightState = event.enableAfterGrant
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }
    }

    LevelContent(
        state = state,
        onAction = viewModel::onAction
    )
}

@Composable
fun LevelContent(
    state: LevelState,
    onAction: (LevelAction) -> Unit
) {
    val levelColor = when (state.levelStatus) {
        LevelStatus.LEVEL -> Color(0xFF2EAD63)
        LevelStatus.SLIGHT_TILT -> Color(0xFFFFB300)
        LevelStatus.SIGNIFICANT_TILT -> Color(0xFFE53935)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Digital Spirit Level",
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Цифровий рівень для монтажних робіт",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = state.levelStatus.label,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = levelColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                TwoAxisLevel(
                    angleX = state.angleX,
                    angleY = state.angleY,
                    color = levelColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                )

                Text(
                    text = "Нахил X: %.2f°".format(state.angleX),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Нахил Y: %.2f°".format(state.angleY),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Дані акселерометра",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("X = %.2f м/с²".format(state.x))
                Text("Y = %.2f м/с²".format(state.y))
                Text("Z = %.2f м/с²".format(state.z))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Пороги: до 2° — рівно; до 5° — невеликий нахил; понад 5° — значний нахил.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        CloudSyncPanel(syncStatus = state.syncStatus)

        Spacer(modifier = Modifier.height(20.dp))

        ManualControlPanel(
            state = state,
            onAction = onAction
        )

        Spacer(modifier = Modifier.height(20.dp))

        RemoteControlPanel(state = state)

        Spacer(modifier = Modifier.height(20.dp))

        BleControlPanel(
            state = state,
            onAction = onAction
        )
    }
}

@Composable
private fun ManualControlPanel(
    state: LevelState,
    onAction: (LevelAction) -> Unit
) {
    val alarmColor = if (state.isAlarmActive) {
        Color(0xFFE53935)
    } else {
        Color(0xFF2EAD63)
    }
    val flashlightColor = if (state.isFlashlightOn) {
        Color(0xFFFFC107)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Ручне керування (Manual Control)",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (state.automaticControlEnabled) {
                    "Автоматичний режим: нахил понад 5° активує тривогу"
                } else {
                    "Ручний режим: стан задає користувач"
                },
                color = if (state.automaticControlEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Text(
                text = if (state.isAlarmActive) {
                    "Тривога: АКТИВНА"
                } else {
                    "Тривога: вимкнена"
                },
                color = alarmColor,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedButton(
                onClick = {
                    onAction(LevelAction.ToggleAutomaticControl)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (state.automaticControlEnabled) {
                        "Перейти в ручний режим"
                    } else {
                        "Увімкнути автоматичний режим"
                    }
                )
            }

            Button(
                onClick = { onAction(LevelAction.ToggleManualAlarm) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (state.isAlarmActive) {
                        "Вимкнути тривогу"
                    } else {
                        "Увімкнути тривогу"
                    }
                )
            }

            Button(
                onClick = { onAction(LevelAction.TriggerVibration) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Перевірити вібровідгук")
            }

            OutlinedButton(
                onClick = {
                    onAction(
                        LevelAction.SetFlashlight(!state.isFlashlightOn)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (state.isFlashlightOn) {
                        "Вимкнути ліхтарик"
                    } else {
                        "Увімкнути ліхтарик"
                    },
                    color = flashlightColor
                )
            }

            Text(
                text = buildString {
                    append("Вібратор: ")
                    append(if (state.hasVibrator) "доступний" else "недоступний")
                    append("; апаратний спалах: ")
                    append(if (state.hasFlashlight) "доступний" else "недоступний")
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RemoteControlPanel(state: LevelState) {
    val lampColor = if (state.isRemoteLampOn) {
        Color(0xFFFFC107)
    } else {
        Color(0xFF616161)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Cloud-to-Device",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(lampColor, CircleShape)
            )

            Text(
                text = if (state.isRemoteLampOn) {
                    "Віртуальна лампа: ON"
                } else {
                    "Віртуальна лампа: OFF"
                },
                color = lampColor,
                fontWeight = FontWeight.SemiBold
            )

            Text(state.remoteControlStatus.label)
            Text(
                text = state.lastRemoteCommand,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Retrofit опитує тестові команди JSONPlaceholder кожні 5 секунд",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CloudSyncPanel(syncStatus: SyncStatus) {
    val statusColor = when (syncStatus) {
        SyncStatus.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
        SyncStatus.SYNCING -> MaterialTheme.colorScheme.primary
        SyncStatus.SYNCED -> Color(0xFF2EAD63)
        SyncStatus.PENDING -> Color(0xFFE53935)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Хмарна синхронізація",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (syncStatus == SyncStatus.SYNCING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
                Text(
                    text = "Статус: ${syncStatus.label}",
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = "Дані акселерометра надсилаються кожні 5 секунд",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BleControlPanel(
    state: LevelState,
    onAction: (LevelAction) -> Unit
) {
    val statusText = if (state.isScanning) {
        "Scanning"
    } else {
        state.connectionState.label
    }
    val statusColor = when {
        state.isScanning -> MaterialTheme.colorScheme.primary
        state.connectionState == BleConnectionState.CONNECTED -> Color(0xFF2EAD63)
        state.connectionState == BleConnectionState.DATA_TRANSFER -> Color(0xFF2EAD63)
        else -> MaterialTheme.colorScheme.error
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "BLE-з'єднання",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Статус: $statusText",
                color = statusColor,
                fontWeight = FontWeight.SemiBold
            )

            Button(
                onClick = { onAction(LevelAction.SearchDevices) },
                enabled = !state.isScanning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Пошук пристроїв")
            }

            if (state.isScanning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Сканування...")
                }
            }

            state.devices.forEach { device ->
                HorizontalDivider()
                Text(device.name, fontWeight = FontWeight.SemiBold)
                Text(
                    device.address,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                when (state.connectionState) {
                    BleConnectionState.DISCONNECTED -> {
                        OutlinedButton(
                            onClick = { onAction(LevelAction.Connect) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Підключити")
                        }
                    }

                    BleConnectionState.CONNECTING -> {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    BleConnectionState.CONNECTED,
                    BleConnectionState.DATA_TRANSFER -> {
                        Button(
                            onClick = { onAction(LevelAction.SendLevelData) },
                            enabled = state.connectionState == BleConnectionState.CONNECTED,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Надіслати дані рівня")
                        }

                        OutlinedButton(
                            onClick = { onAction(LevelAction.Disconnect) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Відключити")
                        }
                    }
                }
            }

            Text(text = "Термінал", fontWeight = FontWeight.Bold)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = state.terminalMessages.joinToString("\n")
                            .ifEmpty { "Повідомлення відсутні" },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
fun TwoAxisLevel(
    angleX: Float,
    angleY: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = minOf(size.width, size.height) * 0.40f

        drawCircle(
            color = Color.Gray,
            radius = radius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 4f)
        )
        drawCircle(
            color = Color.LightGray,
            radius = radius * 0.5f,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2f)
        )
        drawLine(
            color = Color.Gray,
            start = Offset(centerX - radius, centerY),
            end = Offset(centerX + radius, centerY),
            strokeWidth = 2f
        )
        drawLine(
            color = Color.Gray,
            start = Offset(centerX, centerY - radius),
            end = Offset(centerX, centerY + radius),
            strokeWidth = 2f
        )

        val normalizedX = (angleX / 15f).coerceIn(-1f, 1f)
        val normalizedY = (angleY / 15f).coerceIn(-1f, 1f)
        val bubbleCenter = Offset(
            x = centerX + normalizedX * radius * 0.85f,
            y = centerY - normalizedY * radius * 0.85f
        )

        drawCircle(
            color = color,
            radius = radius * 0.14f,
            center = bubbleCenter
        )
        drawCircle(
            color = Color.DarkGray,
            radius = radius * 0.14f,
            center = bubbleCenter,
            style = Stroke(width = 2f)
        )
    }
}
