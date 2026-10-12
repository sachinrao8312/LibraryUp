package ai.sach.libraryup.ui.common.dialog

import ai.sach.libraryup.data.local.entity.Property
import ai.sach.libraryup.ui.common.composables.AvatarSelectionSection
import ai.sach.libraryup.ui.common.responsiveness.LocalAccessibilityScaling
import ai.sach.libraryup.ui.common.responsiveness.LocalDimensions
import ai.sach.libraryup.ui.screens.settingsScreen.appprefernecs.PhoneNumberFormatter
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantFormFields(
    avatarUri: Uri?,
    onAvatarSelected: (Uri?) -> Unit,
    tenantName: String,
    onTenantNameChange: (String) -> Unit,
    tenantNameError: Boolean,
    phoneNumbers: List<String>,
    phoneNumberErrors: List<Boolean>,
    onPhoneNumbersChanged: (List<String>, List<Boolean>) -> Unit,
    anyPhoneNumberError: Boolean,
    selectedPropertyIds: List<String>,
    propertyFloorMap: Map<String, String>,
    propertyFloorErrors: Map<String, Boolean>,
    propertyError: Boolean,
    floorInputError: Boolean,
    availableProperties: List<Property>,
    onAddPropertyClicked: () -> Unit,
    onPropertyRemoved: (String) -> Unit,
    onFloorChanged: (String, String) -> Unit,
    dueDate: String,
    onDueDateClick: () -> Unit,
    dueDateError: Boolean,
    amount: String,
    onAmountChange: (String) -> Unit,
    currencySymbol: String,
    permanentAddress: String?,
    onPermanentAddressChange: ((String) -> Unit)? = null,
    isPaid: Boolean,
    onPaidChange: (Boolean) -> Unit,
    status: String,
    countryCode: String = "US", // Default to US if not provided
    textFieldFontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    labelFontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    errorFontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    dialogPadding: Dp = 16.dp,
    itemSpacing: Dp = 8.dp,
    manualMode: Boolean = true,
    onManualModeChange: ((Boolean) -> Unit)? = null,
    showManualModeToggleInTitle: Boolean = false
) {
    val dimensions = LocalDimensions.current
    val accessibilityScaling = LocalAccessibilityScaling.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dialogPadding, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showManualModeToggleInTitle) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Switch(
                    checked = manualMode,
                    onCheckedChange = onManualModeChange
                )
                Text(
                    text = "Show photo and permanent address fields",
                    fontSize = labelFontSize,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        if (manualMode) {
            AvatarSelectionSection(
                avatarUri = avatarUri,
                onAvatarSelected = onAvatarSelected,
                tenantName = tenantName
            )
        }
        OutlinedTextField(
            value = tenantName,
            onValueChange = onTenantNameChange,
            label = { Text("Tenant Name*", fontSize = labelFontSize) },
            modifier = Modifier.fillMaxWidth(),
            isError = tenantNameError,
            textStyle = TextStyle(fontSize = textFieldFontSize),
            supportingText = {
                if (tenantNameError) Text(
                    "Name is required",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = errorFontSize
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(dimensions.iconSizeMedium * accessibilityScaling.contentScale)
                )
            }
        )
        // Phone Numbers Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Phone Numbers*",
                fontWeight = FontWeight.Bold,
                fontSize = labelFontSize,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    val newList = phoneNumbers + ""
                    val newErrors = phoneNumberErrors + false
                    onPhoneNumbersChanged(newList, newErrors)
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                shape = RoundedCornerShape(50),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Add Number", fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
            }
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            phoneNumbers.forEachIndexed { idx, number ->
                OutlinedTextField(
                    value = number,
                    onValueChange = { newValue ->
                        // Allow any input while typing, but validate on submission
                        val updated = phoneNumbers.toMutableList(); updated[idx] = newValue
                        val updatedErr = phoneNumberErrors.toMutableList(); updatedErr[idx] = false // Don't show errors while typing
                        onPhoneNumbersChanged(updated, updatedErr)
                    },
                    label = { Text("Phone Number ${idx + 1}", fontSize = labelFontSize) },
                    placeholder = { Text(PhoneNumberFormatter.getPlaceholder(countryCode), fontSize = labelFontSize, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                    isError = phoneNumberErrors[idx],
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (phoneNumbers.size > 1) {
                            IconButton(onClick = {
                                val updated = phoneNumbers.toMutableList(); updated.removeAt(idx)
                                val updatedErr = phoneNumberErrors.toMutableList(); updatedErr.removeAt(idx)
                                onPhoneNumbersChanged(updated, updatedErr)
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                )
                if (phoneNumberErrors[idx]) {
                    val phoneNumber = phoneNumbers[idx]
                    val errorMessage = PhoneNumberFormatter.getValidationErrorMessage(phoneNumber, countryCode)
                    Text(
                        errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = errorFontSize,
                        modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                    )
                }
            }
            if (anyPhoneNumberError && phoneNumbers.any { it.isNotBlank() }) {
                Text(
                    "Please fix the phone number errors above",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = errorFontSize,
                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                )
            } else if (phoneNumbers.all { it.isBlank() }) {
                Text(
                    "At least one phone number is required",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = errorFontSize,
                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, modifier = Modifier.padding(vertical = 6.dp))
        
        // Properties Section with improved UI
        PropertiesSection(
            selectedPropertyIds = selectedPropertyIds,
            propertyFloorMap = propertyFloorMap,
            propertyFloorErrors = propertyFloorErrors,
            propertyError = propertyError,
            floorInputError = floorInputError,
            availableProperties = availableProperties,
            onAddPropertyClicked = onAddPropertyClicked,
            onPropertyRemoved = onPropertyRemoved,
            onFloorChanged = onFloorChanged,
            textFieldFontSize = textFieldFontSize,
            labelFontSize = labelFontSize,
            errorFontSize = errorFontSize
        )
        HorizontalDivider(thickness = 1.dp, modifier = Modifier.padding(vertical = 6.dp))
        OutlinedTextField(
            value = dueDate,
            onValueChange = { /* Set via picker */ },
            readOnly = true,
            label = { Text("Due Date* (YYYY-MM-DD)", fontSize = labelFontSize) },
            modifier = Modifier.fillMaxWidth(),
            isError = dueDateError,
            textStyle = TextStyle(fontSize = textFieldFontSize),
            supportingText = {
                if (dueDateError) Text(
                    "Due date is required",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = errorFontSize
                )
            },
            trailingIcon = {
                IconButton(onClick = onDueDateClick) {
                    Icon(
                        Icons.Default.DateRange,
                        "Select Date",
                        modifier = Modifier.size(dimensions.iconSizeMedium * accessibilityScaling.contentScale)
                    )
                }
            }
        )
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            label = { Text("Rent Amount", fontSize = labelFontSize) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = TextStyle(fontSize = textFieldFontSize),
            leadingIcon = {
                Text(
                    currencySymbol,
                    fontSize = textFieldFontSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        if (permanentAddress != null && onPermanentAddressChange != null) {
            if (manualMode) {
                OutlinedTextField(
                    value = permanentAddress,
                    onValueChange = onPermanentAddressChange,
                    label = { Text("Permanent Address", fontSize = labelFontSize) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontSize = textFieldFontSize)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isPaid,
                onCheckedChange = onPaidChange
            )
            Text(
                text = "Mark as Paid",
                fontSize = textFieldFontSize,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun PropertiesSection(
    selectedPropertyIds: List<String>,
    propertyFloorMap: Map<String, String>,
    propertyFloorErrors: Map<String, Boolean>,
    propertyError: Boolean,
    floorInputError: Boolean,
    availableProperties: List<Property>,
    onAddPropertyClicked: () -> Unit,
    onPropertyRemoved: (String) -> Unit,
    onFloorChanged: (String, String) -> Unit,
    textFieldFontSize: androidx.compose.ui.unit.TextUnit,
    labelFontSize: androidx.compose.ui.unit.TextUnit,
    errorFontSize: androidx.compose.ui.unit.TextUnit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Enhanced header with better styling
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Properties & Units*",
                fontWeight = FontWeight.Bold,
                fontSize = labelFontSize,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onAddPropertyClicked,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add", fontSize = 13.sp)
            }
        }

        // Compact property list with better empty state
        if (selectedPropertyIds.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAddPropertyClicked() }
                    .background(
                        color = if (propertyError) 
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                        else 
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.HomeWork,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (propertyError) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Tap to select properties",
                    fontSize = 13.sp,
                    color = if (propertyError) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                selectedPropertyIds.forEach { propertyId ->
                    CompactPropertyRow(
                        propertyId = propertyId,
                        property = availableProperties.find { it.id == propertyId },
                        floorValue = propertyFloorMap[propertyId] ?: "",
                        isFloorError = propertyFloorErrors[propertyId] ?: false,
                        onFloorChanged = { newValue -> onFloorChanged(propertyId, newValue) },
                        onRemoveProperty = { onPropertyRemoved(propertyId) },
                        textFieldFontSize = textFieldFontSize,
                        labelFontSize = labelFontSize
                    )
                }
            }
        }

        // Compact error messages
        if (propertyError) {
            Text(
                "At least one property must be selected",
                color = MaterialTheme.colorScheme.error,
                fontSize = errorFontSize,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        } else if (floorInputError) {
            Text(
                "Floor/Unit required for all properties",
                color = MaterialTheme.colorScheme.error,
                fontSize = errorFontSize,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}

@Composable
private fun CompactPropertyRow(
    propertyId: String,
    property: Property?,
    floorValue: String,
    isFloorError: Boolean,
    onFloorChanged: (String) -> Unit,
    onRemoveProperty: () -> Unit,
    textFieldFontSize: androidx.compose.ui.unit.TextUnit,
    labelFontSize: androidx.compose.ui.unit.TextUnit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Property chip with better styling
            InputChip(
                selected = true,
                onClick = { /* No-op */ },
                label = {
                    Text(
                        property?.name ?: "Property $propertyId",
                        maxLines = 1,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.HomeWork,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                },
                colors = InputChipDefaults.inputChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    leadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.weight(0.6f)
            )

            // Floor input with better proportions
            OutlinedTextField(
                value = floorValue,
                onValueChange = onFloorChanged,
                label = { Text("Floor/Unit*", fontSize = 11.sp) },
                placeholder = { Text("e.g. 2A or 1, 2", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.weight(0.3f),
                isError = isFloorError,
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp),
                shape = RoundedCornerShape(8.dp)
            )

            // Remove button with better styling
            IconButton(
                onClick = onRemoveProperty,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove ${property?.name ?: "property"}",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
        
        // Inline error message with better styling
        if (isFloorError) {
            Text(
                "Floor/Unit required",
                color = MaterialTheme.colorScheme.error,
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 12.dp, top = 2.dp)
            )
        }
    }
}

// Phone number formatting is now handled by PhoneNumberFormatter