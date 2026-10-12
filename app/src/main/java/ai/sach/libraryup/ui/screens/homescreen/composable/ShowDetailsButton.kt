// ShowDetailsButton.kt

package ai.sach.rentup.ui.screens.homescreen.composable

import ai.sach.rentup.ui.common.responsiveness.LocalAccessibilityScaling
import ai.sach.rentup.ui.common.responsiveness.rememberDimensions
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun ShowDetailsButton(
    expanded: Boolean,
    onClick: () -> Unit,
    isDarkTheme: Boolean = false
) {
    // Get responsive dimensions
    val dimensions = rememberDimensions()
    val fontScaling = LocalAccessibilityScaling.current

    // Use proper color for dark theme
    val textColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f) // Theme-aware
    } else {
        MaterialTheme.colorScheme.primary
    }

    // Control button text size
    val buttonTextSize = dimensions.fontSizeMedium

    // Adjust padding based on screen and font size
    val verticalPadding = when {
        fontScaling.fontScale > 1.2f -> dimensions.spacingSmall / 2 // Reduce padding for large fonts
        else -> dimensions.spacingSmall
    }

    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = verticalPadding),
        colors = ButtonDefaults.textButtonColors(
            contentColor = textColor
        ),
        // Add semantics for accessibility
        contentPadding = PaddingValues(
            vertical = dimensions.spacingSmall / 2, // Reduced padding
            horizontal = dimensions.paddingSmall
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics {
                // Improved accessibility description based on state
                contentDescription = if (expanded) {
                    "Hide payment history details"
                } else {
                    "Show payment history details"
                }
            }
        ) {
            Text(
                text = if (expanded) "Hide Payment History" else "Show Payment History", // Shortened text for large fonts
                style = MaterialTheme.typography.bodyMedium,
                fontSize = buttonTextSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(dimensions.spacingTiny))

            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(dimensions.iconSizeMedium * fontScaling.contentScale.coerceAtMost(1.2f))
            )
        }
    }
}