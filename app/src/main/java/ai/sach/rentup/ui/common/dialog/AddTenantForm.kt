package ai.sach.rentup.ui.common.dialog

import ai.sach.rentup.RentUpApplication
import ai.sach.rentup.data.local.entity.Property
import ai.sach.rentup.data.local.entity.Tenant
import ai.sach.rentup.data.local.entity.TenantFloors
import ai.sach.rentup.data.viewmodel.TenantViewModel
import ai.sach.rentup.data.viewmodel.TenantViewModelFactory
import ai.sach.rentup.ui.common.composables.processSelectedImage
import ai.sach.rentup.ui.common.responsiveness.LocalAccessibilityScaling
import ai.sach.rentup.ui.common.responsiveness.LocalDimensions
import ai.sach.rentup.ui.screens.homescreen.composable.shareOnboardingLinkGeneric
import ai.sach.rentup.ui.screens.homescreen.composable.shareOnboardingLinkWhatsApp
import ai.sach.rentup.ui.screens.settingsScreen.appprefernecs.getCurrencySymbol
import ai.sach.rentup.ui.screens.settingsScreen.appprefernecs.PhoneNumberFormatter
import ai.sach.rentup.ui.viewmodel.PropertyViewModel
import ai.sach.rentup.ui.viewmodel.PropertyViewModelFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.min

