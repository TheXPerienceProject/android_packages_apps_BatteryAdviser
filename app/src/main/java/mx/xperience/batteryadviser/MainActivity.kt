package mx.xperience.batteryadviser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.*
import androidx.work.*
import androidx.work.WorkManager
import mx.xperience.batteryadviser.data.workers.BatterySyncWorker
import mx.xperience.batteryadviser.ui.BatteryViewModel
import mx.xperience.batteryadviser.ui.screens.MainScreen
import mx.xperience.batteryadviser.ui.theme.BatteryTheme
import mx.xperience.batteryadviser.ui.settings.SettingsViewModel
import mx.xperience.batteryadviser.ui.screens.SettingsScreen

import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val syncRequest = PeriodicWorkRequestBuilder<BatterySyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(false).build())
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "BatterySync",
            ExistingPeriodicWorkPolicy.KEEP, // Mantiene el trabajo si ya existe
            syncRequest
        )

        setContent {
            val settingsVm: SettingsViewModel = viewModel()
            val batteryVm: BatteryViewModel = viewModel()
            val themeMode by settingsVm.themeMode.collectAsState()

            var showSettings by remember { mutableStateOf(false) }

            BatteryTheme(themeMode = themeMode) {
                if (showSettings) {
                    SettingsScreen(
                        viewModel = settingsVm,
                        onBack = { showSettings = false }
                    )
                } else {
                    MainScreen(
                        viewModel = batteryVm,
                        onOpenSettings = { showSettings = true }
                    )
                }
            }
        }
    }
}