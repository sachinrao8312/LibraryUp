package ai.sach.libraryup.ui.common.composables

import ai.sach.libraryup.ui.common.responsiveness.LocalAccessibilityScaling
import ai.sach.libraryup.ui.common.responsiveness.LocalDimensions
import ai.sach.libraryup.ui.theme.AppColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

/**
 * Improved responsive component for managing multiple phone number input fields.
 * Features WhatsApp-inspired design with better handling for different screen sizes
 * and accessibility settings.
 * 
 * @param phoneNumbers List of phone number string values
 * @param phoneNumberErrors List of booleans indicating validation errors
 * @param onPhoneNumbersChanged Callback when phone numbers or their error states change
 */
@Composable
fun ResponsiveMultiplePhoneNumbersSection(
    phoneNumbers: List<String>,
    phoneNumberErrors: List<Boolean>,
    onPhoneNumbersChanged: (List<String>, List<Boolean>) -> Unit
) {
    // Get responsive dimensions
    val dimensions = LocalDimensions.current
    // Get accessibility scaling
    val accessibilityScaling = LocalAccessibilityScaling.current

    // Get screen configuration for responsive layout
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isCompactScreen = screenWidthDp < 360 // Small screens like older phones

    // Limit the scaling factor to prevent text from getting too large
    // This helps maintain the WhatsApp-like compact appearance
    val scalingFactor = minOf(accessibilityScaling.contentScale, 1.3f)

    // Calculate responsive sizes with limits
    val textSize = dimensions.fontSizeMedium
    val iconSize = dimensions.iconSizeSmall * scalingFactor
    
    // Adjust button size based on screen dimensions
    val buttonHeight = when {
        isCompactScreen -> 36.dp * scalingFactor
        else -> 40.dp * scalingFactor
    }
    
    // Limit button width for better visual appearance
    val buttonMaxWidth = when {
        isCompactScreen -> 120.dp
        screenWidthDp < 600 -> 140.dp
        else -> 160.dp
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
    ) {
        // Header row with label and Add button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Phone Numbers*",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = textSize)
                )
                Text(
                    text = "Auto-formats as you type: 9876543210 → 98765 43210",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = min(dimensions.fontSizeSmall.value, 11f).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // WhatsApp-like "Add Number" button with constrained size
            Button(
                onClick = {
                    val newPhoneNumbers = phoneNumbers + ""
                    val newErrors = phoneNumberErrors + false
                    onPhoneNumbersChanged(newPhoneNumbers, newErrors)
                },
                modifier = Modifier
                    .height(buttonHeight)
                    .widthIn(max = buttonMaxWidth),
                shape = RoundedCornerShape(percent = 50), // More rounded like WhatsApp
                contentPadding = PaddingValues(
                    horizontal = dimensions.paddingMedium,
                    vertical = dimensions.paddingSmall * scalingFactor
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Phone Number",
                        modifier = Modifier.size(iconSize)
                    )
                    
                    // Only show text if we have enough space
                    if (!isCompactScreen || screenWidthDp >= 340) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Add Number",
                            // Explicitly limit text size to prevent it from getting too large
                            fontSize = min(textSize.value, 12f).sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Display existing phone number fields
        phoneNumbers.forEachIndexed { index, phoneNumber ->
            PhoneNumberField(
                value = phoneNumber,
                error = phoneNumberErrors.getOrElse(index) { false },
                onValueChange = { newValue ->
                    // Auto-format the phone number as user types
                    val formattedValue = formatPhoneNumber(newValue)
                    val newPhoneNumbers = phoneNumbers.toMutableList().also { it[index] = formattedValue }
                    val newErrors = phoneNumberErrors.toMutableList().also {
                        it[index] = formattedValue.isNotBlank() && !isValidPhoneNumber(formattedValue)
                    }
                    onPhoneNumbersChanged(newPhoneNumbers, newErrors)
                },
                onRemove = {
                    if (phoneNumbers.size > 1) {
                        val newPhoneNumbers = phoneNumbers.toMutableList().apply { removeAt(index) }
                        val newErrors = phoneNumberErrors.toMutableList().apply { removeAt(index) }
                        onPhoneNumbersChanged(newPhoneNumbers, newErrors)
                    } else if (phoneNumbers.size == 1) {
                        onPhoneNumbersChanged(listOf(""), listOf(false))
                    }
                },
                showRemoveButton = phoneNumbers.size > 1 || (phoneNumbers.size == 1 && phoneNumber.isNotBlank()),
                index = index,
                scalingFactor = scalingFactor
            )
        }
    }
}

/**
 * Individual phone number input field with validation and remove button.
 * Designed to be responsive and accessible while maintaining visual consistency.
 */
@Composable
private fun PhoneNumberField(
    value: String,
    error: Boolean,
    onValueChange: (String) -> Unit,
    onRemove: () -> Unit,
    showRemoveButton: Boolean,
    index: Int,
    scalingFactor: Float
) {
    // Get responsive dimensions
    val dimensions = LocalDimensions.current
    
    // Calculate sizes with scaling limits
    val textSize = dimensions.fontSizeMedium
    val iconSize = dimensions.iconSizeSmall * scalingFactor
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(
                    "Phone Number ${index + 1}",
                    // Limit label text size
                    fontSize = min(dimensions.fontSizeSmall.value, 14f).sp
                )
            },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            textStyle = TextStyle(
                // Limit input text size
                fontSize = min(textSize.value, 16f).sp
            ),
            leadingIcon = {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = "Phone",
                    modifier = Modifier.size(dimensions.iconSizeSmall * scalingFactor)
                )
            },
            isError = error,
            supportingText = {
                if (error) {
                    val errorMessage = when {
                        value.isBlank() -> "Phone number is required"
                        !value.all { it.isDigit() || it in setOf('+', '-', ' ', '(', ')', '.') } -> "Only digits, +, -, spaces, (), . allowed"
                        value.filter { it.isDigit() }.length < 7 -> "Need at least 7 digits (e.g., 9876543210)"
                        value.startsWith("+") && value.filter { it.isDigit() }.length < 10 -> "International numbers need 10+ digits (e.g., +91 9876543210)"
                        else -> "Invalid phone number format"
                    }
                    Text(
                        errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        // Limit error text size
                        fontSize = min(dimensions.fontSizeSmall.value, 12f).sp
                    )
                }
            },
            singleLine = true
        )

        if (showRemoveButton) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .padding(start = dimensions.spacingSmall)
                    .size(40.dp * scalingFactor)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove Phone Number ${index + 1}",
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

