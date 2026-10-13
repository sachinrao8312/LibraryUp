package ai.sach.libraryup.ui.screens.authentication

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.credentials.CredentialManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Centralized authentication repository that handles all authentication methods
 * and provides a single source of truth for the authentication state.
 */
class AuthRepository(private val context: Context) {
    private val TAG = "AuthRepository"

    // Firebase instances
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Credential Manager for modern authentication
    private val credentialManager by lazy { CredentialManager.create(context) }

    // Authentication state
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    // Secure storage for tokens and device fingerprints with fallback
    private val securePrefs = EncryptedPrefsUtils.createSecurePrefs(
        context = context,
        fileName = "auth_secure_prefs",
        fallbackFileName = "auth_prefs_fallback"
    )

    // Constants
    companion object {
        private const val USERS_COLLECTION = "users"
        private const val GUESTS_COLLECTION = "guests"
        private const val SESSIONS_COLLECTION = "sessions"
        private const val SESSION_DURATION = 24 * 60 * 60 * 1000L // 24 hours
        private const val PREF_IS_GUEST = "is_guest_user"
        private const val PREF_GUEST_CREATED_ACCOUNT = "guest_created_account"
    }

    init {
        // Initialize auth state listener
        initAuthStateListener()

        // Check initial auth state
        checkInitialAuthState()
    }

    /**
     * Sets up Firebase Auth state listener
     */
    private fun initAuthStateListener() {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                updateAuthState(user)
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
        auth.addAuthStateListener(authStateListener!!)
    }

    /**
     * Removes the Firebase Auth state listener to prevent memory leaks
     */
    fun removeAuthStateListener() {
        authStateListener?.let { auth.removeAuthStateListener(it) }
        authStateListener = null
    }

    /**
     * Gets the current Firebase user directly.
     * Used for accessing user properties like photoUrl that may not be stored in Firestore.
     *
     * @return The current FirebaseUser or null if not authenticated
     */
    fun getCurrentFirebaseUser(): FirebaseUser? {
        return auth.currentUser
    }

