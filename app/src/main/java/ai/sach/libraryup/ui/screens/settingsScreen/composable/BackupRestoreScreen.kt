package ai.sach.libraryup.ui.screens.settingsScreen.composable

import ai.sach.libraryup.LibraryUpApplication
import ai.sach.libraryup.R
import ai.sach.libraryup.data.backup.BackupManager
import ai.sach.libraryup.data.backup.BackupPasswordRequiredException
import ai.sach.libraryup.data.backup.BackupSecrets
import ai.sach.libraryup.data.backup.BackupSummary
import ai.sach.libraryup.data.backup.drive.DriveAccess
import ai.sach.libraryup.data.backup.drive.DriveBackupStatus
import ai.sach.libraryup.data.backup.drive.DriveFile
import ai.sach.libraryup.data.backup.drive.NothingToBackUpException
import ai.sach.libraryup.ui.library.LibraryTopBar
import android.app.Activity
import android.content.Context
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
    val application = context.applicationContext as LibraryUpApplication
    val drive = application.driveBackupManager
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var isWorking by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    val secrets = remember { BackupSecrets(context) }
    var protectionOn by remember { mutableStateOf(secrets.isEnabled) }
    var askNewPassword by remember { mutableStateOf(false) }
    var recoveryCodeToShow by remember { mutableStateOf<String?>(null) }
    // A restore waiting for the password of an encrypted backup: retried with what the user types.
    var passwordPrompt by remember { mutableStateOf<PasswordPrompt?>(null) }

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
            val result = application.backupManager.exportTo(uri, secrets.current())
            isWorking = false
            result.fold(
                onSuccess = { summary -> snackbar.showSnackbar(backedUpMessage(context, summary)) },
                onFailure = { error ->
                    snackbar.showSnackbar(context.getString(R.string.backup_failed, error.message ?: context.getString(R.string.error_unknown)))
                }
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
        // Restores ask for a password themselves; see passwordPrompt.
        isWorking = true
        val message: String? = try {
            when (action) {
                DriveAction.TURN_ON -> {
                    drive.enable()
                    val summary = drive.backUp(token)
                    context.getString(R.string.backup_drive_on_with, backedUpMessage(context, summary))
                }
                DriveAction.BACK_UP_NOW -> {
                    val summary = drive.backUp(token)
                    backedUpMessage(context, summary, toDrive = true)
                }
                DriveAction.RESTORE -> {
                    val backups = drive.listBackups(token)
                    if (backups.isEmpty()) {
                        context.getString(R.string.backup_drive_none_found)
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
                context.getString(R.string.backup_drive_on_first)
            } else {
                e.message
            }
        } catch (e: Exception) {
            context.getString(R.string.backup_drive_error, e.message ?: context.getString(R.string.error_unknown))
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
                snackbar.showSnackbar(context.getString(R.string.backup_drive_not_granted))
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
                snackbar.showSnackbar(context.getString(R.string.backup_drive_connect_failed, e.message ?: context.getString(R.string.error_unknown)))
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
            source = stringResource(R.string.backup_source_file),
            onDismiss = { pendingImportUri = null },
            onConfirm = {
                val uri = pendingImportUri!!
                pendingImportUri = null
                isWorking = true
                scope.launch {
                    val result = application.backupManager.importFrom(uri, secrets.password())
                    isWorking = false
                    result.fold(
                        onSuccess = { summary -> snackbar.showSnackbar(restoredMessage(context, summary)) },
                        onFailure = { error ->
                            if (error is BackupPasswordRequiredException) {
                                passwordPrompt = PasswordPrompt(wrong = false) { pw ->
                                    application.backupManager.importFrom(uri, pw).getOrThrow()
                                }
                            } else {
                                snackbar.showSnackbar(context.getString(R.string.backup_restore_failed, error.message ?: context.getString(R.string.error_unknown)))
                            }
                        }
                    )
                }
            }
        )
    }

    driveRestoreChoice?.let { choice ->
        AlertDialog(
            onDismissRequest = { driveRestoreChoice = null },
            title = { Text(stringResource(R.string.backup_restore_from_drive)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        stringResource(R.string.backup_choose),
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
                TextButton(onClick = { driveRestoreChoice = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    pendingDriveRestore?.let { pending ->
        ReplaceAllDataDialog(
            source = stringResource(R.string.backup_source_drive, backupLabel(pending.backup)),
            onDismiss = { pendingDriveRestore = null },
            onConfirm = {
                pendingDriveRestore = null
                isWorking = true
                scope.launch {
                    val message = try {
                        restoredMessage(context, drive.restore(pending.token, pending.backup))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: BackupPasswordRequiredException) {
                        passwordPrompt = PasswordPrompt(wrong = false) { pw -> drive.restore(pending.token, pending.backup, pw) }
                        null
                    } catch (e: Exception) {
                        context.getString(R.string.backup_restore_failed, e.message ?: context.getString(R.string.error_unknown))
                    } finally {
                        isWorking = false
                    }
                    message?.let { snackbar.showSnackbar(it) }
                }
            }
        )
    }

    passwordPrompt?.let { prompt ->
        PasswordDialog(
            title = stringResource(R.string.backup_password_title),
            message = if (prompt.wrong) stringResource(R.string.backup_password_wrong)
                else stringResource(R.string.backup_password_needed),
            confirmTwice = false,
            onDismiss = { passwordPrompt = null },
            onConfirm = { pw ->
                passwordPrompt = null
                isWorking = true
                scope.launch {
                    try {
                        snackbar.showSnackbar(restoredMessage(context, prompt.retry(pw)))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: BackupPasswordRequiredException) {
                        passwordPrompt = prompt.copy(wrong = true)
                    } catch (e: Exception) {
                        snackbar.showSnackbar(context.getString(R.string.backup_restore_failed, e.message ?: context.getString(R.string.error_unknown)))
                    } finally {
                        isWorking = false
                    }
                }
            }
        )
    }

    if (askNewPassword) {
        PasswordDialog(
            title = stringResource(R.string.backup_protect_title),
            message = stringResource(R.string.backup_protect_text),
            confirmTwice = true,
            onDismiss = { askNewPassword = false },
            onConfirm = { pw ->
                askNewPassword = false
                runCatching { secrets.enable(pw) }
                    .onSuccess { code -> protectionOn = true; recoveryCodeToShow = code }
                    .onFailure { scope.launch { snackbar.showSnackbar(context.getString(R.string.backup_protect_failed)) } }
            }
        )
    }

    recoveryCodeToShow?.let { code ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.backup_recovery_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.backup_recovery_text))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(code, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            },
            confirmButton = { Button(onClick = { recoveryCodeToShow = null }) { Text(stringResource(R.string.backup_recovery_saved)) } }
        )
    }

    // ---------------------------------------------------------------- layout

    Scaffold(
        topBar = {
            LibraryTopBar(stringResource(R.string.title_backup), onBack = { navController.popBackStack() })
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
                        contentDescription = stringResource(R.string.backup_action),
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = stringResource(R.string.backup_stays_title),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.backup_stays_text),
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
                                Text(stringResource(R.string.backup_restore))
                            }
                            Button(
                                onClick = { exportLauncher.launch(BackupManager.suggestedFileName()) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.backup_action))
                            }
                        }

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

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.backup_password_protect), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            if (!secrets.isAvailable) stringResource(R.string.backup_protect_unavailable)
                            else if (protectionOn) stringResource(R.string.backup_protect_on)
                            else stringResource(R.string.backup_protect_off),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = protectionOn,
                        enabled = secrets.isAvailable && !isWorking,
                        onCheckedChange = { on ->
                            if (on) askNewPassword = true else { secrets.disable(); protectionOn = false }
                        }
                    )
                }
            }

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
                        text = stringResource(R.string.backup_what_title),
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.backup_what_list),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

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
                    text = stringResource(R.string.backup_drive_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val (statusText, isProblem) = when {
                !status.enabled ->
                    stringResource(R.string.backup_drive_off_text) to false
                status.needsReconnect ->
                    stringResource(R.string.backup_drive_reconnect) to true
                status.lastError != null ->
                    stringResource(R.string.backup_drive_last_failed, status.lastError.orEmpty()) to true
                status.lastBackupAt != null ->
                    stringResource(
                        R.string.backup_drive_on_last,
                        formatInstant(Instant.ofEpochMilli(status.lastBackupAt))
                    ) to false
                else -> stringResource(R.string.backup_drive_on) to false
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
                ) { Text(stringResource(R.string.backup_restore)) }
                Button(
                    onClick = if (status.enabled) onBackUpNow else onTurnOn,
                    enabled = !isWorking,
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(if (status.enabled) R.string.backup_now else R.string.action_turn_on)) }
            }

            if (status.enabled) {
                TextButton(
                    onClick = onTurnOff,
                    enabled = !isWorking,
                    modifier = Modifier.align(Alignment.End)
                ) { Text(stringResource(R.string.action_turn_off)) }
            }
        }
    }
}

