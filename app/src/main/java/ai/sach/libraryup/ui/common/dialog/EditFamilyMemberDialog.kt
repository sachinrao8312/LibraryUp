package ai.sach.rentup.ui.common.dialog

import ai.sach.rentup.data.local.entity.TenantFamilyInfo
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import ai.sach.rentup.ui.common.composables.ResponsiveMultiplePhoneNumbersSection

// EditFamilyMemberDialog.kt
@Composable
fun EditFamilyMemberDialog(
    familyMember: TenantFamilyInfo,
    onDismiss: () -> Unit,
    onUpdateFamilyMember: (TenantFamilyInfo) -> Unit,
    onDeleteFamilyMember: (TenantFamilyInfo) -> Unit
) {
    var name by remember { mutableStateOf(familyMember.name) }
    var relationship by remember { mutableStateOf(familyMember.relationship) }
    var age by remember { mutableStateOf(if (familyMember.isEmergencyContact) "" else familyMember.age.toString()) }
    var mobileNumber by remember { mutableStateOf(familyMember.mobileNumber ?: "") }
    var phoneNumbers by remember { mutableStateOf(listOf(mobileNumber)) }
    var phoneNumberErrors by remember { mutableStateOf(listOf(false)) }

    val isEmergencyContact = familyMember.isEmergencyContact

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEmergencyContact) "Edit Emergency Contact" else "Edit Family Member") },
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

                // Show age field only for family members, not emergency contacts
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
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = {
                        onDeleteFamilyMember(familyMember)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Delete")
                }

                Button(
                    onClick = {
                        if (name.isNotBlank() && relationship.isNotBlank() &&
                            (!isEmergencyContact || phoneNumbers.firstOrNull()?.isNotBlank() == true)) {
                            val ageValue = if (isEmergencyContact) 0 else (age.toIntOrNull() ?: 0)
                            val updatedMember = familyMember.copy(
                                name = name,
                                relationship = relationship,
                                age = ageValue,
                                mobileNumber = if (isEmergencyContact) phoneNumbers.firstOrNull() else null
                            )
                            onUpdateFamilyMember(updatedMember)
                        }
                    },
                    enabled = name.isNotBlank() && relationship.isNotBlank() &&
                            (!isEmergencyContact || phoneNumbers.firstOrNull()?.isNotBlank() == true)
                ) {
                    Text("Update")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}