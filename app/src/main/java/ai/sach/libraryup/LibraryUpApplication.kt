package ai.sach.libraryup

import ai.sach.libraryup.data.backup.BackupManager
import ai.sach.libraryup.data.backup.drive.DriveBackupManager
import ai.sach.libraryup.data.local.LibraryUpDatabase
import ai.sach.libraryup.data.repository.LibraryRepository
import ai.sach.libraryup.ui.library.StaffMode
import ai.sach.libraryup.notifications.NotificationHelper
import ai.sach.libraryup.notifications.NotificationPrefs
import ai.sach.libraryup.notifications.ReminderScheduler
import ai.sach.libraryup.ui.screens.authentication.AuthRepository
import ai.sach.libraryup.ui.screens.settingsScreen.ThemePreferencesRepository
import ai.sach.libraryup.ui.screens.settingsScreen.appprefernecs.AppPreferencesRepository
import ai.sach.libraryup.utils.GooglePlayServicesHelper
import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application class and the app's dependency container. Screens reach the repository
 * and helpers through `LocalContext.current.applicationContext as LibraryUpApplication`.
 */
@HiltAndroidApp
class LibraryUpApplication : Application(), Configuration.Provider {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { LibraryUpDatabase.getDatabase(this) }

    val libraryRepository by lazy { LibraryRepository(database) }

    val staffMode by lazy { StaffMode(this) }

    val authRepository by lazy { AuthRepository(applicationContext) }

    val themeRepository by lazy { ThemePreferencesRepository(applicationContext) }

    val appPreferencesRepository by lazy { AppPreferencesRepository(applicationContext) }

    val notificationHelper by lazy { NotificationHelper(applicationContext) }

    val notificationPrefs by lazy { NotificationPrefs(applicationContext) }

    val reminderScheduler by lazy { ReminderScheduler(applicationContext) }

    val backupManager by lazy { BackupManager(applicationContext, database) }

    val driveBackupManager by lazy { DriveBackupManager(applicationContext, backupManager) }

    override val workManagerConfiguration: Configuration =
        Configuration.Builder()
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR)
            .build()

    override fun onCreate() {
        super.onCreate()
        WorkManager.initialize(this, workManagerConfiguration)

        runCatching { notificationHelper.createChannels() }
            .onFailure { Log.e(TAG, "Failed to create notification channels", it) }

        runCatching {
            if (GooglePlayServicesHelper.initializeGooglePlayServices(this)) {
                GooglePlayServicesHelper.logDiagnosticInfo(this)
            }
        }.onFailure { Log.w(TAG, "Google Play Services check failed", it) }

        applicationScope.launch(Dispatchers.IO) {
            runCatching {
                if (notificationPrefs.dailyDigestEnabled) reminderScheduler.ensureScheduled() else reminderScheduler.cancel()
                // Re-arms the daily Drive backup if it's on and its job was lost.
                DriveBackupManager.ensureScheduled(this@LibraryUpApplication)
            }.onFailure { Log.e(TAG, "Failed to schedule background work", it) }
        }
    }

    private companion object {
        const val TAG = "LibraryUpApplication"
    }
}
