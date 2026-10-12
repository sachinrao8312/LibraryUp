package ai.sach.libraryup.ui.screens.authentication

import ai.sach.libraryup.R
import ai.sach.libraryup.ui.common.responsiveness.Dimensions
import ai.sach.libraryup.ui.common.responsiveness.LocalDimensions
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Merge
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Dialog shown when a Google account already exists in the system
 * Provides options to sign in with existing account or create new one
 */
@Composable
fun AccountExistsDialog(
    email: String,
    onSignInExisting: () -> Unit,
    onCreateNew: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalDimensions.current
    
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(dimensions.paddingMedium),
            shape = RoundedCornerShape(dimensions.radiusLarge),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = dimensions.cardElevationLarge
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensions.paddingLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
            ) {
                // Icon
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = stringResource(R.string.conflict_account_exists_title),
                    modifier = Modifier.size(dimensions.iconSizeLarge * 1.5f),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                // Title
                Text(
                    text = stringResource(R.string.conflict_account_exists_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = dimensions.fontSizeLarge
                    ),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Message
                Text(
                    text = stringResource(R.string.conflict_account_exists_text, email),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = dimensions.fontSizeMedium
                    ),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(dimensions.spacingMedium))
                
                // Action buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
                ) {
                    // Sign in with existing account (primary)
                    Button(
                        onClick = onSignInExisting,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(dimensions.radiusMedium)
                    ) {
                        Text(
                            text = stringResource(R.string.conflict_sign_in_existing),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    // Create new account (secondary)
                    OutlinedButton(
                        onClick = onCreateNew,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(dimensions.radiusMedium)
                    ) {
                        Text(
                            text = stringResource(R.string.conflict_create_new),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Cancel
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.action_cancel),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialog shown when guest has data and there's a potential conflict
 * Provides options to merge data or create fresh account
 */
@Composable
fun MergeDataDialog(
    conflictType: AuthRepository.ConflictType,
    onMergeData: () -> Unit,
    onCreateFresh: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalDimensions.current
    
    val (title, message, icon) = when (conflictType) {
        AuthRepository.ConflictType.BOTH_HAVE_DATA -> Triple(
            stringResource(R.string.conflict_both_data_title),
            stringResource(R.string.conflict_both_data_text),
            Icons.Default.Merge
        )
        AuthRepository.ConflictType.GUEST_DATA_EXISTS -> Triple(
            stringResource(R.string.conflict_guest_data_title),
            stringResource(R.string.conflict_guest_data_text),
            Icons.Default.Info
        )
        AuthRepository.ConflictType.GOOGLE_ACCOUNT_EXISTS -> Triple(
            stringResource(R.string.conflict_google_exists_title),
            stringResource(R.string.conflict_google_exists_text),
            Icons.Default.AccountCircle
        )
    }
    
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(dimensions.paddingMedium),
            shape = RoundedCornerShape(dimensions.radiusLarge),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = dimensions.cardElevationLarge
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensions.paddingLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
            ) {
                // Icon
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(dimensions.iconSizeLarge * 1.5f),
                    tint = MaterialTheme.colorScheme.secondary
                )
                
                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = dimensions.fontSizeLarge
                    ),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Message
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = dimensions.fontSizeMedium
                    ),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                
                // Benefits of merging
                if (conflictType == AuthRepository.ConflictType.BOTH_HAVE_DATA) {
                    DataMergeBenefits(dimensions = dimensions)
                }
                
                Spacer(modifier = Modifier.height(dimensions.spacingMedium))
                
                // Action buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
                ) {
                    // Merge data (primary)
                    Button(
                        onClick = onMergeData,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(dimensions.radiusMedium)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Merge,
                                contentDescription = null,
                                modifier = Modifier.size(dimensions.iconSizeSmall)
                            )
                            Text(
                                text = stringResource(R.string.conflict_merge_data),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    
                    // Create fresh account (secondary)
                    OutlinedButton(
                        onClick = onCreateFresh,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(dimensions.radiusMedium)
                    ) {
                        Text(
                            text = stringResource(R.string.conflict_start_fresh),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Cancel
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.action_cancel),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialog for creating a completely new account
 */
@Composable
fun CreateNewDialog(
    onCreateNew: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalDimensions.current
    
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(dimensions.paddingMedium),
            shape = RoundedCornerShape(dimensions.radiusLarge),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = dimensions.cardElevationLarge
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensions.paddingLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
            ) {
                // Icon
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = stringResource(R.string.conflict_create_new),
                    modifier = Modifier.size(dimensions.iconSizeLarge * 1.5f),
                    tint = MaterialTheme.colorScheme.tertiary
                )
                
                // Title
                Text(
                    text = stringResource(R.string.conflict_create_new),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = dimensions.fontSizeLarge
                    ),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Message
                Text(
                    text = stringResource(R.string.conflict_create_new_text),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = dimensions.fontSizeMedium
                    ),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(dimensions.spacingMedium))
                
                // Action buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
                ) {
                    // Create new account (primary)
                    Button(
                        onClick = onCreateNew,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ),
                        shape = RoundedCornerShape(dimensions.radiusMedium)
                    ) {
                        Text(
                            text = stringResource(R.string.conflict_create_new_confirm),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    // Cancel (secondary)
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(dimensions.radiusMedium)
                    ) {
                        Text(
                            text = stringResource(R.string.action_cancel),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Unified dialog for handling all types of account collisions
 */
@Composable
fun AccountCollisionDialog(
    collisionType: AuthRepository.ConflictType,
    email: String? = null,
    onMergeData: () -> Unit,
    onCreateNew: () -> Unit,
    onSignInExisting: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (collisionType) {
        AuthRepository.ConflictType.GOOGLE_ACCOUNT_EXISTS -> {
            if (email != null) {
                AccountExistsDialog(
                    email = email,
                    onSignInExisting = onSignInExisting,
                    onCreateNew = onCreateNew,
                    onCancel = onCancel,
                    modifier = modifier
                )
            }
        }
        AuthRepository.ConflictType.GUEST_DATA_EXISTS,
        AuthRepository.ConflictType.BOTH_HAVE_DATA -> {
            MergeDataDialog(
                conflictType = collisionType,
                onMergeData = onMergeData,
                onCreateFresh = onCreateNew,
                onCancel = onCancel,
                modifier = modifier
            )
        }
    }
}

/**
 * Component showing benefits of data merging
 */
@Composable
private fun DataMergeBenefits(
    dimensions: Dimensions
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(dimensions.radiusSmall)
    ) {
        Column(
            modifier = Modifier.padding(dimensions.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingTiny)
        ) {
            Text(
                text = stringResource(R.string.conflict_merge_will),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = dimensions.fontSizeSmall
                ),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            val benefits = listOf(
                stringResource(R.string.conflict_merge_benefit_keep),
                stringResource(R.string.conflict_merge_benefit_google),
                stringResource(R.string.conflict_merge_benefit_backup),
                stringResource(R.string.conflict_merge_benefit_safe)
            )
            
            benefits.forEach { benefit ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimensions.spacingTiny)
                ) {
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = benefit,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = dimensions.fontSizeSmall
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}