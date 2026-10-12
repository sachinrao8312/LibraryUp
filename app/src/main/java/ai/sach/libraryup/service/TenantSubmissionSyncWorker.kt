package ai.sach.libraryup.service

import ai.sach.libraryup.LibraryUpApplication
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters

/**
 * The single consumer of a tenant-submission push.
 *
 * Previously one message was processed three times: the service called the repository
 * directly and also sent a local broadcast, which both TenantFormSubmissionHandler and
 * MainScreen picked up. Now the service enqueues this worker and the UI observes the
 * database, so one message means one round of Firestore reads.
 */
class TenantSubmissionSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "TenantSubmissionSync"
        private const val UNIQUE_WORK = "tenant_submission_sync"

        /**
         * Queues a submission sync. Uses KEEP so a burst of submissions coalesces into one
         * pass - the pass fetches every pending form anyway.
         */
        fun enqueue(context: Context) {
            val builder = OneTimeWorkRequestBuilder<TenantSubmissionSyncWorker>()
            // Below Android 12 WorkManager runs expedited work as a foreground service,
            // which needs getForegroundInfo() - without it the work fails instead of
            // running. There, plain work runs promptly enough off the FCM wake-up.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            }
            val request = builder.build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.KEEP, request)
        }
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as? LibraryUpApplication ?: return Result.success()

        return try {
            app.tenantRepository.processTenantFormSubmissions().fold(
                onSuccess = { count ->
                    Log.d(TAG, "Processed $count tenant form submission(s)")
                    Result.success()
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to process tenant form submissions", error)
                    Result.retry()
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Submission sync failed", e)
            Result.retry()
        }
    }
}
