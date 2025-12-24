/*
 * Copyright (C) 2025 The XPerience Project
 * SPDX-License-Identifier: Apache-2.0
 */

package mx.xperience.batteryadviser.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Main Room database configuration for the application.
 * Manages the persistence of battery telemetry and charging patterns.
 */
@Database(entities = [BatteryEntry::class], version = 1, exportSchema = false)
abstract class BatteryDatabase : RoomDatabase() {

    abstract fun batteryDao(): BatteryDao

    companion object {
        @Volatile
        private var INSTANCE: BatteryDatabase? = null

        /**
         * Returns the singleton instance of the BatteryDatabase.
         * Thread-safe initialization using a synchronized block.
         */
        fun getDatabase(context: Context): BatteryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BatteryDatabase::class.java,
                    "battery_adviser_db"
                )
                    .fallbackToDestructiveMigration() // Useful during initial development phases
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}