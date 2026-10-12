package ai.sach.libraryup.data.backup.drive

import ai.sach.libraryup.data.backup.BackupManager
import ai.sach.libraryup.data.backup.BackupSummary
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

data class DriveBackupStatus(
    val enabled: Boolean,
    val lastBackupAt: Long?,
    val lastError: String?,
    /** Drive access was withdrawn; only the user can grant it again, from the Backup screen. */
    val needsReconnect: Boolean
)

sealed interface DriveAccess {
    data class Granted(val token: String) : DriveAccess

    /** The user has to approve Drive access - launch this from an Activity. */
    data class NeedsConsent(val pendingIntent: PendingIntent) : DriveAccess
}

class NothingToBackUpException :
    IllegalStateException("There's nothing to back up yet - add a student first.")

/**
 * Automatic backup of the same `.zip` [BackupManager] writes, into a "LibraryUp Backups"
 * folder in the user's own Google Drive, plus restore from it.
 *
 * Access uses the `drive.file` scope: LibraryUp can see only the files it created, and the
 * scope needs no Google verification review. The access token is fetched fresh for every
 * run and never stored.
 *
 * This is backup, not live sync: a restore replaces the local database wholesale, exactly
 * like restoring a file picked from storage.
 */
class DriveBackupManager(
    private val context: Context,
    private val backupManager: BackupManager,
    private val client: DriveClient = DriveClient(defaultHttpClient())
) {
    private val secrets by lazy { ai.sach.libraryup.data.backup.BackupSecrets(context) }

    companion object {
        private const val TAG = "DriveBackupManager"

        // Excluded from Auto Backup (see data_extraction_rules.xml): restored onto a new
        // phone, "enabled" would start daily uploads of a database whose photos and
        // documents didn't come with it.
        private const val PREFS = "drive_backup"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_LAST_BACKUP_AT = "last_backup_at"
        private const val KEY_LAST_UPLOADED_HASH = "last_uploaded_hash"
        private const val KEY_LAST_ERROR = "last_error"
        private const val KEY_NEEDS_RECONNECT = "needs_reconnect"

        /** In noBackupFilesDir, so every install - including a restored one - gets its own. */
        private const val INSTALL_ID_FILE = "drive_backup_install_id"

        private const val PERIODIC_WORK = "drive_backup_periodic"

        /**
         * A scheduled run skips if the last backup is newer than this. Covers turning
         * backup on (the periodic job's first run starts straight away, right after the
         * screen's own upload) and "Back up now" shortly before the daily run.
         */
        private val MIN_SCHEDULED_INTERVAL_MS = TimeUnit.HOURS.toMillis(20)

        /** This install's backups beyond this are deleted after each successful upload. */
        const val KEEP_BACKUPS = 5

        private fun defaultHttpClient() = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.MINUTES)
            .writeTimeout(2, TimeUnit.MINUTES)
            .build()

        /**
         * Daily, on Wi-Fi only: a backup carrying student photos and ID proofs can run to tens of MB,
         * which shouldn't come out of a mobile data plan every day. "Back up now" works on
         * any network.
         *
         * Static so app start-up can re-arm the job without building the backup stack.
         */
        fun ensureScheduled(context: Context) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_ENABLED, false)) return
            val request = PeriodicWorkRequestBuilder<DriveBackupWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.UNMETERED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }

    enum class Outcome { BACKED_UP, SKIPPED, NEEDS_RECONNECT, FAILED }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Serialises Drive runs, so a scheduled and a manual backup can't both upload. */
    private val mutex = Mutex()

    private val _status = MutableStateFlow(readStatus())
    val status: StateFlow<DriveBackupStatus> = _status.asStateFlow()

    /** Tags this install's uploads, so retention only ever deletes its own backups. */
    private val installId: String by lazy {
        val file = File(context.noBackupFilesDir, INSTALL_ID_FILE)
        file.takeIf { it.isFile }?.readText()?.trim()?.takeIf { it.isNotEmpty() }
            ?: UUID.randomUUID().toString().also { file.writeText(it) }
    }

    // ------------------------------------------------------------------ access

    suspend fun requestAccess(): DriveAccess {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DriveClient.SCOPE_DRIVE_FILE)))
            .build()
        val result = Identity.getAuthorizationClient(context).authorize(request).await()
        return accessFrom(result)
    }

    /** Reads the outcome of the consent screen launched from [DriveAccess.NeedsConsent]. */
    fun accessFromConsentResult(data: Intent): DriveAccess =
        accessFrom(Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data))

    private fun accessFrom(result: AuthorizationResult): DriveAccess {
        val pendingIntent = result.pendingIntent
        if (result.hasResolution() && pendingIntent != null) {
            return DriveAccess.NeedsConsent(pendingIntent)
        }
        val token = result.accessToken
            ?: throw IOException("Google didn't grant access to Google Drive.")
        if (_status.value.needsReconnect) update { remove(KEY_NEEDS_RECONNECT) }
        return DriveAccess.Granted(token)
    }

    // ------------------------------------------------------------------ schedule

    fun enable() {
        update {
            putBoolean(KEY_ENABLED, true)
            remove(KEY_NEEDS_RECONNECT)
        }
        ensureScheduled(context)
    }

    fun disable() {
        update {
            putBoolean(KEY_ENABLED, false)
            remove(KEY_NEEDS_RECONNECT)
            remove(KEY_LAST_ERROR)
        }
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK)
    }

    // ------------------------------------------------------------------ backup

    /** @throws NothingToBackUpException when the database is empty. */
    suspend fun backUp(token: String): BackupSummary =
        mutex.withLock { checkNotNull(backUpLocked(token, skipIfUnchanged = false)) }

    /** What the periodic worker runs. Never throws except for cancellation. */
    suspend fun runScheduledBackup(): Outcome {
        if (!prefs.getBoolean(KEY_ENABLED, false)) {
            // Auto Backup restores WorkManager's database but not this setting, so a restored
            // phone can inherit the job with backup off. Stop it rather than wake daily.
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK)
            return Outcome.SKIPPED
        }
        if (backedUpRecently()) return Outcome.SKIPPED

        val access = try {
            requestAccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't get Drive access for scheduled backup", e)
            update { putString(KEY_LAST_ERROR, e.message ?: "Couldn't connect to Google Drive") }
            return Outcome.FAILED
        }

        val token = when (access) {
            is DriveAccess.Granted -> access.token
            is DriveAccess.NeedsConsent -> {
                update { putBoolean(KEY_NEEDS_RECONNECT, true) }
                return Outcome.NEEDS_RECONNECT
            }
        }

        return try {
            mutex.withLock {
                // Checked again under the lock: a manual backup may have just finished.
                if (backedUpRecently()) return Outcome.SKIPPED
                backUpLocked(token, skipIfUnchanged = true) ?: return Outcome.SKIPPED
            }
            Outcome.BACKED_UP
        } catch (e: CancellationException) {
            throw e
        } catch (e: NothingToBackUpException) {
            Outcome.SKIPPED
        } catch (e: DriveAuthException) {
            Outcome.NEEDS_RECONNECT
        } catch (e: Exception) {
            Outcome.FAILED
        }
    }

    /** Returns null when [skipIfUnchanged] and the data matches the last upload, so nothing was sent. */
    private suspend fun backUpLocked(token: String, skipIfUnchanged: Boolean): BackupSummary? = withContext(Dispatchers.IO) {
        val temp = File(context.cacheDir, "drive-backup-${System.currentTimeMillis()}.zip")
        try {
            val summary = backupManager.exportTo(Uri.fromFile(temp), secrets.current()).getOrThrow()
            if (summary.studentCount == 0) throw NothingToBackUpException()
            if (skipIfUnchanged && summary.contentHash == prefs.getString(KEY_LAST_UPLOADED_HASH, null)) {
                return@withContext null
            }

            val folderId = client.findOrCreateFolder(token)
            client.upload(
                token, folderId, newBackupName(), temp,
                appProperties = mapOf(DriveClient.PROPERTY_INSTALL_ID to installId)
            )
            pruneOldBackups(token, folderId)

            update {
                putLong(KEY_LAST_BACKUP_AT, System.currentTimeMillis())
                putString(KEY_LAST_UPLOADED_HASH, summary.contentHash)
                remove(KEY_LAST_ERROR)
                remove(KEY_NEEDS_RECONNECT)
            }
            summary
        } catch (e: CancellationException) {
            throw e
        } catch (e: NothingToBackUpException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Drive backup failed", e)
            if (e is DriveAuthException) onAccessRejected(token)
            update { putString(KEY_LAST_ERROR, e.message ?: "Backup failed") }
            throw e
        } finally {
            temp.delete()
        }
    }

    // ------------------------------------------------------------------ restore

    /** Newest first. Empty when this Google account has no LibraryUp backups. */
    suspend fun listBackups(token: String): List<DriveFile> = withContext(Dispatchers.IO) {
        rejectingStaleToken(token) {
            val folderId = client.findFolder(token) ?: return@rejectingStaleToken emptyList()
            client.listBackups(token, folderId).filter { it.name.startsWith(DriveClient.BACKUP_NAME_PREFIX) }
        }
    }

    /** Replaces all local data with [backup] - confirm with the user first. */
    /** @param secret password or recovery code for an encrypted backup; the saved password is tried first. */
    suspend fun restore(token: String, backup: DriveFile, secret: String? = null): BackupSummary = mutex.withLock {
        withContext(Dispatchers.IO) {
            val temp = File(context.cacheDir, "drive-restore-${System.currentTimeMillis()}.zip")
            try {
                rejectingStaleToken(token) { client.download(token, backup.id, temp) }
                backupManager.importFrom(Uri.fromFile(temp), secret ?: secrets.password()).getOrThrow()
            } finally {
                temp.delete()
            }
        }
    }

    // ------------------------------------------------------------------ internals

    private fun backedUpRecently(): Boolean {
        val last = prefs.getLong(KEY_LAST_BACKUP_AT, 0L)
        val now = System.currentTimeMillis()
        // A last-backup time in the future (clock changed) doesn't count as recent.
        return last in (now - MIN_SCHEDULED_INTERVAL_MS)..now
    }

    private inline fun <T> rejectingStaleToken(token: String, block: () -> T): T =
        try {
            block()
        } catch (e: DriveAuthException) {
            onAccessRejected(token)
            throw e
        }

    /**
     * Drive refused the token (revoked or expired). Play services would otherwise hand the
     * same cached token back on the next authorize() for up to an hour, so "reconnect"
     * would keep failing instead of showing the consent screen. Blocking - call off main.
     */
    private fun onAccessRejected(token: String) {
        try {
            GoogleAuthUtil.clearToken(context, token)
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't clear the rejected Drive token", e)
        }
        update { putBoolean(KEY_NEEDS_RECONNECT, true) }
    }

    private fun pruneOldBackups(token: String, folderId: String) {
        try {
            backupsBeyondRetention(client.listBackups(token, folderId), KEEP_BACKUPS, installId)
                .forEach { client.delete(token, it.id) }
        } catch (e: IOException) {
            // The new backup is already safely uploaded; a leftover old one only costs space.
            Log.w(TAG, "Couldn't remove old Drive backups", e)
        }
    }

    private fun newBackupName(): String {
        val stamp = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(Date())
        return "${DriveClient.BACKUP_NAME_PREFIX}$stamp.zip"
    }

    private fun readStatus() = DriveBackupStatus(
        enabled = prefs.getBoolean(KEY_ENABLED, false),
        lastBackupAt = prefs.getLong(KEY_LAST_BACKUP_AT, 0L).takeIf { it > 0L },
        lastError = prefs.getString(KEY_LAST_ERROR, null),
        needsReconnect = prefs.getBoolean(KEY_NEEDS_RECONNECT, false)
    )

    /** apply(), not commit(): callers include the main thread, and the in-memory map updates at once. */
    private fun update(block: SharedPreferences.Editor.() -> Unit) {
        prefs.edit { block() }
        _status.value = readStatus()
    }
}
