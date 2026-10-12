package ai.sach.rentup.ui.screens.settingsScreen.composable

import ai.sach.rentup.BuildConfig
import ai.sach.rentup.RentUpApplication
import ai.sach.rentup.data.backup.BackupManager
import ai.sach.rentup.data.backup.BackupSummary
import ai.sach.rentup.data.backup.drive.DriveAccess
import ai.sach.rentup.data.backup.drive.DriveBackupStatus
import ai.sach.rentup.data.backup.drive.DriveFile
import ai.sach.rentup.data.backup.drive.NothingToBackUpException
import ai.sach.rentup.ui.common.composables.RentUpTopAppBar
import android.app.Activity
import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** What to do once Google Drive access is granted - survives the trip to the consent screen. */
private enum class DriveAction { TURN_ON, BACK_UP_NOW, RESTORE }

private class DriveRestoreChoice(val token: String, val backups: List<DriveFile>)

private class PendingDriveRestore(val token: String, val backup: DriveFile)

/**
 * Backup and Restore screen for local data.
 *
 * Two ways to keep a copy of the local database, both producing the same `.zip`:
 * - a file the user saves anywhere via the system "Save as" picker, restored the same way;
 * - automatic daily backups to the user's own Google Drive, restorable from any device
 *   signed in to that Google account.
 *
 * Either restore fully replaces what's on this device. See [BackupManager] for exactly
 * what is and isn't included.
 *
 * @param navController Navigation controller for handling screen navigation
 */
