package com.zhehr.auralis.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zhehr.auralis.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auralis_settings")

data class SavedSession(val ids: List<Long>, val index: Int, val positionMs: Long)

class SettingsStore(private val context: Context) {
    private object K {
        val THEME = stringPreferencesKey("theme_mode")
        val REDUCE = booleanPreferencesKey("reduce_motion")
        val QUEUE = stringPreferencesKey("queue_ids")
        val QUEUE_INDEX = intPreferencesKey("queue_index")
        val QUEUE_POS = longPreferencesKey("queue_pos")
        val SPEED = floatPreferencesKey("speed")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { p ->
        ThemeMode.entries.firstOrNull { it.name == p[K.THEME] } ?: ThemeMode.SYSTEM
    }
    val reduceMotion: Flow<Boolean> = context.dataStore.data.map { it[K.REDUCE] ?: false }

    suspend fun setThemeMode(mode: ThemeMode) { context.dataStore.edit { it[K.THEME] = mode.name } }
    suspend fun setReduceMotion(on: Boolean) { context.dataStore.edit { it[K.REDUCE] = on } }

    suspend fun getSpeed(): Float = context.dataStore.data.first()[K.SPEED] ?: 1f
    suspend fun setSpeed(speed: Float) { context.dataStore.edit { it[K.SPEED] = speed } }

    suspend fun saveSession(ids: List<Long>, index: Int, positionMs: Long) {
        context.dataStore.edit {
            it[K.QUEUE] = ids.joinToString(",")
            it[K.QUEUE_INDEX] = index
            it[K.QUEUE_POS] = positionMs
        }
    }

    suspend fun loadSession(): SavedSession? {
        val p = context.dataStore.data.first()
        val ids = (p[K.QUEUE] ?: return null).split(',').mapNotNull { it.toLongOrNull() }
        if (ids.isEmpty()) return null
        return SavedSession(ids, p[K.QUEUE_INDEX] ?: 0, p[K.QUEUE_POS] ?: 0L)
    }
}
