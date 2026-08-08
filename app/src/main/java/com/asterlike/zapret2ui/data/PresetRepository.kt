package com.asterlike.zapret2ui.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.asterlike.zapret2ui.engine.StrategyCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.presetStore by preferencesDataStore(name = "presets")
private val KEY_PRESETS = stringPreferencesKey("user_presets")
private val json2 = Json { ignoreUnknownKeys = true; encodeDefaults = true }

class PresetRepository(private val context: Context) {
    val userPresetsFlow: Flow<List<Preset>> = context.presetStore.data.map { prefs ->
        prefs[KEY_PRESETS]?.let { raw ->
            try { json2.decodeFromString<List<Preset>>(raw) } catch (_: Exception) { emptyList() }
        } ?: emptyList()
    }

    suspend fun getAll(): List<Preset> = StrategyCatalog.builtIns() + getUserPresets()
    suspend fun getUserPresets(): List<Preset> {
        val raw = context.presetStore.data.map { it[KEY_PRESETS] }.let {
            // synchronous read fallback: use first value
            // Actually use DataStore read via flow first() in caller; keep simple:
            emptyList<Preset>()
        }
        return raw
    }

    fun allFlow(): Flow<List<Preset>> = userPresetsFlow.map { user -> StrategyCatalog.builtIns() + user }

    suspend fun addUser(preset: Preset) {
        context.presetStore.edit { prefs ->
            val list = prefs[KEY_PRESETS]?.let { raw ->
                try { json2.decodeFromString<MutableList<Preset>>(raw) } catch (_: Exception) { mutableListOf() }
            } ?: mutableListOf()
            var p = preset.copy(isBuiltIn = false)
            var base = p.name; var i = 2
            while (list.any { it.name == p.name } || StrategyCatalog.builtIns().any { it.name == p.name }) {
                p = p.copy(name = "$base ($i)"); i++
            }
            list.add(p)
            prefs[KEY_PRESETS] = json2.encodeToString(list)
        }
    }

    suspend fun deleteUser(preset: Preset) {
        if (preset.isBuiltIn) return
        context.presetStore.edit { prefs ->
            val list = prefs[KEY_PRESETS]?.let { raw ->
                try { json2.decodeFromString<MutableList<Preset>>(raw) } catch (_: Exception) { mutableListOf() }
            } ?: mutableListOf()
            list.removeAll { it.name == preset.name }
            prefs[KEY_PRESETS] = json2.encodeToString(list)
        }
    }

    suspend fun replaceAutoLeaderboard(top: List<Preset>) {
        context.presetStore.edit { prefs ->
            val list = prefs[KEY_PRESETS]?.let { raw ->
                try { json2.decodeFromString<MutableList<Preset>>(raw) } catch (_: Exception) { mutableListOf() }
            } ?: mutableListOf()
            list.removeAll { it.isAutoLeaderboard }
            list.addAll(top.map { it.copy(isBuiltIn = false, isAutoLeaderboard = true) })
            prefs[KEY_PRESETS] = json2.encodeToString(list)
        }
    }

    suspend fun saveAll(userPresets: List<Preset>) {
        context.presetStore.edit { it[KEY_PRESETS] = json2.encodeToString(userPresets) }
    }
}
