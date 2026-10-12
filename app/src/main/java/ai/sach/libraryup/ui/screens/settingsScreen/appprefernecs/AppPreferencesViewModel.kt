package ai.sach.libraryup.ui.screens.settingsScreen.appprefernecs

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the App Preferences screen.
 * Provides access to app preference data and methods to update preferences.
 *
 * @param repository The repository for managing app preferences
 */
class AppPreferencesViewModel(private val repository: AppPreferencesRepository) : ViewModel() {

    /**
     * StateFlow that emits the current primary color as a hex string.
     * Collects from the repository and caches the latest value.
     */
    val primaryColor: StateFlow<String> = repository.primaryColor
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Keeps active for 5 seconds after last subscriber
            initialValue = AppPreferencesRepository.DEFAULT_PRIMARY_COLOR
        )

    /**
     * StateFlow that emits the current currency code.
     * Collects from the repository and caches the latest value.
     */
    val currency: StateFlow<String> = repository.currency
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppPreferencesRepository.DEFAULT_CURRENCY
        )

    /**
     * StateFlow that emits the current country code.
     * Collects from the repository and caches the latest value.
     */
    val country: StateFlow<String> = repository.country
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppPreferencesRepository.DEFAULT_COUNTRY
        )

    /**
     * StateFlow that emits whether to show the Properties screen in navigation.
     * Collects from the repository and caches the latest value.
     */
    val showProperties: StateFlow<Boolean> = repository.showProperties
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppPreferencesRepository.DEFAULT_SHOW_PROPERTIES
        )

    /**
     * Update the primary color setting.
     *
     * @param colorHex The hex color code to set as the primary color
     */
    suspend fun setPrimaryColor(colorHex: String) {
        repository.setPrimaryColor(colorHex)
    }

    /**
     * Update the currency setting.
     *
     * @param currency The currency code to set
     */
    suspend fun setCurrency(currency: String) {
        repository.setCurrency(currency)
    }

    /**
     * Update the country setting.
     *
     * @param country The country code to set
     */
    suspend fun setCountry(country: String) {
        repository.setCountry(country)
    }

    /**
     * Update the show Properties setting.
     *
     * @param show Boolean indicating whether to show the Properties screen
     */
    suspend fun setShowProperties(show: Boolean) {
        repository.setShowProperties(show)
    }
}

/**
 * Factory for creating AppPreferencesViewModel instances with the required repository dependency.
 *
 * @param context Android application context used to create the repository
 */
class AppPreferencesViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppPreferencesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppPreferencesViewModel(AppPreferencesRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}