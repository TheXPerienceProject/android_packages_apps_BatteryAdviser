/*
 * Copyright (C) 2025 The XPerience Project
 * SPDX-License-Identifier: Apache-2.0
 */

package mx.xperience.batteryadviser.ui.settings

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mx.xperience.batteryadviser.data.SettingsDataStore
import mx.xperience.batteryadviser.ui.theme.ThemeMode

/**
 * ViewModel responsible for managing user preferences and UI settings state.
 * Interfaces with DataStore to persist configurations like notification triggers.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    // Internal state for the UI theme mode
    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    val themeMode: StateFlow<ThemeMode> = _themeMode

    // DataStore instance for persistent settings
    private val dataStore = SettingsDataStore(application)

    /**
     * Flow observable that emits the current state of notification preferences.
     */
    val notifyChargeEnabled = dataStore.notifyChargeEnabled

    /**
     * Updates the application's visual theme mode.
     * @param mode The selected ThemeMode (LIGHT, DARK, or AMOLED).
     */
    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    /**
     * Toggles the battery prediction notification setting.
     * @param enabled Boolean flag to enable or disable notifications.
     */
    fun toggleNotifyCharge(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.saveNotifyCharge(enabled)
        }
    }

    /**
     * Triggers a test notification to verify channel configuration and user permissions.
     * Only executes if the notification setting is currently enabled.
     */
    fun sendTestNotification() {
        viewModelScope.launch {
            val isEnabled = dataStore.notifyChargeEnabled.first()
            if (isEnabled) {
                val context = getApplication<Application>()
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                val testNote = NotificationCompat.Builder(context, "battery_monitor_channel")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("XPerience Battery Test")
                    .setContentText("Smart notifications are working correctly! 🔋")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()

                manager.notify(99, testNote)
            }
        }
    }
}