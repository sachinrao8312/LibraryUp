package ai.sach.libraryup.ui.common.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun PropertyFormFields(
    propertyName: String,
    onPropertyNameChange: (String) -> Unit,
    propertyNameError: String?,
    address: String,
    onAddressChange: (String) -> Unit,
    addressError: String?,
    totalUnits: String,
    onTotalUnitsChange: (String) -> Unit,
    totalUnitsError: String?,
    floors: String,
    onFloorsChange: (String) -> Unit,
    floorsError: String?,
    notes: String,
    onNotesChange: (String) -> Unit,
    isDarkTheme: Boolean,
    textColor: Color,
    borderColor: Color,
    errorColor: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = propertyName,
            onValueChange = onPropertyNameChange,
            label = { Text("Property Name") },
            placeholder = { Text("e.g., Green Valley Apartments") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            leadingIcon = {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "Property",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            isError = propertyNameError != null,
            supportingText = {
                if (propertyNameError != null) {
                    Text(
                        text = propertyNameError,
                        color = errorColor
                    )
                } else {
                    Text(
                        text = "Give your building a name that's easy to remember",
                        color = textColor.copy(alpha = 0.6f)
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = borderColor,
                errorBorderColor = errorColor,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor
            )
        )
        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            label = { Text("Address") },
            placeholder = { Text("123 Main Street, City, State") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            leadingIcon = {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Address",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            isError = addressError != null,
            supportingText = {
                if (addressError != null) {
                    Text(
                        text = addressError,
                        color = errorColor
                    )
                } else {
                    Text(
                        text = "Where is your building located? Include street, city, and state",
                        color = textColor.copy(alpha = 0.6f)
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = borderColor,
                errorBorderColor = errorColor,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor
            )
        )
        OutlinedTextField(
            value = floors,
            onValueChange = onFloorsChange,
            label = { Text("Number of Floors") },
            placeholder = { Text("e.g., 3") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            leadingIcon = {
                Icon(
                    Icons.Default.Layers,
                    contentDescription = "Floors",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            isError = floorsError != null,
            supportingText = {
                if (floorsError != null) {
                    Text(
                        text = floorsError,
                        color = errorColor
                    )
                } else {
                    Text(
                        text = "How many floors does your building have? (Count ground floor as 1)",
                        color = textColor.copy(alpha = 0.6f)
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = borderColor,
                errorBorderColor = errorColor,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor
            )
        )
        OutlinedTextField(
            value = totalUnits,
            onValueChange = onTotalUnitsChange,
            label = { Text("Total Units") },
            placeholder = { Text("e.g., 12") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            leadingIcon = {
                Icon(
                    Icons.Default.Apartment,
                    contentDescription = "Units",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            isError = totalUnitsError != null,
            supportingText = {
                if (totalUnitsError != null) {
                    Text(
                        text = totalUnitsError,
                        color = errorColor
                    )
                } else {
                    Text(
                        text = "Units are separate rooms/apartments, people can rent in your property (like apartment 1A, 2B, etc.)",
                        color = textColor.copy(alpha = 0.6f)
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = borderColor,
                errorBorderColor = errorColor,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor
            )
        )
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Notes (Optional)") },
            placeholder = { Text("Any extra details about your property...") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = RoundedCornerShape(8.dp),
            leadingIcon = {
                Icon(
                    Icons.AutoMirrored.Filled.Notes,
                    contentDescription = "Notes",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            minLines = 3,
            supportingText = {
                Text(
                    text = "Add any extra info like amenities, parking, or special features",
                    color = textColor.copy(alpha = 0.6f)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = borderColor,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor
            )
        )
    }
} 