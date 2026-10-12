package ai.sach.libraryup.service

import ai.sach.libraryup.LibraryUpApplication
import ai.sach.libraryup.ui.screens.settingsScreen.notifications.NotificationHelper
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives pushes from Firebase.
 *
 * Handles both data-only messages and messages that carry a `notification` payload, so a
 * sender that adds one (recommended, since the system can then display it even when this
 * process cannot start) does not produce a duplicate notification here.
 */
class RentUpFirebaseMessagingService : FirebaseMessagingService() {

    /**
     * Called when Firebase issues a new registration token. Without this the app kept
     * using a token the backend no longer had, and push died permanently on the first
     * rotation.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token rotated; publishing the new one")
        FcmTokenSyncWorker.enqueue(applicationContext, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data

        when (data["type"]) {
            TYPE_TENANT_SUBMISSION -> handleTenantSubmission(
                tenantId = data["tenantId"],
                formId = data["formId"],
                // When the sender includes a notification payload and the app is in the
                // foreground, the system does not display it - but it will display it
                // itself in the background, so don't post a second one.
                systemWillDisplay = remoteMessage.notification != null && !isAppInForeground()
            )

            else -> Log.d(TAG, "Ignoring push of unknown type '${data["type"]}'")
        }
    }

    private fun handleTenantSubmission(tenantId: String?, formId: String?, systemWillDisplay: Boolean) {
        Log.d(TAG, "Tenant submission received: tenantId=$tenantId, formId=$formId")

        val preferences = (application as? LibraryUpApplication)?.notificationPreferences
        val alreadySeen = formId != null && preferences?.isOnboardingNotificationSeen(formId) == true

        val announcementsEnabled = preferences?.areTenantUpdatesEnabled() ?: true

        if (!alreadySeen && announcementsEnabled && !systemWillDisplay) {
            showTenantSubmissionNotification(formId)
        } else if (alreadySeen) {
            Log.d(TAG, "Onboarding notification for formId=$formId already seen; not re-posting")
        } else if (!announcementsEnabled) {
            Log.d(TAG, "Tenant update notifications disabled; importing submission silently")
        }

        // Record the announcement either way: whether this service posted it or the
        // system did, the owner has been told, and the import that follows must not
        // raise a second notification for the same submission.
        if (!alreadySeen && formId != null) {
            preferences?.markOnboardingNotificationPosted(formId)
        }

        // Import the submission regardless of whether a notification was shown - the data
        // still needs to reach the database. One worker, so one round of Firestore reads.
        TenantSubmissionSyncWorker.enqueue(applicationContext)
    }

    private fun showTenantSubmissionNotification(formId: String?) {
        try {
            val notificationHelper = (application as? LibraryUpApplication)?.notificationHelper
                ?: NotificationHelper(applicationContext)

            notificationHelper.showNewTenantNotification(count = 1, formId = formId)
            Log.d(TAG, "Tenant submission notification shown")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing tenant submission notification", e)
        }
    }

    private fun isAppInForeground(): Boolean = try {
        androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.currentState
            .isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)
    } catch (e: Exception) {
        false
    }

    companion object {
        private const val TAG = "FCMService"
        private const val TYPE_TENANT_SUBMISSION = "tenant_submission"

        const val EXTRA_TENANT_ID = "tenantId"
        const val EXTRA_FORM_ID = "formId"
    }
}
