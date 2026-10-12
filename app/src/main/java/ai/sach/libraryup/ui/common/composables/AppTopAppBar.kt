package ai.sach.libraryup.ui.common.composables

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


/**
 * Enhanced TopAppBar for the LibraryUp application.
 * Takes inspiration from WhatsApp's clean interface with a transparent app bar
 * and the app name displayed prominently in the brand color.
 *
 * The cloud icon that used to sit here has been removed. It offered to sync data to
 * an account, but the service behind it does nothing, so it reported success without
 * having backed anything up. Screens that pass [onNavigateToBackup] now show a backup
 * action in its place, which opens Backup & Restore and writes a real file.
 *
 * @param title The title to display in the app bar
 * @param showBackButton Whether to show a back button (for sub-screens)
 * @param onBackPressed Callback when back button is pressed
 * @param actions Custom actions to display in the app bar
 * @param showDefaultMenu Whether to show the default overflow menu (CheeseBallsMenu)
 * @param onNavigateToBackup Opens Backup & Restore. The action is hidden when null.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopAppBar(
    title: String = "LibraryUp",
    showBackButton: Boolean = false,
    onBackPressed: () -> Unit = {},
    actions: @Composable () -> Unit = {},
    showDefaultMenu: Boolean = true,
    onNavigateToBackup: (() -> Unit)? = null,
    showAppActions: Boolean = true
) {
    val isDarkTheme = isSystemInDarkTheme()

    // Determine colors based on theme and whether this is the main screen
    val isMainScreen = !showBackButton && title.equals("LibraryUp", ignoreCase = true)

    // Use system background color instead of a colored app bar
    val backgroundColor = Color.Transparent

    // Icons and text use the system theme colors, except for the main title which uses the brand color
    val iconColor = if (isDarkTheme) Color.White else Color.Black
    val titleColor = if (isMainScreen) MaterialTheme.colorScheme.primary else if (isDarkTheme) Color.White else Color.Black

    TopAppBar(
        title = {
            if(isMainScreen){
                Text(
                    text = title,
                    color = titleColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1
                )
            }
            else{
                Text(
                    text = title,
                    color = titleColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    maxLines = 1
                )
            }

        },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = iconColor
                    )
                }
            }
        },
        actions = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Backup shortcut. Opens the screen that actually writes a file,
                // rather than reporting a sync that never happened.
                if (showAppActions && onNavigateToBackup != null) {
                    IconButton(onClick = onNavigateToBackup) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = "Backup & Restore",
                            tint = iconColor
                        )
                    }
                }

                // Custom actions provided by the caller
                actions()

                // --- Share App Button (moved out of cheeseball menu) ---
                if (showAppActions) {
                    val context = LocalContext.current
                    val appPackageName = context.packageName
                    IconButton(onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Check out this awesome app to manage your properties and tenants: https://play.google.com/store/apps/details?id=$appPackageName"
                            )
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, null))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share this App", tint = iconColor)
                    }
                }
                // --- End Share App Button ---

                // Overflow menu for app options
                if (showAppActions && showDefaultMenu) {
                    CheeseBallsMenu(iconColor)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundColor,
            navigationIconContentColor = iconColor,
            titleContentColor = titleColor,
            actionIconContentColor = iconColor
        ),
        windowInsets = WindowInsets(0.dp)
    )
}

/**
 * Overflow menu (three dots menu) for the app bar.
 * Contains options for sharing the app, viewing more apps from the developer,
 * and contacting support.
 */
@Composable
fun CheeseBallsMenu(iconTint: Color = Color.White) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val appPackageName = context.packageName

    IconButton(onClick = { expanded = !expanded }) {
        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = iconTint)
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        // Removed Share App option from here
        DropdownMenuItem(
            text = { Text("More Apps") },
            onClick = {
                try {
                    // This is a Play Console developer id, so it needs the "dev" path,
                    // not "details" (which is for a single app's package name and 404s here).
                    val uri = Uri.parse("https://play.google.com/store/apps/dev?id=4663935264593570514")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Handle error, e.g., show a toast message
                    println("Error opening Play Store: ${e.message}")
                }
                expanded = false
            },
            leadingIcon = {
                Icon(Icons.Default.Apps, contentDescription = "More Apps")
            }
        )

        DropdownMenuItem(
            text = { Text("Contact Us") },
            onClick = {
                try {
                    val uri = Uri.parse("https://t.me/skiiingg")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Handle error, e.g., show a toast message
                    println("Error opening Telegram: ${e.message}")
                }
                expanded = false
            },
            leadingIcon = {
                Icon(Icons.Default.SupportAgent, contentDescription = "Contact Us")
            }
        )
    }
}
