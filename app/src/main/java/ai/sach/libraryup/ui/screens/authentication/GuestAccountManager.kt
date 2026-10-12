package ai.sach.libraryup.ui.screens.authentication

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.sach.libraryup.ui.viewmodel.PropertyViewModel
import ai.sach.libraryup.data.viewmodel.TenantViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Smart trigger system for guest account conversion
 * Shows conversion prompts based on user engagement and data value
 */
class GuestAccountManager(private val context: Context) {
    
    companion object {
        private const val TAG = "GuestAccountManager"
        private const val PREFS_NAME = "guest_account_manager"
        private const val KEY_GUEST_CREATED_DATE = "guest_created_date"
        private const val KEY_LAST_CONVERSION_PROMPT = "last_conversion_prompt"
        private const val KEY_CONVERSION_PROMPT_COUNT = "conversion_prompt_count"
        private const val KEY_USER_ENGAGEMENT_SCORE = "user_engagement_score"
        
        // Conversion trigger thresholds
        private const val PROPERTIES_THRESHOLD = 2
        private const val TENANTS_THRESHOLD = 3
        private const val USAGE_SESSIONS_THRESHOLD = 5
        private const val DAYS_GENTLE_PROMPT = 7
        private const val DAYS_DATA_WARNING = 30
        private const val DAYS_MANDATORY = 60
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Track when guest account was created
     */
    fun trackGuestCreation() {
        if (!prefs.contains(KEY_GUEST_CREATED_DATE)) {
            prefs.edit()
                .putLong(KEY_GUEST_CREATED_DATE, System.currentTimeMillis())
                .apply()
            Log.d(TAG, "Guest account creation date tracked")
        }
    }
    
    /**
     * Get number of days since guest account creation
     */
    fun getDaysAsGuest(): Int {
        val createdDate = prefs.getLong(KEY_GUEST_CREATED_DATE, System.currentTimeMillis())
        val daysDiff = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - createdDate)
        return daysDiff.toInt()
    }
    
    /**
     * Calculate user engagement score based on app usage
     */
    fun calculateEngagementScore(propertiesCount: Int, tenantsCount: Int, usageSessions: Int): Int {
        return (propertiesCount * 10) + (tenantsCount * 5) + usageSessions
    }
    
    /**
     * Check if user has valuable data worth backing up
     */
    fun hasValuableData(propertiesCount: Int, tenantsCount: Int): Boolean {
        return propertiesCount >= PROPERTIES_THRESHOLD || tenantsCount >= TENANTS_THRESHOLD
    }
    
    /**
     * Determine what type of conversion prompt to show
     */
    fun getConversionPromptType(propertiesCount: Int, tenantsCount: Int, usageSessions: Int): ConversionPromptType {
        val daysAsGuest = getDaysAsGuest()
        val hasValuableData = hasValuableData(propertiesCount, tenantsCount)
        val engagementScore = calculateEngagementScore(propertiesCount, tenantsCount, usageSessions)
        
        return when {
            daysAsGuest >= DAYS_MANDATORY -> ConversionPromptType.MANDATORY
            daysAsGuest >= DAYS_DATA_WARNING && hasValuableData -> ConversionPromptType.DATA_WARNING
            daysAsGuest >= DAYS_GENTLE_PROMPT && engagementScore > 20 -> ConversionPromptType.GENTLE_HINT
            hasValuableData && engagementScore > 30 -> ConversionPromptType.SMART_BACKUP
            propertiesCount >= PROPERTIES_THRESHOLD -> ConversionPromptType.PROPERTY_SYNC
            tenantsCount >= TENANTS_THRESHOLD -> ConversionPromptType.TENANT_BACKUP
            else -> ConversionPromptType.NONE
        }
    }
    
    /**
     * Check if we should show a conversion prompt now
     */
    fun shouldShowConversionPrompt(propertiesCount: Int, tenantsCount: Int, usageSessions: Int): Boolean {
        val promptType = getConversionPromptType(propertiesCount, tenantsCount, usageSessions)
        if (promptType == ConversionPromptType.NONE) return false
        
        val lastPrompt = prefs.getLong(KEY_LAST_CONVERSION_PROMPT, 0)
        val timeSinceLastPrompt = System.currentTimeMillis() - lastPrompt
        
        // Show mandatory prompts immediately
        if (promptType == ConversionPromptType.MANDATORY) return true
        
        // For other prompts, respect cooldown periods
        val cooldownHours = when (promptType) {
            ConversionPromptType.DATA_WARNING -> 12
            ConversionPromptType.SMART_BACKUP -> 24
            ConversionPromptType.GENTLE_HINT -> 48
            else -> 72
        }
        
        return timeSinceLastPrompt >= TimeUnit.HOURS.toMillis(cooldownHours.toLong())
    }
    
