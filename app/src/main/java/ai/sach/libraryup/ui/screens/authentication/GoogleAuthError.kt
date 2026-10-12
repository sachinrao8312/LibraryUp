package ai.sach.rentup.ui.screens.authentication

import android.content.Context
import android.util.Log

/**
 * Sealed class representing different types of Google authentication errors
 * with specific error types and user-friendly messages
 */
sealed class GoogleAuthError {
    object NetworkError : GoogleAuthError()
    object AccountExists : GoogleAuthError()
    object AccountCollision : GoogleAuthError()
    object ConversionFailed : GoogleAuthError()
    object DataMigrationFailed : GoogleAuthError()
    object GooglePlayServicesUnavailable : GoogleAuthError()
    object InvalidCredentials : GoogleAuthError()
    object AccountDisabled : GoogleAuthError()
    object RateLimitExceeded : GoogleAuthError()
    object PermissionDenied : GoogleAuthError()
    data class Unknown(val message: String, val cause: Throwable? = null) : GoogleAuthError()
}

/**
 * Data class representing an error response with user-friendly information
 */
data class ErrorResponse(
    val message: String,
    val canRetry: Boolean,
    val suggestedAction: String,
    val technicalDetails: String? = null,
    val errorCode: String? = null
)

/**
 * Comprehensive error handler for Google authentication
 * Provides user-friendly messages and recovery suggestions
 */
class GoogleAuthErrorHandler {
    
