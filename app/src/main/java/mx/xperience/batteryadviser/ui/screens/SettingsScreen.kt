package mx.xperience.batteryadviser.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mx.xperience.batteryadviser.ui.settings.SettingsViewModel
import mx.xperience.batteryadviser.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val notifyEnabled by viewModel.notifyChargeEnabled.collectAsState(initial = true)
    val themeMode by viewModel.themeMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
        ) {

            Text(
                "Theme",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            ThemeOption(
                title = "Light",
                selected = themeMode == ThemeMode.LIGHT
            ) { viewModel.setThemeMode(ThemeMode.LIGHT) }

            ThemeOption(
                title = "Dark",
                selected = themeMode == ThemeMode.DARK
            ) { viewModel.setThemeMode(ThemeMode.DARK) }

            ThemeOption(
                title = "Dark (AMOLED)",
                selected = themeMode == ThemeMode.AMOLED
            ) { viewModel.setThemeMode(ThemeMode.AMOLED) }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                "Notifications",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleNotifyCharge(!notifyEnabled) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Charge Reminder", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Notify if battery won't reach your usual charge time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = notifyEnabled,
                    onCheckedChange = { viewModel.toggleNotifyCharge(it) }
                )
            }
            /*Button(
                onClick = { viewModel.sendTestNotification() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                // Solo habilitamos el botón si las notificaciones están ON en el switch
                enabled = notifyEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text("Test Notification")
            }*/
        }
    }
}

@Composable
private fun ThemeOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title)
        RadioButton(
            selected = selected,
            onClick = null
        )
    }
}
