package ai.sach.libraryup.ui.screens.authentication

import android.accounts.AccountManager.KEY_ACCOUNT_NAME
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import android.content.pm.PackageManager
import java.security.MessageDigest
import android.app.Activity
import android.content.pm.PackageInfo
import android.content.pm.PackageManager.GET_SIGNATURES
import ai.sach.libraryup.BuildConfig

/**
 * Centralized service for Google authentication that can be used by both
 * user authentication and Google Drive functionality.
 */
class GoogleAuthService(private val context: Context) {
    companion object {
        private const val TAG = "GoogleAuthService"
        private const val PREFS_NAME = "google_auth_prefs"
        private const val KEY_SIGNED_IN = "is_signed_in"
        private const val KEY_ACCOUNT_EMAIL = "account_email"
        private const val KEY_LAST_SIGN_IN = "last_sign_in_time"
        private const val KEY_DRIVE_ACCESS = "has_drive_access"
        private const val USERS_COLLECTION = "users"
        
        // Flag to completely disable Google Sign-In when errors occur
        private var isGoogleSignInDisabled = false
    }

    // Firebase instances
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Preferences for storing Google auth state
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // StateFlow for GoogleSignIn state
    private val _isSignedIn = MutableStateFlow(prefs.getBoolean(KEY_SIGNED_IN, false))
    val isSignedIn: StateFlow<Boolean> = _isSignedIn

    // StateFlow for Google Drive access
    private val _hasDriveAccess = MutableStateFlow(prefs.getBoolean(KEY_DRIVE_ACCESS, false))
    val hasDriveAccess: StateFlow<Boolean> = _hasDriveAccess

    // Google Sign In clients with different scopes
    private val basicSignInClient: GoogleSignInClient by lazy {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getWebClientId())
            .requestEmail()
            .build()

