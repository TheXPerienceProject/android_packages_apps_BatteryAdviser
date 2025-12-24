/*
 * Copyright (C) 2025 The XPerience Project
 * SPDX-License-Identifier: Apache-2.0
 */

package mx.xperience.batteryadviser.data

/**
 * Pure logic utility for battery-related physics and mathematical calculations.
 * Decoupled from Android Framework for easier unit testing.
 */
object BatteryLogic {

    /**
     * Normalizes raw current sensor data from various hardware implementations.
     * Handles different scales (Amperes, MicroAmperes, MilliAmperes).
     * * @param rawCurrent The direct output from BatteryManager.
     * @return Current in MilliAmperes (mA).
     */
    fun getRealCurrentMA(rawCurrent: Double): Double {
        val absCurrent = Math.abs(rawCurrent)
        return when {
            absCurrent == 0.0 -> 0.0
            absCurrent < 50 -> absCurrent * 1000.0   // 0.21A -> 210mA
            absCurrent > 10000 -> absCurrent / 1000.0 // 210000uA -> 210mA
            else -> absCurrent
        }
    }

    /**
     * Estimates remaining time until depletion based on current consumption.
     * * @param percent Current battery percentage (0-100).
     * @param avgCurrentMA Average discharge rate in mA.
     * @param capacity Design capacity of the battery in mAh.
     * @return Estimated hours remaining.
     */
    fun calculateHoursRemaining(percent: Int, avgCurrentMA: Double, capacity: Double): Double {
        val remainingMAh = (percent * capacity) / 100.0
        // Avoid division by zero and handle extremely low consumption states
        val safeCurrent = if (avgCurrentMA < 10.0) 10.0 else avgCurrentMA

        // Applying an 80% efficiency factor to account for chemical losses and OS background tasks
        return remainingMAh / (safeCurrent * 0.8)
    }
}