
import ai.sach.rentup.RentUpApplication
import ai.sach.rentup.data.local.entity.Tenant
import ai.sach.rentup.ui.common.responsiveness.LocalAccessibilityScaling
import ai.sach.rentup.ui.common.responsiveness.rememberDimensions
import ai.sach.rentup.ui.screens.settingsScreen.composable.notifyPaymentCollected
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Comprehensive currency parsing function that handles all international currency formats
 * Supports: ₹, Rs, Rs., INR, $, €, £, ¥, USD, EUR, GBP, JPY, AED, SAR, and more
 */
fun parseAmountFromCurrency(input: String): Pair<Boolean, Double> {
    if (input.isBlank()) return Pair(false, 0.0)

    var cleaned = input.trim()

    // Remove currency codes and symbols (prefix)
    cleaned = cleaned.replace(Regex("^(Rs\\.?\\s*|INR\\s*|USD\\s*|EUR\\s*|GBP\\s*|JPY\\s*|AED\\s*|SAR\\s*|CNY\\s*|KRW\\s*|SGD\\s*|AUD\\s*|CAD\\s*|CHF\\s*|SEK\\s*|NOK\\s*|DKK\\s*|PLN\\s*|CZK\\s*|HUF\\s*|RUB\\s*|BRL\\s*|MXN\\s*|ZAR\\s*|NZD\\s*|THB\\s*|MYR\\s*|IDR\\s*|PHP\\s*|VND\\s*)", RegexOption.IGNORE_CASE), "")

    // Remove currency codes and symbols (suffix)
    cleaned = cleaned.replace(Regex("\\s*(Rs\\.?|INR|USD|EUR|GBP|JPY|AED|SAR|CNY|KRW|SGD|AUD|CAD|CHF|SEK|NOK|DKK|PLN|CZK|HUF|RUB|BRL|MXN|ZAR|NZD|THB|MYR|IDR|PHP|VND)\\s*$", RegexOption.IGNORE_CASE), "")

    // Remove currency symbols
    cleaned = cleaned.replace(Regex("[$₹£€¥¢¤₽₩₪₦₨₡₴₸₼₺₵₶₷₾₿＄￠￡￢￥￦]"), "")

    // Remove commas and spaces
    cleaned = cleaned.replace(Regex("[,\\s]"), "")

    // Final cleanup
    cleaned = cleaned.trim()

    return try {
        val numericValue = cleaned.toDouble()
        if (numericValue > 0) {
            Pair(true, numericValue)
        } else {
            Pair(false, 0.0)
        }
    } catch (e: NumberFormatException) {
        Pair(false, 0.0)
    }
}

