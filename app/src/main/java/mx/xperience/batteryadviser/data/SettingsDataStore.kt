/*
 * Copyright (C) 2025 The XPerience Project
 * SPDX-License-Identifier: Apache-2.0
 */

package mx.xperience.batteryadviser.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Global extension property to provide a single DataStore instance across the app.
 */
private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * Manages persistent user preferences using Jetpack DataStore.
 * Optimized for lightweight key-value pairs with asynchronous updates via Coroutines and Flow.
 */
class SettingsDataStore(private val context: Context) {

    companion object {
        /** Key for the smart charge reminder notification setting. */
        val NOTIFY_CHARGE_KEY = booleanPreferencesKey("notify_charge_time")
    }

    /**
     * Observable stream for the charge notification toggle.
     * Defaults to 'true' to ensure users receive helpful battery alerts by default.
     */
    val notifyChargeEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[NOTIFY_CHARGE_KEY] ?: true }

    /**
     * Persists the user's preference for charge notifications.
     * @param enabled Set to true to enable predictive alerts.
     */
    suspend fun saveNotifyCharge(enabled: Boolean) {
        context.dataStore.edit { it[NOTIFY_CHARGE_KEY] = enabled }
    }
}