@Composable
private fun ReplaceAllDataDialog(source: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_replace_title)) },
        text = {
            Text(stringResource(R.string.backup_replace_text, source))
        },
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(R.string.action_replace)) } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

/** "5 students, 12 payments, 3 photos/documents" in the app's language. */
private fun summaryCounts(context: Context, summary: BackupSummary): String {
    val parts = mutableListOf(
        context.getString(R.string.backup_count_students, summary.studentCount),
        context.getString(R.string.backup_count_payments, summary.paymentCount)
    )
    if (summary.attachmentCount > 0) {
        parts += context.getString(R.string.backup_count_files, summary.attachmentCount)
    }
    return parts.joinToString(", ")
}

private fun backedUpMessage(context: Context, summary: BackupSummary, toDrive: Boolean = false): String {
    val notes = buildList {
        if (summary.attachmentsSkipped > 0) {
            add(context.getString(R.string.backup_files_skipped, summary.attachmentsSkipped))
        }
        if (summary.encrypted) add(context.getString(R.string.backup_encrypted))
    }
    val counts = (listOf(summaryCounts(context, summary)) + notes).joinToString(" ")
    return context.getString(
        if (toDrive) R.string.backup_msg_backed_up_drive else R.string.backup_msg_backed_up,
        counts
    )
}

