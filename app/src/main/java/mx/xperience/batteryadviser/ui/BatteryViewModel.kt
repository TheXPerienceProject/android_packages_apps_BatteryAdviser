/*
 * Copyright (C) 2025 The XPerience Project
 * SPDX-License-Identifier: Apache-2.0
 */
package mx.xperience.batteryadviser.ui

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mx.xperience.batteryadviser.data.BatteryRepository
import mx.xperience.batteryadviser.data.db.BatteryDatabase
import mx.xperience.batteryadviser.data.db.BatteryEntry
import java.util.concurrent.TimeUnit

/**
 * ViewModel responsible for battery data processing, estimation logic, and UI state management.
 * * @param application The application context used for system services and DB access.
 */
class BatteryViewModel(application: Application) : AndroidViewModel(application) {

    // Database and Repository initialization
    private val db = BatteryDatabase.getDatabase(application)
    private val repository = BatteryRepository(db.batteryDao())

    /**
     * Stream of historical battery entries for UI charting.
     */
    val historyData = repository.chartData.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // UI States
    private val _batteryLevel = MutableStateFlow(0)
    val batteryLevel = _batteryLevel.asStateFlow()

    private val _remainingTime = MutableStateFlow("Calculating...")
    val remainingTime = _remainingTime.asStateFlow()

    private val _isCharging = MutableStateFlow(false)
    val isCharging = _isCharging.asStateFlow()

    private val _dischargeRate = MutableStateFlow(0.0)
    val dischargeRate = _dischargeRate.asStateFlow()

    private val _estimatedHours = MutableStateFlow("--")
    val estimatedHours = _estimatedHours.asStateFlow()

    private val batteryManager = application.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    // Internal calculation variables
    private var lastBatteryLevel = 0
    private var currentReadings = mutableListOf<Double>()
    private var deviceCapacityMAh: Double = 5000.0

    /**
     * Listens for system battery broadcasts to update the UI instantly.
     */
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let { updateBatteryStatus(it) }
        }
    }

    init {
        deviceCapacityMAh = getBatteryCapacity(application)
        setupBatteryObservables(application)
        startRealTimeMonitoring()
    }

    /**
     * Registers receivers and fetches the initial battery state.
     */
    private fun setupBatteryObservables(application: Application) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        application.registerReceiver(batteryReceiver, filter)

        val initialIntent = application.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        initialIntent?.let { updateBatteryStatus(it) }
    }

    /**
     * Starts a polling loop to refresh battery metrics every 5 seconds.
     */
    private fun startRealTimeMonitoring() {
        viewModelScope.launch {
            while (true) {
                updateBatteryStatus()
                delay(5000)
            }
        }
    }

    /**
     * Orchestrates the update of all battery metrics (level, current, time estimates).
     */
    fun updateBatteryStatus(intent: Intent? = null) {
        val batteryPercent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        _batteryLevel.value = batteryPercent
        _isCharging.value = isCharging

        // Handle Current (mA) calculation and smoothing
        val rawCurrent = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW).toDouble()
        val currentMA = normalizeCurrentMA(rawCurrent)

        smoothCurrentReadings(currentMA)
        val avgCurrentMA = currentReadings.average()
        _dischargeRate.value = avgCurrentMA

        // Determine which estimation logic to apply
        if (isCharging) {
            handleChargingState(avgCurrentMA)
        } else {
            if (currentReadings.size >= 3) {
                handleDischargingState(batteryPercent, avgCurrentMA)
            } else {
                _remainingTime.value = "Stabilizing..."
            }
        }

        // Persist level changes to DB
        if (batteryPercent != lastBatteryLevel) {
            saveToDatabase(batteryPercent)
            lastBatteryLevel = batteryPercent
        }
    }

    /**
     * Saves a new battery entry and performs cleanup of old data.
     */
    private fun saveToDatabase(batteryPercent: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.batteryDao().insertEntry(
                    BatteryEntry(
                        level = batteryPercent.toFloat(),
                        timestamp = System.currentTimeMillis()
                    )
                )
                // Retain only the last 24 hours of data
                val threshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
                db.batteryDao().deleteOldData(threshold)
            } catch (e: Exception) {
                Log.e("BatteryVM", "DB insertion failed", e)
            }
        }
    }

    /**
     * Estimates remaining time until full charge.
     */
    private fun handleChargingState(avgCurrentMA: Double) {
        val timeToFull = batteryManager.computeChargeTimeRemaining()
        if (timeToFull > 0) {
            val totalHours = timeToFull.toDouble() / (1000 * 60 * 60)
            updateTimeDisplay(totalHours, "UNTIL FULL")
        } else {
            _remainingTime.value = "Charging..."
            _estimatedHours.value = "--"
        }
    }

    /**
     * Estimates discharging time based on current consumption and total capacity.
     */
    private fun handleDischargingState(percent: Int, avgCurrentMA: Double) {
        val remainingMAh = (percent * deviceCapacityMAh) / 100.0
        val safeCurrent = if (Math.abs(avgCurrentMA) < 10.0) 10.0 else Math.abs(avgCurrentMA)

        // Apply 80% efficiency factor for realistic estimation
        val hoursRemaining = remainingMAh / (safeCurrent * 0.8)

        if (hoursRemaining > 150) {
            _remainingTime.value = "Standby\nLong"
            _estimatedHours.value = ">150h"
        } else {
            updateTimeDisplay(hoursRemaining, "LEFT")
        }
    }

    /**
     * Formats raw hours into human-readable strings for the UI.
     */
    private fun updateTimeDisplay(hours: Double, label: String) {
        val h = hours.toInt()
        val m = ((hours - h) * 60).toInt()
        _remainingTime.value = String.format("%02d:%02d\n$label", h, m)
        _estimatedHours.value = String.format("%.1f h", hours)
    }

    /**
     * Normalizes different hardware current scales into milliAmperes.
     */
    private fun normalizeCurrentMA(raw: Double): Double {
        val abs = Math.abs(raw)
        return when {
            abs == 0.0 -> 0.0
            abs < 50 -> abs * 1000.0   // Scale Amps to mA
            abs > 10000 -> abs / 1000.0 // Scale uA to mA
            else -> abs
        }
    }

    private fun smoothCurrentReadings(value: Double) {
        if (currentReadings.size >= 20) currentReadings.removeAt(0)
        currentReadings.add(value)
    }

    /**
     * Attempts to calculate the real physical capacity of the battery.
     */
    private fun getBatteryCapacity(context: Context): Double {
        val chargeCounter = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER).toDouble()
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).toDouble()

        return if (chargeCounter > 0 && level > 0) {
            (chargeCounter / level * 100) / 1000.0
        } else 5000.0
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(batteryReceiver)
        } catch (e: Exception) { /* Ignore */ }
    }
}