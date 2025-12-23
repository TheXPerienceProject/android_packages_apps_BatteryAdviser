package mx.xperience.batteryadviser.data

import kotlinx.coroutines.flow.map
import mx.xperience.batteryadviser.data.db.BatteryDao
import mx.xperience.batteryadviser.ui.components.BatteryBar
import java.text.SimpleDateFormat
import java.util.*

/*class BatteryRepository(private val batteryDao: BatteryDao) {
    val chartData = batteryDao.getRecentHistory().map { entries ->
        entries.reversed().map {
            val hour = SimpleDateFormat("HH'h'", Locale.getDefault()).format(Date(it.timestamp))
            BatteryBar(
                value = it.level,
                label = hour,
                isPrediction = it.isPrediction
            )
        }
    }
}*/
class BatteryRepository(private val batteryDao: BatteryDao) {
    val chartData = batteryDao.getRecentHistory().map { entries ->
        if (entries.isEmpty()) {
            emptyList<BatteryBar>()
        } else {
            // Ordenamos cronológicamente (Pasado -> Presente)
            entries.sortedBy { it.timestamp }.takeLast(10) //limitamos a 10 barras para mantener clean
                .map {
                val hour = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it.timestamp))
                BatteryBar(
                    value = it.level,
                    label = hour,
                    isPrediction = it.isPrediction
                )
            }
        }
    }
}