@Composable
fun BackupRestoreScreen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as RentUpApplication
    val drive = application.driveBackupManager
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var isWorking by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val driveStatus by drive.status.collectAsState()
    var pendingDriveAction by rememberSaveable { mutableStateOf<DriveAction?>(null) }
    var driveRestoreChoice by remember { mutableStateOf<DriveRestoreChoice?>(null) }
    var pendingDriveRestore by remember { mutableStateOf<PendingDriveRestore?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(BackupManager.MIME_TYPE_ZIP)
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isWorking = true
        scope.launch {
            val result = application.backupManager.exportTo(uri)
            isWorking = false
            result.fold(
                onSuccess = { summary -> snackbar.showSnackbar(backedUpMessage(summary)) },
                onFailure = { error ->
                    snackbar.showSnackbar("Backup failed: ${error.message ?: "unknown error"}")
                }
            )
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(BackupManager.MIME_TYPE_CSV)
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isWorking = true
        scope.launch {
            val result = application.backupManager.exportTenantsCsv(uri)
            isWorking = false
            result.fold(
                onSuccess = { count -> snackbar.showSnackbar("Exported $count tenants to CSV") },
                onFailure = { error -> snackbar.showSnackbar("CSV export failed: ${error.message ?: "unknown error"}") }
            )
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        // Confirm before touching anything - restore replaces the whole local database.
        if (uri != null) pendingImportUri = uri
    }

    // ---------------------------------------------------------------- Google Drive

    suspend fun performDriveAction(action: DriveAction, token: String) {
        isWorking = true
        val message: String? = try {
            when (action) {
                DriveAction.TURN_ON -> {
                    drive.enable()
                    val summary = drive.backUp(token)
                    "Google Drive backup is on. ${backedUpMessage(summary)}"
                }
                DriveAction.BACK_UP_NOW -> {
                    val summary = drive.backUp(token)
                    "${backedUpMessage(summary)} to Google Drive"
                }
                DriveAction.RESTORE -> {
                    val backups = drive.listBackups(token)
                    if (backups.isEmpty()) {
                        "No RentUp backups found in this Google Drive."
                    } else {
                        driveRestoreChoice = DriveRestoreChoice(token, backups)
                        null
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: NothingToBackUpException) {
            if (action == DriveAction.TURN_ON) {
                "Google Drive backup is on. The first backup runs once you've added a property or tenant."
            } else {
                e.message
            }
        } catch (e: Exception) {
            "Google Drive: ${e.message ?: "something went wrong"}"
        } finally {
            isWorking = false
        }
        message?.let { snackbar.showSnackbar(it) }
    }

    val driveConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val action = pendingDriveAction ?: return@rememberLauncherForActivityResult
        pendingDriveAction = null
        scope.launch {
            val data = result.data
            val access = if (result.resultCode == Activity.RESULT_OK && data != null) {
                runCatching { drive.accessFromConsentResult(data) }.getOrNull()
            } else {
                null
            }
            if (access is DriveAccess.Granted) {
                performDriveAction(action, access.token)
            } else {
                snackbar.showSnackbar("Google Drive access wasn't granted.")
            }
        }
    }

    fun startDriveAction(action: DriveAction) {
        scope.launch {
            isWorking = true
            val access = try {
                drive.requestAccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                isWorking = false
                snackbar.showSnackbar("Couldn't connect to Google Drive: ${e.message ?: "unknown error"}")
                return@launch
            }
            when (access) {
                is DriveAccess.Granted -> performDriveAction(action, access.token)
                is DriveAccess.NeedsConsent -> {
                    isWorking = false
                    pendingDriveAction = action
                    driveConsentLauncher.launch(IntentSenderRequest.Builder(access.pendingIntent).build())
                }
            }
        }
    }

    // ---------------------------------------------------------------- dialogs

    if (pendingImportUri != null) {
        ReplaceAllDataDialog(
            source = "this backup",
            onDismiss = { pendingImportUri = null },
            onConfirm = {
                val uri = pendingImportUri!!
                pendingImportUri = null
                isWorking = true
                scope.launch {
                    val result = application.backupManager.importFrom(uri)
                    isWorking = false
                    result.fold(
                        onSuccess = { summary -> snackbar.showSnackbar(restoredMessage(summary)) },
                        onFailure = { error ->
                            snackbar.showSnackbar("Restore failed: ${error.message ?: "unknown error"}")
                        }
                    )
                }
            }
        )
    }

    driveRestoreChoice?.let { choice ->
        AlertDialog(
            onDismissRequest = { driveRestoreChoice = null },
            title = { Text("Restore from Google Drive") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "Choose a backup:",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    choice.backups.forEach { backup ->
                        TextButton(
                            onClick = {
                                driveRestoreChoice = null
                                pendingDriveRestore = PendingDriveRestore(choice.token, backup)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(backupLabel(backup), fontWeight = FontWeight.Medium)
                                backup.size?.let { bytes ->
                                    Text(
                                        Formatter.formatShortFileSize(context, bytes),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { driveRestoreChoice = null }) { Text("Cancel") }
            }
        )
    }

    pendingDriveRestore?.let { pending ->
        ReplaceAllDataDialog(
            source = "the backup from ${backupLabel(pending.backup)}",
            onDismiss = { pendingDriveRestore = null },
            onConfirm = {
                pendingDriveRestore = null
                isWorking = true
                scope.launch {
                    val message = try {
                        restoredMessage(drive.restore(pending.token, pending.backup))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        "Restore failed: ${e.message ?: "unknown error"}"
                    } finally {
                        isWorking = false
                    }
                    snackbar.showSnackbar(message)
                }
            }
        )
    }

    // ---------------------------------------------------------------- layout

    Scaffold(
        topBar = {
            RentUpTopAppBar(
                title = "Backup & Restore",
                showBackButton = true,
                onBackPressed = { navController.popBackStack() },
                showDefaultMenu = true,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // Main Backup Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = "Backup",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Your data stays on this device",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "RentUp doesn't upload your tenant records. Save a backup file " +
                            "yourself before changing phones or reinstalling, or turn on " +
                            "Google Drive backup below.",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isWorking) {
                        CircularProgressIndicator()
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                // Not filtered by MIME type: a .zip arrives as
                                // application/zip, x-zip-compressed or octet-stream
                                // depending on which provider hands it over, and
                                // filtering hides the user's own backup from the
                                // picker. The file is validated when it is opened.
                                onClick = { importLauncher.launch(arrayOf("*/*")) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Restore")
                            }
                            Button(
                                onClick = { exportLauncher.launch(BackupManager.suggestedFileName()) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Backup")
                            }
                        }

                        // A lighter-weight secondary action: a flat spreadsheet of the
                        // tenant roster, not a restorable backup - see exportTenantsCsv.
                        TextButton(
                            onClick = { csvExportLauncher.launch(BackupManager.suggestedCsvFileName()) },
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export tenant list as CSV")
                        }
                    }

                    if (BuildConfig.DEBUG) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = {
                            scope.launch {
                                val count = application.seedMockData()
                                snackbar.showSnackbar(if (count == 0) "Demo data already exists" else "Added $count demo tenants")
                            }
                        }) { Text("Seed demo data") }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            DriveBackupCard(
                status = driveStatus,
                isWorking = isWorking,
                onTurnOn = { startDriveAction(DriveAction.TURN_ON) },
                onBackUpNow = { startDriveAction(DriveAction.BACK_UP_NOW) },
                onRestore = { startDriveAction(DriveAction.RESTORE) },
                onTurnOff = { drive.disable() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "What gets backed up?",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• All your properties and tenants\n" +
                            "• Family members and payment history\n" +
                            "• Tenant photos and uploaded documents",
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Android's own device backup also saves your records to your Google " +
                            "account, but not tenant photos or documents. A backup file or Google " +
                            "Drive backup keeps those too.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Need a spreadsheet instead? \"Export tenant list as CSV\" above gives " +
                            "you a flat list of tenants to open in Excel or Sheets - it's not a full " +
                            "backup, and can't be restored back into the app.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DriveBackupCard(
    status: DriveBackupStatus,
    isWorking: Boolean,
    onTurnOn: () -> Unit,
    onBackUpNow: () -> Unit,
    onRestore: () -> Unit,
    onTurnOff: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Google Drive backup",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val (statusText, isProblem) = when {
                !status.enabled ->
                    "Back up automatically every day on Wi-Fi to a \"RentUp Backups\" folder in " +
                        "your own Google Drive - photos and documents included. RentUp can only " +
                        "see the files it creates there." to false
                status.needsReconnect ->
                    "Google Drive access was removed. Tap \"Back up now\" to reconnect." to true
                status.lastError != null ->
                    "Last backup failed: ${status.lastError}" to true
                status.lastBackupAt != null ->
                    "On - last backup ${formatInstant(Instant.ofEpochMilli(status.lastBackupAt))}. " +
                        "Backs up daily on Wi-Fi." to false
                else -> "On - backs up daily on Wi-Fi." to false
            }
            Text(
                text = statusText,
                fontSize = 14.sp,
                color = if (isProblem) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onRestore,
                    enabled = !isWorking,
                    modifier = Modifier.weight(1f)
                ) { Text("Restore") }
                Button(
                    onClick = if (status.enabled) onBackUpNow else onTurnOn,
                    enabled = !isWorking,
                    modifier = Modifier.weight(1f)
                ) { Text(if (status.enabled) "Back up now" else "Turn on") }
            }

            if (status.enabled) {
                TextButton(
                    onClick = onTurnOff,
                    enabled = !isWorking,
                    modifier = Modifier.align(Alignment.End)
                ) { Text("Turn off") }
            }
        }
    }
}

@Composable
private fun ReplaceAllDataDialog(source: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Replace all data on this device?") },
        text = {
            Text(
                "Restoring $source replaces every property, tenant, and payment " +
                    "currently on this device with what's in it. This can't be undone."
            )
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Replace") } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun backedUpMessage(summary: BackupSummary): String {
    val photos = if (summary.attachmentCount > 0) ", ${summary.attachmentCount} photos/documents" else ""
    val missing = if (summary.attachmentsSkipped > 0) {
        " (${summary.attachmentsSkipped} file(s) couldn't be read and were left out)"
    } else {
        ""
    }
    return "Backed up ${summary.propertyCount} properties, ${summary.tenantCount} tenants$photos$missing"
}

private fun restoredMessage(summary: BackupSummary): String {
    val photos = if (summary.attachmentCount > 0) ", ${summary.attachmentCount} photos/documents" else ""
    val damaged = if (summary.rowsSkipped > 0) " (${summary.rowsSkipped} damaged row(s) skipped)" else ""
    return "Restored ${summary.propertyCount} properties, ${summary.tenantCount} tenants$photos$damaged"
}

private val backupTimeFormat: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

private fun formatInstant(instant: Instant): String =
    backupTimeFormat.withZone(ZoneId.systemDefault()).format(instant)

private fun backupLabel(backup: DriveFile): String =
    backup.createdTime
        ?.let { runCatching { formatInstant(Instant.parse(it)) }.getOrNull() }
        ?: backup.name
