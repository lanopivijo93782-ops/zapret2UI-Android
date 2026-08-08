package com.asterlike.zapret2ui.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "settings")
private val KEY_SETTINGS = stringPreferencesKey("app_settings_json")
private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

class SettingsRepository(private val context: Context) {
    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        prefs[KEY_SETTINGS]?.let { raw ->
            try { json.decodeFromString<AppSettings>(raw) } catch (_: Exception) { AppSettings() }
        } ?: AppSettings()
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_SETTINGS]?.let { raw ->
                try { json.decodeFromString<AppSettings>(raw) } catch (_: Exception) { AppSettings() }
            } ?: AppSettings()
            prefs[KEY_SETTINGS] = json.encodeToString(transform(current))
        }
    }

    suspend fun save(settings: AppSettings) {
        context.dataStore.edit { it[KEY_SETTINGS] = json.encodeToString(settings) }
    }
}
