/*
 * Copyright (C) 2025 The XPerience Project
 * SPDX-License-Identifier: Apache-2.0
 */

package mx.xperience.batteryadviser.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


/**
 * A highly customizable circular progress indicator used to display battery
 * telemetry metrics such as charge level and estimated time.
 *
 * @param label The descriptive title displayed below the gauge.
 * @param value The primary metric string (e.g., "85%" or "12:30").
 * @param progress A float value between 0.0 and 1.0 representing the fill state.
 * @param color The primary color for the progress stroke.
 */
@Composable
fun BatteryIndicator(
    label: String,
    value: String,
    progress: Float,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(150.dp)
        ) {
            // Background track and animated progress stroke
            CircularProgressIndicator(
                // Use coerceIn to ensure progress stays within valid 0.0 - 1.0 range
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxSize(),
                color = color,
                strokeWidth = 12.dp,
                trackColor = color.copy(alpha = 0.12f), // Subtle track transparency
                strokeCap = StrokeCap.Round // Modern rounded terminals
            )

            // Central metric display with adaptive typography
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // econdary descriptive text with professional letter spacing
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            letterSpacing = 1.2.sp // This will now resolve correctly
        )
    }
}