package ai.sach.libraryup.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf

/**
 * Publishes the FCM token with retry.
 *
 * [FirebaseMessagingService.onNewToken] fires exactly once per rotation and may arrive
 * with no network, so the upload cannot be a fire-and-forget coroutine - a dropped write
 * would mean silently dead push until the next rotation. WorkManager gives it a network
 * constraint and exponential backoff instead.
 */
class FcmTokenSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val UNIQUE_WORK = "fcm_token_sync"
        private const val KEY_TOKEN = "token"
        private const val KEY_FORCE = "force"

        /** Queues a token publish. Replaces any pending one, since only the newest matters. */
        fun enqueue(context: Context, token: String? = null, force: Boolean = false) {
            val request = OneTimeWorkRequestBuilder<FcmTokenSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInputData(workDataOf(KEY_TOKEN to token, KEY_FORCE to force))
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.REPLACE, request)
        }
    }

    override suspend fun doWork(): Result {
        val token = inputData.getString(KEY_TOKEN)
        val force = inputData.getBoolean(KEY_FORCE, false)
        val published = FcmTokenRegistrar(applicationContext).syncToken(token, force)
        // Signed out or offline: retry rather than lose the rotation.
        return if (published) Result.success() else Result.retry()
    }
}
