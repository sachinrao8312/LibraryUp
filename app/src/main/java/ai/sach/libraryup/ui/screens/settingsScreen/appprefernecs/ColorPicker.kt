package ai.sach.libraryup.ui.screens.settingsScreen.appprefernecs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Component for displaying and selecting a theme color.
 * 
 * @param currentColor Current selected color hex string
 * @param onColorSelected Callback when a new color is selected
 * @param isDarkTheme Whether the app is in dark mode
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColorPicker(
    currentColor: String,
    onColorSelected: (String) -> Unit,
    isDarkTheme: Boolean
) {
    val categories = SupportedThemeColors.getAvailableCategories()
    val scope = rememberCoroutineScope()
    
    // Find the currently selected color
    val currentColorOption = SupportedThemeColors.findByHexColor(currentColor)
    
    // Find the initial tab index based on the current color's category
    val initialCategoryIndex = categories.indexOf(currentColorOption.colorCategory).coerceAtLeast(0)
    var selectedCategoryIndex by remember { mutableIntStateOf(initialCategoryIndex) }
    
    // Pager state for swiping between color categories
    val pagerState = rememberPagerState(
        initialPage = initialCategoryIndex,
        pageCount = { categories.size }
    )
    
    // Synchronize tab selection with pager
    LaunchedEffect(selectedCategoryIndex) {
        pagerState.animateScrollToPage(selectedCategoryIndex)
    }
    
    LaunchedEffect(pagerState.currentPage) {
        selectedCategoryIndex = pagerState.currentPage
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Text(
                text = "Theme Color",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Current color display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // Color preview
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(currentColor)))
                )
                
                Spacer(modifier = Modifier.padding(8.dp))
                
                // Color name
                Text(
                    text = currentColorOption.name,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Tab row for color categories
            TabRow(
                selectedTabIndex = selectedCategoryIndex,
                indicator = { /* No indicator */ },
                divider = { /* No divider */ }
            ) {
                categories.forEachIndexed { index, category ->
                    Tab(
                        selected = selectedCategoryIndex == index,
                        onClick = {
                            selectedCategoryIndex = index
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Text(
                                text = category.name.lowercase().capitalize(),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    )
                }
            }
            
            // Pager navigation controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous button
                IconButton(
                    onClick = {
                        scope.launch {
                            val targetPage = (pagerState.currentPage - 1)
                                .coerceAtLeast(0)
                            pagerState.animateScrollToPage(targetPage)
                        }
                    },
                    modifier = Modifier.alpha(if (pagerState.currentPage > 0) 1f else 0.5f),
                    enabled = pagerState.currentPage > 0
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous category"
                    )
                }
                
                // Page indicator
                Text(
                    text = "${pagerState.currentPage + 1}/${categories.size}",
                    style = MaterialTheme.typography.bodySmall
                )
                
                // Next button
                IconButton(
                    onClick = {
                        scope.launch {
                            val targetPage = (pagerState.currentPage + 1)
                                .coerceAtMost(categories.size - 1)
                            pagerState.animateScrollToPage(targetPage)
                        }
                    },
                    modifier = Modifier.alpha(if (pagerState.currentPage < categories.size - 1) 1f else 0.5f),
                    enabled = pagerState.currentPage < categories.size - 1
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next category"
                    )
                }
            }
            
            // Color palettes with horizontal pager
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(8.dp)
            ) { page ->
                val categoryColors = SupportedThemeColors.getColorsByCategory(categories[page])
                
                // Grid of colors for this category
                Column {
                    val rows = categoryColors.chunked(5) // Display 5 colors per row
                    
                    rows.forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            rowColors.forEach { colorOption ->
                                val isSelected = colorOption.hexColor == currentColor
                                
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(colorOption.hexColor)))
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isDarkTheme) Color.White else Color.Black,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            onColorSelected(colorOption.hexColor)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = getContrastColor(colorOption.hexColor)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper function to determine a contrasting color for text on a background.
 * Returns white for dark backgrounds and black for light backgrounds.
 */
fun getContrastColor(hexColor: String): Color {
    val color = android.graphics.Color.parseColor(hexColor)
    val luminance = (0.299 * android.graphics.Color.red(color) +
            0.587 * android.graphics.Color.green(color) +
            0.114 * android.graphics.Color.blue(color)) / 255
    
    return if (luminance > 0.5) Color.Black else Color.White
}

/**
 * Extension function to capitalize strings properly.
 */
fun String.capitalize(): String {
    return this.replaceFirstChar { 
        if (it.isLowerCase()) it.titlecase() else it.toString() 
    }
}