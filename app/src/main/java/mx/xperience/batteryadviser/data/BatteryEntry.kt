/*
 * Copyright (C) 2025 The XPerience Project
 * SPDX-License-Identifier: Apache-2.0
 */

package mx.xperience.batteryadviser.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Database entity representing a single battery status snapshot.
 *
 * @property id Unique identifier for the database record.
 * @property level Battery percentage at the time of recording (0-100).
 * @property timestamp Epoch time in milliseconds when the entry was created.
 * @property isPrediction Flag to distinguish between real historical data and simulated points.
 */
@Entity(tableName = "battery_history")
data class BatteryEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val level: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isPrediction: Boolean = false
)

@Dao
interface BatteryDao {
    @Query("SELECT * FROM battery_history ORDER BY timestamp DESC LIMIT 100")
    fun getHistory(): Flow<List<BatteryEntry>>

    @Insert
    suspend fun insert(entry: BatteryEntry)
}