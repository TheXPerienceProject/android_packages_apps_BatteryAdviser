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

data class UsualChargingPoint(
    val barIndex: Int, // En qué posición de la lista de datos está
    val batteryLevel: Float // A qué altura (0-100)
)

@Composable
fun BatteryChart(
    data: List<BatteryBar>,
    usualChargingPoint: UsualChargingPoint? = null,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = Color.Gray, fontSize = 10.sp)

    // Colores del tema (Azul/Naranja en Light, Dinámicos en Dark)
    val historyColor = MaterialTheme.colorScheme.primary
    val predictionColor = MaterialTheme.colorScheme.tertiary
    val gridColor = Color.LightGray.copy(alpha = 0.5f)
    val surfaceColor = MaterialTheme.colorScheme.surface

    Column(modifier = modifier.fillMaxWidth().height(250.dp).padding(16.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val paddingLeft = 40.dp.toPx()
            val paddingBottom = 20.dp.toPx()

            val chartWidth = canvasWidth - paddingLeft
            val chartHeight = canvasHeight - paddingBottom

            // 1. Dibujar Líneas de Guía Horizontales (0%, 20%... 100%)
            val steps = 5
            for (i in 0..steps) {
                val y = chartHeight - (i * (chartHeight / steps))
                drawLine(
                    color = gridColor,
                    start = Offset(paddingLeft, y),
                    end = Offset(canvasWidth, y),
                    strokeWidth = 1f
                )
                // Etiquetas de porcentaje (0%, 20%...)
                drawText(
                    textMeasurer = textMeasurer,
                    text = "${i * 20} %",
                    style = labelStyle,
                    topLeft = Offset(0f, y - 15f)
                )
            }

            val barSpacing = 4.dp.toPx()
            val barWidth = (chartWidth / data.size) - barSpacing

            data.forEachIndexed { index, bar ->
                val x = paddingLeft + (index * (barWidth + barSpacing))
                val barHeight = (bar.value / 100f) * chartHeight

                // Si es predicción, es un bloque ancho
                // Si es historial, son barras delgadas
                drawRect(
                    color = if (bar.isPrediction) predictionColor else historyColor,
                    topLeft = Offset(x, chartHeight - barHeight),
                    size = Size(if (bar.isPrediction) barWidth + barSpacing else barWidth, barHeight)
                )

                // Dibujar etiquetas de hora abajo (01h, 05h...)
                if (index % 4 == 0) {
                    drawText(
                        textMeasurer = textMeasurer,
                        text = bar.label,
                        style = labelStyle,
                        topLeft = Offset(x, chartHeight + 5f)
                    )
                }

                usualChargingPoint?.let {  point ->
                    val barspacing = 4.dp.toPx()
                    val barWidth = (chartWidth / data.size) - barSpacing

                    //calculamos la posicion x basada en el indice de la barra
                    val x =  paddingLeft + (point.barIndex * (barWidth + barSpacing)) + (barWidth / 2)
                    //val y = chartHeight - (point.batteryLevel / 100f) * chartHeight
                    val y = chartHeight - (point.batteryLevel / 100f * chartHeight)

                    drawCircle(
                        color = Color(0xFF0097A7), //turquesa oscuro
                        radius = 6.dp.toPx(),
                        center = Offset(x, y),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    // Círculo interior (Fondo que se adapta al tema)
                    drawCircle(
                        color = surfaceColor,
                        radius = 4.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}