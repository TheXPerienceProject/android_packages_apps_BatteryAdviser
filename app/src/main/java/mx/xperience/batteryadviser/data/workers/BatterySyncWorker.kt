package mx.xperience.batteryadviser.data.workers

import android.content.Context
import android.os.BatteryManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import mx.xperience.batteryadviser.data.db.BatteryDatabase
import mx.xperience.batteryadviser.data.db.BatteryEntry

class BatterySyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val db = BatteryDatabase.getDatabase(applicationContext)
        val bm = applicationContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).toFloat()

        // Guardamos el registro actual
        db.batteryDao().insertEntry(
            BatteryEntry(
                level = level,
                timestamp = System.currentTimeMillis(),
                isPrediction = false
            )
        )

        return Result.success()
    }
}