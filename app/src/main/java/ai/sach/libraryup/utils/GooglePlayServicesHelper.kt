package ai.sach.rentup.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import java.security.MessageDigest

/**
 * Helper class to check Google Play Services availability and handle authentication issues
 * that were showing in the logs as "Unknown calling package name 'com.google.android.gms'"
 */
object GooglePlayServicesHelper {
    private const val TAG = "GooglePlayServicesHelper"
    
    // Cache the GoogleApiAvailability instance
    private val googleApiAvailability: GoogleApiAvailability by lazy { GoogleApiAvailability.getInstance() }

    /**
     * Check if Google Play Services is available on this device
     */
    fun isGooglePlayServicesAvailable(context: Context): Boolean {
        return try {
            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
            
            when (resultCode) {
                ConnectionResult.SUCCESS -> {
                    Log.d(TAG, "Google Play Services is available")
                    true
                }
                ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> {
                    Log.w(TAG, "Google Play Services needs to be updated")
                    false
                }
                ConnectionResult.SERVICE_DISABLED -> {
                    Log.w(TAG, "Google Play Services is disabled")
                    false
                }
                ConnectionResult.SERVICE_MISSING -> {
                    Log.w(TAG, "Google Play Services is not installed")
                    false
                }
                else -> {
                    Log.e(TAG, "Google Play Services error: $resultCode")
                    false
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Security exception checking Google Play Services: ${e.message}")
            // This is the "Unknown calling package name" error - log it but don't crash
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error checking Google Play Services", e)
            false
        }
    }

    /**
     * Get a user-friendly error message for Google Play Services issues
     */
    fun getErrorMessage(context: Context): String {
        return try {
            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
            
            when (resultCode) {
                ConnectionResult.SUCCESS -> "Google Play Services is available"
                ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> "Please update Google Play Services"
                ConnectionResult.SERVICE_DISABLED -> "Please enable Google Play Services"
                ConnectionResult.SERVICE_MISSING -> "Please install Google Play Services"
                else -> "Google Play Services is not available"
            }
        } catch (e: SecurityException) {
            "Google Play Services configuration issue detected"
        } catch (e: Exception) {
            "Unable to check Google Play Services"
        }
    }

    /**
     * Safely initialize Google Play Services with proper error handling
     */
    fun initializeGooglePlayServices(context: Context): Boolean {
        return try {
            if (isGooglePlayServicesAvailable(context)) {
                Log.d(TAG, "Google Play Services initialized successfully")
                true
            } else {
                Log.w(TAG, "Google Play Services not available, using fallback mechanisms")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error initializing Google Play Services", e)
            false
        }
    }

    /**
     * Log detailed diagnostic information about the app signature and Google Play Services
     */
    fun logDiagnosticInfo(context: Context) {
        try {
            Log.d(TAG, "=== Google Play Services Diagnostic Information ===")
            
            // Log app signature information
            @Suppress("DEPRECATION")
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
            )
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            
            Log.d(TAG, "Package name: ${context.packageName}")
            
            signatures?.forEach { signature ->
                // Calculate SHA1
                val md = MessageDigest.getInstance("SHA1")
                md.update(signature.toByteArray())
                val digest = md.digest()
                val sha1 = digest.joinToString(":") { "%02X".format(it) }
                Log.d(TAG, "App SHA1: $sha1")
                
                // Calculate SHA256
                val md256 = MessageDigest.getInstance("SHA256")
                md256.update(signature.toByteArray())
                val digest256 = md256.digest()
                val sha256 = digest256.joinToString(":") { "%02X".format(it) }
                Log.d(TAG, "App SHA256: $sha256")
            }
            
            // Check Google Play Services status
            try {
                val status = googleApiAvailability.isGooglePlayServicesAvailable(context)
                Log.d(TAG, "Google Play Services status: $status")
                Log.d(TAG, "Is user resolvable: ${googleApiAvailability.isUserResolvableError(status)}")
            } catch (e: SecurityException) {
                Log.w(TAG, "Security exception checking Google Play Services: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking Google Play Services status", e)
            }
            
            Log.d(TAG, "==================================================")
        } catch (e: Exception) {
            Log.e(TAG, "Error logging diagnostic information", e)
        }
    }
}