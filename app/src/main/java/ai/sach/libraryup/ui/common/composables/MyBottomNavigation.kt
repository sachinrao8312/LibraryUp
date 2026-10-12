package ai.sach.libraryup.ui.common.composables

import ai.sach.libraryup.ui.common.responsiveness.LocalAccessibilityScaling
import ai.sach.libraryup.ui.navigation.NavigationItems
import ai.sach.libraryup.ui.screens.settingsScreen.appprefernecs.AppPreferencesRepository
import ai.sach.libraryup.ui.theme.AppColors
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavigationBar(navController: NavController) {
    // Get current route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Get font scaling
    val fontScaling = LocalAccessibilityScaling.current

    // Control icon and label size based on font scaling
    val iconSize = 24.dp * fontScaling.contentScale.coerceAtMost(1.2f)
    val fontSize = 12.sp

    // Always use a simplified version when font scaling is large
    val useSimplifiedNav = fontScaling.fontScale > 1.15f

    // Get app preferences to determine if Properties screen should be shown
    val context = LocalContext.current
    val appPreferencesRepository = AppPreferencesRepository(context)
    val showProperties by appPreferencesRepository.showProperties
        .collectAsState(initial = AppPreferencesRepository.DEFAULT_SHOW_PROPERTIES)

    // Get primary color for active items
    val primaryColorHex by appPreferencesRepository.primaryColor
        .collectAsState(initial = AppPreferencesRepository.DEFAULT_PRIMARY_COLOR)
    val primaryColor = try {
        Color(android.graphics.Color.parseColor(primaryColorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        // Define each navigation item manually instead of using enum entries
        // This ensures we have full control over each item

        // Home item
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = if (currentRoute == NavigationItems.Home.route)
                        primaryColor else Color.Gray,
                    modifier = Modifier.size(iconSize)
                )
            },
            label = if (!useSimplifiedNav) {
                {
                    Text(
                        "Home",
                        fontSize = fontSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else null,
            selected = currentRoute == NavigationItems.Home.route,
            onClick = {
                if (currentRoute != NavigationItems.Home.route) {
                    navController.navigate(NavigationItems.Home.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )

        // History item
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Payment History",
                    tint = if (currentRoute == NavigationItems.PaymentHistory.route)
                        primaryColor else Color.Gray,
                    modifier = Modifier.size(iconSize)
                )
            },
            label = if (!useSimplifiedNav) {
                {
                    Text(
                        "History",
                        fontSize = fontSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else null,
            selected = currentRoute == NavigationItems.PaymentHistory.route,
            onClick = {
                if (currentRoute != NavigationItems.PaymentHistory.route) {
                    navController.navigate(NavigationItems.PaymentHistory.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )

        // Properties item - only show if enabled in preferences
        if (showProperties) {
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Apartment,
                        contentDescription = "Properties",
                        tint = if (currentRoute == NavigationItems.Properties.route)
                            primaryColor else Color.Gray,
                        modifier = Modifier.size(iconSize)
                    )
                },
                label = if (!useSimplifiedNav) {
                    {
                        Text(
                            "Properties",
                            fontSize = fontSize,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else null,
                selected = currentRoute == NavigationItems.Properties.route,
                onClick = {
                    if (currentRoute != NavigationItems.Properties.route) {
                        navController.navigate(NavigationItems.Properties.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }

        // Settings item
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = if (currentRoute == NavigationItems.Settings.route)
                        primaryColor else Color.Gray,
                    modifier = Modifier.size(iconSize)
                )
            },
            label = if (!useSimplifiedNav) {
                {
                    Text(
                        "Settings",
                        fontSize = fontSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else null,
            selected = currentRoute == NavigationItems.Settings.route,
            onClick = {
                if (currentRoute != NavigationItems.Settings.route) {
                    navController.navigate(NavigationItems.Settings.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )
    }
}