package mx.xperience.batteryadviser.ui
import android.app.Application
import android.content.Context
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Usamos AndroidViewModel para tener acceso seguro al contexto de la app
/*class BatteryViewModel(application: Application) : AndroidViewModel(application) {

    private val _batteryLevel = MutableStateFlow(0)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _remainingTime = MutableStateFlow("--:--")
    val remainingTime: StateFlow<String> = _remainingTime.asStateFlow()

    fun updateBatteryStatus() {
        val context = getApplication<Application>().applicationContext
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        _batteryLevel.value = level

        // Cálculo de tiempo para AOSP (Android 16)
        val currentNow = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) // Microamperes
        val chargeCounter = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) // Microampere-hours

        if (currentNow < 0) {
            val hours = chargeCounter.toFloat() / (-currentNow).toFloat()
            val h = hours.toInt()
            val m = ((hours - h) * 60).toInt()
            _remainingTime.value = String.format("%02d:%02dh", h, m)
        } else {
            _remainingTime.value = "Cargando"
        }
    }
}*/
class BatteryViewModel(application: Application) : AndroidViewModel(application) {
    private val _batteryLevel = MutableStateFlow(0)
    val batteryLevel = _batteryLevel.asStateFlow()

    private val _remainingTime = MutableStateFlow("--:--")
    val remainingTime = _remainingTime.asStateFlow()

    fun updateBatteryStatus() {
        val context = getApplication<Application>().applicationContext
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        _batteryLevel.value = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        val currentNow = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val chargeCounter = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)

        if (currentNow < 0) {
            val hours = chargeCounter.toFloat() / (-currentNow).toFloat()
            val h = hours.toInt()
            val m = ((hours - h) * 60).toInt()
            _remainingTime.value = String.format("%d:%02dh", h, m)
        } else {
            _remainingTime.value = "Cargando"
        }
    }
}