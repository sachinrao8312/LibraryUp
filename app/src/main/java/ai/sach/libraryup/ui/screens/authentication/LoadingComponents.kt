package ai.sach.libraryup.ui.screens.authentication

import ai.sach.libraryup.ui.common.responsiveness.Dimensions
import ai.sach.libraryup.ui.common.responsiveness.LocalDimensions
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Enhanced loading indicator with message and progress
 */
@Composable
fun AuthLoadingIndicator(
    message: String = "Loading...",
    progress: Float? = null,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return
    
    val dimensions = LocalDimensions.current
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(dimensions.radiusMedium),
        elevation = CardDefaults.cardElevation(
            defaultElevation = dimensions.cardElevationSmall
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.paddingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
        ) {
            // Progress indicator
            if (progress != null) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(dimensions.iconSizeMedium),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    strokeCap = StrokeCap.Round
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(dimensions.iconSizeMedium),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    strokeCap = StrokeCap.Round
                )
            }
            
            // Message
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = dimensions.fontSizeMedium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Conversion progress indicator with detailed feedback
 */
@Composable
fun ConversionProgressIndicator(
    progress: ConversionProgress,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalDimensions.current
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                progress.isFailed -> MaterialTheme.colorScheme.errorContainer
                progress.isComplete -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(dimensions.radiusMedium),
        elevation = CardDefaults.cardElevation(
            defaultElevation = dimensions.cardElevationMedium
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
        ) {
            // Header with icon and message
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
            ) {
                // Status icon
                when {
                    progress.isFailed -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Failed",
                            modifier = Modifier.size(dimensions.iconSizeMedium),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    progress.isComplete -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Complete",
                            modifier = Modifier.size(dimensions.iconSizeMedium),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    else -> {
                        val infiniteTransition = rememberInfiniteTransition(label = "rotation")
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "rotation"
                        )
                        
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "In Progress",
                            modifier = Modifier
                                .size(dimensions.iconSizeMedium)
                                .rotate(rotation),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Progress message
                Text(
                    text = progress.message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = dimensions.fontSizeMedium
                    ),
                    fontWeight = FontWeight.Medium,
                    color = when {
                        progress.isFailed -> MaterialTheme.colorScheme.onErrorContainer
                        progress.isComplete -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.weight(1f)
                )
                
                // Percentage text
                if (!progress.isFailed) {
                    Text(
                        text = "${progress.percentage}%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = dimensions.fontSizeSmall
                        ),
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            progress.isComplete -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            
            // Progress bar (only if not failed)
            if (!progress.isFailed) {
                LinearProgressIndicator(
                    progress = { progress.progressFloat },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = when {
                        progress.isComplete -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.secondary
                    },
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Simple inline loading indicator
 */
@Composable
fun InlineLoadingIndicator(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    val dimensions = LocalDimensions.current
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(dimensions.iconSizeSmall),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.dp
        )
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = dimensions.fontSizeSmall
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

/**
 * Success feedback component
 */
@Composable
fun SuccessFeedback(
    message: String,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return
    
    val dimensions = LocalDimensions.current
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(dimensions.radiusMedium),
        elevation = CardDefaults.cardElevation(
            defaultElevation = dimensions.cardElevationSmall
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.paddingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                modifier = Modifier.size(dimensions.iconSizeMedium),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = dimensions.fontSizeMedium
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Timeout indicator with countdown
 */
@Composable
fun TimeoutIndicator(
    remainingSeconds: Int,
    totalSeconds: Int,
    message: String = "Please wait",
    modifier: Modifier = Modifier
) {
    val dimensions = LocalDimensions.current
    val progress = (totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(dimensions.radiusMedium)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = dimensions.fontSizeMedium
                    ),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = "${remainingSeconds}s",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = dimensions.fontSizeMedium
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

/**
 * Multi-step progress indicator
 */
@Composable
fun MultiStepProgressIndicator(
    steps: List<String>,
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalDimensions.current
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(dimensions.radiusMedium),
        elevation = CardDefaults.cardElevation(
            defaultElevation = dimensions.cardElevationMedium
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
        ) {
            Text(
                text = "Progress",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = dimensions.fontSizeMedium
                ),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            steps.forEachIndexed { index, step ->
                StepItem(
                    step = step,
                    isCompleted = index < currentStep,
                    isCurrent = index == currentStep,
                    dimensions = dimensions
                )
            }
        }
    }
}

/**
 * Individual step item for multi-step progress
 */
@Composable
private fun StepItem(
    step: String,
    isCompleted: Boolean,
    isCurrent: Boolean,
    dimensions: Dimensions
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
    ) {
        // Step indicator
        Box(
            modifier = Modifier
                .size(dimensions.iconSizeSmall)
                .background(
                    color = when {
                        isCompleted -> MaterialTheme.colorScheme.primary
                        isCurrent -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    },
                    shape = RoundedCornerShape(50)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    modifier = Modifier.size(dimensions.iconSizeSmall * 0.7f),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            } else if (isCurrent) {
                CircularProgressIndicator(
                    modifier = Modifier.size(dimensions.iconSizeSmall * 0.6f),
                    color = MaterialTheme.colorScheme.onSecondary,
                    strokeWidth = 1.5.dp
                )
            }
        }
        
        // Step text
        Text(
            text = step,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = dimensions.fontSizeSmall
            ),
            color = when {
                isCompleted -> MaterialTheme.colorScheme.onSurface
                isCurrent -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            },
            fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal
        )
    }
}