package ai.sach.libraryup.ui.common.dialog

import ai.sach.libraryup.data.local.entity.TenantFamilyInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ai.sach.libraryup.ui.common.composables.ResponsiveMultiplePhoneNumbersSection

// AddFamilyMemberDialog.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFamilyMemberDialog(
    tenantId: Long,
    onDismiss: () -> Unit,
    onAddFamilyMember: (TenantFamilyInfo) -> Unit,
    isEmergencyContact: Boolean = false
) {
    var name by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var age by remember { mutableStateOf(if (isEmergencyContact) "" else "0") }
    var mobileNumber by remember { mutableStateOf("") }
    var propertyInput by remember { mutableStateOf("") } // For emergency contact property
    var phoneNumbers by remember { mutableStateOf(listOf(mobileNumber)) }
    var phoneNumberErrors by remember { mutableStateOf(listOf(false)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEmergencyContact) "Add Emergency Contact" else "Add Family Member") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text("Relationship") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isEmergencyContact) {
                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it },
                        label = { Text("Age") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                // Show phone field for emergency contacts
                if (isEmergencyContact) {
                    ResponsiveMultiplePhoneNumbersSection(
                        phoneNumbers = phoneNumbers,
                        phoneNumberErrors = phoneNumberErrors,
                        onPhoneNumbersChanged = { numbers, errors ->
                            phoneNumbers = numbers
                            phoneNumberErrors = errors
                        }
                    )
                    OutlinedTextField(
                        value = propertyInput,
                        onValueChange = { propertyInput = it },
                        label = { Text("Property/Unit") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && relationship.isNotBlank() &&
                        (!isEmergencyContact || phoneNumbers.firstOrNull()?.isNotBlank() == true)) {
                        val ageValue = if (isEmergencyContact) 0 else (age.toIntOrNull() ?: 0)
                        val familyMember = TenantFamilyInfo(
                            tenantId = tenantId,
                            name = name,
                            relationship = relationship,
                            age = ageValue,
                            isEmergencyContact = isEmergencyContact,
                            mobileNumber = if (isEmergencyContact) phoneNumbers.firstOrNull() else null,
                            propertyId = if (isEmergencyContact) propertyInput else null
                        )
                        onAddFamilyMember(familyMember)
                    }
                },
                enabled = name.isNotBlank() && relationship.isNotBlank() &&
                        (!isEmergencyContact || phoneNumbers.firstOrNull()?.isNotBlank() == true)
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}



