package ai.sach.libraryup.ui.screens.settingsScreen.notifications

import ai.sach.libraryup.ui.screens.authentication.EncryptedPrefsUtils
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages notification preferences for the app.
 * Stores and retrieves user preferences for different notification types.
 */
class NotificationPreferencesManager(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "notification_preferences"
        
        // Preference keys
        private const val KEY_MASTER_TOGGLE = "notifications_enabled"
        private const val KEY_RENT_REMINDERS = "rent_reminders_enabled"
        private const val KEY_RENT_REMINDER_DAYS = "rent_reminder_days"
        private const val KEY_PAYMENT_CONFIRMATIONS = "payment_confirmations_enabled"
        private const val KEY_DAILY_OVERDUE_ALERTS = "daily_overdue_alerts_enabled"
        private const val KEY_TENANT_UPDATES = "tenant_updates_enabled"
        private const val KEY_PROPERTY_ALERTS = "property_alerts_enabled"
        private const val KEY_APP_UPDATES = "app_updates_enabled"
        private const val KEY_WHATSAPP_ENABLED = "whatsapp_enabled"
        private const val KEY_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"
        private const val KEY_LAST_SYSTEM_NOTIFICATIONS_ENABLED = "last_system_notifications_enabled"
    }
    
    // Shared Preferences instance - using encrypted preferences for security
    private val sharedPreferences: SharedPreferences = try {
        EncryptedPrefsUtils.createSecurePrefs(
            context = context,
            fileName = PREFS_NAME
        )
    } catch (e: Exception) {
        Log.w("NotificationPrefs", "Failed to create encrypted preferences, using regular SharedPreferences", e)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // State flow for master toggle (enables reactive UI)
    private val _masterToggleFlow = MutableStateFlow(getMasterToggle())
    val masterToggleFlow: Flow<Boolean> = _masterToggleFlow.asStateFlow()
    
    // State flow for rent reminder days
    private val _reminderDaysFlow = MutableStateFlow(getRentReminderDays())
    val reminderDaysFlow: Flow<Int> = _reminderDaysFlow.asStateFlow()

    /**
     * Get master toggle state for all notifications
     */
    fun getMasterToggle(): Boolean {
        return sharedPreferences.getBoolean(KEY_MASTER_TOGGLE, true)
    }
    
    /**
     * Set master toggle state for all notifications
     */
    fun setMasterToggle(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_MASTER_TOGGLE, enabled).apply()
        _masterToggleFlow.value = enabled
    }
    
    /**
     * Check if rent reminders are enabled
     */
    fun areRentRemindersEnabled(): Boolean {
        if (!getMasterToggle()) return false
        return sharedPreferences.getBoolean(KEY_RENT_REMINDERS, true)
    }
    
    /**
     * Set rent reminders enabled state
     */
    fun setRentRemindersEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_RENT_REMINDERS, enabled).apply()
    }
    
    /**
     * Get the number of days before due date to send rent reminders
     */
    fun getRentReminderDays(): Int {
        return sharedPreferences.getInt(KEY_RENT_REMINDER_DAYS, 3)
    }
    
    /**
     * Set the number of days before due date to send rent reminders
     */
    fun setRentReminderDays(days: Int) {
        sharedPreferences.edit().putInt(KEY_RENT_REMINDER_DAYS, days).apply()
        _reminderDaysFlow.value = days
    }
    
    /**
     * Check if payment confirmations are enabled
     */
    fun arePaymentConfirmationsEnabled(): Boolean {
        if (!getMasterToggle()) return false
        return sharedPreferences.getBoolean(KEY_PAYMENT_CONFIRMATIONS, false)
    }
    
    /**
     * Set payment confirmations enabled state
     */
    fun setPaymentConfirmationsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_PAYMENT_CONFIRMATIONS, enabled).apply()
    }

    fun areDailyOverdueAlertsEnabled(): Boolean =
        getMasterToggle() && sharedPreferences.getBoolean(KEY_DAILY_OVERDUE_ALERTS, false)

    fun setDailyOverdueAlertsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_DAILY_OVERDUE_ALERTS, enabled).apply()
    }
    
    /**
     * Check if tenant updates are enabled
     */
    fun areTenantUpdatesEnabled(): Boolean {
        if (!getMasterToggle()) return false
        return sharedPreferences.getBoolean(KEY_TENANT_UPDATES, true)
    }
    
    /**
     * Set tenant updates enabled state
     */
    fun setTenantUpdatesEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_TENANT_UPDATES, enabled).apply()
    }
    
    /**
     * Check if property alerts are enabled
     */
    fun arePropertyAlertsEnabled(): Boolean {
        if (!getMasterToggle()) return false
        return sharedPreferences.getBoolean(KEY_PROPERTY_ALERTS, false)
    }
    
    /**
     * Set property alerts enabled state
     */
    fun setPropertyAlertsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_PROPERTY_ALERTS, enabled).apply()
    }
    
    /**
     * Check if app updates are enabled
     */
    fun areAppUpdatesEnabled(): Boolean {
        if (!getMasterToggle()) return false
        return sharedPreferences.getBoolean(KEY_APP_UPDATES, true)
    }
    
    /**
     * Set app updates enabled state
     */
    fun setAppUpdatesEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_APP_UPDATES, enabled).apply()
    }
    
    /**
     * Check if WhatsApp notifications are enabled
     */
    fun isWhatsAppEnabled(): Boolean {
        if (!getMasterToggle()) return false
        return sharedPreferences.getBoolean(KEY_WHATSAPP_ENABLED, false)
    }
    
    /**
     * Set WhatsApp notifications enabled state
     */
    fun setWhatsAppEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_WHATSAPP_ENABLED, enabled).apply()
    }
    
    /**
     * Reset all notification preferences to default values
     */
    fun resetToDefaults() {
        val editor = sharedPreferences.edit()
        editor.putBoolean(KEY_MASTER_TOGGLE, true)
        editor.putBoolean(KEY_RENT_REMINDERS, true)
        editor.putInt(KEY_RENT_REMINDER_DAYS, 3)
        editor.putBoolean(KEY_PAYMENT_CONFIRMATIONS, false)
        editor.putBoolean(KEY_DAILY_OVERDUE_ALERTS, false)
        editor.putBoolean(KEY_TENANT_UPDATES, true)
        editor.putBoolean(KEY_PROPERTY_ALERTS, false)
        editor.putBoolean(KEY_APP_UPDATES, true)
        editor.putBoolean(KEY_WHATSAPP_ENABLED, false)
        editor.apply()
        
        // Update flows
        _masterToggleFlow.value = true
        _reminderDaysFlow.value = 3
    }

    // --- Onboarding Notification Tracking ---
    private val KEY_SEEN_ONBOARDING_FORM_IDS = "seen_onboarding_form_ids"
    private val KEY_POSTED_ONBOARDING_FORM_IDS = "posted_onboarding_form_ids"

    /**
     * Records that the owner has been told about the submission on [formId].
     *
     * A submission is discovered twice - by the push that announced it, then by the
     * import that follows - and this is what stops the second one notifying again.
     */
    fun markOnboardingNotificationPosted(formId: String) {
        val posted = sharedPreferences.getStringSet(KEY_POSTED_ONBOARDING_FORM_IDS, emptySet()).orEmpty()
        sharedPreferences.edit().putStringSet(KEY_POSTED_ONBOARDING_FORM_IDS, posted + formId).apply()
    }

    /** Whether a notification for [formId] has already been posted. */
    fun wasOnboardingNotificationPosted(formId: String): Boolean =
        sharedPreferences.getStringSet(KEY_POSTED_ONBOARDING_FORM_IDS, emptySet())
            .orEmpty().contains(formId)

    /**
     * Mark an onboarding notification as seen by formId
     */
    fun markOnboardingNotificationSeen(formId: String) {
        val currentSet = sharedPreferences.getStringSet(KEY_SEEN_ONBOARDING_FORM_IDS, emptySet()) ?: emptySet()
        val newSet = currentSet + formId
        sharedPreferences.edit().putStringSet(KEY_SEEN_ONBOARDING_FORM_IDS, newSet).apply()
    }

    /**
     * Check if an onboarding notification for the given formId has already been seen
     */
    fun isOnboardingNotificationSeen(formId: String): Boolean {
        val seenSet = sharedPreferences.getStringSet(KEY_SEEN_ONBOARDING_FORM_IDS, emptySet()) ?: emptySet()
        return seenSet.contains(formId)
    }

    // --- Notification permission request tracking ---
    //
    // The system notification dialog can only be shown a limited number of times; Android
    // marks POST_NOTIFICATIONS permanently denied once "don't ask again" behavior kicks in,
    // after which requestPermission() is a silent no-op. We record that we have asked so the
    // app can route the user to system settings instead of firing a dialog that will never
    // appear.

    /** Whether the system permission dialog has been launched at least once before. */
    fun hasRequestedNotificationPermission(): Boolean =
        sharedPreferences.getBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, false)

    /** Records that the system permission dialog was launched. */
    fun markNotificationPermissionRequested() {
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, true).apply()
    }

    // --- System settings opt-in reconciliation ---
    //
    // The in-app master toggle and the OS-level notification toggle can drift apart: a user
    // who disabled notifications in-app then flips them back on in Android system settings
    // has expressed that they want them again, but the master toggle is still off. We detect
    // that by remembering the last OS state and observing the disabled -> enabled transition,
    // which only happens when the user acts in system settings (turning the master toggle off
    // in-app never changes the OS toggle, so it can't be mistaken for an opt-in).

    /**
     * Whether the OS-level notification state just transitioned from disabled to enabled
     * since the last check, i.e. the user opted back in from system settings.
     *
     * Records [currentlyEnabled] as the new baseline every call. Defaults to enabled on a
     * clean install so the first run never looks like a transition.
     */
    fun systemNotificationsJustEnabled(currentlyEnabled: Boolean): Boolean {
        val previous = sharedPreferences.getBoolean(
            KEY_LAST_SYSTEM_NOTIFICATIONS_ENABLED, true
        )
        sharedPreferences.edit()
            .putBoolean(KEY_LAST_SYSTEM_NOTIFICATIONS_ENABLED, currentlyEnabled)
            .apply()
        return !previous && currentlyEnabled
    }
}
