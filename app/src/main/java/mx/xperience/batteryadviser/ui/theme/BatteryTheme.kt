package mx.xperience.batteryadviser.ui.theme
import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode {
    LIGHT,
    DARK,
    AMOLED
}

@Composable
fun BatteryTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when (themeMode) {

        ThemeMode.LIGHT -> {
            // Forzamos tus colores originales en el tema claro
            lightColorScheme(
                primary = Color(0xFF00BCD4),   // Azul (Battery Level)
                tertiary = Color(0xFFFFA500),  // Naranja (Prediction)
                surface = Color.White,
                background = Color.White
            )
        }

        ThemeMode.DARK -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicDarkColorScheme(context) // 🔵 dark blue default
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

            base.copy(
                surface = Color.Black,
                background = Color.Black
            )
        }
    }

    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window

        val darkIcons = themeMode == ThemeMode.LIGHT
        val isAmoled = themeMode == ThemeMode.AMOLED

        window.statusBarColor =
            if (isAmoled) Color.Black.toArgb()
            else colorScheme.surface.toArgb()

        window.navigationBarColor =
            if (isAmoled) Color.Black.toArgb()
            else colorScheme.surface.toArgb()

        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = darkIcons
            isAppearanceLightNavigationBars = darkIcons
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}