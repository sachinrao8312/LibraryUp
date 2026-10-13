package ai.sach.libraryup.ui.library

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.security.MessageDigest

/**
 * Staff mode on a shared desk phone. The owner sets a PIN and hands over the phone; staff can
 * admit students, collect fees and mark attendance, but can't edit or delete payments, delete
 * students, or open expenses, settings, export, backup or the activity log. Leaving staff mode
 * needs the PIN. Survives app restarts.
 */
class StaffMode(context: Context) {
    private val prefs = context.getSharedPreferences("staff_mode", Context.MODE_PRIVATE)

    var isOn by mutableStateOf(prefs.getBoolean(KEY_ON, false))
        private set

    val hasPin: Boolean get() = prefs.contains(KEY_PIN)

    fun turnOn(newPin: String? = null) {
        newPin?.let { prefs.edit().putString(KEY_PIN, hash(it)).apply() }
        check(hasPin) { "set a PIN first" }
        set(true)
    }

    /** Returns false when [pin] is wrong. */
    fun turnOff(pin: String): Boolean = (hash(pin) == prefs.getString(KEY_PIN, null)).also { if (it) set(false) }

    private fun set(on: Boolean) {
        isOn = on
        prefs.edit().putBoolean(KEY_ON, on).apply()
    }

    private fun hash(pin: String): String =
        MessageDigest.getInstance("SHA-256").digest("libraryup-staff:$pin".toByteArray()).joinToString("") { "%02x".format(it) }

    companion object {
        const val PIN_LENGTH = 4
        private const val KEY_ON = "on"
        private const val KEY_PIN = "pin_hash"
    }
}
