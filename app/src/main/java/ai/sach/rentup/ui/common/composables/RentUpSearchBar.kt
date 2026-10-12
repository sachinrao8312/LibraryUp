package ai.sach.rentup.ui.common.composables

import ai.sach.rentup.data.local.entity.TenantWithMultipleProperties
import ai.sach.rentup.data.local.entity.Property
import ai.sach.rentup.ui.screens.historyScreen.PaymentHistoryItem
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Modern, responsive search bar component for the RentUp application.
 * Fixes height issues while adapting to different screen sizes and orientations.
 *
 * @param query Current search query text
 * @param onQueryChange Callback when query text changes
 * @param placeholder Placeholder text when search bar is empty
 * @param modifier Modifier for customizing layout
 * @param onSearch Callback when search is triggered via keyboard
 * @param showRecentSearches Whether to show recent searches dropdown
 * @param recentSearches List of recent searches to display
 * @param onRecentSearchSelected Callback when a recent search is selected
 * @param isDarkTheme Flag indicating if dark theme is active
 */
@Composable
fun RentUpSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search...",
    modifier: Modifier = Modifier,
    onSearch: (String) -> Unit = {},
    showRecentSearches: Boolean = false,
    recentSearches: List<String> = emptyList(),
    onRecentSearchSelected: (String) -> Unit = {},
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    // Focus and keyboard controllers
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Get window size to adapt styling
    val windowSizeClass = rememberWindowSizeClass()

    // Determine search bar size based on screen width
    val searchBarHeight = when {
        windowSizeClass.isExpandedWidth -> 60.dp
        windowSizeClass.isMediumWidth -> 56.dp
        else -> 52.dp
    }

    // Adjust padding based on screen size
    val horizontalPadding = when {
        windowSizeClass.isExpandedWidth -> 16.dp
        windowSizeClass.isMediumWidth -> 14.dp
        else -> 12.dp
    }

    // Colors adapted to theme
    val backgroundColor = if (isDarkTheme) {
        Color(0xFF252525) // Darker background for dark theme
    } else {
        Color(0xFFF5F5F5) // Light gray for light theme
    }

    val textColor = if (isDarkTheme) Color.White else Color.Black
    val placeholderColor = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.6f)
    val iconColor = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Gray
    val borderColor = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.5f)

    // Determine if we need an elevation based on theme
    val elevation = if (isDarkTheme) 0.dp else 1.dp

    // Font size based on screen size
    val fontSize = when {
        windowSizeClass.isExpandedWidth -> MaterialTheme.typography.bodyLarge.fontSize
        else -> MaterialTheme.typography.bodyMedium.fontSize
    }

    // Icon sizes based on screen size
    val searchIconSize = when {
        windowSizeClass.isExpandedWidth -> 22.dp
        windowSizeClass.isMediumWidth -> 20.dp
        else -> 18.dp
    }

    Column(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(searchBarHeight),
            color = backgroundColor,
            shape = RoundedCornerShape(16.dp),
            border = if (isDarkTheme) BorderStroke(1.dp, Color(0xFF444444)) else BorderStroke(1.dp, borderColor),
            shadowElevation = elevation
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = LocalTextStyle.current.copy(
                    color = textColor,
                    fontSize = fontSize
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester),
                singleLine = true,
                cursorBrush = SolidColor(if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onSearch(query)
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                ),
                decorationBox = { innerTextField ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalPadding, vertical = 0.dp)
                    ) {
                        // Search icon in circle background
                        Box(
                            modifier = Modifier
                                .size(searchBarHeight * 0.6f)
                                .background(
                                    color = if (isDarkTheme) Color(0xFF374151) else Color(0xFFE8E8E8),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = iconColor,
                                modifier = Modifier.size(searchIconSize)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            // Show placeholder if query is empty
                            if (query.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = LocalTextStyle.current.copy(
                                        color = placeholderColor,
                                        fontSize = fontSize
                                    )
                                )
                            }

                            // Text field
                            innerTextField()
                        }

                        // Clear button with animation when query is not empty
                        AnimatedVisibility(
                            visible = query.isNotEmpty(),
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            IconButton(
                                onClick = {
                                    onQueryChange("")
                                    focusRequester.requestFocus()
                                },
                                modifier = Modifier.size(searchBarHeight * 0.8f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = iconColor,
                                    modifier = Modifier.size(searchIconSize * 0.9f)
                                )
                            }
                        }
                    }
                }
            )
        }

        // Recent searches dropdown
        if (showRecentSearches && query.isEmpty() && recentSearches.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = backgroundColor,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Recent Searches",
                        fontWeight = FontWeight.Medium,
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color.DarkGray,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    recentSearches.take(5).forEach { searchText ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .padding(start = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = searchText,
                                color = textColor,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Utility class to determine window size class for responsive UI
 */
class WindowSizeClass(private val windowWidth: Dp) {
    val isCompactWidth: Boolean get() = windowWidth < 600.dp
    val isMediumWidth: Boolean get() = windowWidth >= 600.dp && windowWidth < 840.dp
    val isExpandedWidth: Boolean get() = windowWidth >= 840.dp
}

/**
 * Remember window size class based on the current window
 */
@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val configuration = LocalConfiguration.current
    val windowWidth = with(LocalDensity.current) {
        configuration.screenWidthDp.dp
    }
    return remember(windowWidth) {
        WindowSizeClass(windowWidth)
    }
}

/**
 * Extension function to filter tenant data based on search query.
 *
 * @param query Current search query
 * @param tenants List of tenants to filter
 * @return Filtered list of tenants
 */
fun filterTenants(query: String, tenants: List<TenantWithMultipleProperties>): List<TenantWithMultipleProperties> {
    if (query.isBlank()) return tenants

    val lowercaseQuery = query.lowercase().trim()
    return tenants.filter { tenantWithMultipleProperties ->
        val tenant = tenantWithMultipleProperties.tenant
        val properties = tenantWithMultipleProperties.properties
        val propertyNames = properties.map { it.name }.joinToString(" ")

        tenant.name.lowercase().contains(lowercaseQuery) ||
                tenant.mobileNumber.lowercase().contains(lowercaseQuery) ||
                propertyNames.lowercase().contains(lowercaseQuery) ||
                tenant.floor.lowercase().contains(lowercaseQuery) ||
//                (tenant.email?.lowercase()?.contains(lowercaseQuery) ?: false) ||
//                tenant.flatNo?.lowercase()?.contains(lowercaseQuery) ?: false ||
                tenant.amount?.lowercase()?.contains(lowercaseQuery) ?: false ||
                tenant.dueDate.lowercase().contains(lowercaseQuery)
    }
}

/**
 * Extension function to filter property data based on search query.
 *
 * @param query Current search query
 * @param properties List of properties to filter
 * @return Filtered list of properties
 */
fun filterProperties(query: String, properties: List<Property>): List<Property> {
    if (query.isBlank()) return properties

    val lowercaseQuery = query.lowercase().trim()
    return properties.filter {
        it.name.lowercase().contains(lowercaseQuery) ||
                it.address.lowercase().contains(lowercaseQuery) ||
                it.type?.lowercase()?.contains(lowercaseQuery) ?: false ||
                it.description?.lowercase()?.contains(lowercaseQuery) ?: false ||
                it.totalUnits.toString().contains(lowercaseQuery) ||
                it.floors.toString().contains(lowercaseQuery)
    }
}

/**
 * Extension function to filter payment history data based on search query.
 *
 * @param query Current search query
 * @param payments List of payment history items to filter
 * @return Filtered list of payment history items
 */
fun filterPaymentHistory(query: String, payments: List<PaymentHistoryItem>): List<PaymentHistoryItem> {
    if (query.isBlank()) return payments

    val lowercaseQuery = query.lowercase().trim()
    return payments.filter {
        it.tenantName.lowercase().contains(lowercaseQuery) ||
                it.propertyName.lowercase().contains(lowercaseQuery) ||
                it.date.contains(lowercaseQuery) ||
                it.amount.lowercase().contains(lowercaseQuery) ||
                it.collectedBy.lowercase().contains(lowercaseQuery) ||
                it.remark?.lowercase()?.contains(lowercaseQuery) ?: false
    }
}

/**
 * A composable that demonstrates how to use the RentUpSearchBar with a debounce
 * effect to avoid excessive data filtering during rapid typing.
 */
@Composable
fun RentUpSearchBarWithDebounce(
    query: String,
    onQueryChange: (String) -> Unit,
    onDebouncedQueryChange: (String) -> Unit,
    debounceTime: Long = 300L, // Debounce time in milliseconds
    placeholder: String = "Search...",
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    // Keep track of the latest query for debouncing
    var debouncedQuery by remember { mutableStateOf(query) }

    // Update debounced query after a delay
    LaunchedEffect(query) {
        if (query != debouncedQuery) {
            delay(debounceTime)
            debouncedQuery = query
            onDebouncedQueryChange(query)
        }
    }

    RentUpSearchBar(
        query = query,
        onQueryChange = onQueryChange,
        placeholder = placeholder,
        modifier = modifier,
        isDarkTheme = isDarkTheme
    )
}