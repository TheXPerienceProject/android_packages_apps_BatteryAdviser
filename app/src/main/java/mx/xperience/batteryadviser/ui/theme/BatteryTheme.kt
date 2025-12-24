/*
 * Copyright (C) 2025 The XPerience Project
 * SPDX-License-Identifier: Apache-2.0
 */

package mx.xperience.batteryadviser.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Enumeration of supported application themes.
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    AMOLED
}

/**
 * Custom theme provider for the Battery Adviser application.
 * Manages dynamic colors, AMOLED pitch-black backgrounds, and system bar synchronization.
 * * @param themeMode The selected theme to apply.
 * @param content The composable content to be themed.
 */
@Composable
fun BatteryTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Configuration of color schemes based on the selected mode
    val colorScheme = when (themeMode) {
        ThemeMode.LIGHT -> {
            lightColorScheme(
                primary = Color(0xFF00BCD4),   // Cyan for Battery Levels
                tertiary = Color(0xFFFFA500),  // Orange for Predictions
                surface = Color.White,
                background = Color.White
            )
        }

        ThemeMode.DARK -> {
            // Use Material You dynamic colors on supported Android 12+ devices
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicDarkColorScheme(context)
            } else {
                darkColorScheme()
            }
        }

        ThemeMode.AMOLED -> {
            val base = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicDarkColorScheme(context)
            } else {
                darkColorScheme()
            }

            // Override background and surface for pure black AMOLED experience
            base.copy(
                surface = Color.Black,
                background = Color.Black
            )
        }
    }

    // Sync system status bars and navigation bars with the current theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val isAmoled = themeMode == ThemeMode.AMOLED
            val darkIcons = themeMode == ThemeMode.LIGHT

            window.statusBarColor = if (isAmoled) Color.Black.toArgb() else colorScheme.surface.toArgb()
            window.navigationBarColor = if (isAmoled) Color.Black.toArgb() else colorScheme.surface.toArgb()

            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = darkIcons
                isAppearanceLightNavigationBars = darkIcons
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}