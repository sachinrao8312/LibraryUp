package ai.sach.libraryup.ui.screens.settingsScreen.composable

import ai.sach.libraryup.LibraryUpApplication
import ai.sach.libraryup.R
import ai.sach.libraryup.ui.library.LibraryTopBar
import ai.sach.libraryup.ui.library.SwitchRow
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

/** The owner's morning digest on/off, plus a shortcut to the system notification settings. */
@Composable
fun NotificationsScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as LibraryUpApplication
    var enabled by remember { mutableStateOf(app.notificationPrefs.dailyDigestEnabled) }
    val allowed = remember { app.notificationHelper.canPost() }

    Column(Modifier.fillMaxSize()) {
        LibraryTopBar(stringResource(R.string.title_notifications), onBack = { navController.popBackStack() })
        Column(Modifier.padding(16.dp)) {
            SwitchRow(stringResource(R.string.notifications_digest), enabled) { on ->
                enabled = on
                app.notificationPrefs.dailyDigestEnabled = on
                if (on) app.reminderScheduler.ensureScheduled() else app.reminderScheduler.cancel()
            }
            Text(
                stringResource(R.string.notifications_digest_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!allowed) {
                Text(
                    stringResource(R.string.notifications_blocked),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            OutlinedButton(
                onClick = {
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    } else {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))
                    }
                    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                },
                modifier = Modifier.padding(top = 16.dp)
            ) { Text(stringResource(R.string.notifications_system_settings)) }
        }
    }
}