    /**
     * Mark that we showed a conversion prompt
     */
    fun markConversionPromptShown() {
        val currentCount = prefs.getInt(KEY_CONVERSION_PROMPT_COUNT, 0)
        prefs.edit()
            .putLong(KEY_LAST_CONVERSION_PROMPT, System.currentTimeMillis())
            .putInt(KEY_CONVERSION_PROMPT_COUNT, currentCount + 1)
            .apply()
    }
    
    /**
     * Get appropriate conversion message based on user data
     */
    fun getConversionMessage(propertiesCount: Int, tenantsCount: Int, promptType: ConversionPromptType): String {
        return when (promptType) {
            ConversionPromptType.MANDATORY -> {
                "🚨 Your ${getDaysAsGuest()}-day trial has ended. Create a permanent account to continue using RentUp."
            }
            ConversionPromptType.DATA_WARNING -> {
                "⚠️ You have $propertiesCount properties and $tenantsCount tenants. Don't risk losing this data! Back it up now."
            }
            ConversionPromptType.SMART_BACKUP -> {
                "💾 Your rental data is valuable! Create an account to back it up and sync across devices."
            }
            ConversionPromptType.PROPERTY_SYNC -> {
                "🏠 You have $propertiesCount properties. Sync them across all your devices with a permanent account."
            }
            ConversionPromptType.TENANT_BACKUP -> {
                "👥 You've added $tenantsCount tenants. Secure their information with account backup."
            }
            ConversionPromptType.GENTLE_HINT -> {
                "✨ Loving RentUp? Create an account to keep your data safe and accessible everywhere."
            }
            ConversionPromptType.NONE -> ""
        }
    }
    
    /**
     * Check if account conversion is mandatory (60+ days)
     */
    fun isConversionMandatory(): Boolean {
        return getDaysAsGuest() >= DAYS_MANDATORY
    }
    
    /**
     * Get days remaining before mandatory conversion
     */
    fun getDaysUntilMandatory(): Int {
        return maxOf(0, DAYS_MANDATORY - getDaysAsGuest())
    }

    fun trackUsageSession() {
        val currentSessions = prefs.getInt(KEY_USER_ENGAGEMENT_SCORE, 0)
        prefs.edit().putInt(KEY_USER_ENGAGEMENT_SCORE, currentSessions + 1).apply()
    }

    /**
     * Get usage sessions
     */
    fun getUsageSessions(): Int {
        return prefs.getInt(KEY_USER_ENGAGEMENT_SCORE, 0)
    }
    
    /**
     * Reset guest account tracking (for testing)
     */
    fun resetGuestTracking() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Guest tracking reset")
    }
}

/**
 * Types of conversion prompts based on smart triggers
 */
enum class ConversionPromptType {
    NONE,           // No prompt needed
    GENTLE_HINT,    // Soft suggestion after engagement
    SMART_BACKUP,   // Data-focused backup suggestion
    PROPERTY_SYNC,  // Property-specific sync benefits
    TENANT_BACKUP,  // Tenant-specific backup benefits
    DATA_WARNING,   // Warning about data loss risk
    MANDATORY       // Required conversion (60+ days)
}

/**
 * Composable to integrate smart triggers with ViewModels
 */
@Composable
fun rememberGuestAccountManager(): GuestAccountManager {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember { GuestAccountManager(context) }
}


/**
* This composable function is responsible for checking the guest account status
* and showing conversion prompts when necessary.
*
* It observes the properties and tenants from their respective ViewModels
* and uses the GuestAccountManager to determine if a prompt should be shown.
*
* @param onShowConversionPrompt A callback to be invoked when a conversion prompt should be shown.
*                               The callback receives the prompt type and a descriptive message.
*/
@Composable
fun CheckConversionPromptsComposable(
   onShowConversionPrompt: (promptType: ConversionPromptType, message: String) -> Unit
) {
   val guestAccountManager = rememberGuestAccountManager()
   val propertyViewModel: PropertyViewModel = viewModel()
   val tenantViewModel: TenantViewModel = viewModel()

   // Observe properties and tenants
   val properties by propertyViewModel.allProperties.observeAsState(initial = emptyList())
   val tenants by tenantViewModel.allTenants.observeAsState(initial = emptyList())

   val usageSessions = guestAccountManager.getUsageSessions()

   // Use LaunchedEffect to check for conversion prompts when data changes
   LaunchedEffect(properties, tenants) {
       withContext(Dispatchers.IO) {
           val propertiesCount = properties.size
           val tenantsCount = tenants.size

           if (guestAccountManager.shouldShowConversionPrompt(propertiesCount, tenantsCount, usageSessions)) {
               val promptType = guestAccountManager.getConversionPromptType(propertiesCount, tenantsCount, usageSessions)
               val message = guestAccountManager.getConversionMessage(propertiesCount, tenantsCount, promptType)
               
               // Show the prompt on the main thread
               withContext(Dispatchers.Main) {
                   onShowConversionPrompt(promptType, message)
                   guestAccountManager.markConversionPromptShown()
               }
           }
       }
   }
}