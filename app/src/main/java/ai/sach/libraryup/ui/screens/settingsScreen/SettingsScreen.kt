package ai.sach.libraryup.ui.screens.settingsScreen

import ai.sach.libraryup.LibraryUpApplication

import ai.sach.libraryup.R
import ai.sach.libraryup.ui.common.composables.AppTopAppBar
import ai.sach.libraryup.ui.screens.settingsScreen.composable.SettingsNavRoutes
import ai.sach.libraryup.ui.screens.settingsScreen.composable.ThemeViewModel
import ai.sach.libraryup.ui.screens.settingsScreen.composable.ThemeViewModelFactory
import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

import kotlinx.coroutines.launch
import java.time.Year

private fun logError(tag: String, message: String, throwable: Throwable? = null) {
    android.util.Log.e(tag, message, throwable)
}

data class SettingsUiState(
    val showLogoutDialog: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Main Settings Screen for the LibraryUp application.
 * Provides access to all app settings including account management, security,
 * notifications, appearance, and more.
 *
 * @param navController Navigation controller for handling screen navigation
 * @param onLogout Callback for logging out of the application
 */
@Composable
fun SettingsScreen(
    navController: NavController,
    onLogout: () -> Unit = {},
    themeViewModel: ThemeViewModel? = null
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("user_prefs", Activity.MODE_PRIVATE)
    val userName = prefs.getString("user_name", "Guest User") ?: "Guest User"
    val userEmail = prefs.getString("user_email", "") ?: ""
    val viewModel = themeViewModel ?: viewModel<ThemeViewModel>(
        factory = ThemeViewModelFactory((context.applicationContext as LibraryUpApplication).themeRepository)
    )
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var uiState by remember { mutableStateOf(SettingsUiState()) }
    

    Scaffold(
        topBar = {
            AppTopAppBar(
                title = stringResource(R.string.settings_title),
                showDefaultMenu = true
                // No backup action here: the Backup & Restore row below goes to the
                // same place, and two ways to the same screen is just clutter.
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
        ) {
            SettingsMenuList(navController)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            AdditionalSettingsMenuList(navController)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            AccountActionsList(
                onLogoutClick = { uiState = uiState.copy(showLogoutDialog = true) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            AppInfoSection()
        }
        if (uiState.showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { uiState = uiState.copy(showLogoutDialog = false) },
                title = { Text(stringResource(R.string.action_sign_out)) },
                text = { Text(stringResource(R.string.settings_sign_out_confirm)) },
                confirmButton = {
                    Button(
                        onClick = {
                            uiState = uiState.copy(showLogoutDialog = false)
                            try {
                                onLogout()
                            } catch (e: Exception) {
                                logError("SettingsScreen", "Error during logout: ${e.message}", e)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.settings_sign_out_error, e.message ?: ""))
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.action_sign_out))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { uiState = uiState.copy(showLogoutDialog = false) }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
        

    }
}



@Composable
fun SettingsMenuList(navController: NavController) {
    SettingsMenuItem(
        icon = Icons.Outlined.Person,
        iconTint = Color(0xFF2196F3),
        title = stringResource(R.string.settings_account),
        subtitle = stringResource(R.string.settings_account_desc),
        onClick = { navController.navigate(SettingsNavRoutes.ACCOUNT) }
    )

    SettingsMenuItem(
        icon = Icons.Outlined.Security,
        iconTint = Color(0xFF4CAF50),
        title = stringResource(R.string.settings_security),
        subtitle = stringResource(R.string.settings_security_desc),
        onClick = { navController.navigate(SettingsNavRoutes.SECURITY) }
    )

    SettingsMenuItem(
        icon = Icons.Outlined.Notifications,
        iconTint = Color(0xFFFFC107),
        title = stringResource(R.string.title_notifications),
        subtitle = stringResource(R.string.settings_notifications_desc),
        onClick = { navController.navigate(SettingsNavRoutes.NOTIFICATIONS) }
    )
}


@Composable
fun AdditionalSettingsMenuList(navController: NavController) {
    SettingsMenuItem(
        icon = Icons.Outlined.Palette,
        iconTint = Color(0xFF673AB7),
        title = stringResource(R.string.settings_app_preferences),
        subtitle = stringResource(R.string.settings_app_preferences_desc),
        onClick = { navController.navigate(SettingsNavRoutes.APP_PREFERENCES) }
    )

    SettingsMenuItem(
        icon = Icons.AutoMirrored.Outlined.Help,
        iconTint = Color(0xFF4CAF50),
        title = stringResource(R.string.settings_help),
        subtitle = stringResource(R.string.settings_help_desc),
        onClick = { navController.navigate(SettingsNavRoutes.HELP) }
    )

    SettingsMenuItem(
        icon = Icons.Outlined.Backup,
        iconTint = Color(0xFF9C27B0),
        title = stringResource(R.string.title_backup),
        subtitle = stringResource(R.string.settings_backup_desc),
        onClick = { navController.navigate(SettingsNavRoutes.BACKUP) }
    )
}



@Composable
fun AccountActionsList(onLogoutClick: () -> Unit) {
    SettingsMenuItem(
        icon = Icons.AutoMirrored.Filled.Logout,
        iconTint = MaterialTheme.colorScheme.error,
        title = stringResource(R.string.action_sign_out),
        subtitle = stringResource(R.string.settings_sign_out_desc),
        onClick = onLogoutClick,
        isDestructive = true
    )
}

@Composable
fun SettingsMenuItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = iconTint.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                color = if (isDestructive) MaterialTheme.colorScheme.error.copy(alpha = 0.7f) else Color.Gray,
                fontSize = 14.sp
            )
        }

        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = stringResource(R.string.cd_open),
            tint = if (isDestructive) MaterialTheme.colorScheme.error.copy(alpha = 0.7f) else Color.Gray
        )
    }
}

/**
 * Retrieves the version name of the application dynamically.
 *
 * @param context The Android context used to access package information
 * @return A string representing the app's version name
 */
@Composable
fun getVersionName(): String? {
    val context = LocalContext.current
    return try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: Exception) {
        "1.0.0"
    }
}

@Composable
fun AppInfoSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.settings_version, getVersionName().orEmpty()),
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = Color.Gray
        )

        Text(
            text = stringResource(R.string.settings_copyright, Year.now().value),
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}