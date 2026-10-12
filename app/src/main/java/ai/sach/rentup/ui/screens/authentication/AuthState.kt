package ai.sach.rentup.ui.screens.authentication

/**
 * Enhanced AuthState with more detailed error information
 */
sealed class AuthState {
    object Guest : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState()

    /**
     * Enhanced error state with more context
     */
    data class Error(
        val type: ErrorType,
        val message: String,
        val errorCode: String? = null,
        val canRetry: Boolean = true,
        val timestamp: Long = System.currentTimeMillis()
    ) : AuthState()

    data class Authenticated(val uid: String) : AuthState()

    /**
     * Enum for different error types with user-friendly messages
     */
    enum class ErrorType {
        NETWORK_ERROR,
        SERVER_ERROR,
        AUTHENTICATION_FAILED,
        TIMEOUT,
        INVALID_CREDENTIALS,
        ACCOUNT_DISABLED,
        UNKNOWN_ERROR;

        fun getUserFriendlyMessage(): String {
            return when (this) {
                NETWORK_ERROR -> "Please check your internet connection"
                SERVER_ERROR -> "Our servers are experiencing issues"
                AUTHENTICATION_FAILED -> "We couldn't verify your account"
                TIMEOUT -> "The connection timed out"
                INVALID_CREDENTIALS -> "Invalid login credentials"
                ACCOUNT_DISABLED -> "Your account has been disabled"
                UNKNOWN_ERROR -> "Something went wrong"
            }
        }
    }

    override fun toString(): String {
        return when (this) {
            is Guest -> "Guest"
            is Unauthenticated -> "Unauthenticated"
            is Loading -> "Loading"
            is Error -> "Error(${type.name}: $message)"
            is Authenticated -> "Authenticated: $uid"
        }
    }
}