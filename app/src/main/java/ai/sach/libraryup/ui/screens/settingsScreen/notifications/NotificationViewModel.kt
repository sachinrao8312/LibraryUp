package ai.sach.libraryup.ui.screens.settingsScreen.notifications

import ai.sach.libraryup.data.local.entity.Tenant
import ai.sach.libraryup.data.repository.PropertyRepository
import ai.sach.libraryup.data.repository.TenantRepository
import ai.sach.libraryup.service.FcmTokenSyncWorker
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel to manage notification settings and operations.
 * Connects the UI with the notification preferences and helper.
 */
class NotificationViewModel(
    application: Application,
    private val tenantRepository: TenantRepository,
    private val propertyRepository: PropertyRepository
) : AndroidViewModel(application) {
    
    // Notification helper
    private val notificationHelper = NotificationHelper(application)
    
    // Notification preferences manager
    private val preferencesManager = NotificationPreferencesManager(application)
    
    // Owns the single daily reminder sweep
    private val reminderScheduler = ReminderScheduler(application)
    
    // LiveData for UI state
    private val _masterToggle = MutableLiveData(preferencesManager.getMasterToggle())
    val masterToggle: LiveData<Boolean> = _masterToggle
    
    private val _rentRemindersEnabled = MutableLiveData(preferencesManager.areRentRemindersEnabled())
    val rentRemindersEnabled: LiveData<Boolean> = _rentRemindersEnabled
    
    private val _reminderDays = MutableLiveData(preferencesManager.getRentReminderDays())
    val reminderDays: LiveData<Int> = _reminderDays
    
    private val _paymentConfirmationsEnabled = MutableLiveData(preferencesManager.arePaymentConfirmationsEnabled())
    val paymentConfirmationsEnabled: LiveData<Boolean> = _paymentConfirmationsEnabled
    private val _dailyOverdueAlertsEnabled = MutableLiveData(preferencesManager.areDailyOverdueAlertsEnabled())
    val dailyOverdueAlertsEnabled: LiveData<Boolean> = _dailyOverdueAlertsEnabled
    
    private val _tenantUpdatesEnabled = MutableLiveData(preferencesManager.areTenantUpdatesEnabled())
    val tenantUpdatesEnabled: LiveData<Boolean> = _tenantUpdatesEnabled
    
    private val _propertyAlertsEnabled = MutableLiveData(preferencesManager.arePropertyAlertsEnabled())
    val propertyAlertsEnabled: LiveData<Boolean> = _propertyAlertsEnabled
    
    private val _appUpdatesEnabled = MutableLiveData(preferencesManager.areAppUpdatesEnabled())
    val appUpdatesEnabled: LiveData<Boolean> = _appUpdatesEnabled
    
    private val _whatsAppEnabled = MutableLiveData(preferencesManager.isWhatsAppEnabled())
    val whatsAppEnabled: LiveData<Boolean> = _whatsAppEnabled
    
    private val _notificationsPermissionGranted = MutableLiveData(notificationHelper.areNotificationsEnabled())
    val notificationsPermissionGranted: LiveData<Boolean> = _notificationsPermissionGranted
    
    init {
        // Create notification channels on initialization
        notificationHelper.createNotificationChannels()
        
        // Observe changes from preferences through flows
        viewModelScope.launch {
            preferencesManager.masterToggleFlow.collect { enabled ->
                _masterToggle.value = enabled
                refreshAllSettings()
            }
        }
        
        viewModelScope.launch {
            preferencesManager.reminderDaysFlow.collect { days ->
                _reminderDays.value = days
                // No reschedule needed: the daily sweep reads this preference at run time.
            }
        }
    }
    
    /**
     * Refreshes all notification settings from the preferences
     */
    private fun refreshAllSettings() {
        _rentRemindersEnabled.value = preferencesManager.areRentRemindersEnabled()
        _paymentConfirmationsEnabled.value = preferencesManager.arePaymentConfirmationsEnabled()
        _dailyOverdueAlertsEnabled.value = preferencesManager.areDailyOverdueAlertsEnabled()
        _tenantUpdatesEnabled.value = preferencesManager.areTenantUpdatesEnabled()
        _propertyAlertsEnabled.value = preferencesManager.arePropertyAlertsEnabled()
        _appUpdatesEnabled.value = preferencesManager.areAppUpdatesEnabled()
        _whatsAppEnabled.value = preferencesManager.isWhatsAppEnabled()
    }
    
    /**
     * Updates the master toggle for all notifications
     */
    fun setMasterToggle(enabled: Boolean) {
        preferencesManager.setMasterToggle(enabled)
        _masterToggle.value = enabled
        refreshAllSettings()

        // Republish the token so users/{uid}.notificationsEnabled reflects the opt-in;
        // the backend skips addressing this device while it is off.
        FcmTokenSyncWorker.enqueue(getApplication(), force = true)

        // If enabling notifications, schedule rent reminders
        if (enabled) {
            scheduleRentReminders()
        }
    }
    
    /**
     * Updates the rent reminders enabled state
     */
    fun setRentRemindersEnabled(enabled: Boolean) {
        preferencesManager.setRentRemindersEnabled(enabled)
        _rentRemindersEnabled.value = enabled
        
        // Schedule or cancel rent reminders based on new setting
        if (enabled) {
            scheduleRentReminders()
        } else {
            cancelRentReminders()
        }
    }
    
    /**
     * Updates the number of days before due date to send rent reminders
     */
    fun setReminderDays(days: Int) {
        preferencesManager.setRentReminderDays(days)
        _reminderDays.value = days
    }
    
    /**
     * Updates the payment confirmations enabled state
     */
    fun setPaymentConfirmationsEnabled(enabled: Boolean) {
        preferencesManager.setPaymentConfirmationsEnabled(enabled)
        _paymentConfirmationsEnabled.value = enabled
    }

    fun setDailyOverdueAlertsEnabled(enabled: Boolean) {
        preferencesManager.setDailyOverdueAlertsEnabled(enabled)
        _dailyOverdueAlertsEnabled.value = enabled
    }
    
    /**
     * Updates the tenant updates enabled state
     */
    fun setTenantUpdatesEnabled(enabled: Boolean) {
        preferencesManager.setTenantUpdatesEnabled(enabled)
        _tenantUpdatesEnabled.value = enabled
    }
    
    /**
     * Updates the property alerts enabled state
     */
    fun setPropertyAlertsEnabled(enabled: Boolean) {
        preferencesManager.setPropertyAlertsEnabled(enabled)
        _propertyAlertsEnabled.value = enabled
    }
    
    /**
     * Updates the app updates enabled state
     */
    fun setAppUpdatesEnabled(enabled: Boolean) {
        preferencesManager.setAppUpdatesEnabled(enabled)
        _appUpdatesEnabled.value = enabled
    }
    
    /**
     * Updates the WhatsApp notifications enabled state
     */
    fun setWhatsAppEnabled(enabled: Boolean) {
        preferencesManager.setWhatsAppEnabled(enabled)
        _whatsAppEnabled.value = enabled
    }
    
    /**
     * Refreshes the notification permission status
     */
    fun refreshPermissionStatus() {
        _notificationsPermissionGranted.value = notificationHelper.areNotificationsEnabled()
    }

    /** Whether the system permission dialog has been requested before (see prefs notes). */
    fun hasRequestedNotificationPermission(): Boolean =
        preferencesManager.hasRequestedNotificationPermission()

    /** Records that the system permission dialog was launched. */
    fun markNotificationPermissionRequested() {
        preferencesManager.markNotificationPermissionRequested()
    }
    
    /**
     * Schedules rent reminder notifications for all unpaid tenants
     */
    fun scheduleRentReminders() {
        if (!preferencesManager.areRentRemindersEnabled()) {
            cancelRentReminders()
            return
        }
        // Nothing per-tenant is scheduled and nothing is sent here. The daily sweep reads
        // live tenant rows when it runs, so changing a setting only needs to ensure it exists.
        reminderScheduler.ensureScheduled()
    }
    
    /**
     * Stops the daily rent reminder sweep
     */
    private fun cancelRentReminders() {
        reminderScheduler.cancel()
    }
    
    /**
     * Sends a test rent reminder notification for testing purposes
     */
    fun sendTestRentReminder() {
        viewModelScope.launch {
            try {
                // Get the first tenant for testing
                val tenants = tenantRepository.allTenantsWithProperty
                    .first()
                    .map { it.tenant }
                
                if (tenants.isNotEmpty()) {
                    val tenant = tenants.first()
                    notificationHelper.sendRentReminderNotification(
                        tenant = tenant,
                        daysUntilDue = preferencesManager.getRentReminderDays()
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Sends a test payment confirmation notification
     */
    fun sendTestPaymentConfirmation() {
        viewModelScope.launch {
            try {
                // Get the first tenant for testing
                val tenants = tenantRepository.allTenantsWithProperty
                    .first()
                    .map { it.tenant }
                
                if (tenants.isNotEmpty()) {
                    val tenant = tenants.first()
                    notificationHelper.sendPaymentConfirmationNotification(
                        tenant = tenant,
                        amount = tenant.amount ?: "$500"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Sends a payment confirmation notification for a specific tenant
     */
    fun sendPaymentConfirmation(tenant: Tenant, amount: String) {
        if (!preferencesManager.arePaymentConfirmationsEnabled()) return
        
        notificationHelper.sendPaymentConfirmationNotification(tenant, amount)
    }
    
    /**
     * Sends a tenant update notification
     */
    fun sendTenantUpdateNotification(title: String, message: String, tenantId: Long? = null) {
        if (!preferencesManager.areTenantUpdatesEnabled()) return
        
        notificationHelper.sendTenantUpdateNotification(title, message, tenantId)
    }
    
}
