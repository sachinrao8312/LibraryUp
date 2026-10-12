package ai.sach.rentup.service

import android.content.Context
import android.util.Log
import ai.sach.rentup.ui.screens.settingsScreen.notifications.NotificationPreferencesManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * Keeps the device's FCM token current wherever the backend looks for it.
 *
 * Firebase rotates a token on reinstall, data clear, restore and long inactivity. The app
 * previously captured the token once, inside the onboarding-link dialog, and never again -
 * so after the first rotation every push was addressed to a dead token and delivery
 * stopped permanently with no error anywhere.
 *
 * Two places are updated:
 *  - `users/{uid}.deviceToken` / `fcmTokens`. This is the registry the backend reads
 *    first when it notifies an owner, so publishing here is what keeps push alive
 *    across a rotation. `fcmTokens` is an array, so an owner signed in on more than
 *    one device is reached on all of them.
 *  - `deviceToken` on the owner's existing `tenant-forms` documents, so a submission
 *    already imported still shows which device it was addressed to.
 *
 * The token captured on a form link when it was generated stays in `form-links`, which
 * the app cannot write; the backend treats it as a fallback for links created before
 * this registry existed.
 */
class FcmTokenRegistrar(
    private val context: Context,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    companion object {
        private const val TAG = "FcmTokenRegistrar"
        private const val PREFS_NAME = "fcm_token_state"
        private const val KEY_LAST_SYNCED = "last_synced"
    }

    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    private val notificationPrefs by lazy { NotificationPreferencesManager(context) }

    /**
     * Publishes [token] (or the current token when null) for the signed-in user.
     *
     * Safe to call on every app start: a token already synced for this user is skipped
     * unless [force] is set, so the common case costs one SharedPreferences read.
     *
     * @return true when the token is published, false when it could not be (signed out,
     *         no network) and the caller should retry.
     */
    suspend fun syncToken(token: String? = null, force: Boolean = false): Boolean {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.d(TAG, "No signed-in user; deferring token sync until sign-in")
            return false
        }

        val resolved = token ?: try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Log.w(TAG, "Could not read the FCM token", e)
            return false
        }

        if (resolved.isNullOrBlank()) return false

        val fingerprint = "$uid:$resolved"
        if (!force && prefs.getString(KEY_LAST_SYNCED, null) == fingerprint) {
            Log.d(TAG, "Token already published for this user")
            return true
        }

        return try {
            publishToUserDocument(uid, resolved)
            val updated = refreshTokenOnOnboardingForms(uid, resolved)
            prefs.edit().putString(KEY_LAST_SYNCED, fingerprint).apply()
            Log.d(TAG, "Published FCM token; refreshed $updated onboarding form(s)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to publish FCM token", e)
            false
        }
    }

    /** Clears the cached fingerprint so the next sync republishes. Call on sign-out. */
    fun onSignedOut() {
        prefs.edit().remove(KEY_LAST_SYNCED).apply()
    }

    private suspend fun publishToUserDocument(uid: String, token: String) {
        val payload = mapOf(
            "deviceToken" to token,
            "fcmTokens" to FieldValue.arrayUnion(token),
            "fcmTokenUpdatedAt" to FieldValue.serverTimestamp(),
            // Lets the backend skip addressing this device when the user opted out of
            // notifications in-app. Republished on toggle via FcmTokenSyncWorker(force).
            "notificationsEnabled" to notificationPrefs.getMasterToggle()
        )
        firestore.collection("users").document(uid)
            .set(payload, com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    /**
     * Rewrites `deviceToken` on the owner's onboarding forms, so a form document says
     * which device it was addressed to rather than naming a token that is long gone.
     * Delivery itself rides on `users/{uid}`, published above.
     */
    private suspend fun refreshTokenOnOnboardingForms(uid: String, token: String): Int {
        val forms = firestore.collection("tenant-forms")
            .whereEqualTo("ownerId", uid)
            .get()
            .await()

        val stale = forms.documents.filter { it.getString("deviceToken") != token }
        if (stale.isEmpty()) return 0

        // Firestore caps a batch at 500 writes.
        var written = 0
        stale.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { doc -> batch.update(doc.reference, "deviceToken", token) }
            batch.commit().await()
            written += chunk.size
        }
        return written
    }
}
