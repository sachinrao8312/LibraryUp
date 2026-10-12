package ai.sach.libraryup.ui.screens.settingsScreen

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository to manage theme preferences using DataStore.
 * This handles saving and retrieving the dark mode setting.
 *
 * @param context Android application context used to access DataStore
 */
class ThemePreferencesRepository(private val context: Context) {

    // Create a DataStore for storing preferences
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    }

    /**
     * Get the current dark mode setting as a Flow.
     * This Flow will emit a new value whenever the setting changes.
     */
    val isDarkMode: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            // Return the current value or false if not set
            preferences[DARK_MODE_KEY] ?: false
        }

    /**
     * Update the dark mode setting.
     *
     * @param isDarkMode Boolean indicating whether dark mode should be enabled
     */
    suspend fun setDarkMode(isDarkMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = isDarkMode
        }
    }
}