package mx.xperience.batteryadviser.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "battery_history")
data class BatteryEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val level: Float,
    val timestamp: Long, // Hora exacta del registro
    val isPrediction: Boolean = false
)

@Dao
interface BatteryDao {
    @Insert
    suspend fun insertEntry(entry: BatteryEntry)

    // Obtenemos los últimos 24 registros para la gráfica
    @Query("SELECT * FROM battery_history ORDER BY timestamp DESC LIMIT 24")
    fun getRecentHistory(): Flow<List<BatteryEntry>>

    @Query("DELETE FROM battery_history WHERE timestamp < :threshold")
    suspend fun deleteOldData(threshold: Long)

    @Query("SELECT * FROM battery_history WHERE timestamp >= :sinceTime ORDER BY timestamp ASC")
    suspend fun getHistorySince(sinceTime: Long): List<BatteryEntry>

    @Query("""
    SELECT AVG(CAST(strftime('%H', timestamp / 1000, 'unixepoch', 'localtime') AS INTEGER)) 
    FROM battery_history 
    WHERE level < 30 OR level > 90 
    LIMIT 100
""")
    suspend fun getAverageChargeHour(): Double?
}