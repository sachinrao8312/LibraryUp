package ai.sach.libraryup.ui.common.dialog

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material3.rememberModalBottomSheetState
import ai.sach.libraryup.ui.common.dialog.PropertyFormFields

private const val TAG = "AddPropertyForm"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPropertyForm(
    onDismiss: () -> Unit,
    onAddProperty: (Map<String, String>) -> Unit,
    isDarkTheme: Boolean
) {
    // Define colors from the second component
    val surfaceColor = if (isDarkTheme) Color(0xFF1F2937) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val borderColor = if (isDarkTheme) Color(0xFF374151) else Color(0xFFE5E7EB)
    val errorColor = if (isDarkTheme) Color(0xFFEF5350) else Color(0xFFB71C1C)

    // Form state
    var propertyName by remember { mutableStateOf("") }
    var propertyNameError by remember { mutableStateOf<String?>(null) }

    var address by remember { mutableStateOf("") }
    var addressError by remember { mutableStateOf<String?>(null) }

    var totalUnits by remember { mutableStateOf("") }
    var totalUnitsError by remember { mutableStateOf<String?>(null) }

    var floors by remember { mutableStateOf("") }
    var floorsError by remember { mutableStateOf<String?>(null) }

    var notes by remember { mutableStateOf("") }

    // Track form validity
    var isFormValid by remember { mutableStateOf(false) }

    // Validation functions
    val validatePropertyName = { value: String ->
        ValidationUtils.validatePropertyName(value)
    }

    val validateAddress = { value: String ->
        ValidationUtils.validateAddress(value)
    }

    val validateTotalUnits = { value: String ->
        ValidationUtils.validatePositiveNumber(value, "Total units")
    }

    val validateFloors = { value: String ->
        ValidationUtils.validatePositiveNumber(value, "Number of floors")
    }

    // Function to check if form is valid
    val checkFormValidity = {
        val valid = propertyNameError == null && propertyName.isNotBlank() &&
                addressError == null && address.isNotBlank() &&
                totalUnitsError == null && totalUnits.isNotBlank() &&
                floorsError == null && floors.isNotBlank()

        Log.d(TAG, "Form validity check: $valid")
        Log.d(TAG, "Property name: '$propertyName', Error: $propertyNameError")
        Log.d(TAG, "Address: '$address', Error: $addressError")
        Log.d(TAG, "Total units: '$totalUnits', Error: $totalUnitsError")
        Log.d(TAG, "Floors: '$floors', Error: $floorsError")

        isFormValid = valid
    }

    // Initialize form validation
    LaunchedEffect(Unit) {
        Log.d(TAG, "Form initialized")
    }

    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = {
            Log.d(TAG, "Form dismissed")
            onDismiss()
        },
        containerColor = surfaceColor,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(min = 400.dp, max = 700.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Add New Property",
                style = MaterialTheme.typography.headlineSmall,
                color = textColor
            )

            Text(
                "Fill in the details about your building or property",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor.copy(alpha = 0.7f)
            )

            // Make the form fields scrollable
            val scrollState = rememberScrollState()
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                PropertyFormFields(
                    propertyName = propertyName,
                    onPropertyNameChange = { value ->
                        propertyName = value
                        propertyNameError = validatePropertyName(value)
                        checkFormValidity()
                    },
                    propertyNameError = propertyNameError,
                    address = address,
                    onAddressChange = { value ->
                        address = value
                        addressError = validateAddress(value)
                        checkFormValidity()
                    },
                    addressError = addressError,
                    totalUnits = totalUnits,
                    onTotalUnitsChange = { value ->
                        totalUnits = value
                        totalUnitsError = validateTotalUnits(value)
                        checkFormValidity()
                    },
                    totalUnitsError = totalUnitsError,
                    floors = floors,
                    onFloorsChange = { value ->
                        floors = value
                        floorsError = validateFloors(value)
                        checkFormValidity()
                    },
                    floorsError = floorsError,
                    notes = notes,
                    onNotesChange = { value -> notes = value },
                    isDarkTheme = isDarkTheme,
                    textColor = textColor,
                    borderColor = borderColor,
                    errorColor = errorColor
                )
            }
            // Action buttons always visible at the bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        Log.d(TAG, "Cancel button clicked")
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        Log.d(TAG, "Submit button clicked")

                        // Run validations one more time before submitting
                        propertyNameError = validatePropertyName(propertyName)
                        addressError = validateAddress(address)
                        totalUnitsError = validateTotalUnits(totalUnits)
                        floorsError = validateFloors(floors)

                        // Check form validity after validation
                        checkFormValidity()

                        // Submit if valid
                        if (isFormValid) {
                            Log.d(TAG, "Form is valid, submitting data")
                            val propertyData = mapOf(
                                "propertyName" to propertyName,
                                "address" to address,
                                "totalUnits" to totalUnits,
                                "floors" to floors,
                                "notes" to notes
                            )
                            Log.d(TAG, "Property data: $propertyData")
                            onAddProperty(propertyData)
                        } else {
                            Log.e(TAG, "Form validation failed on submit")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    ),
                    shape = CircleShape,
                    enabled = isFormValid
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Property")
                    }
                }
            }
        }
    }

    // Log when form is recomposed
    Log.d(TAG, "Form recomposed, form valid: $isFormValid")
}