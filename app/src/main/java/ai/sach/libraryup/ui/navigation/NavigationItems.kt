package ai.sach.rentup.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector


sealed class NavigationItems (
    val route: String,
    val title: String,
    val icon: ImageVector
)
{
    object Home: NavigationItems(
        route = AppRoutes.HOME,
        title = "Home",
        icon = Icons.Default.Home
    )

    object Properties : NavigationItems(
        route = AppRoutes.PROPERTIES,
        title = "Properties",
        icon = Icons.Default.LocationCity
    )

    object PaymentHistory : NavigationItems(
        route = AppRoutes.PAYMENT_HISTORY,
        title = "History",
        icon = Icons.Default.History
    )

    object Settings : NavigationItems(
        route = AppRoutes.SETTINGS,
        title = "Settings",
        icon = Icons.Default.Settings
    )
}
