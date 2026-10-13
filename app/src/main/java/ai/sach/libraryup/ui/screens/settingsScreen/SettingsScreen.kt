package ai.sach.libraryup.ui.screens.settingsScreen

import ai.sach.libraryup.R
import ai.sach.libraryup.ui.library.LibraryTopBar
import ai.sach.libraryup.ui.screens.settingsScreen.composable.SettingsNavRoutes
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.time.Year

/** App settings (account, security, notifications, look, help, backup) and sign out. */
@Composable
fun SettingsScreen(navController: NavController, onLogout: () -> Unit = {}) {
    var confirmSignOut by remember { mutableStateOf(false) }
    @Composable
    fun item(icon: ImageVector, title: Int, subtitle: Int, route: String) =
        SettingsItem(icon, stringResource(title), stringResource(subtitle)) { navController.navigate(route) }

    Column(Modifier.fillMaxSize()) {
        LibraryTopBar(stringResource(R.string.settings_title), onBack = { navController.popBackStack() })
        Column(Modifier.verticalScroll(rememberScrollState())) {
            item(Icons.Outlined.Person, R.string.settings_account, R.string.settings_account_desc, SettingsNavRoutes.ACCOUNT)
            item(Icons.Outlined.Security, R.string.settings_security, R.string.settings_security_desc, SettingsNavRoutes.SECURITY)
            item(Icons.Outlined.Notifications, R.string.title_notifications, R.string.settings_notifications_desc, SettingsNavRoutes.NOTIFICATIONS)
            item(Icons.Outlined.Palette, R.string.settings_app_preferences, R.string.settings_app_preferences_desc, SettingsNavRoutes.APP_PREFERENCES)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            item(Icons.Outlined.Backup, R.string.title_backup, R.string.settings_backup_desc, SettingsNavRoutes.BACKUP)
            item(Icons.AutoMirrored.Outlined.Help, R.string.settings_help, R.string.settings_help_desc, SettingsNavRoutes.HELP)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SettingsItem(
                Icons.AutoMirrored.Filled.Logout, stringResource(R.string.action_sign_out), stringResource(R.string.settings_sign_out_desc),
                destructive = true
            ) { confirmSignOut = true }
            Text(
                stringResource(R.string.settings_version, appVersion()) + "\n" + stringResource(R.string.settings_copyright, Year.now().value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp)
            )
        }
    }

    if (confirmSignOut) AlertDialog(
        onDismissRequest = { confirmSignOut = false },
        title = { Text(stringResource(R.string.action_sign_out)) },
        text = { Text(stringResource(R.string.settings_sign_out_confirm)) },
        confirmButton = {
            Button(
                onClick = { confirmSignOut = false; runCatching(onLogout).onFailure { Log.e("SettingsScreen", "Sign out failed", it) } },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text(stringResource(R.string.action_sign_out)) }
        },
        dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun SettingsItem(icon: ImageVector, title: String, subtitle: String, destructive: Boolean = false, onClick: () -> Unit) {
    val accent = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        colors = ListItemDefaults.colors(
            leadingIconColor = accent,
            headlineColor = if (destructive) accent else MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun appVersion(): String {
    val context = LocalContext.current
    return runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
}
