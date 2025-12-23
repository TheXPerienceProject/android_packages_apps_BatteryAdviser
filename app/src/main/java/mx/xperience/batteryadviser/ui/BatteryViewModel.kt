package mx.xperience.batteryadviser.ui

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mx.xperience.batteryadviser.data.BatteryRepository
import mx.xperience.batteryadviser.data.db.BatteryDatabase
import mx.xperience.batteryadviser.data.db.BatteryEntry
import java.util.concurrent.TimeUnit

class BatteryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(application, BatteryDatabase::class.java, "battery.db").build()
    private val repository = BatteryRepository(db.batteryDao())

    // Flow que la UI observará para actualizar la gráfica automáticamente
    val historyData = repository.chartData.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _batteryLevel = MutableStateFlow(0)
    val batteryLevel = _batteryLevel.asStateFlow()

    private val _remainingTime = MutableStateFlow("Calculando...")
    val remainingTime = _remainingTime.asStateFlow()

    private val _isCharging = MutableStateFlow(false)
    val isCharging = _isCharging.asStateFlow()

    private val _dischargeRate = MutableStateFlow(0.0)
    val dischargeRate = _dischargeRate.asStateFlow() // En mA

    private val _estimatedHours = MutableStateFlow("--")
    val estimatedHours = _estimatedHours.asStateFlow()

    private val batteryManager = application.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    // Para cálculos más precisos
    private var lastBatteryLevel = 0
    private var lastUpdateTime = System.currentTimeMillis()
    private var currentReadings = mutableListOf<Double>() // Historial de lecturas de corriente
    private var isFirstReading = true

    // Receiver para actualizar automáticamente cuando cambie la batería
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let { updateBatteryStatus(it) }
        }
    }

    private var deviceCapacityMAh: Double = 5000.0

    init {
        // 1. Detectar capacidad real al arrancar
        deviceCapacityMAh = getBatteryCapacity(application)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        application.registerReceiver(batteryReceiver, filter)

        val initialIntent = application.registerReceiver(null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        initialIntent?.let { updateBatteryStatus(it) }

        viewModelScope.launch {
            while (true) {
                updateBatteryStatus()
                delay(5000) // Refresca cada 5 segundos
            }
        }

        viewModelScope.launch {
            val count = db.batteryDao().getRecentHistory().first().size
            Log.d("BatteryVM", "Registros en DB: $count")
        }
    }

    fun updateBatteryStatus(intent: Intent? = null) {
        val batteryPercent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val currentNow = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW).toDouble()
        val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val chargeCounter = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val batteryCapacity = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).toLong()

        _batteryLevel.value = batteryPercent
        _isCharging.value = isCharging

        val absCurrent = Math.abs(currentNow)
        val currentMA = when {
            absCurrent == 0.0 -> 0.0
            absCurrent < 50 -> absCurrent * 1000.0   // Amperios a mA (Tu caso 0.21 -> 210)
            absCurrent > 10000 -> absCurrent / 1000.0 // MicroAmperios a mA
            else -> absCurrent                        // Ya son mA
        }

        if (currentReadings.size >= 20) currentReadings.removeAt(0)
        currentReadings.add(currentMA)
        // --------------------------------------------------

        val avgCurrentMA = if (currentReadings.isNotEmpty()) currentReadings.average() else currentMA
        _dischargeRate.value = avgCurrentMA

        Log.d("BatteryDebug", "Raw: $currentNow | Real: $currentMA mA | Avg: $avgCurrentMA mA | Count: ${currentReadings.size}")

        if (isCharging) {
            handleChargingState(batteryPercent, currentNow.toLong(), chargeCounter, batteryCapacity)
        } else {
            // Ahora sí pasará de 3 después de 1.5 segundos (3 ciclos de 500ms)
            if (currentReadings.size < 3) {
                _remainingTime.value = "Estabilizando..."
            } else {
                handleDischargingState(batteryPercent, avgCurrentMA)
            }
        }

        if (batteryPercent != lastBatteryLevel) {
            saveToDatabase(batteryPercent)
            lastBatteryLevel = batteryPercent
        }
    }

    private fun saveToDatabase(batteryPercent: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.batteryDao().insertEntry(
                    BatteryEntry(
                        level = batteryPercent.toFloat(),
                        timestamp = System.currentTimeMillis(),
                        isPrediction = false
                    )
                )

                // Mantenimiento: Borrar datos de más de 24 horas para que la DB no pese GBs
                val twentyFourHoursAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
                db.batteryDao().deleteOldData(twentyFourHoursAgo)

                Log.d("BatteryVM", "Nivel $batteryPercent% guardado en la base de datos")
            } catch (e: Exception) {
                Log.e("BatteryVM", "Error al guardar en DB: ${e.message}")
            }
        }
    }

    private fun estimateLikeAndroid(percent: Int, avgCurrent: Double, counter: Long, capacity: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            // Miramos las últimas 12 horas para un promedio más estable como AOSP
            val since = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(12)
            val history = db.batteryDao().getHistorySince(since)

            if (history.size >= 5) { // Necesitamos al menos unos puntos para promediar
                val first = history.first()
                val last = history.last()
                val levelDiff = first.level - last.level
                val timeDiffHours = (last.timestamp - first.timestamp).toDouble() / (1000 * 60 * 60)

                if (levelDiff > 0 && timeDiffHours > 0.1) { // Al menos 6 minutos de datos
                    val percentPerHour = levelDiff / timeDiffHours
                    val hoursRemaining = percent / percentPerHour

                    launch(Dispatchers.Main) {
                        updateTimeDisplay(hoursRemaining, "ESTIMADO")
                    }
                } else {
                    launch(Dispatchers.Main) { handleDischargingState(percent, avgCurrent) }
                }
            } else {
                // Si la base de datos es nueva, usamos el cálculo por corriente
                launch(Dispatchers.Main) { handleDischargingState(percent, avgCurrent) }
            }
        }
    }

    private fun handleChargingState(
        batteryPercent: Int,
        currentNow: Long,
        chargeCounter: Long,
        batteryCapacity: Long
    ) {
        val timeToFull = batteryManager.computeChargeTimeRemaining()

        if (timeToFull > 0 && timeToFull < TimeUnit.DAYS.toMillis(7)) { // Máximo 7 días
            val hours = TimeUnit.MILLISECONDS.toHours(timeToFull)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(timeToFull) % 60
            _remainingTime.value = String.format("%02d:%02d\nPARA LLENO", hours, minutes)
            _estimatedHours.value = formatHoursToText(hours + minutes/60.0)
        } else {
            // Cálculo manual
            if (currentNow > 100000 && batteryCapacity > 0 && chargeCounter < batteryCapacity) { // >100mA
                val remainingCharge = batteryCapacity - chargeCounter
                val remainingHours = remainingCharge.toDouble() / (currentNow / 1000.0) // Convertir uA a mA

                if (remainingHours < 168 && remainingHours > 0) { // Máximo 7 días
                    updateTimeDisplay(remainingHours, "PARA LLENO")
                } else {
                    _remainingTime.value = "Cargando..."
                    _estimatedHours.value = "--"
                }
            } else {
                _remainingTime.value = "Cargando..."
                _estimatedHours.value = "--"
            }
        }
    }

    private fun handleDischargingState(batteryPercent: Int, avgCurrentMA: Double) {
        val remainingMAh = (batteryPercent * deviceCapacityMAh) / 100.0

        // Si la corriente es muy baja (p.ej. 0.21mA reales), el tiempo es infinito.
        // Pongamos un suelo mínimo de 10mA para el cálculo si el sensor se queda pegado.
        val safeCurrent = if (avgCurrentMA < 10) 10.0 else avgCurrentMA
        val realisticCurrent = safeCurrent * 0.8

        val hoursRemaining = remainingMAh / realisticCurrent

        if (hoursRemaining > 150) { // Aumentamos el límite
            _remainingTime.value = "Standby\nMuy largo"
            _estimatedHours.value = ">150h"
        } else {
            updateTimeDisplay(hoursRemaining, "HORAS RESTANTES")
        }
    }

    private fun updateTimeDisplay(hoursRemaining: Double, label: String) {
        when {
            hoursRemaining > 48 -> {
                val days = (hoursRemaining / 24).toInt()
                _remainingTime.value = "$days DÍAS\n$label"
                _estimatedHours.value = "${hoursRemaining.format(1)}h"
            }
            hoursRemaining >= 1 -> {
                val h = hoursRemaining.toInt()
                val m = ((hoursRemaining - h) * 60).toInt()
                _remainingTime.value = String.format("%02d:%02d\n$label", h, m)
                _estimatedHours.value = "${hoursRemaining.format(1)}h"
            }
            else -> {
                val minutes = (hoursRemaining * 60).toInt()
                _remainingTime.value = "$minutes MIN\n$label"
                _estimatedHours.value = "${(hoursRemaining * 60).format(0)}min"
            }
        }
    }

    private fun getBatteryCapacity(context: Context): Double {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val chargeCounter = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER).toDouble()
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).toDouble()

        return if (chargeCounter > 0 && level > 0) {
            (chargeCounter / level * 100) / 1000.0
        } else {
            // Si el sensor no responde, intentamos reflexión o valor base
            5000.0
        }
    }

    private fun estimateBasedOnCurrent(batteryPercent: Int, avgCurrentMA: Double) {
        // Estimación basada en corriente promedio típica
        val estimatedHours = when {
            avgCurrentMA < 50 -> batteryPercent * 0.15  // Muy bajo consumo
            avgCurrentMA < 150 -> batteryPercent * 0.12  // Bajo consumo
            avgCurrentMA < 300 -> batteryPercent * 0.08  // Consumo moderado
            avgCurrentMA < 500 -> batteryPercent * 0.05  // Consumo medio
            avgCurrentMA < 1000 -> batteryPercent * 0.03  // Alto consumo
            else -> batteryPercent * 0.02  // Consumo muy alto
        }

        updateTimeDisplay(estimatedHours, "ESTIMADO")

        Log.d("BatteryVM", "Estimado: $avgCurrentMA mA -> ${estimatedHours.format(1)}h")
    }

    private fun estimateBasedOnScenario(batteryPercent: Int) {
        // Estimación cuando no hay datos de corriente válidos
        val estimatedHours = batteryPercent * 0.1  // Estimación conservadora (10% por hora)

        if (estimatedHours < 0.5) {
            _remainingTime.value = "BAJA\nBATERÍA"
            _estimatedHours.value = "<30min"
        } else if (estimatedHours < 24) {
            _remainingTime.value = "${estimatedHours.format(1)} H\nAPROX."
            _estimatedHours.value = "${estimatedHours.format(1)}h"
        } else {
            _remainingTime.value = "1+ DÍAS\nAPROX."
            _estimatedHours.value = ">24h"
        }
    }

    private fun formatHoursToText(hours: Double): String {
        return when {
            hours >= 24 -> String.format("%.1f d", hours/24)
            hours >= 1 -> String.format("%.1f h", hours)
            else -> String.format("%.0f min", hours * 60)
        }
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // Ya estaba desregistrado
        }
    }
}
