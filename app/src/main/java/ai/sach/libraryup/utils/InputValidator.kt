package ai.sach.libraryup.utils

import android.util.Patterns
import java.util.regex.Pattern

/**
 * Input validation utility for security and data integrity
 * Provides sanitization and validation methods for user inputs
 */
object InputValidator {
    
    // Regex patterns for validation
    private val phonePattern = Pattern.compile("^[+]?[0-9\\s\\-()]{7,15}$")
    private val namePattern = Pattern.compile("^[a-zA-Z\\s.'-]{1,50}$")
    private val addressPattern = Pattern.compile("^[a-zA-Z0-9\\s,.#\\-/]{1,200}$")
    private val amountPattern = Pattern.compile("^[0-9]{1,10}(\\.[0-9]{1,2})?$")
    
    /**
     * Sanitize phone number by removing invalid characters
     */
    fun sanitizePhoneNumber(phone: String): String {
        return phone.replace(Regex("[^0-9+\\-\\s()]"), "").trim()
    }
    
    /**
     * Validate phone number format
     */
    fun isValidPhoneNumber(phone: String): Boolean {
        val sanitized = sanitizePhoneNumber(phone)
        return sanitized.isNotEmpty() && phonePattern.matcher(sanitized).matches()
    }
    
    /**
     * Sanitize name by removing potentially harmful characters
     */
    fun sanitizeName(name: String): String {
        return name.replace(Regex("[^a-zA-Z\\s.'-]"), "").trim()
    }
    
    /**
     * Validate name format
     */
    fun isValidName(name: String): Boolean {
        val sanitized = sanitizeName(name)
        return sanitized.isNotEmpty() && namePattern.matcher(sanitized).matches()
    }
    
    /**
     * Validate email format
     */
    fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    /**
     * Sanitize email by trimming and converting to lowercase
     */
    fun sanitizeEmail(email: String): String {
        return email.trim().lowercase()
    }
    
    /**
     * Sanitize address by removing potentially harmful characters
     */
    fun sanitizeAddress(address: String): String {
        return address.replace(Regex("[^a-zA-Z0-9\\s,.#\\-/]"), "").trim()
    }
    
    /**
     * Validate address format
     */
    fun isValidAddress(address: String): Boolean {
        val sanitized = sanitizeAddress(address)
        return sanitized.isNotEmpty() && addressPattern.matcher(sanitized).matches()
    }
    
    /**
     * Sanitize file name to prevent directory traversal and other attacks
     */
    fun sanitizeFileName(fileName: String): String {
        return fileName
            .replace(Regex("[^a-zA-Z0-9._\\-]"), "_")
            .replace(Regex("^\\.+"), "") // Remove leading dots
            .replace(Regex("\\.{2,}"), ".") // Replace multiple dots with single dot
            .take(100) // Limit length
    }
    
    /**
     * Validate file name
     */
    fun isValidFileName(fileName: String): Boolean {
        val sanitized = sanitizeFileName(fileName)
        return sanitized.isNotEmpty() && 
               !sanitized.contains("..") && 
               !sanitized.startsWith("/") &&
               !sanitized.contains("\\")
    }
    
    /**
     * Sanitize amount/currency input
     */
    fun sanitizeAmount(amount: String): String {
        return amount.replace(Regex("[^0-9.]"), "").trim()
    }
    
    /**
     * Validate amount format
     */
    fun isValidAmount(amount: String): Boolean {
        val sanitized = sanitizeAmount(amount)
        return sanitized.isNotEmpty() && amountPattern.matcher(sanitized).matches()
    }
    
    /**
     * Sanitize general text input to prevent XSS and injection attacks
     */
    fun sanitizeText(text: String): String {
        return text
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;")
            .trim()
    }
    
    /**
     * Validate text length
     */
    fun isValidTextLength(text: String, maxLength: Int): Boolean {
        return text.length <= maxLength
    }
    
    /**
     * Check if string contains potentially malicious content
     */
    fun containsMaliciousContent(input: String): Boolean {
        val maliciousPatterns = listOf(
            "script", "javascript:", "vbscript:", "onload", "onerror",
            "SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "UNION",
            "../", "..\\", "<script", "</script"
        )
        
        val lowerInput = input.lowercase()
        return maliciousPatterns.any { pattern ->
            lowerInput.contains(pattern.lowercase())
        }
    }
    
    /**
     * Comprehensive input validation
     */
    fun validateInput(input: String, type: InputType, maxLength: Int = 255): ValidationResult {
        if (input.isEmpty()) {
            return ValidationResult(false, "Input cannot be empty")
        }
        
        if (!isValidTextLength(input, maxLength)) {
            return ValidationResult(false, "Input exceeds maximum length of $maxLength characters")
        }
        
        if (containsMaliciousContent(input)) {
            return ValidationResult(false, "Input contains potentially malicious content")
        }
        
        return when (type) {
            InputType.NAME -> {
                if (isValidName(input)) ValidationResult(true, "Valid")
                else ValidationResult(false, "Invalid name format")
            }
            InputType.EMAIL -> {
                if (isValidEmail(input)) ValidationResult(true, "Valid")
                else ValidationResult(false, "Invalid email format")
            }
            InputType.PHONE -> {
                if (isValidPhoneNumber(input)) ValidationResult(true, "Valid")
                else ValidationResult(false, "Invalid phone number format")
            }
            InputType.ADDRESS -> {
                if (isValidAddress(input)) ValidationResult(true, "Valid")
                else ValidationResult(false, "Invalid address format")
            }
            InputType.AMOUNT -> {
                if (isValidAmount(input)) ValidationResult(true, "Valid")
                else ValidationResult(false, "Invalid amount format")
            }
            InputType.FILENAME -> {
                if (isValidFileName(input)) ValidationResult(true, "Valid")
                else ValidationResult(false, "Invalid file name")
            }
            InputType.TEXT -> ValidationResult(true, "Valid")
        }
    }
    
    enum class InputType {
        NAME, EMAIL, PHONE, ADDRESS, AMOUNT, FILENAME, TEXT
    }
    
    data class ValidationResult(
        val isValid: Boolean,
        val message: String
    )
}