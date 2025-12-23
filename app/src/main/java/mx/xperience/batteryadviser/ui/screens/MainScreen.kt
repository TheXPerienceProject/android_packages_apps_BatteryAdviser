package mx.xperience.batteryadviser.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.xperience.batteryadviser.ui.BatteryViewModel
import mx.xperience.batteryadviser.ui.components.BatteryCircle

@OptIn(ExperimentalMaterial3Api::class) // Add this annotation here
@Composable
fun MainScreen(
    onOpenSettings: () -> Unit,
    viewModel: BatteryViewModel) {
    val level by viewModel.batteryLevel.collectAsState()
    val time by viewModel.remainingTime.collectAsState()

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
                        label = "Predicted time",
                        value = time,
                        progress = 0.75f,
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

                // Aquí es donde dibujaremos la gráfica después
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Gráfica de Historial",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}