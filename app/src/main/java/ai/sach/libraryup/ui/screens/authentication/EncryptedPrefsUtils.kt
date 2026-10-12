package ai.sach.rentup.ui.screens.authentication

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Utility object for creating EncryptedSharedPreferences with proper error handling
 * and fallback to regular SharedPreferences when encryption fails
 */
object EncryptedPrefsUtils {
    private const val TAG = "EncryptedPrefsUtils"

    /**
     * Creates EncryptedSharedPreferences with fallback to regular SharedPreferences
     * 
     * @param context Android context
     * @param fileName Name of the preferences file
     * @param fallbackFileName Name of the fallback file (will be regular SharedPreferences)
     * @return SharedPreferences instance (either encrypted or regular)
     */
    fun createSecurePrefs(
        context: Context,
        fileName: String,
        fallbackFileName: String = "${fileName}_fallback"
    ): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                fileName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create EncryptedSharedPreferences for '$fileName', falling back to regular SharedPreferences", e)
            
            // Clear any corrupted encrypted preferences
            clearCorruptedPrefs(context, fileName)
            
            // Return regular SharedPreferences as fallback
            context.getSharedPreferences(fallbackFileName, Context.MODE_PRIVATE)
        }
    }

    /**
     * Clears corrupted EncryptedSharedPreferences files
     * 
     * @param context Android context
     * @param fileName Name of the preferences file to clear
     */
    private fun clearCorruptedPrefs(context: Context, fileName: String) {
        try {
            // Try to delete the file
            context.deleteSharedPreferences(fileName)
            Log.d(TAG, "Successfully cleared corrupted preferences file: $fileName")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear corrupted preferences file: $fileName", e)
            
            // Alternative approach: try to clear the content
            try {
                context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply()
                Log.d(TAG, "Successfully cleared corrupted preferences content: $fileName")
            } catch (clearException: Exception) {
                Log.w(TAG, "Failed to clear corrupted preferences content: $fileName", clearException)
            }
        }
    }
} 