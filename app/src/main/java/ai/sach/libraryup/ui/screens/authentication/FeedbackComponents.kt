package ai.sach.libraryup.ui.screens.authentication

import ai.sach.libraryup.ui.common.responsiveness.LocalDimensions
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Feedback message types
 */
enum class FeedbackType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

/**
 * Data class for feedback messages
 */
data class FeedbackMessage(
    val message: String,
    val type: FeedbackType,
    val duration: Long = 5000L, // 5 seconds default
    val isDismissible: Boolean = true,
    val actionText: String? = null,
    val onAction: (() -> Unit)? = null
)

/**
 * Enhanced feedback message component with animations
 */
@Composable
fun FeedbackMessageCard(
    feedbackMessage: FeedbackMessage,
    isVisible: Boolean = true,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalDimensions.current
    var shouldShow by remember(feedbackMessage) { mutableStateOf(isVisible) }
    
    // Auto-dismiss after duration
    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage.duration > 0) {
            delay(feedbackMessage.duration)
            shouldShow = false
            onDismiss?.invoke()
        }
    }
    
    AnimatedVisibility(
        visible = shouldShow && isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        val (backgroundColor, contentColor, icon) = getFeedbackStyling(feedbackMessage.type)
        
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            ),
            shape = RoundedCornerShape(dimensions.radiusMedium),
            elevation = CardDefaults.cardElevation(
                defaultElevation = dimensions.cardElevationMedium
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensions.paddingMedium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
            ) {
                // Feedback icon
                Icon(
                    imageVector = icon,
                    contentDescription = feedbackMessage.type.name,
                    modifier = Modifier.size(dimensions.iconSizeMedium),
                    tint = contentColor
                )
                
                // Message content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(dimensions.spacingTiny)
                ) {
                    Text(
                        text = feedbackMessage.message,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = dimensions.fontSizeMedium
                        ),
                        color = contentColor,
                        fontWeight = FontWeight.Medium
                    )
                    
                    // Action text if provided
                    if (feedbackMessage.actionText != null && feedbackMessage.onAction != null) {
                        Text(
                            text = feedbackMessage.actionText,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = dimensions.fontSizeSmall
                            ),
                            color = contentColor.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = dimensions.spacingTiny)
                        )
                    }
                }
                
                // Dismiss button
                if (feedbackMessage.isDismissible && onDismiss != null) {
                    IconButton(
                        onClick = {
                            shouldShow = false
                            onDismiss()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            modifier = Modifier.size(dimensions.iconSizeSmall),
                            tint = contentColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Simple toast-like message
 */
@Composable
fun ToastMessage(
    message: String,
    type: FeedbackType = FeedbackType.INFO,
    isVisible: Boolean = true,
    duration: Long = 3000L,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalDimensions.current
    var shouldShow by remember(message) { mutableStateOf(isVisible) }
    
    LaunchedEffect(message) {
        if (duration > 0) {
            delay(duration)
            shouldShow = false
            onDismiss?.invoke()
        }
    }
    
    AnimatedVisibility(
        visible = shouldShow && isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        val (backgroundColor, contentColor, icon) = getFeedbackStyling(type)
        
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            ),
            shape = RoundedCornerShape(dimensions.radiusLarge),
            elevation = CardDefaults.cardElevation(
                defaultElevation = dimensions.cardElevationLarge
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensions.paddingMedium,
                        vertical = dimensions.paddingSmall
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = type.name,
                    modifier = Modifier.size(dimensions.iconSizeSmall),
                    tint = contentColor
                )
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = dimensions.fontSizeSmall
                    ),
                    color = contentColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Welcome message component
 */
@Composable
fun WelcomeMessage(
    userName: String,
    isVisible: Boolean = true,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalDimensions.current
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(dimensions.radiusLarge),
            elevation = CardDefaults.cardElevation(
                defaultElevation = dimensions.cardElevationMedium
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensions.paddingLarge),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Welcome",
                    modifier = Modifier.size(dimensions.iconSizeLarge),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(dimensions.spacingTiny)
                ) {
                    Text(
                        text = "Welcome, $userName!",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = dimensions.fontSizeLarge
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Text(
                        text = "You're all set to manage your rentals",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = dimensions.fontSizeMedium
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                
                if (onDismiss != null) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            modifier = Modifier.size(dimensions.iconSizeSmall),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Operation status indicator
 */
@Composable
fun OperationStatusIndicator(
    operation: String,
    isInProgress: Boolean,
    isCompleted: Boolean,
    hasError: Boolean,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalDimensions.current
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                hasError -> MaterialTheme.colorScheme.errorContainer
                isCompleted -> MaterialTheme.colorScheme.primaryContainer
                isInProgress -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(dimensions.radiusSmall)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.paddingSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
        ) {
            // Status icon
            when {
                hasError -> {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        modifier = Modifier.size(dimensions.iconSizeSmall),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                isCompleted -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        modifier = Modifier.size(dimensions.iconSizeSmall),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                isInProgress -> {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(dimensions.iconSizeSmall),
                        color = MaterialTheme.colorScheme.secondary,
                        strokeWidth = 2.dp
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Pending",
                        modifier = Modifier.size(dimensions.iconSizeSmall),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Operation text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = operation,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = dimensions.fontSizeSmall
                    ),
                    color = when {
                        hasError -> MaterialTheme.colorScheme.onErrorContainer
                        isCompleted -> MaterialTheme.colorScheme.onPrimaryContainer
                        isInProgress -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (isInProgress) FontWeight.Medium else FontWeight.Normal
                )
                
                if (hasError && errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = dimensions.fontSizeSmall * 0.9f
                        ),
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

/**
 * Get styling for different feedback types
 */
@Composable
private fun getFeedbackStyling(type: FeedbackType): Triple<Color, Color, ImageVector> {
    return when (type) {
        FeedbackType.SUCCESS -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Default.CheckCircle
        )
        FeedbackType.ERROR -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Default.Error
        )
        FeedbackType.WARNING -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            Icons.Default.Warning
        )
        FeedbackType.INFO -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.Info
        )
    }
}