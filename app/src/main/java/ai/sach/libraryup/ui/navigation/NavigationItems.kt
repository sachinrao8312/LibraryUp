package ai.sach.libraryup.ui.navigation

import ai.sach.libraryup.R
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.ui.graphics.vector.ImageVector

/** Bottom navigation: Home · Seats · Students · Payments · More. */
enum class NavigationItems(val route: String, val navigateTo: String, @StringRes val title: Int, val icon: ImageVector) {
    Home(AppRoutes.HOME, AppRoutes.HOME, R.string.nav_home, Icons.Default.Home),
    Seats(AppRoutes.SEATS, AppRoutes.SEATS, R.string.nav_seats, Icons.Default.EventSeat),
    Students(AppRoutes.STUDENTS, AppRoutes.students(), R.string.nav_students, Icons.Default.People),
    Payments(AppRoutes.PAYMENTS, AppRoutes.PAYMENTS, R.string.nav_payments, Icons.Default.CurrencyRupee),
    More(AppRoutes.MORE, AppRoutes.MORE, R.string.nav_more, Icons.Default.MoreHoriz);

    companion object {
        val routes: Set<String> = values().map { it.route }.toSet()
    }
}
