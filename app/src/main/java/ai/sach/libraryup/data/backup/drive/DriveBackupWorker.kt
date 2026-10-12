package ai.sach.rentup.data.backup.drive

import ai.sach.rentup.RentUpApplication
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** The daily Google Drive backup. Scheduled by [DriveBackupManager.ensureScheduled]. */
class DriveBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "DriveBackupWorker"
        private const val MAX_ATTEMPTS = 3
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as? RentUpApplication ?: return Result.success()
        val outcome = app.driveBackupManager.runScheduledBackup()
        Log.d(TAG, "Scheduled Drive backup: $outcome")
        // Retry only what a later attempt could fix; a missing consent needs the user.
        // runAttemptCount is 0 on the first run.
        return if (outcome == DriveBackupManager.Outcome.FAILED && runAttemptCount + 1 < MAX_ATTEMPTS) {
            Result.retry()
        } else {
            Result.success()
        }
    }
}
