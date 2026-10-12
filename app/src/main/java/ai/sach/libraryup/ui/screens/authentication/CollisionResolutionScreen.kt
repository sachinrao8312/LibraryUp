package ai.sach.rentup.ui.screens.authentication

import ai.sach.rentup.ui.common.responsiveness.Dimensions
import ai.sach.rentup.ui.common.responsiveness.LocalDimensions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
 * Comprehensive collision resolution screen
 * Provides detailed analysis and resolution options for account conflicts
 */
@Composable
fun CollisionResolutionScreen(
    analysis: AccountCollisionHandler.CollisionAnalysis,
    strategies: List<AccountCollisionHandler.ResolutionStrategy>,
    onStrategySelected: (AccountCollisionHandler.ResolutionStrategy) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalDimensions.current
    var selectedStrategy by remember { mutableStateOf<AccountCollisionHandler.ResolutionStrategy?>(null) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(dimensions.paddingLarge),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingLarge)
    ) {
        // Header
        CollisionAnalysisHeader(
            analysis = analysis,
            dimensions = dimensions
        )
        
        // Data Impact Assessment
        DataImpactCard(
            dataImpact = analysis.dataImpact,
            riskLevel = analysis.riskLevel,
            dimensions = dimensions
        )
        
        // Resolution Strategies
        Text(
            text = "Resolution Options",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = dimensions.fontSizeLarge
            ),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        strategies.forEach { strategy ->
            ResolutionStrategyCard(
                strategy = strategy,
                isSelected = selectedStrategy == strategy,
                onSelect = { selectedStrategy = strategy },
                dimensions = dimensions
            )
        }
        
        Spacer(modifier = Modifier.height(dimensions.spacingLarge))
        
        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(dimensions.radiusMedium)
            ) {
                Text("Cancel")
            }
            
            Button(
                onClick = {
                    selectedStrategy?.let { onStrategySelected(it) }
                },
                enabled = selectedStrategy != null,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(dimensions.radiusMedium)
            ) {
                Text("Continue")
            }
        }
    }
}

/**
 * Header showing collision analysis summary
 */
@Composable
private fun CollisionAnalysisHeader(
    analysis: AccountCollisionHandler.CollisionAnalysis,
    dimensions: Dimensions
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (analysis.riskLevel) {
                AccountCollisionHandler.RiskLevel.HIGH -> MaterialTheme.colorScheme.errorContainer
                AccountCollisionHandler.RiskLevel.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer
                AccountCollisionHandler.RiskLevel.LOW -> MaterialTheme.colorScheme.primaryContainer
            }
        ),
        shape = RoundedCornerShape(dimensions.radiusLarge)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.paddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
        ) {
            // Risk level icon
            Icon(
                imageVector = when (analysis.riskLevel) {
                    AccountCollisionHandler.RiskLevel.HIGH -> Icons.Default.Error
                    AccountCollisionHandler.RiskLevel.MEDIUM -> Icons.Default.Warning
                    AccountCollisionHandler.RiskLevel.LOW -> Icons.Default.Info
                },
                contentDescription = "Risk Level",
                modifier = Modifier.size(dimensions.iconSizeLarge * 1.5f),
                tint = when (analysis.riskLevel) {
                    AccountCollisionHandler.RiskLevel.HIGH -> MaterialTheme.colorScheme.error
                    AccountCollisionHandler.RiskLevel.MEDIUM -> MaterialTheme.colorScheme.secondary
                    AccountCollisionHandler.RiskLevel.LOW -> MaterialTheme.colorScheme.primary
                }
            )
            
            // Title
            Text(
                text = when (analysis.collisionType) {
                    AuthRepository.ConflictType.BOTH_HAVE_DATA -> "Account Conflict Detected"
                    AuthRepository.ConflictType.GOOGLE_ACCOUNT_EXISTS -> "Existing Account Found"
                    AuthRepository.ConflictType.GUEST_DATA_EXISTS -> "Guest Data Present"
                    null -> "Account Analysis"
                },
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = dimensions.fontSizeXLarge
                ),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = when (analysis.riskLevel) {
                    AccountCollisionHandler.RiskLevel.HIGH -> MaterialTheme.colorScheme.onErrorContainer
                    AccountCollisionHandler.RiskLevel.MEDIUM -> MaterialTheme.colorScheme.onSecondaryContainer
                    AccountCollisionHandler.RiskLevel.LOW -> MaterialTheme.colorScheme.onPrimaryContainer
                }
            )
            
            // Email if available
            if (analysis.googleAccountEmail != null) {
                Text(
                    text = "Google Account: ${analysis.googleAccountEmail}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = dimensions.fontSizeMedium
                    ),
                    color = when (analysis.riskLevel) {
                        AccountCollisionHandler.RiskLevel.HIGH -> MaterialTheme.colorScheme.onErrorContainer
                        AccountCollisionHandler.RiskLevel.MEDIUM -> MaterialTheme.colorScheme.onSecondaryContainer
                        AccountCollisionHandler.RiskLevel.LOW -> MaterialTheme.colorScheme.onPrimaryContainer
                    }.copy(alpha = 0.8f)
                )
            }
            
            // Risk level indicator
            Text(
                text = "Risk Level: ${analysis.riskLevel.name}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = dimensions.fontSizeSmall
                ),
                fontWeight = FontWeight.SemiBold,
                color = when (analysis.riskLevel) {
                    AccountCollisionHandler.RiskLevel.HIGH -> MaterialTheme.colorScheme.onErrorContainer
                    AccountCollisionHandler.RiskLevel.MEDIUM -> MaterialTheme.colorScheme.onSecondaryContainer
                    AccountCollisionHandler.RiskLevel.LOW -> MaterialTheme.colorScheme.onPrimaryContainer
                }.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Card showing data impact assessment
 */