private fun restoredMessage(context: Context, summary: BackupSummary): String =
    context.getString(R.string.backup_msg_restored, summaryCounts(context, summary))

private val backupTimeFormat: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

private fun formatInstant(instant: Instant): String =
    backupTimeFormat.withZone(ZoneId.systemDefault()).format(instant)

private fun backupLabel(backup: DriveFile): String =
    backup.createdTime
        ?.let { runCatching { formatInstant(Instant.parse(it)) }.getOrNull() }
        ?: backup.name

/** A restore that stopped for a password; [retry] runs it again with the one typed. */
private data class PasswordPrompt(val wrong: Boolean, val retry: suspend (String) -> BackupSummary)

@Composable
private fun PasswordDialog(
    title: String,
    message: String,
    confirmTwice: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    val tooShort = confirmTwice && password.length < 6
    val mismatch = confirmTwice && password != again
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(message, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(if (confirmTwice) R.string.backup_password_min else R.string.backup_password_or_code)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                if (confirmTwice) {
                    OutlinedTextField(
                        value = again,
                        onValueChange = { again = it },
                        label = { Text(stringResource(R.string.backup_password_repeat)) },
                        singleLine = true,
                        isError = again.isNotEmpty() && mismatch,
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            }
        },
        confirmButton = {
            Button(enabled = password.isNotEmpty() && !tooShort && !mismatch, onClick = { onConfirm(password) }) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}
