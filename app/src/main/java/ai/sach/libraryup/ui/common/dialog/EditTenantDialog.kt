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
import ai.sach.rentup.ui.screens.settingsScreen.appprefernecs.AppPreferencesRepository
import ai.sach.rentup.ui.screens.settingsScreen.appprefernecs.getCurrencySymbol
import ai.sach.rentup.ui.screens.settingsScreen.appprefernecs.PhoneNumberFormatter
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlin.math.min
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

// Import the ResponsivePropertySelectionDialog from AddTenantForm
import ai.sach.rentup.ui.common.dialog.ResponsivePropertySelectionDialog
// Import TenantFormFields
import ai.sach.rentup.ui.common.dialog.TenantFormFields

/**
 * Data class to hold all tenant edit state in a single object
 * This reduces the number of individual state variables and improves compiler performance
 */
private data class TenantEditState(
    val name: String = "",
    val dueDate: String = "",
    val status: String = "Due",
    val isPaid: Boolean = false,
    val amount: String = "",
    val avatarUri: Uri? = null,
    val avatarInternalUriString: String? = null,
    val permanentAddress: String = "",
    val phoneNumbers: List<String> = listOf(""),
    val phoneNumberErrors: List<Boolean> = listOf(false),
    val selectedPropertyIds: List<String> = emptyList(),
    val propertyFloorMap: Map<String, String> = emptyMap(),
    val propertyFloorErrors: Map<String, Boolean> = emptyMap(),
    val nameError: Boolean = false,
    val dueDateError: Boolean = false,
    val propertyError: Boolean = false,
    val floorInputError: Boolean = false,
    val anyPhoneNumberError: Boolean = false
)