@Composable
private fun DataImpactCard(
    dataImpact: AccountCollisionHandler.DataImpact,
    riskLevel: AccountCollisionHandler.RiskLevel,
    dimensions: Dimensions
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Data Impact",
                    modifier = Modifier.size(dimensions.iconSizeMedium),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Data Impact Assessment",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = dimensions.fontSizeMedium
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = dataImpact.impactDescription,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = dimensions.fontSizeMedium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            
            // Data status indicators
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensions.spacingTiny)
            ) {
                DataStatusIndicator(
                    label = "Guest Data",
                    atRisk = dataImpact.guestDataAtRisk,
                    dimensions = dimensions
                )
                
                DataStatusIndicator(
                    label = "Google Account Data",
                    atRisk = dataImpact.googleDataAtRisk,
                    dimensions = dimensions
                )
                
                if (dataImpact.canMergeData) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingTiny)
                    ) {
                        Text(
                            text = "✓",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Data merging is possible",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = dimensions.fontSizeSmall
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual data status indicator
 */
@Composable
private fun DataStatusIndicator(
    label: String,
    atRisk: Boolean,
    dimensions: Dimensions
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingTiny)
    ) {
        Text(
            text = if (atRisk) "⚠" else "✓",
            color = if (atRisk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$label: ${if (atRisk) "At Risk" else "Safe"}",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = dimensions.fontSizeSmall
            ),
            color = if (atRisk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Resolution strategy selection card
 */
@Composable
private fun ResolutionStrategyCard(
    strategy: AccountCollisionHandler.ResolutionStrategy,
    isSelected: Boolean,
    onSelect: () -> Unit,
    dimensions: Dimensions
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(dimensions.radiusMedium),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) dimensions.cardElevationMedium else dimensions.cardElevationSmall
        ),
        onClick = onSelect
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
        ) {
            // Strategy title
            Text(
                text = getStrategyTitle(strategy.strategy),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = dimensions.fontSizeMedium
                ),
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            
            // Strategy description
            Text(
                text = strategy.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = dimensions.fontSizeMedium
                ),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                }
            )
            
            // Data preservation indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
            ) {
                DataPreservationIndicator(
                    label = "Guest Data",
                    preserved = strategy.preservesGuestData,
                    dimensions = dimensions,
                    isSelected = isSelected
                )
                
                DataPreservationIndicator(
                    label = "Google Data",
                    preserved = strategy.preservesGoogleData,
                    dimensions = dimensions,
                    isSelected = isSelected
                )
            }
        }
    }
}

/**
 * Data preservation indicator
 */
@Composable
private fun DataPreservationIndicator(
    label: String,
    preserved: Boolean,
    dimensions: Dimensions,
    isSelected: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingTiny)
    ) {
        Text(
            text = if (preserved) "✓" else "✗",
            color = if (preserved) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = dimensions.fontSizeSmall
            ),
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            }
        )
    }
}

/**
 * Get user-friendly title for strategy type
 */
private fun getStrategyTitle(strategyType: AccountCollisionHandler.StrategyType): String {
    return when (strategyType) {
        AccountCollisionHandler.StrategyType.AUTO_CONVERT -> "Convert Guest Account"
        AccountCollisionHandler.StrategyType.AUTO_SIGN_IN -> "Use Existing Account"
        AccountCollisionHandler.StrategyType.MERGE_DATA -> "Merge Account Data"
        AccountCollisionHandler.StrategyType.USER_CHOICE -> "Choose Resolution"
        AccountCollisionHandler.StrategyType.CREATE_NEW -> "Create New Account"
    }
}