package ai.sach.rentup.ui.screens.settingsScreen.composable

import ai.sach.rentup.RentUpApplication
import ai.sach.rentup.data.local.entity.Tenant
import ai.sach.rentup.ui.common.composables.RentUpTopAppBar
import ai.sach.rentup.ui.screens.settingsScreen.notifications.NotificationViewModel
import ai.sach.rentup.ui.screens.settingsScreen.notifications.NotificationViewModelFactory
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch

/**
 * Notification Settings screen for managing app notification preferences.
 *
 * @param navController Navigation controller for handling screen navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as? Activity
    val application = context.applicationContext as RentUpApplication

    // Create ViewModel using factory
    val viewModel: NotificationViewModel = viewModel(
        factory = NotificationViewModelFactory(
            application = application,
            tenantRepository = application.tenantRepository,
            propertyRepository = application.propertyRepository
        )
    )

    // Notification preference state from ViewModel
    val masterToggleEnabled by viewModel.masterToggle.observeAsState(true)
    val rentRemindersEnabled by viewModel.rentRemindersEnabled.observeAsState(true)
    val reminderDays by viewModel.reminderDays.observeAsState(3)
    val paymentConfirmationsEnabled by viewModel.paymentConfirmationsEnabled.observeAsState(true)
    val dailyOverdueAlertsEnabled by viewModel.dailyOverdueAlertsEnabled.observeAsState(false)
    val tenantUpdatesEnabled by viewModel.tenantUpdatesEnabled.observeAsState(true)
    val propertyAlertsEnabled by viewModel.propertyAlertsEnabled.observeAsState(false)
    val appUpdatesEnabled by viewModel.appUpdatesEnabled.observeAsState(true)
    val whatsAppEnabled by viewModel.whatsAppEnabled.observeAsState(false)
    val notificationsPermissionGranted by viewModel.notificationsPermissionGranted.observeAsState(false)

    // Dialog state
    var showRentReminderOptions by remember { mutableStateOf(false) }
    var showWhatsAppDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var whatsAppRegistrationComplete by remember { mutableStateOf(false) }

    // For handling permission requests
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.refreshPermissionStatus()
        } else {
            showPermissionDialog = true
        }
    }

    // First-time ask, or a denial that did not pick "don't ask again", can still show the
    // system dialog. Once the dialog has been shown before AND the system no longer offers a
    // rationale, POST_NOTIFICATIONS is permanently denied and launch() would silently no-op.
    val canRequestNotificationPermission =
        viewModel.hasRequestedNotificationPermission().let { requested ->
            requested.not() || (activity != null &&
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.POST_NOTIFICATIONS
                ))
        }

    fun launchOrRouteToSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && canRequestNotificationPermission) {
            viewModel.markNotificationPermissionRequested()
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            showPermissionDialog = true
        }
    }

    // Check permission on first composition
    LaunchedEffect(Unit) {
        viewModel.refreshPermissionStatus()

        // If permission is not granted and we're on Android 13+, show the dialog
        if (!notificationsPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            showPermissionDialog = true
        }
    }

    // Coroutine scope for triggering test notifications
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            RentUpTopAppBar(
                title = "Notifications",
                showBackButton = true,
                onBackPressed = { navController.popBackStack() },
                actions = {
                    // Test notification button (only visible when notifications are enabled)
                    if (masterToggleEnabled && notificationsPermissionGranted) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    viewModel.sendTestRentReminder()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Test Notification"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Notification permission banner (only shown if permission is not granted)
            if (!notificationsPermissionGranted) {
                NotificationPermissionBanner(
                    onRequestPermission = {
                        launchOrRouteToSettings()
                    }
                )
            }

            // Master toggle for all notifications
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
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Notifications",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Text(
                        text = "Control all app notifications",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }

                Switch(
                    checked = masterToggleEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && !notificationsPermissionGranted) {
                            // If enabling but permission not granted, request permission
                            launchOrRouteToSettings()
                        } else {
                            viewModel.setMasterToggle(enabled)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.LightGray
                    )
                )
            }

            // Explanation text when notifications are disabled
            if (!masterToggleEnabled) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )

                        Text(
                            text = "You won't receive any notifications from the app until you turn notifications back on.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Rent Reminders Section - Only enabled if master toggle is on
            NotificationSection(
                title = "Rent Reminders",
                items = listOf(
                    NotificationSwitchItem(
                        icon = Icons.Default.CalendarMonth,
                        title = "Rent Due Reminders",
                        subtitle = "Notify $reminderDays days before rent is due",
                        hasDetail = true,
                        isEnabled = masterToggleEnabled && rentRemindersEnabled,
                        onToggle = { enabled ->
                            viewModel.setRentRemindersEnabled(enabled)
                        },
                        onDetail = {
                            showRentReminderOptions = true
                        }
                    ),
                    NotificationSwitchItem(
                        icon = Icons.Default.Payments,
                        title = "Payment Confirmations",
                        subtitle = "Receive confirmation of tenant payments",
                        isEnabled = masterToggleEnabled && paymentConfirmationsEnabled,
                        onToggle = { enabled ->
                            viewModel.setPaymentConfirmationsEnabled(enabled)
                        }
                    ),
                    NotificationSwitchItem(
                        icon = Icons.Default.Warning,
                        title = "Daily Overdue Alerts",
                        subtitle = "Repeat once daily until rent is marked paid",
                        isEnabled = masterToggleEnabled && dailyOverdueAlertsEnabled,
                        onToggle = { enabled -> viewModel.setDailyOverdueAlertsEnabled(enabled) }
                    )
                ),
                isEnabled = masterToggleEnabled
            )

            // Tenant Updates Section
            NotificationSection(
                title = "Tenant Updates",
                items = listOf(
                    NotificationSwitchItem(
                        icon = Icons.Default.PersonAdd,
                        title = "New Tenant Notifications",
                        subtitle = "When tenants are added or updated",
                        isEnabled = masterToggleEnabled && tenantUpdatesEnabled,
                        onToggle = { enabled ->
                            viewModel.setTenantUpdatesEnabled(enabled)
                        }
                    ),
                    NotificationSwitchItem(
                        icon = Icons.Default.Home,
                        title = "Property Alerts",
                        subtitle = "Updates about your properties",
                        isEnabled = masterToggleEnabled && propertyAlertsEnabled,
                        onToggle = { enabled ->
                            viewModel.setPropertyAlertsEnabled(enabled)
                        }
                    )
                ),
                isEnabled = masterToggleEnabled
            )

            // App Updates Section
            NotificationSection(
                title = "App Updates",
                items = listOf(
                    NotificationSwitchItem(
                        icon = Icons.Default.NewReleases,
                        title = "New Features & Updates",
                        subtitle = "Learn about new app features",
                        isEnabled = masterToggleEnabled && appUpdatesEnabled,
                        onToggle = { enabled ->
                            viewModel.setAppUpdatesEnabled(enabled)
                        }
                    )
                ),
                isEnabled = masterToggleEnabled
            )

            // WhatsApp Integration Section
//            NotificationSection(
//                title = "WhatsApp Integration",
//                items = listOf(
//                    NotificationSwitchItem(
//                        icon = Icons.Default.Message,
//                        title = "WhatsApp Notifications",
//                        subtitle = if (whatsAppRegistrationComplete)
//                            "Send rent reminders via WhatsApp"
//                        else
//                            "Connect your WhatsApp account for reminders",
//                        hasDetail = !whatsAppRegistrationComplete,
//                        isEnabled = masterToggleEnabled && whatsAppEnabled,
//                        onToggle = { enabled ->
//                            if (enabled && !whatsAppRegistrationComplete) {
//                                showWhatsAppDialog = true
//                            } else {
//                                viewModel.setWhatsAppEnabled(enabled)
//                            }
//                        },
//                        onDetail = {
//                            if (!whatsAppRegistrationComplete) {
//                                showWhatsAppDialog = true
//                            }
//                        }
//                    )
//                ),
//                isEnabled = masterToggleEnabled
//            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Rent reminder days selection dialog
        if (showRentReminderOptions) {
            val reminderOptions = listOf(1, 2, 3, 5, 7, 10)

            AlertDialog(
                onDismissRequest = { showRentReminderOptions = false },
                title = { Text("Rent Reminder Days") },
                text = {
                    Column {
                        Text("Select how many days before the due date to send a reminder")

                        Spacer(modifier = Modifier.height(16.dp))

                        // Days options
                        reminderOptions.forEach { days ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = reminderDays == days,
                                    onClick = { viewModel.setReminderDays(days) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    )
                                )

                                Text(
                                    text = "$days ${if (days == 1) "day" else "days"} before due date",
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showRentReminderOptions = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRentReminderOptions = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

//        // WhatsApp integration dialog
//        if (showWhatsAppDialog) {
//            AlertDialog(
//                onDismissRequest = { showWhatsAppDialog = false },
//                title = { Text("Connect WhatsApp") },
//                text = {
//                    Column {
//                        Text(
//                            "To receive notifications via WhatsApp, you need to connect your account. " +
//                                    "We'll send a verification code to your registered phone number."
//                        )
//
//                        Spacer(modifier = Modifier.height(16.dp))
//
//                        Text(
//                            "Note: Standard messaging rates may apply from your mobile carrier.",
//                            color = Color.Gray,
//                            fontSize = 14.sp
//                        )
//                    }
//                },
//                confirmButton = {
//                    Button(
//                        onClick = {
//                            // Simulate WhatsApp registration
//                            whatsAppRegistrationComplete = true
//                            viewModel.setWhatsAppEnabled(true)
//                            showWhatsAppDialog = false
//                        },
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = MaterialTheme.colorScheme.primary
//                        )
//                    ) {
//                        Text("Connect WhatsApp")
//                    }
//                },
//                dismissButton = {
//                    TextButton(onClick = { showWhatsAppDialog = false }) {
//                        Text("Not Now")
//                    }
//                }
//            )
//        }

        // Permission dialog
        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                title = { Text("Enable Notifications") },
                text = {
                    Text(
                        "To receive notifications from RentUp, you need to enable them in your device settings."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPermissionDialog = false

                            // Open app notification settings
                            val intent = Intent().apply {
                                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Open Settings")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionDialog = false }) {
                        Text("Later")
                    }
                }
            )
        }
    }
}

@Composable
private fun NotificationPermissionBanner(
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )

                Text(
                    text = "Notifications are disabled",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Text(
                text = "To receive notifications about rent due dates and other important updates, please enable notifications for RentUp.",
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Enable Notifications")
            }
        }
    }
}

@Composable
private fun NotificationSection(
    title: String,
    items: List<NotificationSwitchItem>,
    isEnabled: Boolean
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

        // Section card with items
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
                    NotificationSwitchRow(
                        icon = item.icon,
                        title = item.title,
                        subtitle = item.subtitle,
                        isEnabled = if (isEnabled) item.isEnabled else false,
                        isMasterEnabled = isEnabled,
                        hasDetail = item.hasDetail,
                        onToggle = item.onToggle,
                        onDetail = item.onDetail
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
private fun NotificationSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    isMasterEnabled: Boolean,
    hasDetail: Boolean = false,
    onToggle: (Boolean) -> Unit,
    onDetail: () -> Unit = {}
) {
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
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = if (isMasterEnabled) 1f else 0.5f)
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
                color = if (isMasterEnabled) Color.Unspecified else Color.Gray
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = if (isMasterEnabled) Color.Gray else Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (hasDetail && isMasterEnabled) {
                    TextButton(
                        onClick = onDetail,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "Edit",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            enabled = isMasterEnabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.LightGray,
                disabledCheckedThumbColor = Color.White.copy(alpha = 0.5f),
                disabledCheckedTrackColor = Color.Gray.copy(alpha = 0.5f),
                disabledUncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                disabledUncheckedTrackColor = Color.Gray.copy(alpha = 0.5f)
            )
        )
    }
}

/**
 * Data class representing a notification switch item.
 */
data class NotificationSwitchItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val isEnabled: Boolean,
    val hasDetail: Boolean = false,
    val onToggle: (Boolean) -> Unit,
    val onDetail: () -> Unit = {}
)

/**
 * Updates the RentUp application to receive tenant payment updates
 * when payments are made, so they can be notified to the user.
 *
 * This extension method should be called in the MainActivity when the payment
 * is collected to trigger a notification.
 */
fun RentUpApplication.notifyPaymentCollected(tenant: Tenant, amount: String) {
    // Get notification preferences
    val preferencesManager = notificationPreferences

    // Only send if payment confirmations are enabled
    if (preferencesManager.arePaymentConfirmationsEnabled()) {
        notificationHelper.sendPaymentConfirmationNotification(tenant, amount)
    }
}
