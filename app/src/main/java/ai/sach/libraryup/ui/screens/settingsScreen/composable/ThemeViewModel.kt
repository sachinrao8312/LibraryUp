package ai.sach.libraryup.ui.screens.settingsScreen.composable

import ai.sach.libraryup.ui.screens.settingsScreen.ThemePreferencesRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel that handles theme-related UI state.
 * Provides functionality to toggle between light and dark mode.
 *
 * @param themeRepository Repository that manages theme preferences
 */
class ThemeViewModel(private val themeRepository: ThemePreferencesRepository) : ViewModel() {

    /**
     * StateFlow that emits whether dark mode is enabled.
     * Collects from the repository and caches the latest value.
     */
    val isDarkMode: StateFlow<Boolean> = themeRepository.isDarkMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Keeps active for 5 seconds after last subscriber
            initialValue = false // Default to light mode
        )

    /**
     * Toggle the dark mode setting.
     */
    fun toggleDarkMode() {
        viewModelScope.launch {
            themeRepository.setDarkMode(!isDarkMode.value)
        }
    }

    /**
     * Set dark mode to a specific value.
     *
     * @param isDarkMode Boolean indicating whether dark mode should be enabled
     */
    fun setDarkMode(isDarkMode: Boolean) {
        viewModelScope.launch {
            themeRepository.setDarkMode(isDarkMode)
        }
    }
}

/**
 * Factory for creating ThemeViewModel instances with the required repository dependency.
 */
class ThemeViewModelFactory(private val themeRepository: ThemePreferencesRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ThemeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ThemeViewModel(themeRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}