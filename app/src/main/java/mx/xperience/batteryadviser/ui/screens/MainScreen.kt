package mx.xperience.batteryadviser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.xperience.batteryadviser.ui.BatteryViewModel
import mx.xperience.batteryadviser.ui.components.BatteryBar
import mx.xperience.batteryadviser.ui.components.BatteryChart
import mx.xperience.batteryadviser.ui.components.BatteryCircle
import mx.xperience.batteryadviser.ui.components.UsualChargingPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class) // Add this annotation here
@Composable
fun MainScreen(
    onOpenSettings: () -> Unit,
    viewModel: BatteryViewModel) {
    val level by viewModel.batteryLevel.collectAsState()
    val currentLevel by viewModel.batteryLevel.collectAsState()
    val history by viewModel.historyData.collectAsState()
    val avgCurrent by viewModel.dischargeRate.collectAsState()
    val timeText by viewModel.remainingTime.collectAsState()
    val isCharging by viewModel.isCharging.collectAsState()

    // datos de ejemplo los traeremos de room
    /*val chartData = remember {
        listOf(
            BatteryBar(100f, "01h"),
            BatteryBar(85f, "05h"),
            BatteryBar(60f, "09h"),
            BatteryBar(40f, "13h"),
            BatteryBar(20f, "17h"),
            BatteryBar(90f, "22h"), // Carga detectada
            BatteryBar(85f, "02h", isPrediction = true), // Inicio de predicción
            BatteryBar(70f, "06h", isPrediction = true),
            BatteryBar(55f, "10h", isPrediction = true)
        )
    }*/
    val fullChartData = remember(history, level, avgCurrent) {
        val list = history.toMutableList()
        val combinedList = history.toMutableList()

        // Calculamos cuánto baja la batería por cada bloque de tiempo (ej. cada 30 min)
        // Usamos tu avgCurrent para que la pendiente de la predicción sea realista
        val hourlyDrop = (avgCurrent / 5000.0) * 100 // % que baja por hora aprox

        if (combinedList.isNotEmpty()) {
            var simulatedLevel = currentLevel.toFloat()
            val currentTime = System.currentTimeMillis()
            for (i in 1..10) { // <--- LIMITAMOS A 10 DE PREDICCIÓN
                simulatedLevel -= (hourlyDrop.toFloat() / 2) // Bajada estimada cada 30 min
                if (simulatedLevel < 0) simulatedLevel = 0f
                val futureTimestamp = currentTime + (i * 30 * 60 * 1000L)
                val hourLabel = SimpleDateFormat("HH:mm", Locale.getDefault()).format(
                    Date(
                        futureTimestamp
                    )
                )

                combinedList.add(
                    BatteryBar(
                        value = simulatedLevel,
                        //label = "+${i * 30}m", // Etiqueta: +30m, +60m...
                        label = hourLabel,
                        isPrediction = true
                    )
                )
            }
        }
        combinedList
    }

    // Forzamos que todo el contenido use el fondo del tema
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface // Aquí se vuelve negro o blanco
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Battery Adviser") },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                )
            },
            containerColor = MaterialTheme.colorScheme.surface // Asegura el color del Scaffold
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // circulos superiores
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BatteryCircle(
                        label = "Battery Level",
                        value = "$level%",
                        progress = level / 100f,
                        color = MaterialTheme.colorScheme.primary // Azul en light
                    )
                    BatteryCircle(
                        label = if (isCharging) "Charging time" else "Predicted time",
                        value = timeText,
                        progress = level / 100f,
                        color = MaterialTheme.colorScheme.tertiary // Naranja en light
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Línea divisoria que se adapta al fondo
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Leyenda (History / Prediction)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChartLegendItem("History", MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    ChartLegendItem("Prediction", MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(12.dp))

                    // Nueva leyenda para el punto
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .drawBehind {
                                    drawCircle(color = Color(0xFF0097A7), style = Stroke(width = 2.dp.toPx()))
                                }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Usual charging time", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Componente de la Gráfica
                BatteryChart(
                    data = fullChartData,
                    //usualChargingPoint = UsualChargingPoint(barIndex = 8, batteryLevel = 90f),
                    usualChargingPoint = if (fullChartData.isNotEmpty()) UsualChargingPoint(8, 90f) else null,
                    modifier = Modifier.fillMaxWidth().height(280.dp)
                        .fillMaxWidth()
                        .height(280.dp) // Altura suficiente para ver las horas abajo
                )
            }
        }
    }
}

@Composable
fun ChartLegendItem(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}