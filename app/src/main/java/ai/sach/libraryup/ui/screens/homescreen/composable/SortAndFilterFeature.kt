package ai.sach.libraryup.ui.screens.homescreen.composable


import ai.sach.libraryup.ui.common.responsiveness.rememberDimensions
import ai.sach.libraryup.ui.screens.homescreen.TenantFilterOption
import ai.sach.libraryup.ui.screens.homescreen.TenantSortOption
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun FilterChips(
    selectedFilter: TenantFilterOption,
    onFilterSelected: (TenantFilterOption) -> Unit,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    // Get responsive dimensions
    val dimensions = rememberDimensions()

    // Check screen width for responsive layout
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    // Adjust font size based on screen width
    val fontSize = when {
        screenWidthDp < 360 -> dimensions.fontSizeSmall * 0.95f
        else -> dimensions.fontSizeSmall
    }

    // Chip height based on screen size
    val chipHeight = when {
        screenWidthDp < 360 -> 32.dp
        else -> 36.dp
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensions.spacingSmall),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TenantFilterOption.entries.forEach { option ->
            FilterChip(
                selected = selectedFilter == option,
                onClick = { onFilterSelected(option) },
                label = {
                    // Add semantics for accessibility
                    Text(
                        text = option.displayName,
                        fontSize = fontSize,
                        maxLines = 1,
                        color = if (selectedFilter == option) {
                            // Selected text color is white for both themes
                            Color.White
                        } else {
                            // Unselected text color should be visible in both themes
                            if (isDarkTheme) Color.White else Color.Black.copy(alpha = 0.7f)
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "${option.displayName} filter ${if (selectedFilter == option) "selected" else ""}"
                        }
                    )
                },
                modifier = Modifier.height(chipHeight),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White,
                    containerColor = if (isDarkTheme) Color(0xFF2A2A2A) else Color(0xFFE8E8E8)
                ),
                // Make sure the chip shape is consistent with design system
                shape = RoundedCornerShape(dimensions.radiusSmall)
            )
        }
    }
}

@Composable
fun SortButton(
    selectedSort: TenantSortOption,
    onSortSelected: (TenantSortOption) -> Unit,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { showSortMenu = true }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = "Sort tenants",
                tint = Color.White
            )
        }

        DropdownMenu(
            expanded = showSortMenu,
            onDismissRequest = { showSortMenu = false },
            modifier = Modifier.background(if (isDarkTheme) Color(0xFF2A2A2A) else Color.White)
        ) {
            TenantSortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.displayName,
                            color = if (isDarkTheme) Color.White else Color.Black
                        )
                    },
                    leadingIcon = {
                        if (selectedSort == option) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    onClick = {
                        onSortSelected(option)
                        showSortMenu = false
                    }
                )
            }
        }
    }
}