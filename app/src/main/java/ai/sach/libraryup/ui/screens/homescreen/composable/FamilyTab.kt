package ai.sach.libraryup.ui.screens.homescreen.composable

import ai.sach.libraryup.data.local.entity.Tenant
import ai.sach.libraryup.data.local.entity.TenantFamilyInfo
import ai.sach.libraryup.data.local.entity.Property
import ai.sach.libraryup.data.viewmodel.TenantViewModel
import ai.sach.libraryup.ui.common.dialog.AddFamilyMemberDialog
import ai.sach.libraryup.ui.common.dialog.EditFamilyMemberDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign


// FamilyTab.kt
@Composable
fun FamilyTab(
    familyMembers: List<TenantFamilyInfo>,
    tenantId: Long,
    viewModel: TenantViewModel,
    tenant: Tenant,
    availableProperties: List<Property> = emptyList(), // New parameter
    onEditFamilyMember: (TenantFamilyInfo) -> Unit = {}
) {
    // State for dialog visibility
    var showFamilyDialog by remember { mutableStateOf(false) }
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var selectedFamilyMember by remember { mutableStateOf<TenantFamilyInfo?>(null) }

    // Separate family members from emergency contacts
    val regularFamilyMembers = familyMembers.filter { !it.isEmergencyContact }
    val emergencyContactsFromFamily = familyMembers.filter { it.isEmergencyContact && it.mobileNumber != null }
    // Merge with emergency contacts from Tenant entity (from Firestore sync)
    val emergencyContactsFromTenant = tenant.emergencyContacts.map {
        // Map to a pseudo-TenantFamilyInfo for display
        TenantFamilyInfo(
            id = -1, // Use -1 to indicate not from DB
            tenantId = tenant.id,
            name = it.name,
            relationship = it.relationship,
            age = 0,
            isEmergencyContact = true,
            mobileNumber = it.phone,
            address = it.address
        )
    }
    // Merge and deduplicate by phone number
    val mergedEmergencyContacts = (emergencyContactsFromFamily + emergencyContactsFromTenant)
        .distinctBy { it.mobileNumber?.trim() ?: it.name.trim() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {

        // Family Members Section with add button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Family Members",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = { showFamilyDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Family Member")
            }
        }

        // Family members list
        if (regularFamilyMembers.isEmpty()) {
            Text(
                text = "No family members added yet",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        } else {
            regularFamilyMembers.forEach { member ->
                FamilyMemberItem(
                    name = member.name,
                    description = "${member.relationship}, ${member.age} years old",
                    onEditClick = {
                        selectedFamilyMember = member
                        showFamilyDialog = true
                    }
                )
            }
        }

        // Emergency Contacts Section with divider
        HorizontalDivider(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )

        // Emergency Contacts Header with add button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Emergency Contacts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = {
                selectedFamilyMember = null
                showEmergencyDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Emergency Contact")
            }
        }

        // Emergency contacts list
        if (mergedEmergencyContacts.isEmpty()) {
            Text(
                text = "No emergency contacts added yet",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        } else {
            mergedEmergencyContacts.forEach { contact ->
                EmergencyContactItem(
                    name = contact.name,
                    relation = contact.relationship,
                    phone = contact.mobileNumber ?: "",
                    address = contact.address, // Pass address to the composable
                    onEditClick = {
                        selectedFamilyMember = if (contact.id != -1L) contact else null // Only allow edit for DB items
                        showEmergencyDialog = contact.id != -1L // Only allow edit for DB items
                    }
                )
            }
        }
    }

    // Show family member dialog when needed
    if (showFamilyDialog) {
        if (selectedFamilyMember == null) {
            // Add new family member
            AddFamilyMemberDialog(
                tenantId = tenantId,
                onDismiss = { showFamilyDialog = false },
                onAddFamilyMember = { newMember ->
                    viewModel.insertFamilyInfo(newMember)
                    showFamilyDialog = false
                },
                isEmergencyContact = false
            )
        } else {
            // Edit existing family member
            EditFamilyMemberDialog(
                familyMember = selectedFamilyMember!!,
                onDismiss = { showFamilyDialog = false },
                onUpdateFamilyMember = { updatedMember ->
                    viewModel.updateFamilyInfo(updatedMember)
                    showFamilyDialog = false
                },
                onDeleteFamilyMember = { memberToDelete ->
                    viewModel.deleteFamilyInfo(memberToDelete)
                    showFamilyDialog = false
                }
            )
        }
    }

    // Show emergency contact dialog when needed
    if (showEmergencyDialog) {
        if (selectedFamilyMember == null) {
            // Add new emergency contact
            AddFamilyMemberDialog(
                tenantId = tenantId,
                onDismiss = { showEmergencyDialog = false },
                onAddFamilyMember = { newContact ->
                    viewModel.insertFamilyInfo(newContact)
                    showEmergencyDialog = false
                },
                isEmergencyContact = true // This makes it an emergency contact
            )
        } else {
            // Edit existing emergency contact
            EditFamilyMemberDialog(
                familyMember = selectedFamilyMember!!,
                onDismiss = { showEmergencyDialog = false },
                onUpdateFamilyMember = { updatedContact ->
                    viewModel.updateFamilyInfo(updatedContact)
                    showEmergencyDialog = false
                },
                onDeleteFamilyMember = { contactToDelete ->
                    viewModel.deleteFamilyInfo(contactToDelete)
                    showEmergencyDialog = false
                }
            )
        }
    }
}