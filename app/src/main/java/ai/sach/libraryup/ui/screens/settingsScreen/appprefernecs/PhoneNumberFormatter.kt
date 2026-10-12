package ai.sach.libraryup.ui.screens.settingsScreen.appprefernecs

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType

/**
 * Phone number formatting and validation backed by Google's libphonenumber.
 */
object PhoneNumberFormatter {

    private val phoneUtil = PhoneNumberUtil.getInstance()

    // "EU" is an app-level pseudo-region; French formatting is used as a representative.
    private fun regionCode(countryCode: String): String = if (countryCode == "EU") "FR" else countryCode

    fun formatPhoneNumber(input: String, countryCode: String): String {
        return try {
            val number = phoneUtil.parse(input, regionCode(countryCode))
            if (phoneUtil.isValidNumber(number)) {
                phoneUtil.format(number, PhoneNumberFormat.NATIONAL)
            } else {
                phoneUtil.formatInOriginalFormat(number, regionCode(countryCode))
            }
        } catch (e: Exception) {
            input
        }
    }

    fun isValidPhoneNumber(phoneNumber: String, countryCode: String): Boolean {
        if (phoneNumber.isBlank()) return true
        return try {
            phoneUtil.isValidNumber(phoneUtil.parse(phoneNumber, regionCode(countryCode)))
        } catch (e: Exception) {
            false
        }
    }

    fun getValidationErrorMessage(phoneNumber: String, countryCode: String): String {
        if (phoneNumber.isBlank()) return "Phone number cannot be empty"
        return try {
            val number = phoneUtil.parse(phoneNumber, regionCode(countryCode))
            when {
                !phoneUtil.isValidNumber(number) -> "Invalid phone number format"
                phoneUtil.getNumberType(number) == PhoneNumberType.UNKNOWN -> "Invalid phone number format"
                else -> "Invalid phone number format"
            }
        } catch (e: Exception) {
            "Phone number contains invalid characters. Use only digits, +, -, spaces, (), and ."
        }
    }

    fun getPlaceholder(countryCode: String): String {
        return try {
            val example = phoneUtil.getExampleNumber(regionCode(countryCode))
                ?: return "Phone number"
            "e.g. ${phoneUtil.format(example, PhoneNumberFormat.NATIONAL)}"
        } catch (e: Exception) {
            "Phone number"
        }
    }
}
