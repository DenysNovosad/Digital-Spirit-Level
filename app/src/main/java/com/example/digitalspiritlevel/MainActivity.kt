
package com.example.digitalspiritlevel

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.atan2
import kotlin.math.sqrt

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // Значення прискорення
    private var xValue by mutableFloatStateOf(0f)
    private var yValue by mutableFloatStateOf(0f)
    private var zValue by mutableFloatStateOf(0f)

    // Кути нахилу
    private var angleX by mutableFloatStateOf(0f)
    private var angleY by mutableFloatStateOf(0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager =
            getSystemService(SENSOR_SERVICE) as SensorManager

        accelerometer =
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setContent {
            MaterialTheme {
                val bleViewModel: BleViewModel = viewModel()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DigitalSpiritLevelScreen(
                        x = xValue,
                        y = yValue,
                        z = zValue,
                        angleX = angleX,
                        angleY = angleY,
                        bleViewModel = bleViewModel
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        accelerometer?.let { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    override fun onPause() {
        sensorManager.unregisterListener(this)
        super.onPause()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) {
            return
        }

        xValue = event.values[0]
        yValue = event.values[1]
        zValue = event.values[2]

        // Кути нахилу відносно вертикальної осі Z.
        // Формули враховують компоненти прискорення
        // по всіх трьох осях.
        angleX = Math.toDegrees(
            atan2(
                xValue.toDouble(),
                sqrt(
                    (yValue * yValue + zValue * zValue).toDouble()
                )
            )
        ).toFloat()

        angleY = Math.toDegrees(
            atan2(
                yValue.toDouble(),
                sqrt(
                    (xValue * xValue + zValue * zValue).toDouble()
                )
            )
        ).toFloat()
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {
        // Не використовується
    }
}

@Composable
fun DigitalSpiritLevelScreen(
    x: Float,
    y: Float,
    z: Float,
    angleX: Float,
    angleY: Float,
    bleViewModel: BleViewModel
) {
    val isScanning by bleViewModel.isScanning.collectAsState()
    val devices by bleViewModel.devices.collectAsState()
    val connectionState by bleViewModel.connectionState.collectAsState()
    val terminalMessages by bleViewModel.terminalMessages.collectAsState()

    val maxAngle = maxOf(
        kotlin.math.abs(angleX),
        kotlin.math.abs(angleY)
    )

    val status = when {
        maxAngle <= 2f -> "РІВНО"
        maxAngle <= 5f -> "НЕВЕЛИКИЙ НАХИЛ"
        else -> "ЗНАЧНИЙ НАХИЛ"
    }

    val levelColor = when {
        maxAngle <= 2f -> Color(0xFF2EAD63)
        maxAngle <= 5f -> Color(0xFFFFB300)
        else -> Color(0xFFE53935)
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

        // Головний індикатор
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = status,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = levelColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                TwoAxisLevel(
                    angleX = angleX,
                    angleY = angleY,
                    color = levelColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                )

                Text(
                    text = "Нахил X: %.2f°".format(angleX),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "Нахил Y: %.2f°".format(angleY),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Дані фізичного сенсора
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Дані акселерометра",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("X = %.2f м/с²".format(x))
                Text("Y = %.2f м/с²".format(y))
                Text("Z = %.2f м/с²".format(z))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Пороги: до 2° — рівно; до 5° — невеликий нахил; понад 5° — значний нахил.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        BleControlPanel(
            isScanning = isScanning,
            devices = devices,
            connectionState = connectionState,
            terminalMessages = terminalMessages,
            onSearch = bleViewModel::searchDevices,
            onConnect = bleViewModel::connect,
            onDisconnect = bleViewModel::disconnect,
            onSendData = {
                bleViewModel.sendLevelData(angleX, angleY, status)
            }
        )
    }
}

@Composable
private fun BleControlPanel(
    isScanning: Boolean,
    devices: List<MockBleDevice>,
    connectionState: BleConnectionState,
    terminalMessages: List<String>,
    onSearch: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSendData: () -> Unit
) {
    val statusText = if (isScanning) "Scanning" else connectionState.label
    val statusColor = when {
        isScanning -> MaterialTheme.colorScheme.primary
        connectionState == BleConnectionState.CONNECTED -> Color(0xFF2EAD63)
        connectionState == BleConnectionState.DATA_TRANSFER -> Color(0xFF2EAD63)
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
                onClick = onSearch,
                enabled = !isScanning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Пошук пристроїв")
            }

            if (isScanning) {
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

            devices.forEach { device ->
                HorizontalDivider()
                Text(device.name, fontWeight = FontWeight.SemiBold)
                Text(
                    device.address,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                when (connectionState) {
                    BleConnectionState.DISCONNECTED -> {
                        OutlinedButton(
                            onClick = onConnect,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Підключити")
                        }
                    }

                    BleConnectionState.CONNECTING -> {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    BleConnectionState.CONNECTED,
                    BleConnectionState.DATA_TRANSFER -> {
                        Button(
                            onClick = onSendData,
                            enabled = connectionState == BleConnectionState.CONNECTED,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Надіслати дані рівня")
                        }

                        OutlinedButton(
                            onClick = onDisconnect,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Відключити")
                        }
                    }
                }
            }

            Text(
                text = "Термінал",
                fontWeight = FontWeight.Bold
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = terminalMessages.joinToString("\n")
                        .ifEmpty { "Повідомлення відсутні" },
                    modifier = Modifier.padding(10.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
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
        val cx = size.width / 2f
        val cy = size.height / 2f

        // Радіус кругового рівня
        val radius = minOf(size.width, size.height) * 0.40f

        // Коло рівня
        drawCircle(
            color = Color.Gray,
            radius = radius,
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 4f
            )
        )

        // Додаткові контрольні кола
        drawCircle(
            color = Color.LightGray,
            radius = radius * 0.5f,
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2f
            )
        )

        // Центральні осі
        drawLine(
            color = Color.Gray,
            start = Offset(cx - radius, cy),
            end = Offset(cx + radius, cy),
            strokeWidth = 2f
        )

        drawLine(
            color = Color.Gray,
            start = Offset(cx, cy - radius),
            end = Offset(cx, cy + radius),
            strokeWidth = 2f
        )

        // Обмежуємо рух бульбашки межами рівня.
        // Кожен кут керує своєю координатою.
        val nx = (angleX / 15f).coerceIn(-1f, 1f)
        val ny = (angleY / 15f).coerceIn(-1f, 1f)

        val bubbleX = cx + nx * radius * 0.85f
        val bubbleY = cy - ny * radius * 0.85f

        // Бульбашка
        drawCircle(
            color = color,
            radius = radius * 0.14f,
            center = Offset(bubbleX, bubbleY)
        )

        // Контур бульбашки
        drawCircle(
            color = Color.DarkGray,
            radius = radius * 0.14f,
            center = Offset(bubbleX, bubbleY),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2f
            )
        )
    }
}