        GoogleSignIn.getClient(context, signInOptions)
    }

    private val driveSignInClient: GoogleSignInClient by lazy {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getWebClientId())
            .requestEmail()
            .requestScopes(
                Scope("https://www.googleapis.com/auth/drive.file"),
                Scope("https://www.googleapis.com/auth/drive.appdata")
            )
            .build()

        GoogleSignIn.getClient(context, signInOptions)
    }

    init {
        // Check if Google Play Services is available at initialization
        val (isAvailable, errorMessage) = checkPlayServicesCompatibility()
        if (!isAvailable) {
            Log.w(TAG, "Google Play Services not available at initialization: $errorMessage")
            isGoogleSignInDisabled = true
        }
        
        // Try to detect common Google Play Services issues early
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account == null) {
                // Try a basic operation that would fail if Google Play Services has issues
                val availability = GoogleApiAvailability.getInstance()
                val status = availability.isGooglePlayServicesAvailable(context)
                if (status != ConnectionResult.SUCCESS) {
                    Log.w(TAG, "Google Play Services check failed with status: $status")
                    isGoogleSignInDisabled = true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during Google Play Services pre-check: ${e.message}", e)
            // If we get any exception during initialization, disable Google Sign-In
            isGoogleSignInDisabled = true
        }
    }

    /**
     * Get Web Client ID from resources to avoid hardcoding
     */
    private fun getWebClientId(): String {
        val resId = context.resources.getIdentifier("google_web_client_id", "string", context.packageName)
        return if (resId != 0) {
            context.getString(resId)
        } else {
            Log.e(TAG, "Web Client ID not found in resources. Add 'google_web_client_id' to strings.xml")
            "" // Empty string will cause authentication to fail, forcing developers to add the ID
        }
    }

    /**
     * Checks if the user is signed in to Google
     * @return true if signed in, false otherwise
     */
    fun isUserSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        val signedIn = account != null && !account.isExpired
        
        // Update state if it doesn't match reality
        if (signedIn != _isSignedIn.value) {
            _isSignedIn.value = signedIn
            prefs.edit().putBoolean(KEY_SIGNED_IN, signedIn).apply()
        }
        
        return signedIn
    }

    /**
     * Get account email of the currently signed-in user
     * @return email address or null if not signed in
     */
    fun getSignedInAccountEmail(): String? {
        return prefs.getString(KEY_ACCOUNT_EMAIL, null)
    }

    /**
     * Get the intent for Google Sign In with basic scopes (for auth only)
     * @return Intent to be launched with ActivityResultLauncher, or null if not available
     */
    fun getBasicSignInIntent(): Intent? {
        // If Google Sign-In is disabled, return dummy intent immediately
        if (isGoogleSignInDisabled) {
            Log.w(TAG, "Google Sign-In is disabled due to previous errors")
            return Intent().apply { 
                action = "ai.sach.libraryup.DUMMY_GOOGLE_SIGN_IN"
                putExtra("ERROR_TYPE", "GOOGLE_SIGNIN_DISABLED")
                putExtra("ERROR_MESSAGE", "Google Sign-In has been disabled due to previous errors")
            }
        }
        
        return try {
            // Log app signature for debugging
            logAppSignatureInfo()
            
            // Check if Google Play Services is available with better diagnostics
            val (isCompatible, errorMessage) = checkPlayServicesCompatibility()
            if (!isCompatible) {
                Log.e(TAG, "Google Play Services compatibility check failed: $errorMessage")
                // Disable Google Sign-In for future attempts
                isGoogleSignInDisabled = true
                // Return dummy intent with detailed error
                return Intent().apply { 
                    action = "ai.sach.libraryup.DUMMY_GOOGLE_SIGN_IN"
                    putExtra("ERROR_TYPE", "PLAY_SERVICES_UNAVAILABLE")
                    putExtra("ERROR_MESSAGE", errorMessage)
                }
            }
            
            try {
                // Try to get the sign-in intent
                val signInIntent = basicSignInClient.signInIntent
                
                // Do a quick test to see if we can access the intent without errors
                signInIntent.action // This will throw if there are resource ID issues
                
                // Return the intent if no errors
                signInIntent
            } catch (e: Exception) {
                // Handle specific known error patterns
                when {
                    e.message?.contains("No package ID 6b found") == true ||
                    e.message?.contains("SecurityException") == true ||
                    e.message?.contains("Unknown calling package name") == true -> {
                        // Use our specialized handler for these specific errors
                        return handleSpecificPlayServicesErrors(e)
                    }
                    else -> {
                        // Handle any other exception
                        Log.e(TAG, "Error getting Google Sign-In intent: ${e.message}", e)
                        
                        // Disable Google Sign-In for future attempts
                        isGoogleSignInDisabled = true
                        
                        // Return dummy intent that will be handled gracefully
                        Intent().apply { 
                            action = "ai.sach.libraryup.DUMMY_GOOGLE_SIGN_IN"
                            putExtra("ERROR_TYPE", "INTENT_CREATION_ERROR")
                            putExtra("ERROR_MESSAGE", e.message ?: "Unknown error")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Critical error creating Google Sign-In intent", e)
            
            // Check if this is one of our specific known errors
            if (e.message?.contains("No package ID 6b found") == true ||
                e.message?.contains("SecurityException") == true ||
                e.message?.contains("Unknown calling package name") == true) {
                return handleSpecificPlayServicesErrors(e)
            }
            
            // Disable Google Sign-In for future attempts
            isGoogleSignInDisabled = true
            
            // Return dummy intent as last resort
            Intent().apply { 
                action = "ai.sach.libraryup.DUMMY_GOOGLE_SIGN_IN"
                putExtra("ERROR_TYPE", "CRITICAL_ERROR")
                putExtra("ERROR_MESSAGE", e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Get the intent for Google Sign In with Drive scopes
     * @return Intent to be launched with ActivityResultLauncher
     */
    fun getDriveSignInIntent(): Intent {
        return driveSignInClient.signInIntent
    }

    /**
     * Process the result from a Google Sign-In attempt
     * @param task The Task returned from sign-in intent
     * @return The Google Sign In account if successful, null otherwise
     */
    suspend fun processSignInResult(task: Task<GoogleSignInAccount>): GoogleSignInAccount? = withContext(Dispatchers.IO) {
        try {
            val account = task.getResult(ApiException::class.java)

            // Log successful sign-in with all account details
            Log.d(TAG, "Google sign-in successful: ${account.displayName}")

            // Save sign-in state and email
            _isSignedIn.value = true
            prefs.edit()
                .putBoolean(KEY_SIGNED_IN, true)
                .putString(KEY_ACCOUNT_EMAIL, account.email)
                .putString(KEY_ACCOUNT_NAME, account.displayName ?: "")
                .putLong(KEY_LAST_SIGN_IN, System.currentTimeMillis())
                .apply()

            // Check if we have Drive scopes
            checkAndUpdateDriveAccess(account)

            return@withContext account
        } catch (e: ApiException) {
            Log.e(TAG, "Google sign-in failed: ${e.statusCode}", e)
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Error processing sign-in result", e)
            return@withContext null
        }
    }

    /**
     * Check if the account has Drive access and update the state
     */
    private fun checkAndUpdateDriveAccess(account: GoogleSignInAccount) {
        val hasDriveFileScope = GoogleSignIn.hasPermissions(account, Scope("https://www.googleapis.com/auth/drive.file"))
        val hasDriveAppDataScope = GoogleSignIn.hasPermissions(account, Scope("https://www.googleapis.com/auth/drive.appdata"))
        
        val hasDriveAccess = hasDriveFileScope && hasDriveAppDataScope
        _hasDriveAccess.value = hasDriveAccess
        prefs.edit().putBoolean(KEY_DRIVE_ACCESS, hasDriveAccess).apply()
    }

    /**
     * Get Firebase credential from Google account for authentication
     * @param account Google Sign In account
     * @return AuthCredential for Firebase
     */
    fun getFirebaseCredential(account: GoogleSignInAccount): AuthCredential {
        return GoogleAuthProvider.getCredential(account.idToken, null)
    }

    /**
     * Sign out from Google and Firebase
     */
    suspend fun signOut(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Sign out from Firebase
            auth.signOut()
            
            // Sign out from Google - need to sign out from both clients
            basicSignInClient.signOut().await()
            driveSignInClient.signOut().await()
            
            // Update state and preferences
            _isSignedIn.value = false
            _hasDriveAccess.value = false
            prefs.edit()
                .putBoolean(KEY_SIGNED_IN, false)
                .putBoolean(KEY_DRIVE_ACCESS, false)
                .putString(KEY_ACCOUNT_EMAIL, null)
                .apply()
            
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out", e)
            return@withContext false
        }
    }

    /**
     * Sign in to Firebase with Google credential and update user profile
     * @param credential Firebase AuthCredential
     * @param googleAccount Original Google account with full profile info
     * @return Result with FirebaseUser if successful
     */
    suspend fun signInToFirebase(
        credential: AuthCredential,
        googleAccount: GoogleSignInAccount? = null
    ): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Authentication failed")

            // Try to update user data in Firestore (optional operation)
            try {
                if (googleAccount != null) {
                    // If we have the original Google account, use its full information
                    val userData = mapOf(
                        "uid" to user.uid,
                        "email" to user.email,
                        "name" to (googleAccount.displayName ?: ""),
                        "displayName" to (googleAccount.displayName ?: ""),
                        "firstName" to (googleAccount.givenName ?: ""),
                        "lastName" to (googleAccount.familyName ?: ""),
                        "photoUrl" to (googleAccount.photoUrl?.toString() ?: ""),
                        "isGuestAccount" to false,
                        "lastLogin" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "accountType" to "google"
                    )

                    Log.d(TAG, "Updating Firestore with complete Google account data")

                    firestore.collection(USERS_COLLECTION)
                        .document(user.uid)
                        .set(userData, SetOptions.merge())
                        .await()
                    
                    Log.d(TAG, "Successfully updated Firestore with Google account data")
                } else {
                    // Fallback to basic user data if Google account details aren't available
                    updateUserInFirestore(user)
                }
            } catch (firestoreError: Exception) {
                Log.w(TAG, "Failed to update Firestore (this is optional): ${firestoreError.message}")
                // Don't fail authentication because of Firestore errors
            }

            return@withContext Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase authentication failed", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Updates user data in Firestore after successful authentication
     * This is optional and won't fail authentication if it doesn't work
     */
    private suspend fun updateUserInFirestore(user: FirebaseUser) = withContext(Dispatchers.IO) {
        try {
            val userData = mapOf(
                "uid" to user.uid,
                "email" to user.email,
                "displayName" to user.displayName,
                "photoUrl" to (user.photoUrl?.toString() ?: ""), // Ensure we store the photo URL
                "isGuestAccount" to false,
                "lastLogin" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            firestore.collection(USERS_COLLECTION)
                .document(user.uid)
                .set(userData, SetOptions.merge())
                .await()
            
            Log.d(TAG, "Successfully updated basic user data in Firestore")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update user in Firestore (this is optional): ${e.message}")
            // Don't fail authentication flow
        }
    }

    /**
     * Complete Google Sign In flow for user authentication with improved name handling
     * @param task The Task<GoogleSignInAccount> returned from sign-in intent
     * @return Result with FirebaseUser if successful
     */
    suspend fun completeGoogleSignIn(task: Task<GoogleSignInAccount>): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val account = processSignInResult(task) ?: return@withContext Result.failure(
                Exception("Google sign-in failed")
            )

            // Log the Google account details before proceeding
            Log.d(TAG, "Google account details:")
            Log.d(TAG, "ID: ${account.id}")
            Log.d(TAG, "DisplayName: ${account.displayName}")
            Log.d(TAG, "GivenName: ${account.givenName}")
            Log.d(TAG, "FamilyName: ${account.familyName}")
            Log.d(TAG, "Email: ${account.email}")
            Log.d(TAG, "PhotoUrl: ${account.photoUrl}")

            val credential = getFirebaseCredential(account)
            val result = signInToFirebase(credential, account)

            return@withContext result
        } catch (e: Exception) {
            Log.e(TAG, "Error in Google sign-in flow", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Checks if Google Play Services is available and properly configured
     */
    private fun isGooglePlayServicesAvailable(): Boolean {
        return try {
            val availability = GoogleApiAvailability.getInstance()
            val status = availability.isGooglePlayServicesAvailable(context)
            val isAvailable = status == ConnectionResult.SUCCESS
            
            if (!isAvailable) {
                Log.w(TAG, "Google Play Services not available. Status: $status")
                when (status) {
                    ConnectionResult.SERVICE_MISSING -> Log.w(TAG, "Google Play Services is missing")
                    ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> Log.w(TAG, "Google Play Services needs update")
                    ConnectionResult.SERVICE_DISABLED -> Log.w(TAG, "Google Play Services is disabled")
                    ConnectionResult.SERVICE_INVALID -> Log.w(TAG, "Google Play Services APK is invalid")
                    else -> Log.w(TAG, "Other Google Play Services issue: $status")
                }
                
                // Try to show error dialog if possible
                try {
                    if (availability.isUserResolvableError(status)) {
                        Log.i(TAG, "Google Play Services error is user-resolvable")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking if Play Services error is resolvable", e)
                }
                
                // Disable Google Sign-In for future attempts to avoid repeated errors
                isGoogleSignInDisabled = true
            }
            
            isAvailable
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Google Play Services availability", e)
            // Disable Google Sign-In for future attempts
            isGoogleSignInDisabled = true
            false
        }
    }
    
    /**
     * Check if the Google Play Services version is compatible with our app
     * This provides more detailed diagnostics
     */
    private fun checkPlayServicesCompatibility(): Pair<Boolean, String> {
        try {
            val availability = GoogleApiAvailability.getInstance()
            val status = availability.isGooglePlayServicesAvailable(context)
            
            // If we have an error, disable Google Sign-In to prevent repeated failures
            if (status != ConnectionResult.SUCCESS) {
                isGoogleSignInDisabled = true
            }
            
            return when (status) {
                ConnectionResult.SUCCESS -> Pair(true, "Google Play Services is available")
                ConnectionResult.SERVICE_MISSING -> Pair(false, "Google Play Services is missing on this device")
                ConnectionResult.SERVICE_UPDATING -> Pair(false, "Google Play Services is currently updating")
                ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> Pair(false, "Google Play Services needs to be updated")
                ConnectionResult.SERVICE_DISABLED -> Pair(false, "Google Play Services is disabled")
                ConnectionResult.SERVICE_INVALID -> Pair(false, "Google Play Services is invalid")
                ConnectionResult.API_UNAVAILABLE -> Pair(false, "The Google API is not available on this device")
                else -> Pair(false, "Google Play Services error: $status")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Google Play Services compatibility", e)
            // Disable Google Sign-In for future attempts
            isGoogleSignInDisabled = true
            return Pair(false, "Error checking Google Play Services: ${e.message}")
        }
    }

    /**
     * Handles specific common Google Play Services errors
     * This is called when we detect specific error patterns
     */
    private fun handleSpecificPlayServicesErrors(exception: Exception): Intent {
        // Look for specific error patterns in the exception message
        val errorMessage = exception.message ?: "Unknown error"
        val errorType = when {
            errorMessage.contains("No package ID 6b found") -> "RESOURCE_ID_ERROR"
            errorMessage.contains("Unknown calling package name") || 
            errorMessage.contains("SecurityException") -> "SECURITY_EXCEPTION"
            else -> "GOOGLE_PLAY_SERVICES_ERROR"
        }
        
        // Log detailed information about the error
        Log.e(TAG, "Google Play Services specific error: $errorType - $errorMessage", exception)
        
        // Disable Google Sign-In for future attempts to prevent repeated errors
        isGoogleSignInDisabled = true
        
        // Return a dummy intent with the specific error information
        return Intent().apply {
            action = "ai.sach.libraryup.DUMMY_GOOGLE_SIGN_IN"
            putExtra("ERROR_TYPE", errorType)
            putExtra("ERROR_MESSAGE", "Google Play Services error: $errorMessage")
        }
    }

    /**
     * Debug method to log current app signature information
     * This helps diagnose Google Play Services configuration issues
     */
    private fun logAppSignatureInfo() {
        try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
            
            Log.d(TAG, "=== Google Services Debug Information ===")
            Log.d(TAG, "Package name: ${context.packageName}")
            
            packageInfo.signatures?.forEach { signature ->
                val md = MessageDigest.getInstance("SHA1")
                md.update(signature.toByteArray())
                val digest = md.digest()
                val sha1 = digest.joinToString(":") { "%02X".format(it) }
                Log.d(TAG, "Current app SHA1: $sha1")
                
                // Also log SHA256 for Firebase console
                val md256 = MessageDigest.getInstance("SHA256")
                md256.update(signature.toByteArray())
                val digest256 = md256.digest()
                val sha256 = digest256.joinToString(":") { "%02X".format(it) }
                Log.d(TAG, "SHA256: $sha256")
            }
            
            // Log additional debugging info
            Log.d(TAG, "Build type: ${if (BuildConfig.DEBUG) "DEBUG" else "RELEASE"}")
            Log.d(TAG, "Version code: ${BuildConfig.VERSION_CODE}")
            Log.d(TAG, "Version name: ${BuildConfig.VERSION_NAME}")
            
            // Check if Google Play Services is available
            try {
                val availability = GoogleApiAvailability.getInstance()
                val status = availability.isGooglePlayServicesAvailable(context)
                Log.d(TAG, "Google Play Services status: $status")
                Log.d(TAG, "Is user resolvable: ${availability.isUserResolvableError(status)}")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking Google Play Services status", e)
            }
            
            Log.d(TAG, "==========================================")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting app signature", e)
        }
    }
}