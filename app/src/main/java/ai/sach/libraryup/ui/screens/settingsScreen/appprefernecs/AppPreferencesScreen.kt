package ai.sach.libraryup.ui.screens.settingsScreen.appprefernecs

import ai.sach.libraryup.LibraryUpApplication
import ai.sach.libraryup.R
import ai.sach.libraryup.ui.library.LibraryTopBar
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@Composable
fun AppPreferencesScreen(navController: NavController) {
    val prefs = (LocalContext.current.applicationContext as LibraryUpApplication).appPreferencesRepository
    val wallpaper by prefs.wallpaperColors.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    Column(Modifier.fillMaxSize()) {
        LibraryTopBar(stringResource(R.string.settings_app_preferences), onBack = { navController.popBackStack() })
        ListItem(
            headlineContent = { Text(stringResource(R.string.prefs_wallpaper_colors)) },
            supportingContent = { Text(stringResource(if (supported) R.string.prefs_wallpaper_colors_desc else R.string.prefs_wallpaper_colors_unsupported)) },
            leadingContent = { Icon(Icons.Default.Palette, contentDescription = null) },
            trailingContent = {
                Switch(checked = wallpaper && supported, enabled = supported, onCheckedChange = { scope.launch { prefs.setWallpaperColors(it) } })
            }
        )
    }
}
