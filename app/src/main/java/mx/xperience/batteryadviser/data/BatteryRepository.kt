/*
 * Copyright (C) 2025 The XPerience Project
 * SPDX-License-Identifier: Apache-2.0
 */

package mx.xperience.batteryadviser.data

import kotlinx.coroutines.flow.map
import mx.xperience.batteryadviser.data.db.BatteryDao
import mx.xperience.batteryadviser.ui.components.BatteryBar
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository pattern implementation to abstract the data source from the UI.
 * Handles transformation of Database Entities into UI-ready models.
 */
class BatteryRepository(private val batteryDao: BatteryDao) {

    /**
     * Converts raw database entries into a formatted list for the chart component.
     * Filters and sorts data chronologically to ensure a clear historical trend.
     */
    val chartData = batteryDao.getRecentHistory().map { entries ->
        if (entries.isEmpty()) {
            emptyList<BatteryBar>()
        } else {
            // Sort by timestamp (Past -> Present) and limit to the last 10 samples for UI clarity
            entries.sortedBy { it.timestamp }
                .takeLast(10)
                .map { entry ->
                    val hourLabel = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entry.timestamp))
                    BatteryBar(
                        value = entry.level.toFloat(),
                        label = hourLabel,
                        isPrediction = entry.isPrediction
                    )
                }
        }
    }
}