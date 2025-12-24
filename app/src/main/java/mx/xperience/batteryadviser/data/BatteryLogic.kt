package mx.xperience.batteryadviser.data

object BatteryLogic {
    // La escala que arreglamos para tus Amperios/mA
    fun getRealCurrentMA(rawCurrent: Double): Double {
        val absCurrent = Math.abs(rawCurrent)
        return when {
            absCurrent == 0.0 -> 0.0
            absCurrent < 50 -> absCurrent * 1000.0   // Caso 0.21 -> 210mA
            absCurrent > 10000 -> absCurrent / 1000.0 // MicroAmperios
            else -> absCurrent
        }
    }

    // El cálculo de horas restantes
    fun calculateHoursRemaining(percent: Int, avgCurrentMA: Double, capacity: Double): Double {
        val remainingMAh = (percent * capacity) / 100.0
        val safeCurrent = if (avgCurrentMA < 10.0) 10.0 else avgCurrentMA
        return remainingMAh / (safeCurrent * 0.8) // 0.8 de factor de salud
    }
}