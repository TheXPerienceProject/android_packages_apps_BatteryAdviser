/*
 * Copyright (C) 2025 The XPerience Project
 * SPDX-License-Identifier: Apache-2.0
 */

package mx.xperience.batteryadviser.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Data model for a reference point on the chart indicating typical charging behavior.
 * * @property barIndex Horizontal position based on the data list index.
 * @property batteryLevel Vertical position representing percentage (0-100).
 */
data class UsualChargingPoint(
    val barIndex: Int,
    val batteryLevel: Float
)

/**
 * A custom-drawn chart component that visualizes battery level history and future predictions.
 * Uses low-level Canvas API for high-performance rendering of bars, grids, and labels.
 *
 * @param data List of [BatteryBar] objects containing telemetry and predictions.
 * @param usualChargingPoint Optional reference point for behavioral analysis.
 * @param modifier Layout modifiers for the chart container.
 */
@Composable
fun BatteryChart(
    data: List<BatteryBar>,
    usualChargingPoint: UsualChargingPoint? = null,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        color = Color.Gray,
        fontSize = 10.sp
    )

    // Theme-aware colors
    val historyColor = MaterialTheme.colorScheme.primary
    val predictionColor = MaterialTheme.colorScheme.tertiary
    val gridColor = Color.LightGray.copy(alpha = 0.3f)
    val surfaceColor = MaterialTheme.colorScheme.surface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val paddingLeft = 45.dp.toPx()
            val paddingBottom = 25.dp.toPx()

            val chartWidth = canvasWidth - paddingLeft
            val chartHeight = canvasHeight - paddingBottom

            // --- 1. Draw Horizontal Grid Lines and Y-Axis Labels ---
            val steps = 5
            for (i in 0..steps) {
                val y = chartHeight - (i * (chartHeight / steps))
                drawLine(
                    color = gridColor,
                    start = Offset(paddingLeft, y),
                    end = Offset(canvasWidth, y),
                    strokeWidth = 1.dp.toPx()
                )

                drawText(
                    textMeasurer = textMeasurer,
                    text = "${i * 20}%",
                    style = labelStyle,
                    topLeft = Offset(5f, y - 15f)
                )
            }

            // --- 2. Render Battery Data Bars ---
            if (data.isNotEmpty()) {
                val barSpacing = 4.dp.toPx()
                val barWidth = (chartWidth / data.size) - barSpacing

                data.forEachIndexed { index, bar ->
                    val x = paddingLeft + (index * (barWidth + barSpacing))
                    val barHeight = (bar.value / 100f) * chartHeight

                    // Rectangular bar rendering
                    drawRect(
                        color = if (bar.isPrediction) predictionColor else historyColor,
                        topLeft = Offset(x, chartHeight - barHeight),
                        size = Size(
                            width = if (bar.isPrediction) barWidth + barSpacing else barWidth,
                            height = barHeight
                        )
                    )

                    // Draw X-Axis Time Labels (Interval-based to avoid overlapping)
                    if (index % 4 == 0) {
                        drawText(
                            textMeasurer = textMeasurer,
                            text = bar.label,
                            style = labelStyle,
                            topLeft = Offset(x, chartHeight + 8f)
                        )
                    }
                }

                // --- 3. Render the Usual Charging Point Indicator ---
                usualChargingPoint?.let { point ->
                    // Guard check to ensure the index is within bounds of current data display
                    if (point.barIndex < data.size) {
                        val pointX = paddingLeft + (point.barIndex * (barWidth + barSpacing)) + (barWidth / 2)
                        val pointY = chartHeight - (point.batteryLevel / 100f * chartHeight)

                        // Outer turquoise ring
                        drawCircle(
                            color = Color(0xFF0097A7),
                            radius = 6.dp.toPx(),
                            center = Offset(pointX, pointY),
                            style = Stroke(width = 2.dp.toPx())
                        )
                        // Inner core blending with background
                        drawCircle(
                            color = surfaceColor,
                            radius = 4.dp.toPx(),
                            center = Offset(pointX, pointY)
                        )
                    }
                }
            }
        }
    }
}