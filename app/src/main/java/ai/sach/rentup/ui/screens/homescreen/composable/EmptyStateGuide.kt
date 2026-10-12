package ai.sach.rentup.ui.screens.homescreen.composable


import ai.sach.rentup.ui.theme.RentUpColors
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Empty state guide that helps new users understand how to get started
 * Shows different guidance based on whether properties exist
 */
@Composable
 fun EmptyStateGuide(
    hasProperties: Boolean,
    onAddPropertyClick: () -> Unit,
    onAddTenantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = RentUpColors.PrimaryGreen.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon based on state
            val (icon, iconColor) = if (hasProperties) {
                Icons.Default.PersonAdd to RentUpColors.PrimaryGreen
            } else {
                Icons.Default.TipsAndUpdates to Color(0xFFFF9800)
            }
            
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = iconColor
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Title based on state
            Text(
                text = if (hasProperties) "Add Your First Tenant" else "Welcome to RentUp!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Description based on state
            Text(
                text = if (hasProperties) {
                    "You have properties set up. Now add your first tenant to start managing rent payments."
                } else {
                    "Let's get you started with managing your rental properties and tenants." +
                            "Follow these steps"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (!hasProperties) {
                // Step-by-step guide for new users
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StepItem(
                        stepNumber = 1,
                        title = "Add Property",
                        description = "Create your first rental property",
                        icon = Icons.Default.Apartment
                    )
                    
                    StepItem(
                        stepNumber = 2,
                        title = "Add Tenants",
                        description = "Add tenants to your property",
                        icon = Icons.Default.PersonAdd
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Action buttons
            if (hasProperties) {
                Button(
                    onClick = onAddTenantClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RentUpColors.PrimaryGreen
                    )
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add First Tenant")
                }
            } else {
                Button(
                    onClick = onAddPropertyClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RentUpColors.PrimaryGreen
                    )
                ) {
                    Icon(Icons.Default.Apartment, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create First Property")
                }
            }
        }
    }
}

/**
 * Individual step item for the getting started guide
 */
@Composable
private fun StepItem(
    stepNumber: Int,
    title: String,
    description: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Step number circle
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = RentUpColors.PrimaryGreen,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Icon
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = RentUpColors.PrimaryGreen,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Text content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}