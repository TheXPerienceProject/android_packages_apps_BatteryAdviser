/*
 * Copyright (C) 2025 The XPerience Project
 * SPDX-License-Identifier: Apache-2.0
 */

package mx.xperience.batteryadviser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.xperience.batteryadviser.ui.BatteryViewModel
import mx.xperience.batteryadviser.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onOpenSettings: () -> Unit,
    viewModel: BatteryViewModel
) {
    val level by viewModel.batteryLevel.collectAsState()
    val history by viewModel.historyData.collectAsState()
    val avgCurrent by viewModel.dischargeRate.collectAsState()
    val timeText by viewModel.remainingTime.collectAsState()
    val isCharging by viewModel.isCharging.collectAsState()

    val fullChartData = remember(history, level, avgCurrent) {
        val combinedList = history.toMutableList()
        val hourlyDrop = if (avgCurrent > 0) (avgCurrent / 5000.0) * 100 else 10.0

        if (combinedList.isNotEmpty() || level > 0) {
            var simulatedLevel = level.toFloat()
            val currentTime = System.currentTimeMillis()

            for (i in 1..10) {
                simulatedLevel -= (hourlyDrop.toFloat() / 2f)
                val futureTimestamp = currentTime + (i * 30 * 60 * 1000L)
                val hourLabel = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(futureTimestamp))

                combinedList.add(
                    BatteryBar(
                        value = simulatedLevel.coerceAtLeast(0f),
                        label = hourLabel,
                        isPrediction = true
                    )
                )
            }
        }
        combinedList
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Battery Adviser", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BatteryIndicator(
                        label = "Battery Level",
                        value = "$level%",
                        progress = level / 100f,
                        color = MaterialTheme.colorScheme.primary
                    )
                    BatteryIndicator(
                        label = if (isCharging) "TimeToFull" else "Predicted",
                        value = timeText,
                        progress = level / 100f,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Área de Leyenda
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChartLegendItem("History", MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    ChartLegendItem("Prediction", MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).drawBehind {
                            drawCircle(color = Color(0xFF0097A7), style = Stroke(width = 2.dp.toPx()))
                        })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Usual Charge", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                BatteryChart(
                    data = fullChartData,
                    usualChargingPoint = if (fullChartData.isNotEmpty()) UsualChargingPoint(8, 90f) else null,
                    modifier = Modifier.fillMaxWidth().height(280.dp)
                )
            }
        }
    }
}

/**
 * Reusable component for chart legends.
 * Defined here to resolve the 'Unresolved reference' error.
 */
@Composable
fun ChartLegendItem(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}