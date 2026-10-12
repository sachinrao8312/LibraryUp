package ai.sach.rentup.ui.screens.settingsScreen.notifications

import ai.sach.rentup.data.repository.PropertyRepository
import ai.sach.rentup.data.repository.TenantRepository
import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory for creating NotificationViewModel instances with the required dependencies.
 */
class NotificationViewModelFactory(
    private val application: Application,
    private val tenantRepository: TenantRepository,
    private val propertyRepository: PropertyRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
            return NotificationViewModel(application, tenantRepository, propertyRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}