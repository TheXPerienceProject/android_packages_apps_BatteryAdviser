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
import androidx.compose.ui.unit.dp

/**
 * High-level circular gauge for battery metrics.
 */
@Composable
fun BatteryCircle(
    label: String,
    value: String,
    progress: Float,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
            CircularProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxSize(),
                color = color,
                strokeWidth = 12.dp,
                trackColor = color.copy(alpha = 0.1f),
                strokeCap = StrokeCap.Round
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}