/*
 * Copyright (C) 2025 The XPerience Project
 * SPDX-License-Identifier: Apache-2.0
 */
package mx.xperience.batteryadviser.ui.components

/**
 * Data model for a single bar in the battery analytics chart.
 */
data class BatteryBar(
    val value: Float, // 0f a 100f
    val label: String, // "01h", "05h", etc.
    val isPrediction: Boolean = false
)