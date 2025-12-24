/*
 * Copyright (C) 2025 The XPerience Project
 * SPDX-License-Identifier: Apache-2.0
 */
package mx.xperience.batteryadviser

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.*
import mx.xperience.batteryadviser.data.workers.BatterySyncWorker
import mx.xperience.batteryadviser.ui.BatteryViewModel
import mx.xperience.batteryadviser.ui.screens.MainScreen
import mx.xperience.batteryadviser.ui.theme.BatteryTheme
import mx.xperience.batteryadviser.ui.settings.SettingsViewModel
import mx.xperience.batteryadviser.ui.screens.SettingsScreen
import mx.xperience.batteryadviser.service.BatteryMonitorService
import java.util.concurrent.TimeUnit

/**
 * Main entry point of the Battery Adviser application.
 * Manages foreground service lifecycle, notification permissions, and background synchronization.
 */
class MainActivity : ComponentActivity() {

    /**
     * Initializes the activity, sets up background tasks, and defines the UI content.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initForegroundService()
        checkNotificationPermissions()
        setupBackgroundSync()

        setContent {
            val settingsVm: SettingsViewModel = viewModel()
            val batteryVm: BatteryViewModel = viewModel()
            val themeMode by settingsVm.themeMode.collectAsState()

            var showSettings by remember { mutableStateOf(false) }
            val navController = rememberNavController()

            // Apply global theme based on user settings
            BatteryTheme(themeMode = themeMode) {
               /* if (showSettings) {
                    SettingsScreen(
                        viewModel = settingsVm,
                        onBack = { showSettings = false }
                    )
                } else {
                    MainScreen(
                        viewModel = batteryVm,
                        onOpenSettings = { showSettings = true }
                    )
                }*/
                NavHost(
                    navController = navController,
                    startDestination = "main"
                ) {
                    composable("main") {
                        MainScreen(
                            onOpenSettings = { navController.navigate("settings") },
                            viewModel = batteryVm
                        )
                    }
                    composable("settings"){
                        SettingsScreen(
                            viewModel = settingsVm,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    /**
     * Starts the BatteryMonitorService as a foreground service to ensure real-time tracking.
     */
    private fun initForegroundService() {
        val serviceIntent = Intent(this, BatteryMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    /**
     * Requests POST_NOTIFICATIONS permission for Android 13+ (Tiramisu) devices.
     */
    private fun checkNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        }
    }

    /**
     * Configures WorkManager to perform periodic battery data synchronization.
     */
    private fun setupBackgroundSync() {
        val syncRequest = PeriodicWorkRequestBuilder<BatterySyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(false).build())
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "BatterySync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}