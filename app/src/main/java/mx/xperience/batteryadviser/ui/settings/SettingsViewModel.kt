package mx.xperience.batteryadviser.ui.settings

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mx.xperience.batteryadviser.data.SettingsDataStore
import mx.xperience.batteryadviser.ui.theme.ThemeMode

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    val themeMode: StateFlow<ThemeMode> = _themeMode
    private val dataStore = SettingsDataStore(application)
    val notifyChargeEnabled = dataStore.notifyChargeEnabled

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun toggleNotifyCharge(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.saveNotifyCharge(enabled) // 'dataStore' es tu SettingsDataStore
        }
    }

    fun sendTestNotification() {
        viewModelScope.launch {
            val isEnabled = dataStore.notifyChargeEnabled.first()
            if (isEnabled) {
                // Creamos un Intent para disparar el servicio o mandar la nota directo
                val context = getApplication<Application>()
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                val testNote = NotificationCompat.Builder(context, "battery_monitor_channel")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Prueba de Battery Adviser")
                    .setContentText("¡La notificación inteligente funciona correctamente! 🔋")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()

                manager.notify(99, testNote)
            }
        }
    }
}
