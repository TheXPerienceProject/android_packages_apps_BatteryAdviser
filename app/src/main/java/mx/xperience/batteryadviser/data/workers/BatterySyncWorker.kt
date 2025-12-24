/*
 * Copyright (C) 2025 The XPerience Project
 * SPDX-License-Identifier: Apache-2.0
 */
package mx.xperience.batteryadviser.data.workers

import android.content.Context
import android.os.BatteryManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import mx.xperience.batteryadviser.data.db.BatteryDatabase
import mx.xperience.batteryadviser.data.db.BatteryEntry

/**
 * Periodic background task that ensures battery snapshots are taken even if the app/service is killed.
 * This ensures the predictive chart has long-term historical data for pattern recognition.
 */
class BatterySyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    /**
     * Fetches current battery level and persists it to the Room database.
     */
    override suspend fun doWork(): Result {
        return try {
            val db = BatteryDatabase.getDatabase(applicationContext)
            val bm = applicationContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).toFloat()

            // Systematic snapshot of battery status
            db.batteryDao().insertEntry(
                BatteryEntry(
                    level = level,// Ensuring level is Int to match our Entity
                    timestamp = System.currentTimeMillis(),
                    isPrediction = false
                )
            )

            return Result.success()
        } catch (e: Exception) {
            // Retry later if a transient error occurs (e.g., database busy)
            Result.retry()
        }
    }
}