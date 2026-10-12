package ai.sach.rentup

import ai.sach.rentup.data.backup.BackupManager
import ai.sach.rentup.data.backup.drive.DriveBackupManager
import ai.sach.rentup.data.firestore.repository.FirestoreRepository
import ai.sach.rentup.data.local.RentUpDatabase
import ai.sach.rentup.data.local.MockDataSeeder
import ai.sach.rentup.data.repository.PropertyRepository
import ai.sach.rentup.data.repository.TenantRepository
import ai.sach.rentup.ui.screens.authentication.AuthRepository
import ai.sach.rentup.service.FcmTokenRegistrar
import ai.sach.rentup.service.FcmTokenSyncWorker
import ai.sach.rentup.ui.screens.homescreen.TenantFormSubmissionHandler
import ai.sach.rentup.ui.screens.settingsScreen.ThemePreferencesRepository
import ai.sach.rentup.ui.screens.settingsScreen.appprefernecs.AppPreferencesRepository
import ai.sach.rentup.ui.screens.settingsScreen.notifications.NotificationHelper
import ai.sach.rentup.ui.screens.settingsScreen.notifications.NotificationPreferencesManager
import ai.sach.rentup.ui.screens.settingsScreen.notifications.ReminderScheduler
import ai.sach.rentup.utils.GooglePlayServicesHelper
import ai.sach.rentup.BuildConfig
import android.app.Application
import android.app.PendingIntent
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.Configuration
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
/**
 * Application class for RentUp.
 * Sets up databases, repositories, WorkManager configuration, and app-wide
 * helpers at process start-up.
 */
@HiltAndroidApp
class RentUpApplication : Application(), Configuration.Provider {

    companion object {
        private const val TAG = "RentUpApplication"
    }

    // Application scope for coroutines that should live as long as the application
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /* ----------- DB & Repository stack ----------- */

    val database by lazy { RentUpDatabase.getDatabase(this) }

    // Firebase instances
    val firebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firebaseFirestore by lazy { FirebaseFirestore.getInstance() }


    // Firestore repository
    val firestoreRepository by lazy {
        FirestoreRepository(firebaseFirestore, firebaseAuth)
    }

    val propertyRepository by lazy {
        PropertyRepository(database.propertyDao())
    }

    val tenantRepository by lazy {
        TenantRepository(
            database,
            database.tenantDao(),
            database.tenantFamilyInfoDao(),
            database.tenantDocumentDao(),
            database.paymentHistoryDao(),
            database.tenantPropertyDao(),
            propertyRepository,  // Add propertyRepository dependency
            firestoreRepository, // Add firestoreRepository dependency
            notificationHelper,
            notificationPreferences,
            reminderScheduler,
            applicationContext
        )
    }

    val authRepository by lazy { AuthRepository(applicationContext) }

    val themeRepository by lazy { ThemePreferencesRepository(applicationContext) }

    val appPreferencesRepository by lazy { AppPreferencesRepository(applicationContext) }

    val tenantFormSubmissionHandler by lazy { TenantFormSubmissionHandler(applicationContext, tenantRepository) }

    /* ----------- Notification stack ----------- */

    val notificationHelper by lazy { NotificationHelper(applicationContext) }

    val notificationPreferences by lazy { NotificationPreferencesManager(applicationContext) }

    val reminderScheduler by lazy { ReminderScheduler(applicationContext) }

    /* ----------- Backup & restore ----------- */

    val backupManager by lazy { BackupManager(applicationContext, database, tenantRepository) }

    val driveBackupManager by lazy { DriveBackupManager(applicationContext, backupManager) }

    val fcmTokenRegistrar by lazy { FcmTokenRegistrar(applicationContext) }

    suspend fun seedMockData(): Int = MockDataSeeder.seed(propertyRepository, tenantRepository)

    /* ----------- WorkManager configuration ----------- */
    override val workManagerConfiguration: Configuration =
        Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    /* ----------- Application lifecycle ----------- */

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "RentUp Application starting in DEBUG mode")
        }
        
        // Initialize WorkManager (lightweight operation)
        WorkManager.initialize(
            this,
            Configuration.Builder()
                .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR)
                .build()
        )
        
        // Initialize only essential components immediately
        initializeNotificationPreferences()
        initializeGooglePlayServices()
        // Arm the daily reminder sweep. This touches no database and sends nothing -
        // DailyReminderWorker decides what is owed when it runs.
        applicationScope.launch(Dispatchers.IO) {
            try {
                if (notificationPreferences.areRentRemindersEnabled()) {
                    reminderScheduler.ensureScheduled()
                } else {
                    reminderScheduler.cancel()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule rent reminders", e)
            }
        }

        // Make sure the backend has the current FCM token. Cheap when nothing has changed,
        // and the only thing that recovers push after a token rotation.
        try {
            FcmTokenSyncWorker.enqueue(this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue FCM token sync", e)
        }

        // Older builds could schedule a Firestore sync worker whose class no longer exists.
        WorkManager.getInstance(this).cancelAllWorkByTag("data_sync_work")

        // Re-arms the daily Drive backup if it's on and its job was lost. Reads one
        // preference; doesn't build the backup stack.
        applicationScope.launch(Dispatchers.IO) {
            try {
                DriveBackupManager.ensureScheduled(this@RentUpApplication)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule Drive backup", e)
            }
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "RentUp Application lightweight initialization completed")
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        
        // Clear repository caches when memory is low
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                try {
                    tenantRepository.clearCacheSync()
                } catch (e: Exception) {
                    Log.e(TAG, "Error clearing tenant repository cache", e)
                }
            }
        }
    }

    /**
     * Initialize notification preferences
     */
    private fun initializeNotificationPreferences() {
        try {
            Log.d(TAG, "Initializing notification preferences")
            val notificationHelper = NotificationHelper(this)
            notificationHelper.createNotificationChannels()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing notification preferences: ${e.message}", e)
        }
    }
    
    /**
     * Initialize Google Play Services with proper error handling
     */
    private fun initializeGooglePlayServices() {
        try {
            Log.d(TAG, "Initializing Google Play Services")
            val isAvailable = GooglePlayServicesHelper.initializeGooglePlayServices(this)
            if (isAvailable) {
                GooglePlayServicesHelper.logDiagnosticInfo(this)
            } else {
                Log.w(TAG, "Google Play Services not available: ${GooglePlayServicesHelper.getErrorMessage(this)}")
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Google Play Services security exception (this is expected in some cases): ${e.message}")
            // This is the "Unknown calling package name" error - not critical for app functionality
        } catch (e: Exception) {
            Log.w(TAG, "Google Play Services initialization failed, continuing with fallback", e)
        }
    }

}