/**
 * Dialog for editing an existing tenant.
 * Uses the same TenantFormFields as AddTenantForm for consistency.
 * Optimized with consolidated state management to reduce compiler memory usage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTenantDialog(
    tenant: Tenant, // The tenant object being edited
    onDismiss: () -> Unit,
    onUpdateTenant: (Tenant) -> Unit,
    viewModel: TenantViewModel = viewModel(
        factory = TenantViewModelFactory(
            (LocalContext.current.applicationContext as RentUpApplication).tenantRepository,
            (LocalContext.current.applicationContext as RentUpApplication).propertyRepository
        )
    )
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val dialogWidth = if (isLandscape) 0.7f else 0.9f

    // --- Initialize State from Tenant Object ---
    val initialPhoneNumbers = remember { tenant.mobileNumber.split(",").filter { it.isNotBlank() } }
    val initialPropertyIds = remember { tenant.propertyId.split(",").filter { it.isNotBlank() } }
    val initialFloors = remember { TenantFloors.entriesFor(tenant) }

    val initialPropertyFloorMap = remember {
        val map = mutableMapOf<String, String>()
        val count = min(initialPropertyIds.size, initialFloors.size)
        for (i in 0 until count) {
            map[initialPropertyIds[i]] = TenantFloors.display(initialFloors[i])
        }
        if (initialPropertyIds.size > count) {
            for (i in count until initialPropertyIds.size) {
                map[initialPropertyIds[i]] = ""
            }
        }
        map.toMap()
    }

    // Validate initial phone numbers and create error states
    val initialPhoneNumberErrors = remember {
        initialPhoneNumbers.map { phoneNumber ->
            val isValid = isValidPhoneNumber(phoneNumber)
            !isValid
        }
    }

    // --- Consolidated State Management ---
    var editState by remember {
        mutableStateOf(
            TenantEditState(
                name = tenant.name ?: "",
                dueDate = tenant.dueDate ?: "",
                status = tenant.status ?: "Due",
                isPaid = tenant.isPaid,
                amount = tenant.amount?.filter { it.isDigit() || it == '.' } ?: "",
                avatarUri = tenant.avatarUri?.let { Uri.parse(it) },
                avatarInternalUriString = tenant.avatarUri,
                permanentAddress = tenant.permanentAddress ?: "",
                phoneNumbers = initialPhoneNumbers.ifEmpty { listOf("") },
                phoneNumberErrors = initialPhoneNumberErrors.ifEmpty { listOf(false) },
                selectedPropertyIds = initialPropertyIds,
                propertyFloorMap = initialPropertyFloorMap,
                propertyFloorErrors = initialPropertyIds.associateWith { initialPropertyFloorMap[it].isNullOrBlank() },
                nameError = tenant.name.isBlank(),
                dueDateError = !isValidDate(tenant.dueDate),
                propertyError = initialPropertyIds.isEmpty(),
                floorInputError = initialPropertyFloorMap.filterKeys { it in initialPropertyIds }.any { it.value.isBlank() },
                anyPhoneNumberError = initialPhoneNumbers.isEmpty() || initialPhoneNumberErrors.any { it }
            )
        )
    }

    val appPreferencesRepository = remember {
        (context.applicationContext as RentUpApplication).appPreferencesRepository
    }
    val currencyCode by appPreferencesRepository.currency.collectAsState(initial = AppPreferencesRepository.DEFAULT_CURRENCY)
    val currencySymbol = getCurrencySymbol(currencyCode)
    val countryCode by appPreferencesRepository.country.collectAsState(initial = "US")

    // --- Other States ---
    val properties by viewModel.allProperties.observeAsState(emptyList())
    var showPropertyDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = try {
            LocalDate.parse(tenant.dueDate).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
                .toEpochMilli()
        } catch (e: Exception) {
            null
        }
    )

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

    // --- Dialog Composable ---
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = true
        ),
        modifier = Modifier.widthIn(max = configuration.screenWidthDp.dp * dialogWidth),
        title = {
            Text(
                "Edit Tenant: ${tenant.name}",
                fontSize = titleFontSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            TenantFormFields(
                avatarUri = editState.avatarUri,
                onAvatarSelected = { selectedUri ->
                    editState = editState.copy(avatarUri = selectedUri)
                    coroutineScope.launch {
                        processSelectedImage(context, selectedUri, editState.name.ifBlank { "tenant_avatar" }) { savedUriString ->
                            editState = editState.copy(avatarInternalUriString = savedUriString)
                        }
                    }
                },
                tenantName = editState.name,
                onTenantNameChange = { editState = editState.copy(name = it, nameError = it.isBlank()) },
                tenantNameError = editState.nameError,
                phoneNumbers = editState.phoneNumbers,
                phoneNumberErrors = editState.phoneNumberErrors,
                onPhoneNumbersChanged = { numbers, errors ->
                    editState = editState.copy(
                        phoneNumbers = numbers,
                        phoneNumberErrors = errors,
                        anyPhoneNumberError = numbers.all { it.isBlank() } || errors.any { it }
                    )
                },
                anyPhoneNumberError = editState.anyPhoneNumberError,
                selectedPropertyIds = editState.selectedPropertyIds,
                propertyFloorMap = editState.propertyFloorMap,
                propertyFloorErrors = editState.propertyFloorErrors,
                propertyError = editState.propertyError,
                floorInputError = editState.floorInputError,
                availableProperties = properties ?: emptyList(),
                onAddPropertyClicked = { showPropertyDialog = true },
                onPropertyRemoved = { propertyId ->
                    val updatedPropertyIds = editState.selectedPropertyIds - propertyId
                    val updatedPropertyFloorMap = editState.propertyFloorMap - propertyId
                    val updatedPropertyFloorErrors = editState.propertyFloorErrors - propertyId
                    editState = editState.copy(
                        selectedPropertyIds = updatedPropertyIds,
                        propertyFloorMap = updatedPropertyFloorMap,
                        propertyFloorErrors = updatedPropertyFloorErrors,
                        propertyError = updatedPropertyIds.isEmpty(),
                        floorInputError = updatedPropertyFloorMap.filterKeys { it in updatedPropertyIds }.any { it.value.isBlank() }
                    )
                },
                onFloorChanged = { propertyId, newValue ->
                    val updatedPropertyFloorMap = editState.propertyFloorMap + (propertyId to newValue)
                    val updatedPropertyFloorErrors = editState.propertyFloorErrors + (propertyId to newValue.isBlank())
                    editState = editState.copy(
                        propertyFloorMap = updatedPropertyFloorMap,
                        propertyFloorErrors = updatedPropertyFloorErrors,
                        floorInputError = updatedPropertyFloorMap.filterKeys { it in editState.selectedPropertyIds }.any { it.value.isBlank() }
                    )
                },
                dueDate = editState.dueDate,
                onDueDateClick = { showDatePicker = true },
                dueDateError = editState.dueDateError,
                amount = editState.amount,
                onAmountChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) editState = editState.copy(amount = it) },
                currencySymbol = currencySymbol,
                permanentAddress = editState.permanentAddress,
                onPermanentAddressChange = { editState = editState.copy(permanentAddress = it) },
                isPaid = editState.isPaid,
                onPaidChange = { checked -> editState = editState.copy(isPaid = checked, status = if (checked) "Paid" else "Due") },
                status = editState.status,
                countryCode = countryCode,
                textFieldFontSize = textFieldFontSize,
                labelFontSize = labelFontSize,
                errorFontSize = errorFontSize,
                dialogPadding = dialogPadding,
                itemSpacing = itemSpacing,
                manualMode = true,
                onManualModeChange = null,
                showManualModeToggleInTitle = false
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    // Manually trigger validation checks before submission
                    val nameError = editState.name.isBlank()
                    val dueDateError = editState.dueDate.isBlank()
                    val propertyError = editState.selectedPropertyIds.isEmpty()
                    
                    // Validate phone numbers using country-specific rules
                    val phoneNumberValidationErrors = editState.phoneNumbers.map { phoneNumber ->
                        if (phoneNumber.isBlank()) false else {
                            !PhoneNumberFormatter.isValidPhoneNumber(phoneNumber, countryCode)
                        }
                    }
                    val anyPhoneNumberError = editState.phoneNumbers.all { it.isBlank() } || phoneNumberValidationErrors.any { it }
                    
                    val floorInputError = editState.propertyFloorMap.filterKeys { it in editState.selectedPropertyIds }.any { it.value.isBlank() }

                    // Update state with validation results
                    editState = editState.copy(
                        nameError = nameError,
                        dueDateError = dueDateError,
                        propertyError = propertyError,
                        phoneNumberErrors = phoneNumberValidationErrors,
                        anyPhoneNumberError = anyPhoneNumberError,
                        floorInputError = floorInputError
                    )

                    // Check all error flags
                    if (!nameError && !dueDateError && !propertyError && !anyPhoneNumberError && !floorInputError) {
                        // Format phone numbers before saving using country-specific formatting
                        val validPhoneNumbers = editState.phoneNumbers.filter { it.isNotBlank() }
                        val formattedPhoneNumbers = validPhoneNumbers.map { PhoneNumberFormatter.formatPhoneNumber(it, countryCode) }
                        val phoneNumbersString = formattedPhoneNumbers.joinToString(",")
                        val propertyIdsString = editState.selectedPropertyIds.joinToString(",")
                        val floorString = editState.selectedPropertyIds.joinToString(",") { TenantFloors.encode(editState.propertyFloorMap[it] ?: "") }

                        val updatedTenant = tenant.copy(
                            name = editState.name,
                            mobileNumber = phoneNumbersString,
                            status = editState.status,
                            isPaid = editState.isPaid,
                            dueDate = editState.dueDate,
                            propertyId = propertyIdsString,
                            floor = floorString,
                            amount = if (editState.amount.isBlank()) null else "$currencySymbol${editState.amount}",
                            avatarUri = editState.avatarInternalUriString,
                            permanentAddress = editState.permanentAddress
                        )
                        onUpdateTenant(updatedTenant)
                        Toast.makeText(context, "Tenant '${editState.name}' updated", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    } else {
                        Toast.makeText(
                            context,
                            "Please fix errors and fill required fields (*)",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            ) {
                Text("Update Tenant")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
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
                            val selectedLocalDate = java.time.Instant
                                .ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()

                            editState = editState.copy(
                                dueDate = selectedLocalDate.toString(),
                                dueDateError = false
                            )
                        }
                        showDatePicker = false
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showPropertyDialog) {
        ResponsivePropertySelectionDialog(
            properties = properties?.filter { property ->
                !editState.selectedPropertyIds.contains(property.id)
            } ?: emptyList(),
            onDismiss = { showPropertyDialog = false },
            onPropertySelected = { propertyId ->
                val updatedPropertyIds = editState.selectedPropertyIds + propertyId
                val updatedPropertyFloorMap = editState.propertyFloorMap + (propertyId to "")
                val updatedPropertyFloorErrors = editState.propertyFloorErrors + (propertyId to true)
                editState = editState.copy(
                    selectedPropertyIds = updatedPropertyIds,
                    propertyFloorMap = updatedPropertyFloorMap,
                    propertyFloorErrors = updatedPropertyFloorErrors,
                    propertyError = false,
                    floorInputError = true
                )
            }
        )
    }
}



// Validation helper functions
private fun isValidDate(dateString: String): Boolean {
    return try {
        LocalDate.parse(dateString)
        true
    } catch (e: DateTimeParseException) {
        false
    }
}

private fun isValidPhoneNumber(phoneNumber: String): Boolean {
    if (phoneNumber.isBlank()) return true // Allow empty for optional fields
    
    val digits = phoneNumber.filter { it.isDigit() }
    
    // Check if it contains only valid characters
    val validChars = setOf('+', '-', ' ', '(', ')', '.')
    if (!phoneNumber.all { it.isDigit() || it in validChars }) {
        return false
    }
    
    // Check minimum length (7 digits for local numbers, 10+ for international)
    return when {
        phoneNumber.startsWith("+") -> digits.length >= 10 // International format
        digits.length >= 7 -> true // Local format (including 8-digit numbers like 15550466)
        else -> false
    }
}

// Phone number formatting is now handled by PhoneNumberFormatter
