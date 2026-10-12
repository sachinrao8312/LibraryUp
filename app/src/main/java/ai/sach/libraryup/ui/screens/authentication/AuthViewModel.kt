package ai.sach.rentup.ui.screens.authentication

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import ai.sach.rentup.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Unified ViewModel for handling all authentication-related actions and state.
 * Supports email passwordless, Google Sign-In, and guest login flows.
 */
class AuthViewModel(
    application: Application,
    private val authRepository: AuthRepository
) : AndroidViewModel(application) {
    private val TAG = "AuthViewModel"

    // Auth state from repository
    val authState: StateFlow<AuthState> = authRepository.authState

    // UI state for authentication screen
    private val _authUiState = MutableStateFlow(AuthUiState())
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    // Account creation state
    private val _accountCreationState = MutableStateFlow<AccountCreationState>(AccountCreationState.Initial)
    val accountCreationState: StateFlow<AccountCreationState> = _accountCreationState.asStateFlow()

    // Guest conversion state
    private val _guestConversionState = MutableStateFlow<GuestConversionState>(GuestConversionState.Initial)
    val guestConversionState: StateFlow<GuestConversionState> = _guestConversionState.asStateFlow()

    // Google Sign-In state for granular control
    private val _googleAuthState = MutableStateFlow<GoogleAuthState>(GoogleAuthState.Initial)
    val googleAuthState: StateFlow<GoogleAuthState> = _googleAuthState.asStateFlow()

    // Rate limiter for auth attempts
    private val rateLimiter = RateLimiter(maxAttempts = 5, windowMs = 5 * 60 * 1000)

    init {
        // Update UI state based on auth state changes, moved to IO dispatcher
        viewModelScope.launch(Dispatchers.IO) {
            authRepository.authState.collectLatest { state ->
                // Process state off main thread
                val processedState = state

                // Only log in debug builds and on background thread
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Auth state updated: $state")
                }

                // Update UI state on main thread only once processing is done
                withContext(Dispatchers.Main) {
                    updateAuthUiState(processedState)
                }
            }
        }
    }

    /**
     * Check if current user is a guest
     */
    fun isCurrentUserGuest(): Boolean {
        return authRepository.isCurrentUserGuest()
    }

    /**
     * Check if guest has created an account
     */
    suspend fun hasGuestCreatedAccount(): Boolean {
        return authRepository.hasGuestCreatedAccount()
    }

    /**
     * Shows a welcome message to the user after successful authentication
     * @param context Android context for displaying Toast
     * @param userName User's name or "Guest" if not available
     */
    fun showWelcomeMessage(context: Context, userName: String? = null) {
        val displayName = when {
            !userName.isNullOrBlank() -> userName
            authState.value is AuthState.Guest -> "Guest"
            else -> "User"
        }
        
                // Use application context to prevent memory leaks
        showToastSafely(context, "Welcome back, $displayName!")
    }
    
    /**
     * Safely show toast messages using application context
     */
    private fun showToastSafely(context: Context, message: String) {
        val appContext = context.applicationContext
        viewModelScope.launch(Dispatchers.Main) {
            try {
                Toast.makeText(appContext, message, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error showing toast message", e)
            }
        }
    }

    private fun logDebug(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(tag, message)
    }
    private fun logError(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) Log.e(tag, message, throwable)
    }
    private fun handleError(
        errorMessage: String,
        throwable: Throwable? = null,
        updateUi: (String) -> Unit
    ) {
        logError(TAG, errorMessage, throwable)
        updateUi(errorMessage)
    }

    /**
     * Signs in as a guest user
     */
    fun signInAsGuest(context: Context? = null) {
        logDebug(TAG, "Signing in as guest")
        if (!checkRateLimit()) return
        _authUiState.value = _authUiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val result = authRepository.signInAsGuest()
                result.fold(
                    onSuccess = {
                        logDebug(TAG, "Guest sign-in successful: ${it.uid}")
                        _authUiState.value = _authUiState.value.copy(
                            isLoading = false,
                            error = null
                        )
                        context?.let { ctx -> showWelcomeMessage(ctx) }
                    },
                    onFailure = { exception ->
                        handleError(
                            "Guest sign-in failed",
                            exception
                        ) { msg -> _authUiState.value = _authUiState.value.copy(isLoading = false, error = msg) }
                    }
                )
            } catch (exception: Exception) {
                handleError(
                    "Error in guest sign-in",
                    exception
                ) { msg -> _authUiState.value = _authUiState.value.copy(isLoading = false, error = msg) }
            }
        }
    }

    /**
     * Convert guest account to permanent account using Google Sign-In
     */
    fun convertGuestToGoogleAccount(task: Task<GoogleSignInAccount>, context: Context? = null) {
        logDebug(TAG, "Converting guest to Google account")
        if (!checkRateLimit()) return

        // First check if conversion is possible
        if (!authRepository.canConvertGuestAccount()) {
            logError(TAG, "Cannot convert guest account - user is not in proper guest state")
            _guestConversionState.value = GuestConversionState.Error(
                "Please sign in as a guest first before converting your account"
            )
            return
        }

        _guestConversionState.value = GuestConversionState.Converting
        _authUiState.value = _authUiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                // Ensure proper guest state before conversion
                val guestCheckResult = authRepository.ensureGuestAccount()
                
                guestCheckResult.fold(
                    onSuccess = { guestUser ->
                        logDebug(TAG, "Guest account verified, proceeding with conversion")
                        
                        // Now proceed with the actual conversion
                        val conversionResult = authRepository.convertGuestToGoogleAccount(task)
                        
                        when (conversionResult) {
                            is AuthRepository.ConversionResult.Success -> {
                                val user = conversionResult.user
                                logDebug(TAG, "Guest to Google conversion successful: ${user.uid}")
                                _guestConversionState.value = GuestConversionState.Converted
                                _authUiState.value = _authUiState.value.copy(
                                    isLoading = false,
                                    error = null
                                )
                                // Show success message
                                context?.let { ctx ->
                                    val displayName = user.displayName ?: "User"
                                    Toast.makeText(
                                        ctx,
                                        "Account converted successfully! Welcome, $displayName!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                            is AuthRepository.ConversionResult.SuccessWithMigrationFailure -> {
                                val user = conversionResult.user
                                logError(TAG, "Guest to Google conversion successful but migration failed: ${user.uid}")
                                _guestConversionState.value = GuestConversionState.Converted
                                _authUiState.value = _authUiState.value.copy(
                                    isLoading = false,
                                    error = "Account converted, but some data might not have been migrated."
                                )
                                context?.let { ctx ->
                                    val displayName = user.displayName ?: "User"
                                    Toast.makeText(
                                        ctx,
                                        "Account converted, but some data might not have been migrated. Welcome, $displayName!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                            is AuthRepository.ConversionResult.Failure -> {
                                val exception = conversionResult.exception
                                logError(TAG, "Guest to Google conversion failed", exception)
                                _guestConversionState.value = GuestConversionState.Error(
                                    exception.message ?: "Conversion failed"
                                )
                                _authUiState.value = _authUiState.value.copy(
                                    isLoading = false,
                                    error = exception.message ?: "Conversion failed"
                                )
                            }
                        }
                    },
                    onFailure = { exception ->
                        handleError(
                            "Failed to ensure guest account for conversion",
                            exception
                        ) { msg -> _guestConversionState.value = GuestConversionState.Error(msg) }
                        _authUiState.value = _authUiState.value.copy(
                            isLoading = false,
                            error = "Unable to prepare guest account for conversion"
                        )
                    }
                )
            } catch (exception: Exception) {
                handleError(
                    "Error in guest to Google conversion",
                    exception
                ) { msg -> _guestConversionState.value = GuestConversionState.Error(msg) }
                _authUiState.value = _authUiState.value.copy(
                    isLoading = false,
                    error = exception.message ?: "Conversion failed"
                )
            }
        }
    }

    /**
     * Reset guest conversion state
     */
    fun resetGuestConversionState() {
        _guestConversionState.value = GuestConversionState.Initial
    }

    /**
     * Starts the Google Sign-In flow by providing the sign-in intent
     * This should be used with an ActivityResultLauncher
     *
     * @return Intent that should be launched with the ActivityResultLauncher
     */
    fun getGoogleSignInIntent(): Intent? {
        if (!checkRateLimit()) {
            // If rate limited, return null to indicate failure
            logDebug(TAG, "Google Sign-In rate limited")
            return null
        }

        return authRepository.getGoogleSignInIntent()
    }

    /**
     * Processes the result from a Google Sign-In attempt with improved profile image handling
     * Call this from your activity's onActivityResult or from ActivityResultLauncher
     *
     * @param task The Task<GoogleSignInAccount> returned from GoogleSignIn.getSignedInAccountFromIntent()
     * @param context Optional context for showing welcome messages
     */
    fun processGoogleSignIn(task: Task<GoogleSignInAccount>, context: Context? = null) {
        if (!checkRateLimit()) return

        _authUiState.value = _authUiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                val result = authRepository.signInWithGoogle(task)

                result.fold(
                    onSuccess = { user ->
                        logDebug(TAG, "Google sign-in successful: ${user.uid}")

                        // Log the profile image URL if available
                        user.photoUrl?.let {
                            logDebug(TAG, "User profile image URL: $it")
                        }

                        _authUiState.value = _authUiState.value.copy(
                            isLoading = false,
                            error = null
                        )
                        // Show welcome message with user display name if context is provided
                        context?.let { ctx -> showWelcomeMessage(ctx, user.displayName) }
                    },
                    onFailure = { exception ->
                        handleError(
                            "Google sign-in failed",
                            exception
                        ) { msg -> _authUiState.value = _authUiState.value.copy(isLoading = false, error = msg) }
                    }
                )
            } catch (exception: Exception) {
                handleError(
                    "Error in Google sign-in",
                    exception
                ) { msg -> _authUiState.value = _authUiState.value.copy(isLoading = false, error = msg) }
            }
        }
    }

    /**
     * Checks if already signed in with Google
     *
     * @return true if signed in, false otherwise
     */
    fun isGoogleSignedIn(): Boolean {
        return authRepository.isGoogleSignedIn()
    }

    /**
     * Validate and sign in with Google with enhanced error handling
     * This method performs pre-validation and handles account conflicts
     * 
     * @param task The Task<GoogleSignInAccount> returned from GoogleSignIn.getSignedInAccountFromIntent()
     * @param context Optional context for showing messages
     */
    fun validateAndSignInWithGoogle(task: Task<GoogleSignInAccount>, context: Context? = null) {
        if (!checkRateLimit()) return

        _googleAuthState.value = GoogleAuthState.SigningIn
        _authUiState.value = _authUiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                _googleAuthState.value = GoogleAuthState.ValidatingAccount
                
                // First, validate the account
                val validationResult = authRepository.validateGuestConversion(task)
                
                when (validationResult) {
                    is AuthRepository.ConversionValidationResult.CanConvert -> {
                        // Proceed with normal sign-in
                        val result = authRepository.signInWithGoogle(task)
                        handleSignInResult(result, context)
                    }
                    is AuthRepository.ConversionValidationResult.AccountExists -> {
                        // Account exists, try to sign in with existing account
                        _googleAuthState.value = GoogleAuthState.AccountExists(validationResult.email)
                        val result = authRepository.signInWithExistingGoogleAccount(task)
                        handleSignInResult(result, context)
                    }
                    is AuthRepository.ConversionValidationResult.RequiresUserChoice -> {
                        // Show collision dialog
                        _googleAuthState.value = GoogleAuthState.AccountCollision(validationResult.conflictType)
                        _authUiState.value = _authUiState.value.copy(isLoading = false)
                    }
                    is AuthRepository.ConversionValidationResult.ValidationFailed -> {
                        _googleAuthState.value = GoogleAuthState.Error(validationResult.reason)
                        _authUiState.value = _authUiState.value.copy(
                            isLoading = false,
                            error = validationResult.reason
                        )
                    }
                }
                
            } catch (exception: Exception) {
                handleError(
                    "Error in Google sign-in validation",
                    exception
                ) { msg -> 
                    _googleAuthState.value = GoogleAuthState.Error(msg)
                    _authUiState.value = _authUiState.value.copy(isLoading = false, error = msg)
                }
            }
        }
    }

    /**
     * Handle account collision by showing user choice dialog
     * 
     * @param conflictType The type of conflict detected
     */
    fun handleAccountCollision(conflictType: AuthRepository.ConflictType) {
        logDebug(TAG, "Handling account collision: $conflictType")
        _googleAuthState.value = GoogleAuthState.AccountCollision(conflictType)
    }

    /**
     * Show account exists dialog and handle user choice
     * 
     * @param email The email of the existing account
     */
    fun showAccountExistsDialog(email: String) {
        logDebug(TAG, "Showing account exists dialog for: $email")
        _googleAuthState.value = GoogleAuthState.AccountExists(email)
    }

    /**
     * Reset Google authentication state
     */
    fun resetGoogleAuthState() {
        _googleAuthState.value = GoogleAuthState.Initial
    }

    /**
     * Handle the result of a Google Sign-In operation
     */
    private fun handleSignInResult(result: Result<com.google.firebase.auth.FirebaseUser>, context: Context?) {
        result.fold(
            onSuccess = { user ->
                logDebug(TAG, "Google sign-in successful: ${user.uid}")
                _googleAuthState.value = GoogleAuthState.Success(user)
                _authUiState.value = _authUiState.value.copy(
                    isLoading = false,
                    error = null
                )
                context?.let { ctx -> showWelcomeMessage(ctx, user.displayName) }
            },
            onFailure = { exception ->
                handleError(
                    "Google sign-in failed",
                    exception
                ) { msg -> 
                    _googleAuthState.value = GoogleAuthState.Error(msg)
                    _authUiState.value = _authUiState.value.copy(isLoading = false, error = msg)
                }
            }
        )
    }

    /**
     * Creates a user account after guest login or other authentication
     * This works for guest users and saves data to Firestore and local storage.
     */
    fun createUserAccount(userData: UserData) {
        logDebug(TAG, "Creating user account: $userData")
        _accountCreationState.value = AccountCreationState.Creating
        _authUiState.value = _authUiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                // Validate user data first
                if (userData.name.isBlank()) {
                    logError(TAG, "Name is required for account creation")
                    _accountCreationState.value = AccountCreationState.Error("Name is required")
                    _authUiState.value = _authUiState.value.copy(
                        isLoading = false,
                        error = "Name is required"
                    )
                    return@launch
                }

                val result = authRepository.createUserAccount(userData)

                result.fold(
                    onSuccess = {
                        logDebug(TAG, "Account created successfully")
                        _accountCreationState.value = AccountCreationState.Created
                        _authUiState.value = _authUiState.value.copy(
                            isLoading = false,
                            error = null
                        )

                        // Ensure we stay in Guest mode if that's what we're in
                        if (authState.value is AuthState.Guest) {
                            authRepository.ensureGuestAccount()
                        }
                    },
                    onFailure = { exception ->
                        handleError(
                            "Failed to create account",
                            exception
                        ) { msg -> _accountCreationState.value = AccountCreationState.Error(msg) }
                        _authUiState.value = _authUiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to create account"
                        )
                    }
                )
            } catch (exception: Exception) {
                handleError(
                    "Error creating account",
                    exception
                ) { msg -> _accountCreationState.value = AccountCreationState.Error(msg) }
                _authUiState.value = _authUiState.value.copy(
                    isLoading = false,
                    error = exception.message ?: "Failed to create account"
                )
            }
        }
    }

    /**
     * Updates user profile
     */
    fun updateUserProfile(userData: UserData) {
        _authUiState.value = _authUiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                val result = authRepository.updateUserProfile(userData)

                result.fold(
                    onSuccess = {
                        logDebug(TAG, "Profile updated successfully")
                        _authUiState.value = _authUiState.value.copy(
                            isLoading = false,
                            message = "Profile updated successfully",
                            error = null
                        )
                    },
                    onFailure = { exception ->
                        handleError(
                            "Failed to update profile",
                            exception
                        ) { msg -> _authUiState.value = _authUiState.value.copy(isLoading = false, error = msg) }
                    }
                )
            } catch (exception: Exception) {
                handleError(
                    "Error updating profile",
                    exception
                ) { msg -> _authUiState.value = _authUiState.value.copy(isLoading = false, error = msg) }
            }
        }
    }

    /**
     * Signs out the current user
     */
    fun signOut() {
        logDebug(TAG, "Signing out")
        _authUiState.value = _authUiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                val result = authRepository.signOut()

                result.fold(
                    onSuccess = {
                        logDebug(TAG, "Sign out successful")
                        // Reset states
                        _accountCreationState.value = AccountCreationState.Initial
                        _guestConversionState.value = GuestConversionState.Initial
                        _authUiState.value = _authUiState.value.copy(
                            isLoading = false,
                            error = null
                        )
                    },
                    onFailure = { exception ->
                        handleError(
                            "Failed to sign out",
                            exception
                        ) { msg -> _authUiState.value = _authUiState.value.copy(isLoading = false, error = msg) }
                    }
                )
            } catch (exception: Exception) {
                handleError(
                    "Error signing out",
                    exception
                ) { msg -> _authUiState.value = _authUiState.value.copy(isLoading = false, error = msg) }
            }
        }
    }

    /**
     * Checks if authentication attempts are within rate limits
     * and updates UI state if rate limited
     */
    private fun checkRateLimit(): Boolean {
        if (!rateLimiter.allowRequest()) {
            val remainingSeconds = (rateLimiter.getRemainingTime() / 1000).toInt()
            _authUiState.value = _authUiState.value.copy(
                isLoading = false,
                error = "Too many attempts. Please try again in $remainingSeconds seconds."
            )
            return false
        }
        return true
    }

    /**
     * Updates the UI state based on the authentication state
     */
    private fun updateAuthUiState(authState: AuthState) {
        _authUiState.value = when (authState) {
            is AuthState.Loading -> {
                _authUiState.value.copy(isLoading = true, error = null)
            }
            is AuthState.Unauthenticated -> {
                _authUiState.value.copy(isLoading = false, isAuthenticated = false, error = null)
            }
            is AuthState.Guest, is AuthState.Authenticated -> {
                _authUiState.value.copy(isLoading = false, isAuthenticated = true, error = null)
            }
            is AuthState.Error -> {
                _authUiState.value.copy(
                    isLoading = false,
                    isAuthenticated = false,
                    error = authState.message
                )
            }
        }
    }
}

/**
 * Factory for creating the AuthViewModel with dependencies
 */
class AuthViewModelFactory(
    private val application: Application,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(application, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * UI state for authentication screens
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

/**
 * Sealed class for account creation states
 */
sealed class AccountCreationState {
    object Initial : AccountCreationState()
    object Creating : AccountCreationState()
    object Created : AccountCreationState()
    data class Error(val message: String) : AccountCreationState()
}

/**
 * Sealed class for guest conversion states
 */
sealed class GuestConversionState {
    object Initial : GuestConversionState()
    object Converting : GuestConversionState()
    object Converted : GuestConversionState()
    data class Error(val message: String) : GuestConversionState()
}

/**
 * Sealed class for Google Sign-In states with granular control
 */
sealed class GoogleAuthState {
    object Initial : GoogleAuthState()
    object SigningIn : GoogleAuthState()
    object ValidatingAccount : GoogleAuthState()
    data class AccountExists(val email: String) : GoogleAuthState()
    data class AccountCollision(val conflictType: AuthRepository.ConflictType) : GoogleAuthState()
    data class Success(val user: com.google.firebase.auth.FirebaseUser) : GoogleAuthState()
    data class Error(val message: String, val canRetry: Boolean = true) : GoogleAuthState()
}