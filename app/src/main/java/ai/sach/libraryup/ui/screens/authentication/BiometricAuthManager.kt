package ai.sach.libraryup.ui.screens.authentication

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Enhanced Biometric Authentication Manager
 * Handles fingerprint/biometric authentication with improved security and error handling.
 * Combines features from both previous implementations into a single, cohesive manager.
 */
@RequiresApi(Build.VERSION_CODES.N)
class BiometricAuthManager(context: Context) {
    private val TAG = "BiometricAuthManager"

    // KeyStore for secure key storage
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    // Biometric manager to check device capabilities
    private val biometricManager = BiometricManager.from(context)

    // Shared preferences for storing biometric settings
    private val sharedPrefs = context.getSharedPreferences("biometric_prefs", Context.MODE_PRIVATE)

    // Authentication key alias
    private val KEY_NAME = "rentup_auth_key"

    // Biometric availability and enabled state
    private val _isBiometricEnabled = MutableStateFlow(
        sharedPrefs.getBoolean("biometric_enabled", false)
    )
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled

    private val _isBiometricAvailable = MutableStateFlow(checkBiometricAvailability())
    val isBiometricAvailable: StateFlow<Boolean> = _isBiometricAvailable

    init {
        // Create authentication key if not exists and biometric is available
        if (isBiometricAvailable.value && !keyStore.containsAlias(KEY_NAME)) {
            createAuthenticationKey()
        }
    }

    /**
     * Checks if biometric authentication is available on this device
     * @return true if biometric authentication is available
     */
    fun checkBiometricAvailability(): Boolean {
        val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Creates a cryptographic key in the Android KeyStore, protected by biometric authentication
     */
    @RequiresApi(Build.VERSION_CODES.N)
    private fun createAuthenticationKey() {
        try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
            )

            val keyGenSpec = KeyGenParameterSpec.Builder(
                KEY_NAME,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationRequired(true)
                // Only allow the key to be used after user authentication
                .setInvalidatedByBiometricEnrollment(true)
                .build()

            keyGenerator.init(keyGenSpec)
            keyGenerator.generateKey()

            Log.d(TAG, "Biometric authentication key created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating authentication key: ${e.message}")
        }
    }
    


    /**
     * Gets the cryptographic key for biometric authentication
     */
    private fun getSecretKey(): SecretKey? {
        return try {
            keyStore.getKey(KEY_NAME, null) as? SecretKey
        } catch (e: Exception) {
            Log.e(TAG, "Error getting secret key: ${e.message}")
            null
        }
    }

    /**
     * Prepares a cipher for use with biometric authentication
     */
    private fun getCipher(): Cipher? {
        return try {
            Cipher.getInstance(
                KeyProperties.KEY_ALGORITHM_AES + "/"
                        + KeyProperties.BLOCK_MODE_CBC + "/"
                        + KeyProperties.ENCRYPTION_PADDING_PKCS7
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cipher: ${e.message}")
            null
        }
    }

    /**
     * Initializes the cipher for authentication
     * @return Initialized cipher or null if error
     */
    private fun initCipher(): Cipher? {
        val cipher = getCipher() ?: return null
        val secretKey = getSecretKey() ?: return null

        return try {
            // Initialize cipher in encrypt mode
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            cipher
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing cipher: ${e.message}")
            null
        }
    }

    /**
     * Sets whether biometric authentication is enabled
     * @param enabled True to enable, false to disable
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun setBiometricEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("biometric_enabled", enabled).apply()
        _isBiometricEnabled.value = enabled

        // Create key if enabling biometrics and key doesn't exist
        if (enabled && !keyStore.containsAlias(KEY_NAME)) {
            createAuthenticationKey()
        }
    }

    /**
     * Determines if biometric authentication should be used
     * @return True if biometric is available and enabled in preferences
     */
    fun shouldUseBiometricAuth(): Boolean {
        return isBiometricAvailable.value && isBiometricEnabled.value
    }

    /**
     * Shows the biometric prompt to authenticate the user
     * @param activity The FragmentActivity context
     * @param title Title for the biometric prompt
     * @param subtitle Subtitle for the biometric prompt
     * @param description Description for the biometric prompt
     * @param negativeButtonText Text for the negative/cancel button
     * @param onSuccess Callback for successful authentication
     * @param onError Callback for authentication errors with error message
     * @param onCancel Callback for when user cancels authentication
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Verify your identity",
        subtitle: String = "Use your fingerprint to access LibraryUp",
        description: String = "Authentication required to continue",
        negativeButtonText: String = "Cancel",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        try {
            // Initialize cipher for authentication
            val cipher = initCipher() ?: run {
                onError("Error initializing biometric authentication")
                return
            }

            // Use the main executor for authentication callbacks
            val executor = ContextCompat.getMainExecutor(activity)

            // Create the authentication callback
            val authCallback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.d(TAG, "Biometric authentication succeeded")

                    // Handle authentication success
                    executor.execute { onSuccess() }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e(TAG, "Authentication error [code: $errorCode]: $errString")

                    // Execute on main thread
                    executor.execute {
                        // Check if it's a cancellation or a genuine error
                        if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                            errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                            onCancel()
                        } else {
                            onError(errString.toString())
                        }
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.e(TAG, "Authentication failed")
                    // This method is called when the fingerprint doesn't match
                    // We let the system handle retries, so we don't call onError here
                }
            }

            // Create the BiometricPrompt with the callback
            val biometricPrompt = BiometricPrompt(activity, executor, authCallback)

            // Configure the prompt
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(description)
                .setNegativeButtonText(negativeButtonText)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()

            // Show the biometric prompt with the initialized cipher
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))

        } catch (e: Exception) {
            Log.e(TAG, "Error showing biometric prompt: ${e.message}")
            onError("Error showing biometric prompt: ${e.message}")
        }
    }

    /**
     * Simplified method to authenticate with biometrics
     * Used for simpler cases without crypto operations
     */
    fun authenticateWithBiometric(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        showBiometricPrompt(
            activity = activity,
            onSuccess = onSuccess,
            onError = onError,
            onCancel = onCancel
        )
    }

    /**
     * Clears all biometric authentication data
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun resetBiometricAuth() {
        try {
            // Delete the key if it exists
            if (keyStore.containsAlias(KEY_NAME)) {
                keyStore.deleteEntry(KEY_NAME)
            }

            // Disable biometric authentication in preferences
            setBiometricEnabled(false)

            Log.d(TAG, "Biometric authentication reset successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting biometric authentication: ${e.message}")
        }
    }

    /**
     * Cleanup method to prevent memory leaks
     */
    fun cleanup() {
        try {
            // Clear any cached cipher objects and references
            // Note: KeyStore and BiometricManager are system services, 
            // so we don't need to explicitly clean them up
            
            Log.d(TAG, "BiometricAuthManager cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during BiometricAuthManager cleanup: ${e.message}")
        }
    }
}

/**
 * Composable helper function to remember BiometricAuthManager instance
 */
@RequiresApi(Build.VERSION_CODES.N)
@Composable
fun rememberBiometricAuthManager(context: Context): BiometricAuthManager {
    return remember { BiometricAuthManager(context) }
}