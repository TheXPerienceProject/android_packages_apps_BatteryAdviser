package mx.xperience.batteryadviser.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import mx.xperience.batteryadviser.R
import mx.xperience.batteryadviser.data.db.BatteryDatabase
import mx.xperience.batteryadviser.data.db.BatteryEntry
import mx.xperience.batteryadviser.data.BatteryLogic
import mx.xperience.batteryadviser.data.BatteryRepository
import mx.xperience.batteryadviser.data.SettingsDataStore
import java.util.concurrent.TimeUnit

class BatteryMonitorService : Service() {
    private lateinit var db: BatteryDatabase
    private lateinit var settingsDataStore: SettingsDataStore
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var batteryManager: BatteryManager
    private lateinit var repository: BatteryRepository // Reutilizamos tu repo de Room
    private val CHANNEL_ID = "battery_monitor_channel"

    override fun onCreate(){
        super.onCreate()

        db = BatteryDatabase.getDatabase(this)
        settingsDataStore = SettingsDataStore(this)
        batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // Creamos la notificación obligatoria para Android 15
        val notification = createPersistentNotification()

        // En Android 14/15, startForeground debe ir acompañado del tipo de servicio si se declaró en el manifest
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }

        startTracking()
        return START_STICKY
    }

    private fun startTracking() {
        serviceScope.launch {
            while (isActive) {
                val rawCurrent = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW).toDouble()
                val currentMA = BatteryLogic.getRealCurrentMA(rawCurrent)
                val percent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

                // Guardar en Room si el porcentaje cambió (lógica compartida)
                // repository.saveIfChanged(percent)

                // Lógica de Notificación de "Carga Usual"
                checkUsualChargePrediction(percent, currentMA)

                delay(5000) // En background 5 seg es suficiente para ahorrar batería
            }
        }
    }

    private fun createPersistentNotification(): Notification {
        // Aquí va el NotificationChannel (necesario para Android 8+)
        // Y el NotificationCompat.Builder
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Battery Adviser Activo")
            .setContentText("Monitoreando consumo en tiempo real")
            .setSmallIcon(/*R.drawable.ic_battery_bolt*/android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Monitoreo de Batería",
                NotificationManager.IMPORTANCE_LOW // Low para que no haga ruido cada 5 segundos
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun checkUsualChargePrediction(percent: Int, currentMA: Double) {
        serviceScope.launch {
            val isEnabled = settingsDataStore.notifyChargeEnabled.first()
            if (!isEnabled) return@launch

            // Obtener la hora promedio de carga del DAO
            val avgChargeHour = db.batteryDao().getAverageChargeHour() ?: 22.0 // 10 PM por defecto

            // Calcular cuánto tiempo queda (Reutilizando BatteryLogic)
            // Usa una capacidad genérica o la que detectamos ayer (p.ej. 5000.0)
            val hoursRemaining = BatteryLogic.calculateHoursRemaining(percent, currentMA, 5000.0)

            // Ver a qué hora se apagaría el cel
            val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val shutDownHour = (currentHour + hoursRemaining) % 24

            // COMPARACIÓN: Si se apaga antes de la hora usual de carga
            if (shutDownHour < avgChargeHour && percent < 50) {
                // AQUÍ: Deberías checar el Switch de ajustes antes de lanzar
                showWarningNotification(avgChargeHour)
            }
        }
    }

    private fun showWarningNotification(usualHour: Double) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val warningNote = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("¡Aviso de Batería!")
            .setContentText("No llegarás a tu hora habitual de carga (${usualHour.toInt()}:00).")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(2, warningNote) // ID 2 para no pisar la notificación persistente
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // Limpiamos las corrutinas al cerrar el servicio
    }

    override fun onBind(intent: Intent?) = null
}