    /**
     * Checks initial authentication state with improved guest handling
     */
    private fun checkInitialAuthState() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "Current user found: ${currentUser.uid}, isAnonymous: ${currentUser.isAnonymous}")

            if (currentUser.isAnonymous) {
                // This is a guest user
                _authState.value = AuthState.Guest
                // Store guest status
                securePrefs.edit().putBoolean(PREF_IS_GUEST, true).apply()
            } else {
                // This is a regular authenticated user
                _authState.value = AuthState.Authenticated(currentUser.uid)
                securePrefs.edit().putBoolean(PREF_IS_GUEST, false).apply()
            }
        } else {
            Log.d(TAG, "No current user found")
            _authState.value = AuthState.Unauthenticated
            securePrefs.edit().remove(PREF_IS_GUEST).apply()
        }
    }

    /**
     * Updates the auth state based on the current Firebase user
     */
    private fun updateAuthState(user: FirebaseUser?) {
        if (user == null) {
            _authState.value = AuthState.Unauthenticated
            securePrefs.edit().remove(PREF_IS_GUEST).apply()
            return
        }

        // Check if it's a guest account
        if (user.isAnonymous) {
            _authState.value = AuthState.Guest
            securePrefs.edit().putBoolean(PREF_IS_GUEST, true).apply()
        } else {
            _authState.value = AuthState.Authenticated(user.uid)
            securePrefs.edit().putBoolean(PREF_IS_GUEST, false).apply()
        }
    }

    /**
     * Check if current user is a guest
     */
    fun isCurrentUserGuest(): Boolean {
        val currentUser = auth.currentUser
        return currentUser != null && currentUser.isAnonymous
    }

    /**
     * Check if guest conversion is possible
     */
    fun canConvertGuestAccount(): Boolean {
        val currentUser = auth.currentUser
        return currentUser != null && currentUser.isAnonymous
    }

    /**
     * Ensure guest account exists and is properly set up
     */
    suspend fun ensureGuestAccount(): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser
            
            if (currentUser != null && currentUser.isAnonymous) {
                Log.d(TAG, "Guest account already exists: ${currentUser.uid}")
                return@withContext Result.success(currentUser)
            }
            
            // If no guest user, create one
            if (currentUser == null) {
                val result = signInAsGuest()
                return@withContext result
            }
            
            // If user exists but is not anonymous, this is an error
            Log.e(TAG, "Cannot ensure guest account: user is already authenticated")
            return@withContext Result.failure(Exception("User is already authenticated"))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring guest account", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Check if Google Sign-In is available and user is signed in
     */
    fun isGoogleSignedIn(): Boolean {
        val currentUser = auth.currentUser
        return currentUser != null && !currentUser.isAnonymous && 
               currentUser.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
    }

    /**
     * Check if a Google account exists in the system by email
     * This method validates account existence before operations
     * 
     * @param email The email address to check
     * @return true if account exists, false otherwise
     */
    suspend fun checkGoogleAccountExists(email: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (email.isBlank()) return@withContext false
            
            Log.d(TAG, "Checking if Google account exists for email: $email")
            
            // Check in users collection for existing Google accounts
            val userQuery = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("email", email)
                .whereEqualTo("accountType", "google")
                .get()
                .await()
            
            val exists = !userQuery.isEmpty
            Log.d(TAG, "Google account exists for $email: $exists")
            
            return@withContext exists
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Google account existence for $email", e)
            return@withContext false
        }
    }

    /**
     * Sign in with existing Google account
     * This method handles the flow for users who already have accounts
     * 
     * @param task The Task returned from GoogleSignIn.getSignedInAccountFromIntent()
     * @return Result with FirebaseUser if successful
     */
    suspend fun signInWithExistingGoogleAccount(task: Task<GoogleSignInAccount>): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to sign in with existing Google account")
            
            val account = googleAuthService.processSignInResult(task)
                ?: return@withContext Result.failure(Exception("Google sign-in failed"))
            
            val email = account.email
            if (email == null) {
                return@withContext Result.failure(Exception("Google account email is null"))
            }
            
            // Verify account exists before proceeding
            val accountExists = checkGoogleAccountExists(email)
            if (!accountExists) {
                Log.w(TAG, "Attempted to sign in with non-existent account: $email")
                return@withContext Result.failure(Exception("Account does not exist. Please create an account first."))
            }
            
            // Proceed with normal Google sign-in flow
            val credential = googleAuthService.getFirebaseCredential(account)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Authentication failed")
            
            // Update last login and session
            createSession(user.uid)
            updateUserInFirestore(user)
            
            Log.d(TAG, "Successfully signed in existing user: ${user.uid}")
            return@withContext Result.success(user)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error signing in with existing Google account", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Validation result for guest conversion attempts
     */
    sealed class ConversionValidationResult {
        object CanConvert : ConversionValidationResult()
        data class AccountExists(val email: String) : ConversionValidationResult()
        data class RequiresUserChoice(val conflictType: ConflictType) : ConversionValidationResult()
        data class ValidationFailed(val reason: String) : ConversionValidationResult()
    }

    /**
     * Types of conflicts that can occur during conversion
     */
    enum class ConflictType {
        GOOGLE_ACCOUNT_EXISTS,
        GUEST_DATA_EXISTS,
        BOTH_HAVE_DATA
    }

    /**
     * Validate guest conversion before attempting it
     * This method performs pre-conversion checks to detect conflicts
     * 
     * @param task The Task returned from GoogleSignIn.getSignedInAccountFromIntent()
     * @return ConversionValidationResult indicating the validation outcome
     */
    suspend fun validateGuestConversion(task: Task<GoogleSignInAccount>): ConversionValidationResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Validating guest conversion")
            
            // Check if current user is actually a guest
            val currentUser = auth.currentUser
            if (currentUser == null || !currentUser.isAnonymous) {
                return@withContext ConversionValidationResult.ValidationFailed("No guest user is currently signed in")
            }
            
            // Process Google sign-in result to get account info
            val account = googleAuthService.processSignInResult(task)
                ?: return@withContext ConversionValidationResult.ValidationFailed("Google sign-in failed")
            
            val email = account.email
            if (email == null) {
                return@withContext ConversionValidationResult.ValidationFailed("Google account email is null")
            }
            
            // Check if Google account already exists
            val googleAccountExists = checkGoogleAccountExists(email)
            
            // Check if guest has valuable data
            val guestHasData = hasGuestCreatedAccount()
            
            Log.d(TAG, "Validation results - Google account exists: $googleAccountExists, Guest has data: $guestHasData")
            
            return@withContext when {
                googleAccountExists && guestHasData -> {
                    ConversionValidationResult.RequiresUserChoice(ConflictType.BOTH_HAVE_DATA)
                }
                googleAccountExists -> {
                    ConversionValidationResult.AccountExists(email)
                }
                guestHasData -> {
                    ConversionValidationResult.RequiresUserChoice(ConflictType.GUEST_DATA_EXISTS)
                }
                else -> {
                    ConversionValidationResult.CanConvert
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error validating guest conversion", e)
            return@withContext ConversionValidationResult.ValidationFailed("Validation error: ${e.message}")
        }
    }

    /**
     * Check if guest user has created an account (has profile data)
     */
    suspend fun hasGuestCreatedAccount(): Boolean = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser
        if (currentUser == null || !currentUser.isAnonymous) return@withContext false

        try {
            val doc = firestore.collection(GUESTS_COLLECTION)
                .document(currentUser.uid)
                .get()
                .await()

            return@withContext doc.exists() &&
                    doc.getString("name")?.isNotBlank() == true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking guest account", e)
            return@withContext false
        }
    }

    /**
     * Handles sign in with Google. This method is designed to work with
     * the activity result from the Google Sign-In intent.
     *
     * @param task The Task returned from GoogleSignIn.getSignedInAccountFromIntent()
     * @return Result with FirebaseUser if successful
     */
    suspend fun signInWithGoogle(task: Task<GoogleSignInAccount>) = withContext(Dispatchers.IO) {
        try {
            // Use our shared GoogleAuthService for the sign-in flow
            val result = googleAuthService.completeGoogleSignIn(task)

            // If successful, create a session
            result.fold(
                onSuccess = { user ->
                    // Log the user's information including photo URL
                    Log.d(TAG, "Google sign-in successful for user: ${user.displayName}")
                    Log.d(TAG, "User email: ${user.email}")
                    Log.d(TAG, "User photo URL: ${user.photoUrl}")

                    createSession(user.uid)

                    // Make sure we update the user profile in Firestore
                    updateUserInFirestore(user)
                },
                onFailure = { /* Session creation will be skipped */ }
            )

            return@withContext result
        } catch (e: Exception) {
            Log.e(TAG, "Error signing in with Google", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Convert guest account to permanent account using Google Sign-In
     * Handles the case where the Google credential is already linked to another account
     */
    sealed class ConversionResult {
        data class Success(val user: FirebaseUser) : ConversionResult()
        data class SuccessWithMigrationFailure(val user: FirebaseUser) : ConversionResult()
        data class Failure(val exception: Exception) : ConversionResult()
    }

    /**
     * Enhanced guest conversion with pre-validation and safe data migration
     * This method now includes comprehensive validation and user choice handling
     */
    suspend fun convertGuestToGoogleAccount(
        task: Task<GoogleSignInAccount>,
        skipValidation: Boolean = false
    ): ConversionResult = withContext(Dispatchers.IO) {
        val guestUid = auth.currentUser?.uid
        Log.i(TAG, "[CONVERSION_START] Attempting to convert guest user $guestUid to Google account.")

        try {
            val currentUser = auth.currentUser
            
            if (currentUser == null || !currentUser.isAnonymous) {
                Log.e(TAG, "[CONVERSION_FAILURE] No guest user is currently signed in. UID: $guestUid")
                return@withContext ConversionResult.Failure(Exception("No guest user is currently signed in."))
            }

            // Pre-conversion validation (unless explicitly skipped)
            if (!skipValidation) {
                Log.d(TAG, "[CONVERSION_VALIDATION] Performing pre-conversion validation")
                val validationResult = validateGuestConversion(task)
                
                when (validationResult) {
                    is ConversionValidationResult.ValidationFailed -> {
                        Log.e(TAG, "[CONVERSION_VALIDATION_FAILED] ${validationResult.reason}")
                        return@withContext ConversionResult.Failure(Exception(validationResult.reason))
                    }
                    is ConversionValidationResult.RequiresUserChoice -> {
                        Log.w(TAG, "[CONVERSION_REQUIRES_CHOICE] User choice required for conflict: ${validationResult.conflictType}")
                        return@withContext ConversionResult.Failure(Exception("User choice required for account conflict"))
                    }
                    is ConversionValidationResult.AccountExists -> {
                        Log.w(TAG, "[CONVERSION_ACCOUNT_EXISTS] Account exists: ${validationResult.email}")
                        return@withContext ConversionResult.Failure(Exception("Account already exists: ${validationResult.email}"))
                    }
                    is ConversionValidationResult.CanConvert -> {
                        Log.d(TAG, "[CONVERSION_VALIDATION_PASSED] Validation passed, proceeding with conversion")
                    }
                }
            }

            val account = googleAuthService.processSignInResult(task)
                ?: run {
                    Log.e(TAG, "[CONVERSION_FAILURE] Google sign-in failed for guest user $guestUid.")
                    return@withContext ConversionResult.Failure(Exception("Google sign-in failed"))
                }

            val credential = googleAuthService.getFirebaseCredential(account)

            // Create conversion progress tracker
            val conversionTracker = ConversionProgressTracker(guestUid ?: "unknown")
            
            try {
                conversionTracker.updateProgress("Linking accounts", 25)
                Log.d(TAG, "[CONVERSION_LINKING] Attempting to link anonymous account for guest user $guestUid")
                
                val result = currentUser.linkWithCredential(credential).await()
                val linkedUser = result.user ?: throw Exception("Account linking failed")
                Log.i(TAG, "[CONVERSION_LINKING_SUCCESS] Successfully linked guest user $guestUid to new user ${linkedUser.uid}.")

                // linkWithCredential does not copy the linked provider's displayName/photoUrl
                // onto the FirebaseUser (unlike a fresh signInWithCredential), so without this
                // the user's profile picture never shows up even though Firestore has it.
                syncProfileFromGoogleAccount(linkedUser, account)

                conversionTracker.updateProgress("Migrating data", 50)
                val migrationSuccess = migrateDataToPermanentAccountSafely(currentUser.uid, linkedUser, account, conversionTracker)

                conversionTracker.updateProgress("Finalizing conversion", 90)
                securePrefs.edit()
                    .putBoolean(PREF_IS_GUEST, false)
                    .putBoolean(PREF_GUEST_CREATED_ACCOUNT, false)
                    .apply()

                conversionTracker.updateProgress("Complete", 100)
                
                if (migrationSuccess) {
                    Log.i(TAG, "[CONVERSION_SUCCESS] Successfully converted guest user $guestUid to permanent user ${linkedUser.uid}.")
                    return@withContext ConversionResult.Success(linkedUser)
                } else {
                    Log.w(TAG, "[CONVERSION_SUCCESS_WITH_MIGRATION_FAILURE] Successfully converted guest user $guestUid to permanent user ${linkedUser.uid}, but data migration failed.")
                    return@withContext ConversionResult.SuccessWithMigrationFailure(linkedUser)
                }
                
            } catch (collisionException: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                Log.w(TAG, "[CONVERSION_COLLISION] Google credential already linked to another account. Guest UID: $guestUid. Collision UID: ${collisionException.email}", collisionException)
                
                conversionTracker.updateProgress("Handling account collision", 30)
                
                try {
                    auth.signOut()
                    
                    val authResult = auth.signInWithCredential(credential).await()
                    val existingUser = authResult.user ?: throw Exception("Failed to sign in with existing account")
                    Log.i(TAG, "[CONVERSION_COLLISION_SWITCH] Switched to existing user ${existingUser.uid} for guest user $guestUid.")
                    syncProfileFromGoogleAccount(existingUser, account)

                    conversionTracker.updateProgress("Migrating to existing account", 60)
                    val migrationSuccess = migrateDataToPermanentAccountSafely(guestUid, existingUser, account, conversionTracker)
                    
                    securePrefs.edit()
                        .putBoolean(PREF_IS_GUEST, false)
                        .putBoolean(PREF_GUEST_CREATED_ACCOUNT, false)
                        .apply()
                    
                    conversionTracker.updateProgress("Complete", 100)
                    
                    if (migrationSuccess) {
                        Log.i(TAG, "[CONVERSION_COLLISION_SUCCESS] Successfully switched to existing Google account ${existingUser.uid} after collision for guest user $guestUid.")
                        return@withContext ConversionResult.Success(existingUser)
                    } else {
                        Log.w(TAG, "[CONVERSION_COLLISION_SUCCESS_WITH_MIGRATION_FAILURE] Successfully switched to existing Google account ${existingUser.uid} after collision for guest user $guestUid, but data migration failed.")
                        return@withContext ConversionResult.SuccessWithMigrationFailure(existingUser)
                    }
                    
                } catch (switchException: Exception) {
                    Log.e(TAG, "[CONVERSION_COLLISION_FAILURE] Failed to switch to existing account after collision for guest user $guestUid.", switchException)
                    conversionTracker.updateProgress("Failed", 0)
                    return@withContext ConversionResult.Failure(switchException)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "[CONVERSION_FAILURE] An unexpected error occurred during guest conversion for guest user $guestUid.", e)
            return@withContext ConversionResult.Failure(e)
        }
    }

    /**
     * Safe data migration with atomic operations and rollback capability
     */
    private suspend fun migrateDataToPermanentAccountSafely(
        guestUserId: String?, 
        permanentUser: FirebaseUser, 
        account: GoogleSignInAccount,
        progressTracker: ConversionProgressTracker
    ): Boolean {
        if (guestUserId == null) return false
        
        val maxRetries = 3
        var currentRetry = 0
        var success = false
        var backupData: Map<String, Any>? = null

        while (currentRetry < maxRetries && !success) {
            try {
                progressTracker.updateProgress("Migrating data (attempt ${currentRetry + 1})", 50 + (currentRetry * 10))
                Log.i(TAG, "[MIGRATION_START] Attempting to migrate data for guest user $guestUserId to permanent user ${permanentUser.uid}. Retry: $currentRetry")
                
                // 1. Create backup of existing permanent user data (if any)
                val permanentUserDocRef = firestore.collection(USERS_COLLECTION).document(permanentUser.uid)
                val existingPermanentDoc = permanentUserDocRef.get().await()
                if (existingPermanentDoc.exists()) {
                    backupData = existingPermanentDoc.data
                    Log.d(TAG, "[MIGRATION_BACKUP] Created backup of existing permanent user data")
                }
                
                // 2. Fetch guest user data from Firestore
                val guestDocRef = firestore.collection(GUESTS_COLLECTION).document(guestUserId)
                val guestDoc = guestDocRef.get().await()
                
                // 3. Create merged user data with conflict resolution
                val userData = createMergedUserData(permanentUser, account, guestDoc, backupData)
                
                // 4. Atomic write operation
                firestore.runTransaction { transaction ->
                    // Write merged data to permanent user document
                    transaction.set(permanentUserDocRef, userData, SetOptions.merge())
                    
                    // Delete guest document only after successful write
                    transaction.delete(guestDocRef)
                }.await()
                
                Log.i(TAG, "[MIGRATION_SUCCESS] User data migrated to permanent account for user ${permanentUser.uid}.")
                

                success = true
                
            } catch (e: Exception) {
                currentRetry++
                Log.w(TAG, "[MIGRATION_FAILURE] Firestore data migration failed for guest user $guestUserId. Retry: $currentRetry", e)
                
                // Attempt rollback if we have backup data
                if (currentRetry == maxRetries && backupData != null) {
                    try {
                        firestore.collection(USERS_COLLECTION)
                            .document(permanentUser.uid)
                            .set(backupData, SetOptions.merge())
                            .await()
                        Log.i(TAG, "[MIGRATION_ROLLBACK] Restored backup data after migration failure")
                    } catch (rollbackException: Exception) {
                        Log.e(TAG, "[MIGRATION_ROLLBACK_FAILED] Failed to restore backup data", rollbackException)
                    }
                }
                
                if (currentRetry < maxRetries) {
                    kotlinx.coroutines.delay(1000L * currentRetry) // Exponential backoff
                } else {
                    Log.e(TAG, "[MIGRATION_FAILURE] All retries failed for guest user $guestUserId.")
                }
            }
        }
        return success
    }

    /**
     * Create merged user data with intelligent conflict resolution
     */
    private fun createMergedUserData(
        permanentUser: FirebaseUser,
        account: GoogleSignInAccount,
        guestDoc: com.google.firebase.firestore.DocumentSnapshot,
        existingData: Map<String, Any>?
    ): Map<String, Any> {
        val userData = mutableMapOf<String, Any>(
            "uid" to permanentUser.uid,
            "email" to (permanentUser.email ?: account.email ?: ""),
            "displayName" to (permanentUser.displayName ?: account.displayName ?: "User"),
            "photoUrl" to (permanentUser.photoUrl?.toString() ?: account.photoUrl?.toString() ?: ""),
            "isGuestAccount" to false,
            "lastLogin" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "accountType" to "google",
            "conversionDate" to FieldValue.serverTimestamp()
        )
        
        // Add guest data if it exists
        if (guestDoc.exists()) {
            userData["previousGuestId"] = guestDoc.id
            
            for (field in guestDoc.data?.entries ?: emptySet()) {
                // Skip fields we've already set or that shouldn't be copied
                if (field.key !in setOf("uid", "email", "isGuestAccount", "accountType")) {
                    // Prefer guest data over existing data for user-generated content
                    if (field.key in listOf("name", "phoneNumber", "preferences")) {
                        userData[field.key] = field.value
                    } else if (!userData.containsKey(field.key)) {
                        userData[field.key] = field.value
                    }
                }
            }
        }
        
        // Merge with existing permanent user data (if any)
        existingData?.let { existing ->
            for (field in existing.entries) {
                // Preserve certain existing fields
                if (field.key in listOf("createdAt", "subscriptionStatus", "premiumFeatures")) {
                    userData[field.key] = field.value
                }
            }
        }
        
        return userData
    }

    private suspend fun migrateDataToPermanentAccount(guestUserId: String?, permanentUser: FirebaseUser, account: GoogleSignInAccount): Boolean {
        if (guestUserId == null) return false
        val maxRetries = 3
        var currentRetry = 0
        var success = false

        while (currentRetry < maxRetries && !success) {
            try {
                Log.i(TAG, "[MIGRATION_START] Attempting to migrate data for guest user $guestUserId to permanent user ${permanentUser.uid}. Retry: $currentRetry")
                // 1. Fetch guest user data from Firestore
                val guestDocRef = firestore.collection(GUESTS_COLLECTION).document(guestUserId)
                val guestDoc = guestDocRef.get().await()
                
                // 2. Create permanent user document with merged data
                val userData = mutableMapOf<String, Any>(
                    "uid" to permanentUser.uid,
                    "email" to (permanentUser.email ?: account.email ?: ""),
                    "displayName" to (permanentUser.displayName ?: account.displayName ?: "User"),
                    "photoUrl" to (permanentUser.photoUrl?.toString() ?: account.photoUrl?.toString() ?: ""),
                    "isGuestAccount" to false,
                    "previousGuestId" to guestUserId,
                    "lastLogin" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "accountType" to "google",
                    "conversionDate" to FieldValue.serverTimestamp()
                )
                
                // Add all fields from guest document if they exist
                if (guestDoc.exists()) {
                    for (field in guestDoc.data?.entries ?: emptySet()) {
                        // Skip fields we've already set or that shouldn't be copied
                        if (field.key !in setOf("uid", "email", "isGuestAccount", "accountType")) {
                            userData[field.key] = field.value
                        }
                    }
                }
                
                // 3. Save the merged data to the permanent user document
                firestore.collection(USERS_COLLECTION)
                    .document(permanentUser.uid)
                    .set(userData, SetOptions.merge())
                    .await()
                
                Log.i(TAG, "[MIGRATION_SUCCESS] User data migrated to permanent account for user ${permanentUser.uid}.")
                
                // 4. Delete the guest document (but only after successful migration)
                guestDocRef.delete().await()
                Log.i(TAG, "[MIGRATION_CLEANUP_SUCCESS] Guest user document deleted for guest user $guestUserId.")
                
                // 5. Update any references to the guest user ID in other collections in Firestore

                success = true
                
            } catch (e: Exception) {
                currentRetry++
                Log.w(TAG, "[MIGRATION_FAILURE] Firestore data migration failed for guest user $guestUserId. Retry: $currentRetry", e)
                if (currentRetry < maxRetries) {
                    kotlinx.coroutines.delay(1000L * currentRetry) // Exponential backoff
                } else {
                    Log.e(TAG, "[MIGRATION_FAILURE] All retries failed for guest user $guestUserId.")
                }
            }
        }
        return success
    }

    /**
     * Signs in as a guest (anonymous authentication)
     */
    suspend fun signInAsGuest(): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            // Sign in anonymously with Firebase
            val result = auth.signInAnonymously().await()
            val user = result.user ?: throw Exception("Guest authentication failed")

            // Create session and guest user document
            createSession(user.uid)
            createGuestUserDocument(user.uid)

            // Update preferences
            securePrefs.edit().putBoolean(PREF_IS_GUEST, true).apply()

            Log.d(TAG, "Guest sign-in successful: ${user.uid}")
            return@withContext Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error in guest authentication", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Create guest user document in Firestore
     */
    private suspend fun createGuestUserDocument(uid: String) {
        try {
            val guestData = mapOf(
                "uid" to uid,
                "isGuestAccount" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "lastLogin" to FieldValue.serverTimestamp()
            )

            firestore.collection(GUESTS_COLLECTION)
                .document(uid)
                .set(guestData, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error creating guest user document", e)
        }
    }

    /**
     * Updates user profile data
     * Works for both guest and authenticated users
     */
    suspend fun updateUserProfile(userData: UserData): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser
                ?: return@withContext Result.failure(Exception("No current user found"))

            val userId = currentUser.uid
            val isGuestAccount = currentUser.isAnonymous

            // Create user data map. photoUrl is only included when we actually have
            // one, so saving a profile edit never wipes out a previously stored photo.
            val userMap = buildMap {
                put("name", userData.name)
                put("email", userData.email ?: "")
                put("phoneNumber", userData.phoneNumber)
                put("updatedAt", FieldValue.serverTimestamp())
                userData.photoUrl?.let { put("photoUrl", it) }
            }

            // Save to the appropriate collection
            val collection = if (isGuestAccount) GUESTS_COLLECTION else USERS_COLLECTION

            firestore.collection(collection)
                .document(userId)
                .update(userMap)
                .await()

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user profile", e)
            Result.failure(e)
        }
    }

    /**
     * Creates or updates a user account (works for both guest and regular users)
     */
    suspend fun createUserAccount(userData: UserData, profileImageUri: Uri? = null): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser
                ?: return@withContext Result.failure(Exception("No current user found"))

            val userId = currentUser.uid
            val isGuestAccount = currentUser.isAnonymous

            // Get photo URL either from Firebase user or from provided URI
            val photoUrl = profileImageUri?.toString() ?: currentUser.photoUrl?.toString() ?: ""

            // Create user data map with photo URL
            val userMap = mapOf(
                "uid" to userId,
                "name" to userData.name,
                "email" to (userData.email ?: ""),
                "phoneNumber" to userData.phoneNumber,
                "photoUrl" to photoUrl,
                "isGuestAccount" to isGuestAccount,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            // Add creation timestamp for new accounts
            val finalMap = if (!documentExists(userId, isGuestAccount)) {
                userMap + ("createdAt" to FieldValue.serverTimestamp())
            } else {
                userMap
            }

            // Save to the appropriate collection
            val collection = if (isGuestAccount) GUESTS_COLLECTION else USERS_COLLECTION

            firestore.collection(collection)
                .document(userId)
                .set(finalMap, SetOptions.merge())
                .await()

            // Mark that guest has created account
            if (isGuestAccount) {
                securePrefs.edit().putBoolean(PREF_GUEST_CREATED_ACCOUNT, true).apply()
            }

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating/updating user account", e)
            Result.failure(e)
        }
    }

    /**
     * Check if a user document exists
     */
    private suspend fun documentExists(userId: String, isGuest: Boolean): Boolean {
        val collection = if (isGuest) GUESTS_COLLECTION else USERS_COLLECTION
        val document = firestore.collection(collection).document(userId).get().await()
        return document.exists()
    }

    /**
     * Signs out the current user
     */
    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()

            // Try to delete session if user is authenticated (optional operation)
            userId?.let {
                try {
                    firestore.collection(SESSIONS_COLLECTION)
                        .document(it)
                        .delete()
                        .await()
                    Log.d(TAG, "Session deleted successfully for user: $it")
                } catch (e: Exception) {
                    Log.w(TAG, "Session deletion failed (this is optional): ${e.message}")
                    // Don't fail the sign out process
                }
            }

            // Clear preferences
            securePrefs.edit()
                .remove(PREF_IS_GUEST)
                .remove(PREF_GUEST_CREATED_ACCOUNT)
                .apply()

            // Sign out from Firebase
            auth.signOut()

            _authState.value = AuthState.Unauthenticated
            Log.d(TAG, "User signed out successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out", e)
            Result.failure(e)
        }
    }

    /**
     * Helper method to create a session for the authenticated user
     * This is optional and won't fail authentication if it doesn't work
     */
    private suspend fun createSession(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val sessionData = mapOf(
                "userId" to userId,
                "createdAt" to FieldValue.serverTimestamp(),
                "expiresAt" to (System.currentTimeMillis() + SESSION_DURATION),
                "deviceInfo" to mapOf(
                    "model" to android.os.Build.MODEL,
                    "manufacturer" to android.os.Build.MANUFACTURER,
                    "os" to "Android ${android.os.Build.VERSION.RELEASE}"
                ),
                "lastActivity" to FieldValue.serverTimestamp()
            )

            firestore.collection(SESSIONS_COLLECTION)
                .document(userId)
                .set(sessionData)
                .await()

            Log.d(TAG, "Session created successfully for user: $userId")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Session creation failed (this is optional): ${e.message}")
            // Return true because session creation is optional
            true
        }
    }

    /**
     * Copies displayName/photoUrl from a Google account onto the FirebaseUser itself.
     * Needed after linkWithCredential, which — unlike signInWithCredential — does not
     * update those fields on the FirebaseUser automatically.
     */
    private suspend fun syncProfileFromGoogleAccount(user: FirebaseUser, account: GoogleSignInAccount) {
        try {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(account.displayName ?: user.displayName)
                .setPhotoUri(account.photoUrl ?: user.photoUrl)
                .build()
            user.updateProfile(profileUpdates).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync profile from Google account: ${e.message}")
        }
    }

    /**
     * Updates user data in Firestore after successful authentication
     * This is optional and won't fail authentication if it doesn't work
     */
    private suspend fun updateUserInFirestore(user: FirebaseUser) {
        try {
            // Make sure we extract displayName correctly
            val displayName = user.displayName ?: ""
            val userData = mapOf(
                "uid" to user.uid,
                "email" to user.email,
                "name" to displayName,  // Store as "name" for consistency
                "displayName" to displayName,  // Also store as displayName
                "photoUrl" to (user.photoUrl?.toString() ?: ""),
                "isGuestAccount" to false,
                "lastLogin" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            firestore.collection(USERS_COLLECTION)
                .document(user.uid)
                .set(userData, SetOptions.merge())
                .await()
            
            Log.d(TAG, "User data updated successfully in Firestore for: ${user.uid}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update user in Firestore (this is optional): ${e.message}")
            // Don't fail authentication flow
        }
    }

    /**
     * Service for Google authentication
     */
    private val googleAuthService: GoogleAuthService by lazy {
        GoogleAuthService(context)
    }

    /**
     * Gets the intent for Google Sign In
     * @return Intent to be launched with ActivityResultLauncher, or null if Google Sign-In is not available
     */
    fun getGoogleSignInIntent(): Intent? {
        return googleAuthService.getBasicSignInIntent()
    }

    /**
     * Gets the GoogleAuthService instance for external access
     * This replaces the need for reflection in AccountCollisionHandler
     */
    fun getGoogleAuthServiceInstance(): GoogleAuthService {
        return googleAuthService
    }

    /**
     * Gets the current user ID
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Gets user data for the current user
     */
    suspend fun getUserData(uid: String): UserData? = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: return@withContext null
            
            Log.d(TAG, "Getting user data for uid: $uid")
            Log.d(TAG, "Firebase displayName: ${currentUser.displayName}")
            Log.d(TAG, "Firebase email: ${currentUser.email}")
            Log.d(TAG, "Firebase phoneNumber: ${currentUser.phoneNumber}")
            Log.d(TAG, "Firebase photoUrl: ${currentUser.photoUrl}")
            
            val isGuest = currentUser.isAnonymous
            val collection = if (isGuest) GUESTS_COLLECTION else USERS_COLLECTION
            
            try {
                val doc = firestore.collection(collection).document(uid).get().await()
                
                if (doc.exists()) {
                    val data = doc.data ?: return@withContext null
                    Log.d(TAG, "Retrieved from Firestore - name: ${data["name"]}, email: ${data["email"]}, photoUrl: ${data["photoUrl"]}")
                    
                    return@withContext UserData(
                        name = data["name"] as? String ?: currentUser.displayName ?: "",
                        email = data["email"] as? String ?: currentUser.email,
                        phoneNumber = data["phoneNumber"] as? String ?: currentUser.phoneNumber ?: "",
                        photoUrl = (data["photoUrl"] as? String)?.takeIf { it.isNotBlank() }
                            ?: currentUser.photoUrl?.toString()
                    )
                } else {
                    Log.d(TAG, "Document doesn't exist in Firestore, using Firebase Auth data")
                    // Fallback to Firebase Auth data
                    Log.d(TAG, "Using Firebase Auth data as fallback")
                    return@withContext UserData(
                        name = currentUser.displayName ?: "",
                        email = currentUser.email,
                        phoneNumber = currentUser.phoneNumber ?: "",
                        photoUrl = currentUser.photoUrl?.toString()
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user data from Firestore, using Firebase Auth data", e)
                // Fallback to Firebase Auth data
                return@withContext UserData(
                    name = currentUser.displayName ?: "",
                    email = currentUser.email,
                    phoneNumber = currentUser.phoneNumber ?: "",
                    photoUrl = currentUser.photoUrl?.toString()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user data", e)
            return@withContext null
        }
    }

}