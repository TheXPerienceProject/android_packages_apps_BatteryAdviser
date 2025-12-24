package mx.xperience.batteryadviser.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Database entity representing a single battery status snapshot.
 *
 * @property id Unique identifier for the database record.
 * @property level Battery percentage at the time of recording (0-100).
 * @property timestamp Epoch time in milliseconds when the entry was created.
 * @property isPrediction Flag to distinguish between real historical data and simulated points.
 */
@Entity(tableName = "battery_history")
data class BatteryEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val level: Float,
    val timestamp: Long, // Hora exacta del registro
    val isPrediction: Boolean = false
)

@Dao
interface BatteryDao {
    /**
     * Persists a new battery snapshot.
     */
    @Insert
    suspend fun insertEntry(entry: BatteryEntry)

    /**
     * Specifically used for the main chart to get recent telemetry.
     */
    @Query("SELECT * FROM battery_history ORDER BY timestamp DESC LIMIT 24")
    fun getRecentHistory(): Flow<List<BatteryEntry>>

    /**
     * Maintenance method to prevent the database from growing indefinitely.
     * @param threshold Timestamp before which data should be purged.
     */
    @Query("DELETE FROM battery_history WHERE timestamp < :threshold")
    suspend fun deleteOldData(threshold: Long)

    /**
     * Retrieves the most recent battery entries for general history views.
     * Returns a Flow to provide real-time updates to the UI.
     */
    @Query("SELECT * FROM battery_history WHERE timestamp >= :sinceTime ORDER BY timestamp ASC")
    suspend fun getHistorySince(sinceTime: Long): List<BatteryEntry>

    /**
     * Calculates the average hour of the day when the battery level increases significantly.
     * This is used by the predictive engine to determine the user's "Usual Charge Time".
     * Returns the hour in 24h format (e.g., 22.5 for 10:30 PM).
     */
    @Query("""
    SELECT AVG(CAST(strftime('%H', timestamp / 1000, 'unixepoch', 'localtime') AS INTEGER)) 
    FROM battery_history 
    WHERE level < 30 OR level > 90 
    LIMIT 100
""")
    suspend fun getAverageChargeHour(): Double?
}