package ai.sach.libraryup.ui.common.dialog

import android.util.Log
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Utility class for form validation logic.
 * Centralizes validation rules to avoid duplication across the application.
 */
object ValidationUtils {
    private const val TAG = "ValidationUtils"

    /**
     * Validates a property name.
     * @param value The property name to validate
     * @return Error message if invalid, null if valid
     */
    fun validatePropertyName(value: String): String? {
        Log.d(TAG, "Validating property name: '$value'")
        return when {
            value.isBlank() -> "Property name is required"
            value.length < 3 -> "Property name must be at least 3 characters"
            else -> null
        }.also { error -> 
            Log.d(TAG, "Property name validation result: ${error ?: "valid"}") 
        }
    }

    /**
     * Validates an address.
     * @param value The address to validate
     * @return Error message if invalid, null if valid
     */
    fun validateAddress(value: String): String? {
        Log.d(TAG, "Validating address: '$value'")
        return when {
            value.isBlank() -> "Address is required"
            value.length < 5 -> "Please enter a valid address"
            else -> null
        }.also { error -> 
            Log.d(TAG, "Address validation result: ${error ?: "valid"}") 
        }
    }

    /**
     * Validates a numeric field that must be greater than zero.
     * @param value The value to validate
     * @param fieldName The name of the field (for error message)
     * @return Error message if invalid, null if valid
     */
    fun validatePositiveNumber(value: String, fieldName: String): String? {
        Log.d(TAG, "Validating $fieldName: '$value'")
        return when {
            value.isBlank() -> "$fieldName is required"
            value.toIntOrNull() == null -> "Please enter a valid number"
            value.toInt() <= 0 -> "$fieldName must be greater than 0"
            else -> null
        }.also { error -> 
            Log.d(TAG, "$fieldName validation result: ${error ?: "valid"}") 
        }
    }

    /**
     * Validates a phone number.
     * Allows digits, spaces, parentheses, hyphens, and plus signs.
     * Requires at least 10 digits.
     * @param phoneNumber The phone number to validate
     * @return true if valid, false otherwise
     */
    fun isValidPhoneNumber(phoneNumber: String): Boolean {
        val digits = phoneNumber.filter { it.isDigit() }
        // Check for at least 10 digits and allowed characters
        return digits.length >= 10 && phoneNumber.all { 
            it.isDigit() || it in setOf('+', '-', ' ', '(', ')') 
        }
    }

    /**
     * Validates a date string in YYYY-MM-DD format.
     * @param date The date string to validate
     * @return true if valid, false otherwise
     */
    fun isValidDate(date: String): Boolean {
        return if (date.isBlank()) {
            false // Blank is not valid
        } else {
            try {
                LocalDate.parse(date) // Try parsing using standard ISO format
                true
            } catch (e: DateTimeParseException) {
                false // Parsing failed
            }
        }
    }
}