    companion object {
        private const val TAG = "GoogleAuthErrorHandler"
        
        /**
         * Handle authentication errors and provide user-friendly responses
         */
        fun handleAuthError(error: GoogleAuthError): ErrorResponse {
            return when (error) {
                is GoogleAuthError.NetworkError -> ErrorResponse(
                    message = "Please check your internet connection and try again",
                    canRetry = true,
                    suggestedAction = "Retry",
                    technicalDetails = "Network connectivity issue"
                )
                
                is GoogleAuthError.AccountExists -> ErrorResponse(
                    message = "This Google account is already registered",
                    canRetry = false,
                    suggestedAction = "Sign In",
                    technicalDetails = "Account already exists in system"
                )
                
                is GoogleAuthError.AccountCollision -> ErrorResponse(
                    message = "Account conflict detected. Please choose how to proceed",
                    canRetry = false,
                    suggestedAction = "Choose Option",
                    technicalDetails = "Multiple accounts with same credentials"
                )
                
                is GoogleAuthError.ConversionFailed -> ErrorResponse(
                    message = "Failed to convert guest account. Your data is safe",
                    canRetry = true,
                    suggestedAction = "Try Again",
                    technicalDetails = "Guest account conversion error"
                )
                
                is GoogleAuthError.DataMigrationFailed -> ErrorResponse(
                    message = "Account created but some data may not have transferred",
                    canRetry = false,
                    suggestedAction = "Continue",
                    technicalDetails = "Data migration partial failure"
                )
                
                is GoogleAuthError.GooglePlayServicesUnavailable -> ErrorResponse(
                    message = "Google Play Services is not available on this device",
                    canRetry = false,
                    suggestedAction = "Update Services",
                    technicalDetails = "Google Play Services missing or outdated"
                )
                
                is GoogleAuthError.InvalidCredentials -> ErrorResponse(
                    message = "Invalid Google credentials. Please try signing in again",
                    canRetry = true,
                    suggestedAction = "Sign In Again",
                    technicalDetails = "Authentication credentials invalid"
                )
                
                is GoogleAuthError.AccountDisabled -> ErrorResponse(
                    message = "This account has been disabled. Please contact support",
                    canRetry = false,
                    suggestedAction = "Contact Support",
                    technicalDetails = "Account disabled by administrator"
                )
                
                is GoogleAuthError.RateLimitExceeded -> ErrorResponse(
                    message = "Too many attempts. Please wait a moment and try again",
                    canRetry = true,
                    suggestedAction = "Wait and Retry",
                    technicalDetails = "Rate limit exceeded"
                )
                
                is GoogleAuthError.PermissionDenied -> ErrorResponse(
                    message = "Permission denied. Please grant necessary permissions",
                    canRetry = true,
                    suggestedAction = "Grant Permissions",
                    technicalDetails = "Required permissions not granted"
                )
                
                is GoogleAuthError.Unknown -> ErrorResponse(
                    message = error.message.ifBlank { "An unexpected error occurred" },
                    canRetry = true,
                    suggestedAction = "Try Again",
                    technicalDetails = error.cause?.message,
                    errorCode = error.cause?.javaClass?.simpleName
                )
            }
        }
        
        /**
         * Convert exceptions to GoogleAuthError types
         */
        fun fromException(exception: Throwable): GoogleAuthError {
            return when {
                exception.message?.contains("network", ignoreCase = true) == true ||
                exception is java.net.UnknownHostException ||
                exception is java.net.SocketTimeoutException -> GoogleAuthError.NetworkError
                
                exception.message?.contains("account exists", ignoreCase = true) == true ||
                exception.message?.contains("already registered", ignoreCase = true) == true -> 
                    GoogleAuthError.AccountExists
                
                exception.message?.contains("collision", ignoreCase = true) == true ||
                exception is com.google.firebase.auth.FirebaseAuthUserCollisionException -> 
                    GoogleAuthError.AccountCollision
                
                exception.message?.contains("conversion", ignoreCase = true) == true -> 
                    GoogleAuthError.ConversionFailed
                
                exception.message?.contains("migration", ignoreCase = true) == true -> 
                    GoogleAuthError.DataMigrationFailed
                
                exception.message?.contains("google play services", ignoreCase = true) == true ||
                exception.message?.contains("play services", ignoreCase = true) == true -> 
                    GoogleAuthError.GooglePlayServicesUnavailable
                
                exception.message?.contains("invalid", ignoreCase = true) == true ||
                exception.message?.contains("credentials", ignoreCase = true) == true -> 
                    GoogleAuthError.InvalidCredentials
                
                exception.message?.contains("disabled", ignoreCase = true) == true -> 
                    GoogleAuthError.AccountDisabled
                
                exception.message?.contains("rate limit", ignoreCase = true) == true ||
                exception.message?.contains("too many", ignoreCase = true) == true -> 
                    GoogleAuthError.RateLimitExceeded
                
                exception.message?.contains("permission", ignoreCase = true) == true -> 
                    GoogleAuthError.PermissionDenied
                
                else -> GoogleAuthError.Unknown(
                    message = exception.message ?: "Unknown error occurred",
                    cause = exception
                )
            }
        }
        
        /**
         * Log error with appropriate level based on error type
         */
        fun logError(error: GoogleAuthError, context: String = "") {
            val logMessage = "GoogleAuth Error in $context: $error"
            
            when (error) {
                is GoogleAuthError.NetworkError,
                is GoogleAuthError.RateLimitExceeded -> {
                    Log.w(TAG, logMessage)
                }
                
                is GoogleAuthError.AccountExists,
                is GoogleAuthError.AccountCollision,
                is GoogleAuthError.ConversionFailed -> {
                    Log.i(TAG, logMessage)
                }
                
                is GoogleAuthError.DataMigrationFailed,
                is GoogleAuthError.GooglePlayServicesUnavailable,
                is GoogleAuthError.InvalidCredentials,
                is GoogleAuthError.AccountDisabled,
                is GoogleAuthError.PermissionDenied -> {
                    Log.e(TAG, logMessage)
                }
                
                is GoogleAuthError.Unknown -> {
                    Log.e(TAG, logMessage, error.cause)
                }
            }
        }
        
        /**
         * Get recovery suggestions based on error type
         */
        fun getRecoveryActions(error: GoogleAuthError): List<RecoveryAction> {
            return when (error) {
                is GoogleAuthError.NetworkError -> listOf(
                    RecoveryAction("Check internet connection", RecoveryAction.Type.USER_ACTION),
                    RecoveryAction("Retry authentication", RecoveryAction.Type.RETRY)
                )
                
                is GoogleAuthError.AccountExists -> listOf(
                    RecoveryAction("Sign in with existing account", RecoveryAction.Type.ALTERNATIVE_FLOW),
                    RecoveryAction("Use different email", RecoveryAction.Type.USER_ACTION)
                )
                
                is GoogleAuthError.AccountCollision -> listOf(
                    RecoveryAction("Choose merge option", RecoveryAction.Type.USER_CHOICE),
                    RecoveryAction("Create new account", RecoveryAction.Type.ALTERNATIVE_FLOW)
                )
                
                is GoogleAuthError.ConversionFailed -> listOf(
                    RecoveryAction("Try conversion again", RecoveryAction.Type.RETRY),
                    RecoveryAction("Continue as guest", RecoveryAction.Type.ALTERNATIVE_FLOW)
                )
                
                is GoogleAuthError.DataMigrationFailed -> listOf(
                    RecoveryAction("Continue with account", RecoveryAction.Type.CONTINUE),
                    RecoveryAction("Manual data backup", RecoveryAction.Type.USER_ACTION)
                )
                
                is GoogleAuthError.GooglePlayServicesUnavailable -> listOf(
                    RecoveryAction("Update Google Play Services", RecoveryAction.Type.SYSTEM_ACTION),
                    RecoveryAction("Use guest mode", RecoveryAction.Type.ALTERNATIVE_FLOW)
                )
                
                is GoogleAuthError.InvalidCredentials -> listOf(
                    RecoveryAction("Sign in again", RecoveryAction.Type.RETRY),
                    RecoveryAction("Clear app data", RecoveryAction.Type.SYSTEM_ACTION)
                )
                
                is GoogleAuthError.AccountDisabled -> listOf(
                    RecoveryAction("Contact support", RecoveryAction.Type.SUPPORT),
                    RecoveryAction("Use different account", RecoveryAction.Type.ALTERNATIVE_FLOW)
                )
                
                is GoogleAuthError.RateLimitExceeded -> listOf(
                    RecoveryAction("Wait and retry", RecoveryAction.Type.WAIT),
                    RecoveryAction("Try again later", RecoveryAction.Type.RETRY)
                )
                
                is GoogleAuthError.PermissionDenied -> listOf(
                    RecoveryAction("Grant permissions", RecoveryAction.Type.SYSTEM_ACTION),
                    RecoveryAction("Check app settings", RecoveryAction.Type.USER_ACTION)
                )
                
                is GoogleAuthError.Unknown -> listOf(
                    RecoveryAction("Try again", RecoveryAction.Type.RETRY),
                    RecoveryAction("Restart app", RecoveryAction.Type.SYSTEM_ACTION),
                    RecoveryAction("Contact support", RecoveryAction.Type.SUPPORT)
                )
            }
        }
    }
}

/**
 * Data class representing a recovery action for errors
 */
data class RecoveryAction(
    val description: String,
    val type: Type
) {
    enum class Type {
        RETRY,
        USER_ACTION,
        SYSTEM_ACTION,
        ALTERNATIVE_FLOW,
        USER_CHOICE,
        CONTINUE,
        WAIT,
        SUPPORT
    }
}

/**
 * Extension functions for easier error handling
 */
fun Throwable.toGoogleAuthError(): GoogleAuthError {
    return GoogleAuthErrorHandler.fromException(this)
}

fun GoogleAuthError.toErrorResponse(): ErrorResponse {
    return GoogleAuthErrorHandler.handleAuthError(this)
}

fun GoogleAuthError.logError(context: String = "") {
    GoogleAuthErrorHandler.logError(this, context)
}

fun GoogleAuthError.getRecoveryActions(): List<RecoveryAction> {
    return GoogleAuthErrorHandler.getRecoveryActions(this)
}