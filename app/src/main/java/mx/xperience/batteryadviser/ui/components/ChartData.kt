package mx.xperience.batteryadviser.ui.components

data class BatteryBar(
    val value: Float, // 0f a 100f
    val label: String, // "01h", "05h", etc.
    val isPrediction: Boolean = false
)