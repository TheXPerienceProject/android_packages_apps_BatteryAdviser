package mx.xperience.batteryadviser.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF006A6A),
    secondary = Color(0xFF4A6363),
    tertiary = Color(0xFFFFB347) // El naranja de tu imagen
)

@Composable
fun BatteryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}