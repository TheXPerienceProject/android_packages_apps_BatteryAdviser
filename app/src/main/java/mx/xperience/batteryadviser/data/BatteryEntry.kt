package mx.xperience.batteryadviser.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "battery_history")
data class BatteryEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val level: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isPrediction: Boolean = false
)

@Dao
interface BatteryDao {
    @Query("SELECT * FROM battery_history ORDER BY timestamp DESC LIMIT 100")
    fun getHistory(): Flow<List<BatteryEntry>>

    @Insert
    suspend fun insert(entry: BatteryEntry)
}