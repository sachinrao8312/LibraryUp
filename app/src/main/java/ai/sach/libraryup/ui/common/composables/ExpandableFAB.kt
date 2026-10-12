package ai.sach.libraryup.ui.common.composables

import ai.sach.libraryup.ui.common.responsiveness.Dimensions
import ai.sach.libraryup.ui.common.responsiveness.rememberDimensions
import ai.sach.libraryup.ui.theme.AppColors
import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Expandable Floating Action Button for the app.
 * When expanded, shows options for adding a tenant or property.
 * Now responsive to different screen sizes and accessibility settings.
 * The main FAB has been increased in size for better visibility and interaction.
 */
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun ExpandableFAB(
    onAddTenant: () -> Unit = {},
    onAddProperty: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    // Get responsive dimensions
    val dimensions = rememberDimensions()

    // Check screen width for responsive positioning
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    // Adjust offset based on screen size
    val horizontalOffset = when {
        screenWidthDp < 360 -> 5.dp
        screenWidthDp < 600 -> 10.dp
        else -> 16.dp
    }

    // Adjust vertical spacing based on screen size
    val verticalSpacing = when {
        screenWidthDp < 360 -> 8.dp
        screenWidthDp < 600 -> 12.dp
        else -> 16.dp
    }

    // Increased FAB size - make it larger than the default buttonHeight
    val fabSize = when {
        screenWidthDp < 360 -> 50.dp  // Small screens
        screenWidthDp < 600 -> 56.dp  // Medium screens
        else -> 68.dp                 // Large screens
    }

    Box(
        modifier = Modifier
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        // FAB Actions that appear when expanded
        AnimatedVisibility(
            visible = expanded,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = horizontalOffset, y = ((-60).dp)),
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.semantics {
                    contentDescription = "Expanded action buttons" // Accessibility description
                }
            ) {
                // Use a reusable function for the expanded FAB items
                ExpandedFABItem(
                    text = "Add Tenant",
                    icon = Icons.Filled.Person,
                    onClick = {
                        onAddTenant()
                        expanded = false
                    },
                    dimensions = dimensions
                )

                Spacer(modifier = Modifier.padding(vertical = verticalSpacing))

                ExpandedFABItem(
                    text = "Add Property",
                    icon = Icons.Filled.Home,
                    onClick = {
                        onAddProperty()
                        expanded = false
                    },
                    dimensions = dimensions
                )
            }
        }

        // Main FAB button with increased size
        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = AppColors.White,
            modifier = Modifier
                .size(fabSize)
                .semantics {
                    contentDescription = if (expanded) "Close actions menu" else "Open actions menu"
                }
        ) {
            Icon(
                imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.Add,
                contentDescription = if (expanded) "Close" else "Add new property/Tenants",
                // Increase icon size proportionally
                modifier = Modifier.size(fabSize * 0.5f)
            )
        }
    }
}

/**
 * Individual items that appear when the FAB is expanded.
 * Now with responsive sizing.
 */
@Composable
private fun ExpandedFABItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    dimensions: Dimensions
) {
    // Get font size from dimensions
    val textSize = dimensions.fontSizeMedium.value.sp

    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = AppColors.White,
        modifier = Modifier.semantics {
            contentDescription = text // Accessibility description
        },
        text = {
            Text(
                text = text,
                fontSize = textSize
            )
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(dimensions.iconSizeSmall)
            )
        }
    )
}