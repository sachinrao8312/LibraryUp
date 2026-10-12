package ai.sach.libraryup.ui.screens.settingsScreen.appprefernecs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for managing app preferences using DataStore.
 * Handles storage and retrieval of app preferences like theme color, currency settings, etc.
 *
 * @param context Android application context used to access DataStore
 */
class AppPreferencesRepository(private val context: Context) {

    // Define the DataStore at companion object level for singleton access
    companion object {
        // DataStore name
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

        // Preference keys
        private val PRIMARY_COLOR_KEY = stringPreferencesKey("primary_color")
        private val CURRENCY_KEY = stringPreferencesKey("currency")
        private val COUNTRY_KEY = stringPreferencesKey("country")
        private val SHOW_PROPERTIES_KEY = booleanPreferencesKey("show_properties")
        private val INSTALL_DATE_KEY = stringPreferencesKey("install_date")

        // Default values
        const val DEFAULT_PRIMARY_COLOR = "#0F9B4B"  // Default RentUp Green
        const val DEFAULT_CURRENCY = "INR"
        const val DEFAULT_COUNTRY = "IN"
        const val DEFAULT_SHOW_PROPERTIES = true
    }

    /**
     * Get the current primary color as a Flow.
     * This Flow will emit a new value whenever the setting changes.
     */
    val primaryColor: Flow<String> = context.dataStore.data
        .map { preferences ->
            // Return the current value or default if not set
            preferences[PRIMARY_COLOR_KEY] ?: DEFAULT_PRIMARY_COLOR
        }

    /**
     * Update the primary color setting.
     *
     * @param colorHex The hex color code to set as the primary color
     */
    suspend fun setPrimaryColor(colorHex: String) {
        context.dataStore.edit { preferences ->
            preferences[PRIMARY_COLOR_KEY] = colorHex
        }
    }

    /**
     * Get the current currency as a Flow.
     * This Flow will emit a new value whenever the setting changes.
     */
    val currency: Flow<String> = context.dataStore.data
        .map { preferences ->
            // Return the current value or default if not set
            preferences[CURRENCY_KEY] ?: DEFAULT_CURRENCY
        }

    /**
     * Update the currency setting.
     *
     * @param currency The currency code to set
     */
    suspend fun setCurrency(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[CURRENCY_KEY] = currency
        }
    }

    /**
     * Get the current country as a Flow.
     * This Flow will emit a new value whenever the setting changes.
     */
    val country: Flow<String> = context.dataStore.data
        .map { preferences ->
            // Return the current value or default if not set
            preferences[COUNTRY_KEY] ?: DEFAULT_COUNTRY
        }

    /**
     * Update the country setting.
     *
     * @param country The country code to set
     */
    suspend fun setCountry(country: String) {
        context.dataStore.edit { preferences ->
            preferences[COUNTRY_KEY] = country
        }
    }

    /**
     * Get whether to show the Properties screen in navigation as a Flow.
     * This Flow will emit a new value whenever the setting changes.
     */
    val showProperties: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            // Return the current value or default if not set
            preferences[SHOW_PROPERTIES_KEY] ?: DEFAULT_SHOW_PROPERTIES
        }

    /**
     * Get the app install date as a Flow (timestamp string or null if not set).
     */
    val installDate: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[INSTALL_DATE_KEY]
        }

    /**
     * Set the app install date (should only be called once).
     */
    suspend fun setInstallDate(installDate: String) {
        context.dataStore.edit { preferences ->
            preferences[INSTALL_DATE_KEY] = installDate
        }
    }

    /**
     * Update the show Properties setting.
     *
     * @param show Boolean indicating whether to show the Properties screen
     */
    suspend fun setShowProperties(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_PROPERTIES_KEY] = show
        }
    }
}