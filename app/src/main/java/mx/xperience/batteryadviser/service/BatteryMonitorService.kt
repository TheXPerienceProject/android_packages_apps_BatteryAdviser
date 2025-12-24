/*
 * Copyright (C) 2025 The XPerience Project
 * SPDX-License-Identifier: Apache-2.0
 */

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
import mx.xperience.batteryadviser.data.BatteryLogic
import mx.xperience.batteryadviser.data.SettingsDataStore
import java.util.Calendar

/**
 * Foreground service responsible for continuous battery monitoring and predictive alerts.
 * This service ensures that the application can track power consumption even when
 * the UI is not in the foreground, complying with Android 15 foreground service requirements.
 */
class BatteryMonitorService : Service() {

    private lateinit var db: BatteryDatabase
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var batteryManager: BatteryManager

    // SupervisorJob ensures that a failure in one child coroutine doesn't kill the entire scope
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val CHANNEL_ID = "battery_monitor_channel"
    private val NOTIFICATION_ID_PERSISTENT = 1
    private val NOTIFICATION_ID_WARNING = 2

    /**
     * Initializes core components. Called once when the service is created.
     */
    override fun onCreate() {
        super.onCreate()
        db = BatteryDatabase.getDatabase(this)
        settingsDataStore = SettingsDataStore(this)
        batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        createNotificationChannel()
    }

    /**
     * Entry point for the service. Initiates foreground state and telemetry tracking.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createPersistentNotification()

        // Required for Android 10+ and specifically enforced in Android 14/15
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID_PERSISTENT,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID_PERSISTENT, notification)
        }

        startTrackingLoop()

        // START_STICKY ensures the system attempts to recreate the service if it's killed by memory pressure
        return START_STICKY
    }

    /**
     * Main telemetry loop. Polls battery hardware properties every 5 seconds.
     */
    private fun startTrackingLoop() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val rawCurrent = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW).toDouble()
                    val currentMA = BatteryLogic.getRealCurrentMA(rawCurrent)
                    val percent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

                    // Execute predictive logic to determine if the user needs a charge reminder
                    checkUsualChargePrediction(percent, currentMA)

                } catch (e: Exception) {
                    // Prevent the loop from crashing due to unexpected hardware read errors
                }
                delay(5000)
            }
        }
    }

    /**
     * Analyzes current discharge rates against historical charging habits.
     * Triggers a high-priority notification if the device won't last until the usual charge time.
     */
    private fun checkUsualChargePrediction(percent: Int, currentMA: Double) {
        serviceScope.launch {
            // Respect user preference from DataStore
            val isEnabled = settingsDataStore.notifyChargeEnabled.first()
            if (!isEnabled) return@launch

            // Retrieve the statistically calculated average charge hour (default to 10 PM)
            val avgChargeHour = db.batteryDao().getAverageChargeHour() ?: 22.0

            // Estimation based on current discharge rate and a standard 5000mAh capacity
            val hoursRemaining = BatteryLogic.calculateHoursRemaining(percent, currentMA, 5000.0)

            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val shutDownHour = (currentHour + hoursRemaining) % 24

            // Logic: If predicted shutdown occurs before the usual charge time and battery is below 50%
            if (shutDownHour < avgChargeHour && percent < 50) {
                showWarningNotification(avgChargeHour)
            }
        }
    }

    /**
     * Creates the mandatory persistent notification for Foreground Services.
     */
    private fun createPersistentNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.battery_adviser_active))
            .setContentText(getString(R.string.monitoring_real_time_power_consumption))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true) // Prevents user from swiping it away
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    /**
     * Dispatches a high-priority alert to the user.
     */
    private fun showWarningNotification(usualHour: Double) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val warningNote = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(getString(R.string.battery_alert))
            .setContentText(
                getString(
                    R.string.battery_won_t_reach_your_usual_charge_time_00,
                    usualHour.toInt()
                ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .build()

        manager.notify(NOTIFICATION_ID_WARNING, warningNote)
    }

    /**
     * Configures the system notification channel required for Android 8.0+.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.battery_monitoring_service),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description =
                    getString(R.string.provides_real_time_battery_analytics_and_predictive_alerts)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Critical: Stop all background work when the service is destroyed to prevent memory leaks
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}