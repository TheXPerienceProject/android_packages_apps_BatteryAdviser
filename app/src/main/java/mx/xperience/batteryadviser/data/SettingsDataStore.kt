package mx.xperience.batteryadviser.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {
    companion object {
        val NOTIFY_CHARGE_KEY = booleanPreferencesKey("notify_charge_time")
    }

    val notifyChargeEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[NOTIFY_CHARGE_KEY] ?: true } // true por defecto

    suspend fun saveNotifyCharge(enabled: Boolean) {
        context.dataStore.edit { it[NOTIFY_CHARGE_KEY] = enabled }
    }
}