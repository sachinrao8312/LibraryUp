package ai.sach.rentup.ui.screens.authentication

import ai.sach.rentup.ui.common.responsiveness.Dimensions
import ai.sach.rentup.ui.common.responsiveness.LocalDimensions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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

/**
 * Enhanced error message component with recovery actions
 */
@Composable
fun EnhancedErrorMessage(
    error: GoogleAuthError,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalDimensions.current
    val errorResponse = error.toErrorResponse()
    val recoveryActions = error.getRecoveryActions()
    
    var showDetails by remember { mutableStateOf(false) }
    
    val (backgroundColor, textColor, iconColor, icon) = getErrorStyling(error)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(dimensions.radiusMedium),
        elevation = CardDefaults.cardElevation(
            defaultElevation = dimensions.cardElevationSmall
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
        ) {
            // Error header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Error",
                    modifier = Modifier.size(dimensions.iconSizeMedium),
                    tint = iconColor
                )
                
                Text(
                    text = errorResponse.message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = dimensions.fontSizeMedium
                    ),
                    color = textColor,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Recovery actions
            if (recoveryActions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(dimensions.spacingSmall))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
                ) {
                    // Primary action (retry if available)
                    if (errorResponse.canRetry && onRetry != null) {
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(dimensions.radiusSmall)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(dimensions.spacingTiny)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(dimensions.iconSizeSmall)
                                )
                                Text(
                                    text = errorResponse.suggestedAction,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                    
                    // Show details button
                    if (errorResponse.technicalDetails != null) {
                        TextButton(
                            onClick = { showDetails = !showDetails }
                        ) {
                            Text(
                                text = if (showDetails) "Hide Details" else "Show Details",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    
                    // Dismiss button
                    if (onDismiss != null) {
                        TextButton(
                            onClick = onDismiss
                        ) {
                            Text(
                                text = "Dismiss",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
            
            // Technical details (expandable)
            if (showDetails && errorResponse.technicalDetails != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(dimensions.radiusSmall)
                ) {
                    Column(
                        modifier = Modifier.padding(dimensions.paddingSmall)
                    ) {
                        Text(
                            text = "Technical Details:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = dimensions.fontSizeSmall
                            ),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = errorResponse.technicalDetails,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = dimensions.fontSizeSmall
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        
                        if (errorResponse.errorCode != null) {
                            Text(
                                text = "Error Code: ${errorResponse.errorCode}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = dimensions.fontSizeSmall
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Simple error message for inline display
 */
@Composable
fun SimpleErrorMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalDimensions.current
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(dimensions.radiusSmall)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.paddingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                modifier = Modifier.size(dimensions.iconSizeSmall),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = dimensions.fontSizeSmall
                ),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

/**
 * Recovery actions list component
 */
@Composable
fun RecoveryActionsList(
    actions: List<RecoveryAction>,
    onActionClick: (RecoveryAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalDimensions.current
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(dimensions.radiusMedium)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
        ) {
            Text(
                text = "Suggested Actions:",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = dimensions.fontSizeMedium
                ),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            actions.forEach { action ->
                RecoveryActionItem(
                    action = action,
                    onClick = { onActionClick(action) },
                    dimensions = dimensions
                )
            }
        }
    }
}

/**
 * Individual recovery action item
 */
@Composable
private fun RecoveryActionItem(
    action: RecoveryAction,
    onClick: () -> Unit,
    dimensions: Dimensions
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensions.radiusSmall)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
        ) {
            Icon(
                imageVector = getActionIcon(action.type),
                contentDescription = null,
                modifier = Modifier.size(dimensions.iconSizeSmall)
            )
            
            Text(
                text = action.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = dimensions.fontSizeSmall
                ),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start
            )
        }
    }
}

/**
 * Get appropriate styling for different error types
 */
@Composable
private fun getErrorStyling(error: GoogleAuthError): ErrorStyling {
    return when (error) {
        is GoogleAuthError.NetworkError,
        is GoogleAuthError.RateLimitExceeded -> ErrorStyling(
            backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
            textColor = MaterialTheme.colorScheme.onSecondaryContainer,
            iconColor = MaterialTheme.colorScheme.secondary,
            icon = Icons.Default.Warning
        )
        
        is GoogleAuthError.AccountExists,
        is GoogleAuthError.AccountCollision -> ErrorStyling(
            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
            textColor = MaterialTheme.colorScheme.onPrimaryContainer,
            iconColor = MaterialTheme.colorScheme.primary,
            icon = Icons.Default.Info
        )
        
        else -> ErrorStyling(
            backgroundColor = MaterialTheme.colorScheme.errorContainer,
            textColor = MaterialTheme.colorScheme.onErrorContainer,
            iconColor = MaterialTheme.colorScheme.error,
            icon = Icons.Default.Error
        )
    }
}

/**
 * Get appropriate icon for recovery action types
 */
private fun getActionIcon(type: RecoveryAction.Type): ImageVector {
    return when (type) {
        RecoveryAction.Type.RETRY -> Icons.Default.Refresh
        else -> Icons.Default.Info
    }
}

/**
 * Data class for error styling
 */
private data class ErrorStyling(
    val backgroundColor: Color,
    val textColor: Color,
    val iconColor: Color,
    val icon: ImageVector
)