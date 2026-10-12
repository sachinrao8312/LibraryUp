package ai.sach.rentup.ui.screens.authentication

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Comprehensive session management system with security features
 * Handles session creation, validation, timeout, and secure token storage
 */
class SessionManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SessionManager"
        private const val SESSION_PREFS_NAME = "session_secure_prefs"
        private const val SESSIONS_COLLECTION = "sessions"
        private const val DEVICE_FINGERPRINTS_COLLECTION = "device_fingerprints"
        
        // Session configuration
        private const val SESSION_TIMEOUT_MS = 24 * 60 * 60 * 1000L // 24 hours
        private const val REFRESH_THRESHOLD_MS = 2 * 60 * 60 * 1000L // 2 hours
        private const val MAX_CONCURRENT_SESSIONS = 5
        
        // Security keys
        private const val KEY_SESSION_TOKEN = "session_token"
        private const val KEY_SESSION_EXPIRY = "session_expiry"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_LAST_ACTIVITY = "last_activity"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_SESSION_ID = "session_id"
    }
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    // Secure preferences for session data
    private val securePrefs: SharedPreferences by lazy {
        createSecurePreferences()
    }
    
    // Session state
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Invalid)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()
    
    // Device fingerprint for security
    private val deviceFingerprint: String by lazy {
        generateDeviceFingerprint()
    }
    
    /**
     * Session state sealed class
     */
    sealed class SessionState {
        object Invalid : SessionState()
        object Expired : SessionState()
        data class Valid(
            val sessionId: String,
            val userId: String,
            val expiresAt: Long,
            val lastActivity: Long
        ) : SessionState()
        data class RefreshRequired(
            val sessionId: String,
            val userId: String
        ) : SessionState()
    }
    
    /**
     * Session data class
     */
    data class SessionData(
        val sessionId: String,
        val userId: String,
        val deviceId: String,
        val deviceFingerprint: String,
        val createdAt: Long,
        val expiresAt: Long,
        val lastActivity: Long,
        val isActive: Boolean = true
    )
    
    /**
     * Create a new session for authenticated user
     */
    suspend fun createSession(user: FirebaseUser): Result<SessionData> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Creating new session for user: ${user.uid}")
            
            val sessionId = generateSessionId()
            val currentTime = System.currentTimeMillis()
            val expiryTime = currentTime + SESSION_TIMEOUT_MS
            
            val sessionData = SessionData(
                sessionId = sessionId,
                userId = user.uid,
                deviceId = getOrCreateDeviceId(),
                deviceFingerprint = deviceFingerprint,
                createdAt = currentTime,
                expiresAt = expiryTime,
                lastActivity = currentTime
            )
            
            // Store session in Firestore
            val firestoreData = mapOf(
                "sessionId" to sessionData.sessionId,
                "userId" to sessionData.userId,
                "deviceId" to sessionData.deviceId,
                "deviceFingerprint" to sessionData.deviceFingerprint,
                "createdAt" to FieldValue.serverTimestamp(),
                "expiresAt" to sessionData.expiresAt,
                "lastActivity" to FieldValue.serverTimestamp(),
                "isActive" to true,
                "deviceInfo" to getDeviceInfo()
            )
            
            firestore.collection(SESSIONS_COLLECTION)
                .document(sessionId)
                .set(firestoreData)
                .await()
            
            // Store session locally with encryption
            storeSessionLocally(sessionData)
            
            // Update session state
            _sessionState.value = SessionState.Valid(
                sessionId = sessionData.sessionId,
                userId = sessionData.userId,
                expiresAt = sessionData.expiresAt,
                lastActivity = sessionData.lastActivity
            )
            
            // Clean up old sessions
            cleanupOldSessions(user.uid)
            
            Log.d(TAG, "Session created successfully: $sessionId")
            return@withContext Result.success(sessionData)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating session", e)
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Validate current session
     */
    suspend fun validateSession(): SessionState = withContext(Dispatchers.IO) {
        try {
            val sessionId = securePrefs.getString(KEY_SESSION_ID, null)
            val userId = securePrefs.getString(KEY_USER_ID, null)
            val expiryTime = securePrefs.getLong(KEY_SESSION_EXPIRY, 0L)
            val lastActivity = securePrefs.getLong(KEY_LAST_ACTIVITY, 0L)
            
            if (sessionId == null || userId == null || expiryTime == 0L) {
                Log.d(TAG, "No valid session found locally")
                _sessionState.value = SessionState.Invalid
                return@withContext SessionState.Invalid
            }
            
            val currentTime = System.currentTimeMillis()
            
            // Check if session is expired
            if (currentTime > expiryTime) {
                Log.d(TAG, "Session expired: $sessionId")
                invalidateSession()
                _sessionState.value = SessionState.Expired
                return@withContext SessionState.Expired
            }
            
            // Check if refresh is needed
            if (currentTime - lastActivity > REFRESH_THRESHOLD_MS) {
                Log.d(TAG, "Session refresh required: $sessionId")
                _sessionState.value = SessionState.RefreshRequired(sessionId, userId)
                return@withContext SessionState.RefreshRequired(sessionId, userId)
            }
            
            // Validate session with server
            val isValidOnServer = validateSessionWithServer(sessionId, userId)
            if (!isValidOnServer) {
                Log.w(TAG, "Session invalid on server: $sessionId")
                invalidateSession()
                _sessionState.value = SessionState.Invalid
                return@withContext SessionState.Invalid
            }
            
            // Update last activity
            updateLastActivity(sessionId)
            
            val validState = SessionState.Valid(sessionId, userId, expiryTime, currentTime)
            _sessionState.value = validState
            return@withContext validState
            
        } catch (e: Exception) {
            Log.e(TAG, "Error validating session", e)
            _sessionState.value = SessionState.Invalid
            return@withContext SessionState.Invalid
        }
    }
    
    /**
     * Refresh session to extend expiry time
     */
    suspend fun refreshSession(sessionId: String): Result<SessionData> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Refreshing session: $sessionId")
            
            val currentTime = System.currentTimeMillis()
            val newExpiryTime = currentTime + SESSION_TIMEOUT_MS
            
            // Update session in Firestore
            firestore.collection(SESSIONS_COLLECTION)
                .document(sessionId)
                .update(
                    mapOf(
                        "expiresAt" to newExpiryTime,
                        "lastActivity" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            
            // Update local storage
            securePrefs.edit()
                .putLong(KEY_SESSION_EXPIRY, newExpiryTime)
                .putLong(KEY_LAST_ACTIVITY, currentTime)
                .apply()
            
            val userId = securePrefs.getString(KEY_USER_ID, "") ?: ""
            val deviceId = securePrefs.getString(KEY_DEVICE_ID, "") ?: ""
            
            val refreshedSession = SessionData(
                sessionId = sessionId,
                userId = userId,
                deviceId = deviceId,
                deviceFingerprint = deviceFingerprint,
                createdAt = 0L, // Not updated during refresh
                expiresAt = newExpiryTime,
                lastActivity = currentTime
            )
            
            _sessionState.value = SessionState.Valid(sessionId, userId, newExpiryTime, currentTime)
            
            Log.d(TAG, "Session refreshed successfully: $sessionId")
            return@withContext Result.success(refreshedSession)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing session", e)
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Invalidate current session
     */
    suspend fun invalidateSession(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sessionId = securePrefs.getString(KEY_SESSION_ID, null)
            
            if (sessionId != null) {
                Log.d(TAG, "Invalidating session: $sessionId")
                
                // Mark session as inactive in Firestore
                firestore.collection(SESSIONS_COLLECTION)
                    .document(sessionId)
                    .update("isActive", false)
                    .await()
            }
            
            // Clear local session data
            clearLocalSession()
            
            _sessionState.value = SessionState.Invalid
            
            Log.d(TAG, "Session invalidated successfully")
            return@withContext Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error invalidating session", e)
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Get all active sessions for a user
     */
    suspend fun getActiveSessions(userId: String): List<SessionData> = withContext(Dispatchers.IO) {
        try {
            val querySnapshot = firestore.collection(SESSIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            return@withContext querySnapshot.documents.mapNotNull { doc ->
                try {
                    SessionData(
                        sessionId = doc.getString("sessionId") ?: "",
                        userId = doc.getString("userId") ?: "",
                        deviceId = doc.getString("deviceId") ?: "",
                        deviceFingerprint = doc.getString("deviceFingerprint") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        expiresAt = doc.getLong("expiresAt") ?: 0L,
                        lastActivity = doc.getLong("lastActivity") ?: 0L,
                        isActive = doc.getBoolean("isActive") ?: false
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing session document: ${doc.id}", e)
                    null
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting active sessions", e)
            return@withContext emptyList()
        }
    }
    
    /**
     * Revoke a specific session
     */
    suspend fun revokeSession(sessionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Revoking session: $sessionId")
            
            firestore.collection(SESSIONS_COLLECTION)
                .document(sessionId)
                .update("isActive", false)
                .await()
            
            // If this is the current session, clear local data
            val currentSessionId = securePrefs.getString(KEY_SESSION_ID, null)
            if (currentSessionId == sessionId) {
                clearLocalSession()
                _sessionState.value = SessionState.Invalid
            }
            
            Log.d(TAG, "Session revoked successfully: $sessionId")
            return@withContext Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error revoking session", e)
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Create secure preferences for session storage
     */
    private fun createSecurePreferences(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            EncryptedSharedPreferences.create(
                context,
                SESSION_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create encrypted preferences, using regular preferences", e)
            context.getSharedPreferences(SESSION_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
    
    /**
     * Store session data locally with encryption
     */
    private fun storeSessionLocally(sessionData: SessionData) {
        securePrefs.edit()
            .putString(KEY_SESSION_ID, sessionData.sessionId)
            .putString(KEY_USER_ID, sessionData.userId)
            .putString(KEY_DEVICE_ID, sessionData.deviceId)
            .putLong(KEY_SESSION_EXPIRY, sessionData.expiresAt)
            .putLong(KEY_LAST_ACTIVITY, sessionData.lastActivity)
            .apply()
    }
    
    /**
     * Clear local session data
     */
    private fun clearLocalSession() {
        securePrefs.edit()
            .remove(KEY_SESSION_ID)
            .remove(KEY_USER_ID)
            .remove(KEY_DEVICE_ID)
            .remove(KEY_SESSION_EXPIRY)
            .remove(KEY_LAST_ACTIVITY)
            .apply()
    }
    
    /**
     * Validate session with server
     */
    private suspend fun validateSessionWithServer(sessionId: String, userId: String): Boolean {
        return try {
            val doc = firestore.collection(SESSIONS_COLLECTION)
                .document(sessionId)
                .get()
                .await()
            
            if (!doc.exists()) {
                Log.w(TAG, "Session not found on server: $sessionId")
                return false
            }
            
            val serverUserId = doc.getString("userId")
            val isActive = doc.getBoolean("isActive") ?: false
            val serverFingerprint = doc.getString("deviceFingerprint")
            
            if (serverUserId != userId || !isActive || serverFingerprint != deviceFingerprint) {
                Log.w(TAG, "Session validation failed: $sessionId")
                return false
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error validating session with server", e)
            false
        }
    }
    
    /**
     * Update last activity timestamp
     */
    private suspend fun updateLastActivity(sessionId: String) {
        try {
            val currentTime = System.currentTimeMillis()
            
            // Update in Firestore (fire and forget)
            firestore.collection(SESSIONS_COLLECTION)
                .document(sessionId)
                .update("lastActivity", FieldValue.serverTimestamp())
            
            // Update locally
            securePrefs.edit()
                .putLong(KEY_LAST_ACTIVITY, currentTime)
                .apply()
                
        } catch (e: Exception) {
            Log.w(TAG, "Error updating last activity", e)
        }
    }
    
    /**
     * Clean up old sessions for a user
     */
    private suspend fun cleanupOldSessions(userId: String) {
        try {
            val activeSessions = getActiveSessions(userId)
            
            if (activeSessions.size > MAX_CONCURRENT_SESSIONS) {
                // Sort by last activity and revoke oldest sessions
                val sessionsToRevoke = activeSessions
                    .sortedBy { it.lastActivity }
                    .take(activeSessions.size - MAX_CONCURRENT_SESSIONS)
                
                sessionsToRevoke.forEach { session ->
                    revokeSession(session.sessionId)
                }
                
                Log.d(TAG, "Cleaned up ${sessionsToRevoke.size} old sessions for user: $userId")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning up old sessions", e)
        }
    }
    
    /**
     * Generate unique session ID
     */
    private fun generateSessionId(): String {
        return UUID.randomUUID().toString()
    }
    
    /**
     * Get or create device ID
     */
    private fun getOrCreateDeviceId(): String {
        var deviceId = securePrefs.getString(KEY_DEVICE_ID, null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            securePrefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        return deviceId
    }
    
    /**
     * Generate device fingerprint for security
     */
    private fun generateDeviceFingerprint(): String {
        return try {
            val deviceInfo = StringBuilder()
            deviceInfo.append(android.os.Build.MODEL)
            deviceInfo.append(android.os.Build.MANUFACTURER)
            deviceInfo.append(android.os.Build.DEVICE)
            deviceInfo.append(android.os.Build.PRODUCT)
            deviceInfo.append(android.os.Build.VERSION.RELEASE)
            
            // Hash the device info for privacy
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(deviceInfo.toString().toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Error generating device fingerprint", e)
            UUID.randomUUID().toString()
        }
    }
    
    /**
     * Get device information for session tracking
     */
    private fun getDeviceInfo(): Map<String, String> {
        return mapOf(
            "model" to android.os.Build.MODEL,
            "manufacturer" to android.os.Build.MANUFACTURER,
            "osVersion" to android.os.Build.VERSION.RELEASE,
            "appVersion" to try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
            } catch (e: Exception) {
                "unknown"
            }
        )
    }
}