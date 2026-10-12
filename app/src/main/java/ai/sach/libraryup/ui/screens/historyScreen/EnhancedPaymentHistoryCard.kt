package ai.sach.libraryup.ui.screens.historyScreen

import ai.sach.libraryup.LibraryUpApplication
import ai.sach.libraryup.ui.common.responsiveness.LocalAccessibilityScaling
import ai.sach.libraryup.ui.screens.homescreen.TenantAvatar
import ai.sach.libraryup.ui.screens.settingsScreen.appprefernecs.AppPreferencesRepository
import ai.sach.libraryup.ui.screens.settingsScreen.appprefernecs.formatCurrencyString
import ai.sach.libraryup.ui.screens.settingsScreen.appprefernecs.formatWithCurrency
import ai.sach.libraryup.ui.screens.settingsScreen.appprefernecs.getCurrencySymbol
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Enhanced Payment History Card with combined design elements
 * Featuring tenant avatar, tenant name as main text, property as subtitle
 * Using better section alignment under "View Details" from the first version
 */
@Composable
fun EnhancedPaymentHistoryCard(
    payment: PaymentHistoryItem,
    isDarkTheme: Boolean = true // Default to dark theme as shown in image
) {
    // Colors based on theme with emphasis on dark theme from image
    val cardBackgroundColor = if (isDarkTheme) {
        Color(0xFF121212) // Dark background as shown in image
    } else {
        Color.White
    }

    val textColor = if (isDarkTheme) Color.White else Color.Black
    val secondaryTextColor = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Gray
    val accentGreen = Color(0xFF4CAF50) // Green color for "PAID" and "View Details" button

    // Get font scaling information
    val fontScaling = LocalAccessibilityScaling.current

    // Controlled font sizes
    val titleFontSize = 18.sp
    val subtitleFontSize = 14.sp
    val smallTextFontSize = 12.sp
    val amountFontSize = 18.sp

    // Context for sending intents
    val context = LocalContext.current

    // State for expanding/collapsing details
    var expanded by remember { mutableStateOf(false) }

    // Format payment date more readably
    val formattedDate = try {
        val date = LocalDate.parse(payment.date)
        date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    } catch (e: Exception) {
        payment.date // Fallback to original format if parsing fails
    }
    val appPreferencesRepository = remember {
        (context.applicationContext as LibraryUpApplication).appPreferencesRepository
    }
    val currencyCode by appPreferencesRepository.currency.collectAsState(initial = AppPreferencesRepository.DEFAULT_CURRENCY)
    val currencySymbol = getCurrencySymbol(currencyCode)

    // Main card with simplified design matching image
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Main content
            PaymentCardHeader(
                payment = payment,
                isDarkTheme = isDarkTheme,
                textColor = textColor,
                secondaryTextColor = secondaryTextColor,
                accentGreen = accentGreen,
                titleFontSize = titleFontSize,
                subtitleFontSize = subtitleFontSize,
                amountFontSize = amountFontSize
            )

            // View Details button with green text
            PaymentCardToggleButton(
                expanded = expanded,
                onToggle = { expanded = !expanded },
                subtitleFontSize = subtitleFontSize,
                accentGreen = accentGreen
            )

            // Expandable content - using the better section from first version
            AnimatedVisibility(visible = expanded) {
                PaymentCardExpandedContent(
                    payment = payment,
                    formattedDate = formattedDate,
                    isDarkTheme = isDarkTheme,
                    textColor = textColor,
                    secondaryTextColor = secondaryTextColor,
                    subtitleFontSize = subtitleFontSize,
                    smallTextFontSize = smallTextFontSize,
                    fontScaling = fontScaling,
                    context = context,
                    currencySymbol = currencySymbol
                )
            }
        }
    }
}

@Composable
private fun PaymentCardHeader(
    payment: PaymentHistoryItem,
    isDarkTheme: Boolean,
    textColor: Color,
    secondaryTextColor: Color,
    accentGreen: Color,
    titleFontSize: TextUnit,
    subtitleFontSize: TextUnit,
    amountFontSize: TextUnit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Tenant avatar - using TenantAvatar component
        TenantAvatar(
            modifier = Modifier.size(48.dp),
            avatarUri = payment.avatarUri,
            isDarkTheme = isDarkTheme
        )

        // Center: Tenant name and property info with controlled text length
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            // Tenant name as main title - reduced size
            Text(
                text = payment.tenantName,
                fontSize = titleFontSize,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Property name as subtitle - reduced size
            Text(
                text = payment.propertyName,
                fontSize = subtitleFontSize,
                color = secondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Right: Amount and paid status with fixed width column
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.width(85.dp) // Fixed width to prevent layout shifts
        ) {
            Text(
                text = formatWithCurrency(payment.amount),
                fontSize = amountFontSize,
                fontWeight = FontWeight.Bold,
                color = accentGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Paid",
                fontSize = subtitleFontSize,
                fontWeight = FontWeight.Medium,
                color = accentGreen
            )
        }
    }
}

