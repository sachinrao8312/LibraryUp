package ai.sach.libraryup.data.backup

import ai.sach.libraryup.domain.BackupCrypto
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * The backup password and recovery code, kept in Keystore-backed encrypted preferences so
 * the scheduled Drive backup can encrypt without asking. If encrypted storage is not
 * available on this device, protection cannot be switched on: the password is never
 * written to plain storage.
 */
class BackupSecrets(context: Context) {

    private val prefs: SharedPreferences? = try {
        val key = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context,
            FILE,
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.w("BackupSecrets", "Encrypted storage unavailable; backup protection disabled", e)
        null
    }

    val isAvailable: Boolean get() = prefs != null

    val isEnabled: Boolean get() = !prefs?.getString(KEY_PASSWORD, null).isNullOrEmpty()

    /** Secrets to encrypt new backups with; empty when protection is off. */
    fun current(): List<String> {
        val p = prefs ?: return emptyList()
        val password = p.getString(KEY_PASSWORD, null) ?: return emptyList()
        return listOfNotNull(password, p.getString(KEY_RECOVERY, null))
    }

    /** Stored password, tried first when restoring an encrypted backup. */
    fun password(): String? = prefs?.getString(KEY_PASSWORD, null)

    /** Turns protection on and returns the recovery code to show the owner once. */
    fun enable(password: String): String {
        val p = prefs ?: throw IllegalStateException("Encrypted storage unavailable")
        val code = BackupCrypto.newRecoveryCode()
        p.edit()
            .putString(KEY_PASSWORD, password)
            .putString(KEY_RECOVERY, BackupCrypto.normalizeRecoveryCode(code))
            .apply()
        return code
    }

    fun disable() {
        prefs?.edit()?.clear()?.apply()
    }

    private companion object {
        const val FILE = "libraryup_backup_secrets"
        const val KEY_PASSWORD = "password"
        const val KEY_RECOVERY = "recovery"
    }
}