/**
 * Dialog for adding a new tenant to the system
 * Includes fields for tenant details and property assignment
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTenantDialog(
    onDismiss: () -> Unit,
    onAddTenant: suspend (Tenant) -> Long,
    onAddPropertyRequest: () -> Unit,
    tenantViewModel: TenantViewModel = viewModel(
        factory = TenantViewModelFactory(
            (LocalContext.current.applicationContext as RentUpApplication).tenantRepository,
            (LocalContext.current.applicationContext as RentUpApplication).propertyRepository
        )
    ),
    propertyViewModel: PropertyViewModel = viewModel(
        factory = PropertyViewModelFactory(
            (LocalContext.current.applicationContext as RentUpApplication).propertyRepository
        )
    )
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Due") }
    var isPaid by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var avatarInternalUriString by remember { mutableStateOf<String?>(null) }
    var permanentAddress by remember { mutableStateOf("") }

    // Initialize due date to today
    LaunchedEffect(Unit) {
        val today = java.time.LocalDate.now().toString()
        dueDate = today
    }


    // State for onboarding link dialog
    var showOnboardingLinkDialog by remember { mutableStateOf(false) }
    var onboardingLink by remember { mutableStateOf("") }
    var onboardingLinkLoading by remember { mutableStateOf(false) }

    // Collect onboarding link result
    val onboardingLinkResult by tenantViewModel.onboardingLinkResult.collectAsState()

    // When onboardingLinkResult changes, update the link and show dialog
    LaunchedEffect(onboardingLinkResult) {
        val result = onboardingLinkResult ?: return@LaunchedEffect
        if (!onboardingLinkLoading) return@LaunchedEffect
        onboardingLinkLoading = false
        tenantViewModel.consumeOnboardingLinkResult()
        if (result.success && result.url.isNotBlank()) {
            onboardingLink = result.url
            showOnboardingLinkDialog = true
        } else {
            // The tenant is already saved; only the link failed. The loading dialog can't be
            // dismissed, so closing here is what keeps the screen usable.
            Toast.makeText(
                context,
                "Tenant added, but the onboarding link couldn't be created. Check your internet connection and try again.",
                Toast.LENGTH_LONG
            ).show()
            onDismiss()
        }
    }

    // Function to generate onboarding link
    suspend fun generateOnboardingLink(propertyAddress: String, tenantId: Long) {
        try {
            val ownerId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
                Toast.makeText(context, "User authentication error. Please try again.", Toast.LENGTH_SHORT).show()
                onDismiss()
                return
            }
            val deviceToken = FirebaseMessaging.getInstance().token.await() // Uses the correct await()
            val baseUrl = "https://rentuppro.vercel.app/"
            onboardingLinkLoading = true
            tenantViewModel.generateOnboardingFormLink(
                ownerId = ownerId,
                propertyAddress = propertyAddress,
                deviceToken = deviceToken,
                baseUrl = baseUrl,
                tenantId = tenantId.toString()
            )
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to generate link: ${e.message}", Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    }

    val appPreferencesRepository = remember {
        (context.applicationContext as RentUpApplication).appPreferencesRepository
    }
    val authRepository = remember {
        (context.applicationContext as RentUpApplication).authRepository
    }
    val currencyCode by appPreferencesRepository.currency.collectAsState(initial = "INR")
    val currencySymbol = getCurrencySymbol(currencyCode)
    val countryCode by appPreferencesRepository.country.collectAsState(initial = "IN")

    // Install date logic
    val installDatePref by appPreferencesRepository.installDate.collectAsState(initial = null)
    LaunchedEffect(installDatePref) {
        if (installDatePref == null) {
            val now = Instant.now().toEpochMilli().toString()
            appPreferencesRepository.setInstallDate(now)
        }
    }

    // Calculate if trial is active
    val isTrialActive = remember(installDatePref) {
        installDatePref?.let {
            val installInstant = Instant.ofEpochMilli(it.toLong())
            val now = Instant.now()
            ChronoUnit.DAYS.between(installInstant, now) < 90
        } ?: true
    }

    // Google login state
    val isGoogleSignedIn = remember { authRepository.isGoogleSignedIn() }

    // Toggle visibility logic
    val showToggle = isTrialActive || isGoogleSignedIn
    // Default mode logic
    val defaultManualMode = !showToggle
    var manualMode by remember { mutableStateOf(defaultManualMode) }

    // If trial ends and not Google signed in, force manual mode
    LaunchedEffect(isTrialActive, isGoogleSignedIn) {
        if (!showToggle) manualMode = true
    }

    var phoneNumbers by remember { mutableStateOf(listOf("")) }
    var phoneNumberErrors by remember { mutableStateOf(listOf(false)) }

    var selectedPropertyIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var propertyFloorMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var propertyFloorErrors by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    var propertyError by remember { mutableStateOf(false) }
    var floorInputError by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf(false) }
    var dueDateError by remember { mutableStateOf(false) }
    var anyPhoneNumberError by remember { mutableStateOf(false) }

    val properties by propertyViewModel.allProperties.observeAsState(emptyList())
    val availableProperties = properties

    // Auto-select single property if only one exists
    LaunchedEffect(availableProperties) {
        if (availableProperties.size == 1 && selectedPropertyIds.isEmpty()) {
            val singleProperty = availableProperties.first()
            selectedPropertyIds = listOf(singleProperty.id)
            propertyFloorMap = mapOf(singleProperty.id to "")
            propertyFloorErrors = mapOf(singleProperty.id to true)
            propertyError = false
            floorInputError = true
        }
    }

    var showPropertyDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    // Get responsive dimensions
    val dimensions = LocalDimensions.current
    val accessibilityScaling = LocalAccessibilityScaling.current

    val dialogPadding = when {
        dimensions.windowSizeClass.isExpandedWidth -> dimensions.paddingLarge
        dimensions.windowSizeClass.isMediumWidth -> dimensions.paddingMedium
        else -> dimensions.paddingSmall
    }

    val itemSpacing = when {
        dimensions.windowSizeClass.isExpandedWidth -> dimensions.spacingMedium
        else -> dimensions.spacingSmall
    } * accessibilityScaling.contentScale

    val textFieldFontSize = dimensions.fontSizeMedium
    val labelFontSize = dimensions.fontSizeSmall
    val titleFontSize = dimensions.fontSizeLarge
    val errorFontSize = dimensions.fontSizeSmall

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = true
        ),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Add New Tenant",
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (showToggle) {
                    androidx.compose.material3.Switch(
                        checked = manualMode,
                        onCheckedChange = { manualMode = it }
                    )
                }
            }
        },
        text = {
            TenantFormFields(
                avatarUri = avatarUri,
                onAvatarSelected = { selectedUri ->
                    avatarUri = selectedUri
                    coroutineScope.launch {
                        processSelectedImage(context, selectedUri, name.ifBlank { "tenant_avatar" }) { savedUriString ->
                            avatarInternalUriString = savedUriString
                        }
                    }
                },
                tenantName = name,
                onTenantNameChange = { name = it; nameError = it.isBlank() },
                tenantNameError = nameError,
                phoneNumbers = phoneNumbers,
                phoneNumberErrors = phoneNumberErrors,
                onPhoneNumbersChanged = { numbers, errors ->
                    phoneNumbers = numbers
                    phoneNumberErrors = errors
                    anyPhoneNumberError = numbers.all { it.isBlank() } || errors.any { it }
                },
                anyPhoneNumberError = anyPhoneNumberError,
                selectedPropertyIds = selectedPropertyIds,
                propertyFloorMap = propertyFloorMap,
                propertyFloorErrors = propertyFloorErrors,
                propertyError = propertyError,
                floorInputError = floorInputError,
                availableProperties = availableProperties,
                onAddPropertyClicked = {
                    if (availableProperties.isEmpty()) {
                        Toast.makeText(context, "No properties found. Please add one first.", Toast.LENGTH_LONG).show()
                        onAddPropertyRequest()
                        onDismiss()
                    } else {
                        showPropertyDialog = true
                    }
                },
                onPropertyRemoved = { propertyId ->
                    selectedPropertyIds = selectedPropertyIds - propertyId
                    propertyFloorMap = propertyFloorMap - propertyId
                    propertyFloorErrors = propertyFloorErrors - propertyId
                    propertyError = selectedPropertyIds.isEmpty()
                    floorInputError = propertyFloorMap.filterKeys { it in selectedPropertyIds }.any { it.value.isBlank() }
                },
                onFloorChanged = { propertyId, newValue ->
                    propertyFloorMap = propertyFloorMap + (propertyId to newValue)
                    propertyFloorErrors = propertyFloorErrors + (propertyId to newValue.isBlank())
                    floorInputError = propertyFloorMap.filterKeys { it in selectedPropertyIds }.any { it.value.isBlank() }
                },
                dueDate = dueDate,
                onDueDateClick = { showDatePicker = true },
                dueDateError = dueDateError,
                amount = amount,
                onAmountChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) amount = it },
                currencySymbol = currencySymbol,
                permanentAddress = permanentAddress,
                onPermanentAddressChange = { permanentAddress = it },
                isPaid = isPaid,
                onPaidChange = { checked -> isPaid = checked; status = if (checked) "Paid" else "Due" },
                status = status,
                countryCode = countryCode,
                textFieldFontSize = textFieldFontSize,
                labelFontSize = labelFontSize,
                errorFontSize = errorFontSize,
                dialogPadding = dialogPadding,
                itemSpacing = itemSpacing,
                manualMode = manualMode,
                onManualModeChange = if (showToggle) { { manualMode = it } } else null,
                showManualModeToggleInTitle = false
            )
        },
        confirmButton = {
            Button(onClick = {
                // Manually trigger validation checks before submission
                nameError = name.isBlank()
                dueDateError = dueDate.isBlank()
                propertyError = selectedPropertyIds.isEmpty()
                
                // Validate phone numbers using country-specific rules
                val phoneNumberValidationErrors = phoneNumbers.map { phoneNumber ->
                    if (phoneNumber.isBlank()) false else {
                        !PhoneNumberFormatter.isValidPhoneNumber(phoneNumber, countryCode)
                    }
                }
                anyPhoneNumberError = phoneNumbers.all { it.isBlank() } || phoneNumberValidationErrors.any { it }
                phoneNumberErrors = phoneNumberValidationErrors
                
                floorInputError = propertyFloorMap.filterKeys { it in selectedPropertyIds }.any { it.value.isBlank() }

                // Check all error flags
                if (!nameError && !dueDateError && !propertyError && !anyPhoneNumberError && !floorInputError) {
                    coroutineScope.launch {
                        // All checks passed, proceed with adding tenant
                        val validPhoneNumbers = phoneNumbers.filter { it.isNotBlank() }
                        val formattedPhoneNumbers = validPhoneNumbers.map { PhoneNumberFormatter.formatPhoneNumber(it, countryCode) }
                        val phoneNumbersString = formattedPhoneNumbers.joinToString(",")
                        val propertyIdsString = selectedPropertyIds.joinToString(",")
                        val floorString = selectedPropertyIds.joinToString(",") { TenantFloors.encode(propertyFloorMap[it] ?: "") }

                        val tenant = Tenant(
                            name = name,
                            mobileNumber = phoneNumbersString,
                            status = status,
                            isPaid = isPaid,
                            dueDate = dueDate,
                            propertyId = propertyIdsString,
                            floor = floorString,
                            amount = amount.ifBlank { null },
                            avatarUri = avatarInternalUriString,
                            permanentAddress = permanentAddress
                        )
                        val newTenantId = onAddTenant(tenant)

                        if (newTenantId > -1) { // Assuming onAddTenant returns a valid ID > -1
                            Toast.makeText(context, "Tenant '$name' added", Toast.LENGTH_SHORT).show()

                            if (!manualMode) {
                                val firstPropertyId = selectedPropertyIds.firstOrNull()
                                val firstProperty = firstPropertyId?.let { id -> properties.find { it.id == id } }
                                val propertyAddress = firstProperty?.address ?: permanentAddress
                                if (propertyAddress.isNotBlank()) {
                                    generateOnboardingLink(propertyAddress, newTenantId)
                                } else {
                                    Toast.makeText(context, "No property address available for onboarding link", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            } else {
                                onDismiss()
                            }
                        } else {
                            Toast.makeText(context, "Failed to add tenant.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Please fix errors and fill required fields (*)", Toast.LENGTH_LONG).show()
                }
            },
                contentPadding = PaddingValues(
                    horizontal = dimensions.paddingMedium,
                    vertical = dimensions.paddingSmall
                )
            ) {
                Text(
                    "Add Tenant",
                    fontSize = dimensions.fontSizeMedium
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                contentPadding = PaddingValues(
                    horizontal = dimensions.paddingMedium,
                    vertical = dimensions.paddingSmall
                )
            ) {
                Text(
                    "Cancel",
                    fontSize = dimensions.fontSizeMedium
                )
            }
        }
    )

    // --- Dialogs ---
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedLocalDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                            dueDate = selectedLocalDate.toString()
                            dueDateError = false
                        }
                        showDatePicker = false
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) {
                    Text(
                        "OK",
                        fontSize = dimensions.fontSizeMedium
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text(
                        "Cancel",
                        fontSize = dimensions.fontSizeMedium
                    )
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showPropertyDialog) {
        ResponsivePropertySelectionDialog(
            properties = availableProperties.filter { property -> !selectedPropertyIds.contains(property.id) },
            onDismiss = { showPropertyDialog = false },
            onPropertySelected = { propertyId ->
                selectedPropertyIds = selectedPropertyIds + propertyId
                propertyFloorMap = propertyFloorMap + (propertyId to "")
                propertyFloorErrors = propertyFloorErrors + (propertyId to true)
                floorInputError = true
                propertyError = false
            }
        )
    }

    // Hybrid loading dialog for onboarding link generation
    if (onboardingLinkLoading) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {},
            title = { androidx.compose.material3.Text("Generating Onboarding Link") },
            text = {
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
                    androidx.compose.material3.Text("Please wait while we generate the link for your tenant...")
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(24.dp))
                    // Simple skeleton placeholder for the link
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(24.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    if (showOnboardingLinkDialog && onboardingLink.isNotBlank()) {
        AlertDialog(
            onDismissRequest = {
                showOnboardingLinkDialog = false
                onDismiss()
            },
            title = { Text("Tenant Onboarding Link Ready") },
            text = {
                Column {
                    Text("Share this link with your tenant to collect their information:")
                    Text(onboardingLink, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                Column {
                    Button(onClick = {
                        shareOnboardingLinkGeneric(context, onboardingLink)
                        showOnboardingLinkDialog = false
                        onDismiss()
                    }) {
                        Text("Share Link")
                    }
                    // WhatsApp button if valid phone number exists
                    val firstValidPhone = phoneNumbers.firstOrNull { it.isNotBlank() }
                    if (firstValidPhone != null && firstValidPhone.length >= 8) { // basic check
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            shareOnboardingLinkWhatsApp(context, onboardingLink, phoneNumbers.joinToString(","), true, countryCode)
                            showOnboardingLinkDialog = false
                            onDismiss()
                        }) {
                            Text("Send via WhatsApp")
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showOnboardingLinkDialog = false
                    onDismiss()
                }) {
                    Text("Close")
                }
            }
        )
    }
}


@Composable
fun ResponsivePropertyRow(
    property: Property?,
    propertyId: String,
    floorValue: String,
    isFloorError: Boolean,
    onFloorChanged: (String) -> Unit,
    onRemoveProperty: () -> Unit
) {
    val dimensions = LocalDimensions.current
    val accessibilityScaling = LocalAccessibilityScaling.current
    val configuration = LocalConfiguration.current

    val textSize = dimensions.fontSizeMedium
    val labelSize = dimensions.fontSizeSmall
    val iconSize = dimensions.iconSizeMedium * accessibilityScaling.contentScale
    val isSmallScreen = dimensions.windowSizeClass.isCompactWidth && configuration.screenWidthDp < 400

    if (isSmallScreen) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingTiny)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InputChip(
                    selected = true,
                    onClick = { /* No-op */ },
                    label = {
                        Text(
                            property?.name ?: "ID: $propertyId",
                            maxLines = 1,
                            fontSize = textSize
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.HomeWork,
                            contentDescription = null,
                            modifier = Modifier.size(InputChipDefaults.AvatarSize * accessibilityScaling.contentScale)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onRemoveProperty,
                    modifier = Modifier.size(36.dp * accessibilityScaling.contentScale)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove ${property?.name ?: propertyId}",
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
            OutlinedTextField(
                value = floorValue,
                onValueChange = onFloorChanged,
                label = { Text("Floor/Room(s)*", fontSize = labelSize) },
                supportingText = { Text("For multiple floors or rooms, separate them with commas") },
                modifier = Modifier.fillMaxWidth(),
                isError = isFloorError,
                singleLine = true,
                textStyle = TextStyle(fontSize = textSize)
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
        ) {
            InputChip(
                selected = true,
                onClick = { /* No-op */ },
                label = {
                    Text(
                        property?.name ?: "ID: $propertyId",
                        maxLines = 1,
                        fontSize = textSize
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.HomeWork,
                        contentDescription = null,
                        modifier = Modifier.size(InputChipDefaults.AvatarSize * accessibilityScaling.contentScale)
                    )
                },
                modifier = Modifier.weight(0.6f)
            )

            OutlinedTextField(
                value = floorValue,
                onValueChange = onFloorChanged,
                label = { Text("Floor/Room(s)*", fontSize = labelSize) },
                supportingText = { Text("For multiple floors or rooms, separate them with commas") },
                modifier = Modifier.weight(0.5f),
                isError = isFloorError,
                singleLine = true,
                textStyle = TextStyle(fontSize = textSize)
            )

            IconButton(
                onClick = onRemoveProperty,
                modifier = Modifier.size(36.dp * accessibilityScaling.contentScale)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove ${property?.name ?: propertyId}",
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

@Composable
fun ResponsivePropertySelectionDialog(
    properties: List<Property>,
    onDismiss: () -> Unit,
    onPropertySelected: (String) -> Unit
) {
    val dimensions = LocalDimensions.current
    val accessibilityScaling = LocalAccessibilityScaling.current

    val titleSize = dimensions.fontSizeLarge
    val textSize = dimensions.fontSizeMedium
    val subTextSize = dimensions.fontSizeSmall
    val maxDialogHeight = when {
        dimensions.windowSizeClass.isExpandedWidth -> 400.dp
        dimensions.windowSizeClass.isMediumWidth -> 350.dp
        else -> 300.dp
    } * accessibilityScaling.contentScale
    val dialogPadding = when {
        dimensions.windowSizeClass.isExpandedWidth -> dimensions.paddingMedium
        else -> dimensions.paddingSmall
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Select Property",
                fontSize = titleSize
            )
        },
        text = {
            if (properties.isEmpty()) {
                Text(
                    "No more properties available to select.",
                    fontSize = textSize
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxDialogHeight)
                ) {
                    items(properties.size) { index ->
                        val property = properties[index]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = dimensions.spacingSmall)
                                .clickable { onPropertySelected(property.id) },
                            shape = RoundedCornerShape(dimensions.radiusSmall)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(dialogPadding)
                            ) {
                                Text(
                                    property.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = textSize)
                                )
                                Text(
                                    property.address,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = subTextSize),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Done",
                    fontSize = textSize
                )
            }
        }
    )
}

// Phone number formatting is now handled by PhoneNumberFormatter
