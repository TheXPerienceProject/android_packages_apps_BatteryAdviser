package mx.xperience.batteryadviser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.xperience.batteryadviser.ui.BatteryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: BatteryViewModel) {
    val level by viewModel.batteryLevel.collectAsState()
    val time by viewModel.remainingTime.collectAsState()

    // Colores exactos de tu captura
    val colorTurquesaBarra = Color(0xFF0097A7)
    val colorAnilloTurquesa = Color(0xFF40E0D0)
    val colorAnilloNaranja = Color(0xFFFFA500)

    LaunchedEffect(Unit) { viewModel.updateBatteryStatus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Battery Adviser", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.ArrowBack, "Atrás", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, "Menú", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colorTurquesaBarra)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Indicador de Nivel
                BatteryCircle(
                    label = "Battery Level",
                    value = "$level%",
                    progress = level / 100f,
                    color = colorAnilloTurquesa
                )
                // Indicador de Predicción
                BatteryCircle(
                    label = "Predicted battery\ntime",
                    value = if(level == 100) ">24:00\nHOURS" else time,
                    progress = 0.8f, // Estético
                    color = colorAnilloNaranja
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

            // Espacio para la futura gráfica
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Gráfica Histórica", color = Color.LightGray)
            }
        }
    }
}

@Composable
fun BatteryCircle(label: String, value: String, progress: Float, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(130.dp)) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = color,
                strokeWidth = 10.dp,
                trackColor = color.copy(alpha = 0.1f),
                strokeCap = StrokeCap.Round
            )
            Text(
                text = value,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                color = Color.DarkGray,
                lineHeight = 22.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = Color.Gray,
            lineHeight = 16.sp
        )
    }
}