@Composable
fun RentalAmountButtons(
    amount: String,
    tenantName: String,
    dueDate: String,
    tenant: Tenant? = null,
    isDarkTheme: Boolean = false,
    onRentCollect: (String, String, String) -> Unit = { _, _, _ -> }
) {
    // Get responsive dimensions
    val dimensions = rememberDimensions()
    val fontScaling = LocalAccessibilityScaling.current

    // Controlled font size scaling - use larger font size for buttons
    val buttonTextSize = 16.sp // Increased from dimensions.fontSizeMedium

    var showPaymentDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val application = context.applicationContext as? RentUpApplication

    // Button colors
    val primaryButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.White
    )

    val outlinedButtonColors = ButtonDefaults.outlinedButtonColors(
        contentColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary
    )

    // Check screen width for responsive layout
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isCompactScreen = screenWidthDp < 360 // Small screens like older phones

    // Adjust button height based on screen size and font scaling
    val buttonHeight = if (isCompactScreen) {
        dimensions.buttonHeightSmall
    } else {
        dimensions.buttonHeight
    } * fontScaling.contentScale.coerceAtMost(1.2f) // Limit height increase

    // Adjust spacing based on screen size
    val buttonSpacing = if (isCompactScreen) {
        dimensions.spacingSmall
    } else {
        dimensions.spacingMedium
    }

    if (isCompactScreen || fontScaling.fontScale > 1.15f) {
        // Stack buttons vertically on small screens or with large fonts
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
        ) {
            Button(
                onClick = { 
                    android.util.Log.d("RentalAmountButtons", "Collect Rent button clicked")
                    showPaymentDialog = true 
                },
                colors = primaryButtonColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight),
                shape = CircleShape, // Using CircleShape for rounded buttons
            ) {
                Text(
                    "Collect Rent",
                    fontSize = buttonTextSize,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedButton(
                onClick = {
                    // Prepare payment reminder message
                    val message = "Dear $tenantName, this is a friendly reminder that your rent payment of $amount is due on $dueDate. Please ensure timely payment. Thank you."

                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, message)
                    }

                    val chooser = Intent.createChooser(intent, "Send payment reminder via")
                    context.startActivity(chooser)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight),
                colors = outlinedButtonColors,
                border = BorderStroke(1.5.dp, if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary),
                shape = CircleShape // Using CircleShape for rounded buttons
            ) {
                Text(
                    "Send Reminder",
                    fontSize = buttonTextSize,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    } else {
        // Side by side buttons on larger screens
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
        ) {
            Button(
                onClick = { showPaymentDialog = true },
                colors = primaryButtonColors,
                modifier = Modifier
                    .weight(1f)
                    .height(buttonHeight),
                shape = CircleShape // Using CircleShape for rounded buttons
            ) {
                Text(
                    "Collect Rent",
                    fontSize = buttonTextSize,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedButton(
                onClick = {
                    // Prepare payment reminder message
                    val message = "Dear $tenantName, this is a friendly reminder that your rent payment of $amount is due on $dueDate. Please ensure timely payment. Thank you."

                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, message)
                    }

                    val chooser = Intent.createChooser(intent, "Send payment reminder via")
                    context.startActivity(chooser)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(buttonHeight),
                colors = outlinedButtonColors,
                border = BorderStroke(1.5.dp, if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary),
                shape = CircleShape // Using CircleShape for rounded buttons
            ) {
                Text(
                    "Send Reminder",
                    fontSize = buttonTextSize,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    if (showPaymentDialog) {
        PaymentCollectionDialog(
            onDismiss = { showPaymentDialog = false },
            onConfirm = { paymentAmount, collectedBy, remark ->
                android.util.Log.d("RentalAmountButtons", "Dialog confirmed: amount=$paymentAmount, collectedBy=$collectedBy, remark=$remark")
                onRentCollect(paymentAmount, collectedBy, remark)
                showPaymentDialog = false

                // Send notification if tenant object is provided
                tenant?.let { tenantObj ->
                    application?.notifyPaymentCollected(tenantObj, paymentAmount)
                }
            },
            initialAmount = amount,
            isDarkTheme = isDarkTheme
        )
    }
}

@Composable
fun PaymentCollectionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit, // Amount, CollectedBy, Remark
    initialAmount: String,
    isDarkTheme: Boolean = isSystemInDarkTheme() // Use system theme by default
) {
    // Get responsive dimensions
    val dimensions = rememberDimensions()

    // Parse initial amount using the robust function
    val (isValidInitial, parsedInitial) = parseAmountFromCurrency(initialAmount)
    val initialAmountClean = if (isValidInitial) parsedInitial.toString() else ""

    var amount by remember { mutableStateOf(initialAmountClean) }
    var collectedBy by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }

    // Validation states
    var amountError by remember { mutableStateOf<String?>(null) }
    var collectedByError by remember { mutableStateOf<String?>(null) }

    // Define text colors based on theme
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val errorColor = if (isDarkTheme) Color(0xFFFF6B6B) else Color(0xFFDC2626)
    val backgroundColor = if (isDarkTheme) Color(0xFF2A2A2A) else Color.White

    // Validate amount format using the improved function
    fun validateAmount(value: String): Boolean {
        val (isValid, _) = parseAmountFromCurrency(value)
        return if (!isValid) {
            amountError = "Please enter a valid amount"
            false
        } else {
            amountError = null
            true
        }
    }

    // Validate collector name
    fun validateCollector(value: String): Boolean {
        return if (value.isBlank()) {
            collectedByError = "Collector name is required"
            false
        } else {
            collectedByError = null
            true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = backgroundColor,
        title = {
            Text(
                "Collect Rent Payment",
                color = textColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensions.paddingSmall),
                verticalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        validateAmount(it)
                    },
                    label = { Text("Amount", fontSize = 16.sp, color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Gray) },
                    isError = amountError != null,
                    supportingText = {
                        amountError?.let {
                            Text(
                                it,
                                fontSize = 14.sp,
                                color = errorColor
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color.Gray
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 16.sp,
                        color = textColor
                    )
                )

                OutlinedTextField(
                    value = collectedBy,
                    onValueChange = {
                        collectedBy = it
                        validateCollector(it)
                    },
                    label = { Text("Collected By", fontSize = 16.sp, color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Gray) },
                    isError = collectedByError != null,
                    supportingText = {
                        collectedByError?.let {
                            Text(
                                it,
                                fontSize = 14.sp,
                                color = errorColor
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color.Gray
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 16.sp,
                        color = textColor
                    )
                )

                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text("Remark (Optional)", fontSize = 16.sp, color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color.Gray
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 16.sp,
                        color = textColor
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Validate inputs before confirming
                    val amountValid = validateAmount(amount)
                    val collectorValid = validateCollector(collectedBy)

                    if (amountValid && collectorValid) {
                        // Parse the final amount properly
                        val (isValid, finalAmount) = parseAmountFromCurrency(amount)
                        if (isValid) {
                            onConfirm(finalAmount.toString(), collectedBy.trim(), remark.trim())
                        }
                    }
                },
                enabled = amountError == null && collectedByError == null && amount.isNotBlank() && collectedBy.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                shape = CircleShape // Using CircleShape for rounded buttons
            ) {
                Text(
                    "Confirm",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isDarkTheme) Color.White else Color.Black
                )
            ) {
                Text(
                    "Cancel",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}