/**
 * Auto-formats phone numbers as user types for better UX
 * Formats: +91 98765 43210, (987) 654-3210, 98765 43210
 */
internal fun formatPhoneNumber(input: String): String {
    if (input.isBlank()) return input
    
    // Remove all non-digit characters except + at the beginning
    val digits = input.filter { it.isDigit() }
    val hasPlus = input.startsWith("+")
    
    return when {
        // International format (starts with +)
        hasPlus && digits.length >= 10 -> {
            val countryCode = digits.take(2)
            val remaining = digits.drop(2)
            when {
                remaining.length >= 10 -> "+$countryCode ${remaining.take(5)} ${remaining.drop(5).take(5)}"
                remaining.length >= 5 -> "+$countryCode ${remaining.take(5)} ${remaining.drop(5)}"
                else -> "+$countryCode $remaining"
            }
        }
        hasPlus && digits.length >= 2 -> "+${digits.take(2)} ${digits.drop(2)}"
        hasPlus -> "+$digits"
        
        // Local format (no +)
        digits.length >= 10 -> {
            val first = digits.take(5)
            val second = digits.drop(5).take(5)
            "$first $second"
        }
        digits.length >= 7 -> {
            val first = digits.take(5)
            val second = digits.drop(5)
            "$first $second"
        }
        else -> digits
    }
}

/**
 * Improved validation function for phone numbers.
 * Supports various international formats and provides better user experience.
 * 
 * ACCEPTED FORMATS:
 * - Local: 9876543210, 98765 43210, (987) 654-3210
 * - International: +91 98765 43210, +1 (987) 654-3210
 * - Minimum: 7 digits for local, 10+ digits for international
 */
internal fun isValidPhoneNumber(phoneNumber: String): Boolean {
    if (phoneNumber.isBlank()) return true // Allow empty for optional fields
    
    val digits = phoneNumber.filter { it.isDigit() }
    
    // Check if it contains only valid characters
    val validChars = setOf('+', '-', ' ', '(', ')', '.')
    if (!phoneNumber.all { it.isDigit() || it in validChars }) {
        return false
    }
    
    // Check minimum length requirements
    return when {
        phoneNumber.startsWith("+") -> digits.length >= 10 // International format needs 10+ digits
        digits.length >= 7 -> true // Local format needs at least 7 digits
        else -> false
    }
}