@Composable
private fun PaymentCardToggleButton(
    expanded: Boolean,
    onToggle: () -> Unit,
    subtitleFontSize: TextUnit,
    accentGreen: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (expanded) "Hide Details" else "View Details",
            fontSize = subtitleFontSize,
            fontWeight = FontWeight.Medium,
            color = accentGreen
        )

        Icon(
            imageVector = if (expanded)
                Icons.Default.KeyboardArrowUp
            else
                Icons.Default.KeyboardArrowDown,
            contentDescription = if (expanded) "Hide" else "View",
            tint = accentGreen,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
private fun PaymentCardExpandedContent(
    payment: PaymentHistoryItem,
    formattedDate: String,
    isDarkTheme: Boolean,
    textColor: Color,
    secondaryTextColor: Color,
    subtitleFontSize: TextUnit,
    smallTextFontSize: TextUnit,
    fontScaling: ai.sach.libraryup.ui.common.responsiveness.AccessibilityScaling,
    context: android.content.Context,
    currencySymbol: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Divider
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.2f)
        )

        // Payment details section
        PaymentDetailsSection(
            payment = payment,
            formattedDate = formattedDate,
            isDarkTheme = isDarkTheme,
            textColor = textColor,
            secondaryTextColor = secondaryTextColor,
            subtitleFontSize = subtitleFontSize,
            smallTextFontSize = smallTextFontSize,
            fontScaling = fontScaling
        )

        // Remark section if available
        if (!payment.remark.isNullOrBlank()) {
            PaymentRemarkSection(
                payment = payment,
                isDarkTheme = isDarkTheme,
                textColor = textColor,
                secondaryTextColor = secondaryTextColor,
                subtitleFontSize = subtitleFontSize,
                smallTextFontSize = smallTextFontSize,
                fontScaling = fontScaling
            )
        }

        // Action buttons section
        PaymentActionButtons(
            payment = payment,
            formattedDate = formattedDate,
            isDarkTheme = isDarkTheme,
            subtitleFontSize = subtitleFontSize,
            fontScaling = fontScaling,
            context = context,
            currencySymbol = currencySymbol
        )
    }
}

@Composable
private fun PaymentDetailsSection(
    payment: PaymentHistoryItem,
    formattedDate: String,
    isDarkTheme: Boolean,
    textColor: Color,
    secondaryTextColor: Color,
    subtitleFontSize: TextUnit,
    smallTextFontSize: TextUnit,
    fontScaling: ai.sach.libraryup.ui.common.responsiveness.AccessibilityScaling
) {
    // If font is very large, stack the payment details vertically
    if (fontScaling.fontScale > 1.2f) {
        // Vertical layout for large font sizes
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Collected By
            PaymentDetailItem(
                icon = Icons.Default.Person,
                iconTint = Color(0xFF4CAF50),
                label = "Collected By:",
                value = payment.collectedBy,
                textColor = textColor,
                labelColor = secondaryTextColor,
                labelFontSize = smallTextFontSize,
                valueFontSize = subtitleFontSize
            )

            // Payment date
            PaymentDetailItem(
                icon = Icons.Default.DateRange,
                iconTint = Color(0xFFFF9800),
                label = "Payment Date:",
                value = formattedDate,
                textColor = textColor,
                labelColor = secondaryTextColor,
                labelFontSize = smallTextFontSize,
                valueFontSize = subtitleFontSize
            )
        }
    } else {
        // Original horizontal layout for normal font sizes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Collected By
            PaymentDetailItemHorizontal(
                icon = Icons.Default.Person,
                iconTint = Color(0xFF4CAF50),
                label = "Collected By:",
                value = payment.collectedBy,
                textColor = textColor,
                labelColor = secondaryTextColor,
                labelFontSize = smallTextFontSize,
                valueFontSize = subtitleFontSize,
                modifier = Modifier.weight(1f)
            )

            // Payment date
            PaymentDetailItemHorizontal(
                icon = Icons.Default.DateRange,
                iconTint = Color(0xFFFF9800),
                label = "Payment Date:",
                value = formattedDate,
                textColor = textColor,
                labelColor = secondaryTextColor,
                labelFontSize = smallTextFontSize,
                valueFontSize = subtitleFontSize,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End
            )
        }
    }
}

