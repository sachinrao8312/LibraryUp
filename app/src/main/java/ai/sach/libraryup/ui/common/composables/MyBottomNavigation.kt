package ai.sach.libraryup.ui.common.composables

import ai.sach.libraryup.ui.navigation.NavigationItems
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavigationBar(navController: NavController) {
    val entry by navController.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    NavigationBar {
        NavigationItems.values().forEach { item ->
            val label = stringResource(item.title)
            NavigationBarItem(
                selected = current == item.route,
                onClick = {
                    if (current != item.route) {
                        navController.navigate(item.navigateTo) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = label) },
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        }
    }
}
