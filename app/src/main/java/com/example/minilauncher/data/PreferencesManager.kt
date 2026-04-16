package com.example.minilauncher.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "minilauncher_preferences")

class PreferencesManager private constructor(
    private val context: Context
) {
    val pinnedApps: Flow<Set<String>> = context.dataStore.data
        .catchPreferences()
        .map { preferences -> (preferences[PINNED_APPS] ?: emptySet()).take(MAX_PINNED_APPS).toSet() }

    val hiddenApps: Flow<Set<String>> = context.dataStore.data
        .catchPreferences()
        .map { preferences -> preferences[HIDDEN_APPS] ?: emptySet() }

    val hiddenUsageApps: Flow<Set<String>> = context.dataStore.data
        .catchPreferences()
        .map { preferences -> preferences[HIDDEN_USAGE_APPS] ?: emptySet() }

    val showIcons: Flow<Boolean> = context.dataStore.data
        .catchPreferences()
        .map { preferences -> preferences[SHOW_ICONS] ?: true }

    val preventDeletion: Flow<Boolean> = context.dataStore.data
        .catchPreferences()
        .map { preferences -> preferences[PREVENT_DELETION] ?: false }

    val requirePasswordToHide: Flow<Boolean> = context.dataStore.data
        .catchPreferences()
        .map { preferences -> preferences[REQUIRE_PASSWORD_TO_HIDE] ?: false }

    val weekStartDay: Flow<Int> = context.dataStore.data
        .catchPreferences()
        .map { preferences -> preferences[WEEK_START_DAY] ?: Calendar.SUNDAY }

    val passwordHash: Flow<String> = context.dataStore.data
        .catchPreferences()
        .map { preferences -> preferences[PASSWORD_HASH] ?: "" }

    val hasSeenLauncherPrompt: Flow<Boolean> = context.dataStore.data
        .catchPreferences()
        .map { preferences -> preferences[HAS_SEEN_LAUNCHER_PROMPT] ?: false }

    suspend fun addPinnedApp(packageName: String): Boolean {
        var added = false
        context.dataStore.edit { preferences ->
            val current = (preferences[PINNED_APPS] ?: emptySet()).take(MAX_PINNED_APPS).toMutableSet()
            when {
                packageName in current -> added = true
                current.size < MAX_PINNED_APPS -> {
                    current += packageName
                    preferences[PINNED_APPS] = current
                    added = true
                }
                else -> added = false
            }
        }
        return added
    }

    suspend fun removePinnedApp(packageName: String) {
        context.dataStore.edit { preferences ->
            preferences[PINNED_APPS] = (preferences[PINNED_APPS] ?: emptySet()) - packageName
        }
    }

    suspend fun setHiddenApp(packageName: String, hidden: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HIDDEN_APPS] = preferences[HIDDEN_APPS].mutated(packageName, hidden)
        }
    }

    suspend fun setHiddenUsageApp(packageName: String, hidden: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HIDDEN_USAGE_APPS] = preferences[HIDDEN_USAGE_APPS].mutated(packageName, hidden)
        }
    }

    suspend fun setShowIcons(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_ICONS] = enabled
        }
    }

    suspend fun setPreventDeletion(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PREVENT_DELETION] = enabled
        }
    }

    suspend fun setRequirePasswordToHide(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[REQUIRE_PASSWORD_TO_HIDE] = enabled
        }
    }

    suspend fun setWeekStartDay(day: Int) {
        context.dataStore.edit { preferences ->
            preferences[WEEK_START_DAY] = day
        }
    }

    suspend fun setPasswordHash(hash: String) {
        context.dataStore.edit { preferences ->
            preferences[PASSWORD_HASH] = hash
        }
    }

    suspend fun setHasSeenLauncherPrompt(seen: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAS_SEEN_LAUNCHER_PROMPT] = seen
        }
    }

    private fun Flow<Preferences>.catchPreferences(): Flow<Preferences> {
        return catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
    }

    private fun Set<String>?.mutated(packageName: String, include: Boolean): Set<String> {
        val updated = (this ?: emptySet()).toMutableSet()
        if (include) {
            updated += packageName
        } else {
            updated -= packageName
        }
        return updated
    }

    companion object {
        private const val MAX_PINNED_APPS = 10
        private val PINNED_APPS = stringSetPreferencesKey("pinned_apps")
        private val HIDDEN_APPS = stringSetPreferencesKey("hidden_apps")
        private val HIDDEN_USAGE_APPS = stringSetPreferencesKey("hidden_usage_apps")
        private val SHOW_ICONS = booleanPreferencesKey("show_icons")
        private val PREVENT_DELETION = booleanPreferencesKey("prevent_deletion")
        private val REQUIRE_PASSWORD_TO_HIDE = booleanPreferencesKey("require_password_to_hide")
        private val WEEK_START_DAY = intPreferencesKey("week_start_day")
        private val PASSWORD_HASH = stringPreferencesKey("password_hash")
        private val HAS_SEEN_LAUNCHER_PROMPT = booleanPreferencesKey("has_seen_launcher_prompt")

        @Volatile
        private var instance: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