@Composable
private fun PaymentRemarkSection(
    payment: PaymentHistoryItem,
    isDarkTheme: Boolean,
    textColor: Color,
    secondaryTextColor: Color,
    subtitleFontSize: TextUnit,
    smallTextFontSize: TextUnit,
    fontScaling: ai.sach.libraryup.ui.common.responsiveness.AccessibilityScaling
) {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.2f)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF9C27B0).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Note,
                contentDescription = "Note",
                tint = Color(0xFF9C27B0),
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = "Note:",
                fontSize = smallTextFontSize,
                color = secondaryTextColor
            )
            payment.remark?.let {
                Text(
                    text = it,
                    fontSize = subtitleFontSize,
                    color = textColor,
                    // Use multiple lines for long remarks
                    maxLines = if (fontScaling.fontScale > 1.2f) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PaymentActionButtons(
    payment: PaymentHistoryItem,
    formattedDate: String,
    isDarkTheme: Boolean,
    subtitleFontSize: TextUnit,
    fontScaling: ai.sach.libraryup.ui.common.responsiveness.AccessibilityScaling,
    context: android.content.Context,
    currencySymbol: String
) {
    // Vertically stack buttons if font size is very large
    if (fontScaling.fontScale > 1.2f) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDarkTheme) Color(0xFF2A2A2A) else Color(0xFFF5F7FA)),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PrintReceiptButton(
                payment = payment,
                formattedDate = formattedDate,
                isDarkTheme = isDarkTheme,
                subtitleFontSize = subtitleFontSize,
                context = context,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(
                color = if (isDarkTheme) Color(0xFF333333) else Color(0xFFE0E0E0)
            )

            SendCopyButton(
                payment = payment,
                formattedDate = formattedDate,
                isDarkTheme = isDarkTheme,
                subtitleFontSize = subtitleFontSize,
                context = context,
                currencySymbol = currencySymbol,
                modifier = Modifier.fillMaxWidth()
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDarkTheme) Color(0xFF2A2A2A) else Color(0xFFF5F7FA))
        ) {
            // Print receipt button
            PrintReceiptButton(
                payment = payment,
                formattedDate = formattedDate,
                isDarkTheme = isDarkTheme,
                subtitleFontSize = subtitleFontSize,
                context = context,
                modifier = Modifier.weight(1f)
            )

            // Vertical divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(if (isDarkTheme) Color(0xFF333333) else Color(0xFFE0E0E0))
            )

            // Send copy button
            SendCopyButton(
                payment = payment,
                formattedDate = formattedDate,
                isDarkTheme = isDarkTheme,
                subtitleFontSize = subtitleFontSize,
                context = context,
                currencySymbol = currencySymbol,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PrintReceiptButton(
    payment: PaymentHistoryItem,
    formattedDate: String,
    isDarkTheme: Boolean,
    subtitleFontSize: TextUnit,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = {
            // Create intent to print/share receipt
            val message = "Receipt for ${payment.tenantName}\n" +
                    "Amount: ${payment.amount}\n" +
                    "Date: $formattedDate\n" +
                    "Property: ${payment.propertyName}\n" +
                    "Collected By: ${payment.collectedBy}"

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Payment Receipt - ${payment.tenantName}")
                putExtra(Intent.EXTRA_TEXT, message)
            }
            context.startActivity(Intent.createChooser(intent, "Print or Share Receipt"))
        },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = if (isDarkTheme) Color.White else Color.Black
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Print,
            contentDescription = "Print",
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            "Print Receipt",
            fontSize = subtitleFontSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SendCopyButton(
    payment: PaymentHistoryItem,
    formattedDate: String,
    isDarkTheme: Boolean,
    subtitleFontSize: TextUnit,
    context: android.content.Context,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = {
            // Create intent to send receipt via messaging app
            val message = "Receipt for ${payment.tenantName}\n" +
                    "Amount: ${formatCurrencyString(
                        payment.amount, currencySymbol,
                    )}\n" +
                    "Date: $formattedDate\n" +
                    "Property: ${payment.propertyName}\n" +
                    "Collected By: ${payment.collectedBy}"

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            context.startActivity(Intent.createChooser(intent, "Send Receipt via"))
        },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = if (isDarkTheme) Color.White else Color.Black
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = "Share",
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            "Send Copy",
            fontSize = subtitleFontSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Helper component for vertical layout
@Composable
private fun PaymentDetailItem(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    textColor: Color,
    labelColor: Color,
    labelFontSize: TextUnit,
    valueFontSize: TextUnit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = label,
                fontSize = labelFontSize,
                color = labelColor
            )

            Text(
                text = value,
                fontSize = valueFontSize,
                fontWeight = FontWeight.Medium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PaymentDetailItemHorizontal(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    textColor: Color,
    labelColor: Color,
    labelFontSize: TextUnit,
    valueFontSize: TextUnit,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = label,
                fontSize = labelFontSize,
                color = labelColor
            )

            Text(
                text = value,
                fontSize = valueFontSize,
                fontWeight = FontWeight.Medium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}