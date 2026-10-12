package ai.sach.libraryup.ui.screens.settingsScreen.composable

import ai.sach.libraryup.ui.common.composables.AppTopAppBar
import ai.sach.libraryup.ui.screens.authentication.BiometricAuthManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController

private fun logError(tag: String, message: String, throwable: Throwable? = null) {
    android.util.Log.e(tag, message, throwable)
}

/**
 * Security Settings screen for managing authentication and privacy options.
 * Updated to use the unified BiometricAuthManager.
 *
 * @param navController Navigation controller for handling screen navigation
 * @param biometricAuthManager Injected BiometricAuthManager instance
 */
@RequiresApi(Build.VERSION_CODES.N)
@Composable
fun SecurityScreen(
    navController: NavController,
    biometricAuthManager: BiometricAuthManager? = null
) {
    // Get the application context for the biometrics manager
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    // Use injected manager or create a new one
    val bioAuthManager = biometricAuthManager ?: remember { BiometricAuthManager(context) }

    // Observe the fingerprint availability and enabled state
    val isBiometricAvailable by bioAuthManager.isBiometricAvailable.collectAsState()
    val isBiometricEnabled by bioAuthManager.isBiometricEnabled.collectAsState()

    // Dialog states
    var showBiometricTestDialog by remember { mutableStateOf(false) }
    var showBiometricPrompt by remember { mutableStateOf(false) }
    var showBiometricInfo by remember { mutableStateOf(false) }
    var showDeleteDataDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            AppTopAppBar(
                title = "Security",
                showBackButton = true,
                onBackPressed = { navController.popBackStack() },
                showDefaultMenu = true,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Security header with icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )

                Text(
                    text = "Account Security",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            // Authentication Options Section
            SecuritySection(
                title = "Authentication",
                items = listOf(
//                    SecurityItem(
//                        icon = Icons.Default.Password,
//                        title = "Change Password",
//                        subtitle = "Update your account password",
//                        onClick = { navController.navigate("security/password") }
//                    ),
                )
            )

            // Biometric Authentication Section (conditionally displayed)
            if (isBiometricAvailable) {
                BiometricSection(
                    isEnabled = isBiometricEnabled,
                    onToggleEnabled = { enabled ->
                        bioAuthManager.setBiometricEnabled(enabled)
                    },
                    onTestFingerprint = {
                        // Show the test dialog first
                        showBiometricTestDialog = true
                    },
                    onInfoClick = { showBiometricInfo = true }
                )
            }

            // Privacy Section
//            SecuritySection(
//                title = "Privacy",
//                items = listOf(
//                    SecurityItem(
//                        icon = Icons.Default.DataUsage,
//                        title = "Data Privacy",
//                        subtitle = "Manage how your data is used",
//                        onClick = { navController.navigate("privacy") }
//                    )
//                )
//            )

            // Advanced Section
//            SecuritySection(
//                title = "Advanced",
//                items = listOf(
//                    SecurityItem(
//                        icon = Icons.Default.Backup,
//                        title = "Data Backup",
//                        subtitle = "Encrypt and back up your data",
//                        onClick = { navController.navigate("backup") }
//                    ),
//                    SecurityItem(
//                        icon = Icons.Default.Delete,
//                        title = "Delete Data",
//                        subtitle = "Clear all app data and settings",
//                        isDestructive = true,
//                        onClick = { showDeleteDataDialog = true }
//                    )
//                )
//            )

//            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Biometric Test Dialog - Fixed to stay visible
    if (showBiometricTestDialog) {
        AlertDialog(
            onDismissRequest = { showBiometricTestDialog = false },
            title = { Text("Test Biometric Authentication") },
            text = {
                Text("This will test if your biometric authentication is working properly. Tap 'Start Test' to continue.")
            },
            confirmButton = {
                Button(onClick = {
                    showBiometricTestDialog = false // Close this dialog
                    showBiometricPrompt = true // Show biometric prompt instead
                }) {
                    Text("Start Test")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showBiometricTestDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Cancel")
                }
            },
            properties = DialogProperties(dismissOnClickOutside = true, dismissOnBackPress = true)
        )
    }

    // Biometric Prompt - Shown after dialog confirmation
    if (showBiometricPrompt && activity != null) {
        // Test biometric authentication
        bioAuthManager.authenticateWithBiometric(
            activity = activity,
            onSuccess = {
                showBiometricPrompt = false
                // You could show a success message here with another dialog or toast
            },
            onError = { error ->
                showBiometricPrompt = false
                // Handle error, maybe show another dialog with the error
            },
            onCancel = {
                showBiometricPrompt = false
                // Handle cancellation
            }
        )
    }

    // Biometric Info Dialog
    if (showBiometricInfo) {
        AlertDialog(
            onDismissRequest = { showBiometricInfo = false },
            title = { Text("Biometric Authentication") },
            text = {
                Text(
                    "Biometric authentication uses your device's fingerprint sensor or facial recognition to securely access the app without entering a password.\n\n" +
                            "Your biometric data never leaves your device and is protected by your device's security system."
                )
            },
            confirmButton = {
                Button(onClick = { showBiometricInfo = false }) {
                    Text("Got it")
                }
            }
        )
    }

    // Delete Data Confirmation Dialog
    if (showDeleteDataDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDataDialog = false },
            title = { Text("Delete All Data?") },
            text = {
                Text(
                    "This will permanently delete all your data including students, payments, attendance and settings. This action cannot be undone.\n\n" +
                            "Are you sure you want to continue?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Implement data deletion
                        showDeleteDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete All Data")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SecuritySection(
    title: String,
    items: List<SecurityItem>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Section title
        Text(
            text = title,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Section items
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    SecurityItemRow(
                        icon = item.icon,
                        title = item.title,
                        subtitle = item.subtitle,
                        isDestructive = item.isDestructive,
                        onClick = item.onClick
                    )

                    // Add divider between items (but not after the last one)
                    if (index < items.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val textColor = if (isDestructive) MaterialTheme.colorScheme.error else Color.Unspecified
    val iconTint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
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
                color = textColor
            )

            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = if (isDestructive) MaterialTheme.colorScheme.error.copy(alpha = 0.7f) else Color.Gray
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = if (isDestructive) MaterialTheme.colorScheme.error else Color.Gray
        )
    }
}

@Composable
private fun BiometricSection(
    isEnabled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onTestFingerprint: () -> Unit,
    onInfoClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Section title with info button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Biometric Authentication",
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onInfoClick) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Biometric Info",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Biometric toggle card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column {
                // Toggle row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "Fingerprint Login",
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = "Use fingerprint for quicker access",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    Switch(
                        checked = isEnabled,
                        onCheckedChange = onToggleEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.LightGray
                        )
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // Test fingerprint button (only shown when enabled)
                if (isEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onTestFingerprint)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CropFree,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = "Test Fingerprint",
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp)
                        )

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

/**
 * Data class representing a security settings item.
 */
data class SecurityItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit
)