package ai.sach.rentup.ui.screens.homescreen.composable

import ai.sach.rentup.data.local.entity.Tenant
import ai.sach.rentup.data.local.entity.TenantFloors
import ai.sach.rentup.data.local.entity.TenantFamilyInfo
import ai.sach.rentup.data.local.entity.Property
import ai.sach.rentup.data.viewmodel.TenantViewModel
import ai.sach.rentup.ui.common.dialog.AddFamilyMemberDialog
import ai.sach.rentup.ui.common.dialog.EditFamilyMemberDialog
import ai.sach.rentup.ui.common.dialog.EditTenantDialog
import ai.sach.rentup.ui.screens.homescreen.TenantAvatar
import ai.sach.rentup.ui.screens.homescreen.documents.DocumentsTab
import ai.sach.rentup.ui.screens.settingsScreen.appprefernecs.formatWithCurrency
import ai.sach.rentup.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Pre-computes property data to minimize recompositions
 */
private data class TenantPropertyData(
    val tenantProperties: List<Pair<Property, String>>,
    val phoneNumbers: List<String>
)

private fun computeTenantPropertyData(
    tenant: Tenant,
    allProperties: List<Property>
): TenantPropertyData {
    val propertyIds = tenant.propertyId.split(",").filter { it.isNotBlank() }
    val floorList = TenantFloors.entriesFor(tenant)

    val tenantProperties = propertyIds.mapIndexedNotNull { index, propId ->
        val property = allProperties.find { it.id == propId }
        val floor = if (index < floorList.size) TenantFloors.display(floorList[index]) else "N/A"
        if (property != null) {
            Pair(property, floor)
        } else null
    }

    val phoneNumbers = tenant.mobileNumber.split(",").filter { it.isNotBlank() }

    return TenantPropertyData(tenantProperties, phoneNumbers)
}

/**
 * Main composable that displays a bottom sheet with tenant details.
 * Shows a button to trigger the bottom sheet and handles its state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantDetailBottomSheet(
    tenant: Tenant,
    familyMembers: List<TenantFamilyInfo>,
    onClose: () -> Unit,
    viewModel: TenantViewModel
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()

    // Define theme colors - computed once
    val colors = remember(isDarkTheme) {
        object {
            val backgroundColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFF8F9FA)
            val textColor = if (isDarkTheme) Color.White else Color.Black
            val secondaryTextColor = if (isDarkTheme) Color(0xFFB0B0B0) else Color.Gray
        }
    }

    // Get all properties from viewModel with efficient observation
    val allProperties by viewModel.allProperties.observeAsState(emptyList())

    // Pre-compute tenant property data
    val propertyData = remember(tenant, allProperties) {
        computeTenantPropertyData(tenant, allProperties)
    }
    val phoneNumbers = propertyData.phoneNumbers
    val tenantProperties = propertyData.tenantProperties

    // Observe documents for the selected tenant
    val documents by viewModel.selectedTenantDocuments.observeAsState(emptyList())

    var selectedTab by remember { mutableIntStateOf(0) }

    var isEditFamilyMemberDialogVisible by remember { mutableStateOf(false) }
    var selectedFamilyMember by remember { mutableStateOf<TenantFamilyInfo?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showEditTenantDialog by remember { mutableStateOf(false) }
    var showOnboardingLinkDialog by remember { mutableStateOf(false) }
    var onboardingLink by remember { mutableStateOf("") }
    var onboardingLinkLoading by remember { mutableStateOf(false) }
    val onboardingLinkResult by viewModel.onboardingLinkResult.collectAsState()

    androidx.compose.runtime.LaunchedEffect(onboardingLinkResult) {
        val result = onboardingLinkResult ?: return@LaunchedEffect
        if (!onboardingLinkLoading) return@LaunchedEffect
        onboardingLinkLoading = false
        viewModel.consumeOnboardingLinkResult()
        if (result.success && result.url.isNotBlank()) {
            onboardingLink = result.url
            showOnboardingLinkDialog = true
        } else {
            scope.launch { snackbarHostState.showSnackbar("Could not generate the tenant link. Try again.") }
        }
    }

//    val appPreferencesRepository = remember {
//        (context.applicationContext as RentUpApplication).appPreferencesRepository
//    }
//    val currencyCode by appPreferencesRepository.currency.collectAsState(initial = AppPreferencesRepository.DEFAULT_CURRENCY)
//    val currencySymbol = getCurrencySymbol(currencyCode)

    // Use lazy init for expensive composables
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)


    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = bottomSheetState,
        containerColor = colors.backgroundColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(bottom = 56.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar with close, edit, delete buttons
                TopAppBar(
                    title = {
                        Text(
                            tenant.name + " Details",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = colors.textColor
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = colors.textColor
                            )
                        }
                    },
                    actions = {
                        
                        
                        IconButton(onClick = { showEditTenantDialog = true }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Tenant",
                                tint = colors.textColor
                            )
                        }
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Tenant",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.backgroundColor,
                        titleContentColor = colors.textColor,
                        navigationIconContentColor = colors.textColor,
                        actionIconContentColor = colors.textColor
                    )
                )

                // Scrollable content area
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Takes remaining space
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp) // Add horizontal padding here
                ) {
                    // Tenant Avatar and Key Details
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), // Increased vertical padding
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TenantAvatar(modifier = Modifier.size(80.dp), avatarUri = tenant.avatarUri, isDarkTheme = isDarkTheme)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tenant.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = colors.textColor)
                            Text(
                                text = tenant.status, // Directly use status field
                                color = if (tenant.isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium, fontSize = 14.sp
                            )
                            Text("Due Date: ${tenant.dueDate}", color = colors.secondaryTextColor, fontSize = 14.sp)
                        }
                    }

                    // Rent Amount Card (if applicable)
                    if (tenant.amount != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isDarkTheme)
                                MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF5F7FA))
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Payments,
                                    contentDescription = "Rent Amount",
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Monthly Rent", fontWeight = FontWeight.Medium, color = colors.textColor)
                                    Text(formatWithCurrency(tenant.amount), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                        }
                    }

                    // Contact Numbers Section
                    if (phoneNumbers.isNotEmpty()) {
                        SectionTitle(title = "Contact Information", textColor = colors.textColor)
                        phoneNumbers.forEachIndexed { index, phoneNumber ->
                            PhoneNumberItem(
                                phoneNumber = phoneNumber,
                                label = if (phoneNumbers.size > 1) "Phone ${index + 1}" else "Phone",
                                context = context
                            )
                        }
                    }

                    // Properties Section
                    if (tenantProperties.isNotEmpty()) {
                        SectionTitle(title = "Rental Properties (${tenantProperties.size})", textColor = colors.textColor)
                        // Add icon to visually indicate multiple properties if more than one
                        if (tenantProperties.size > 1) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Multiple Properties",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "This tenant is renting ${tenantProperties.size} properties",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Display each property with its floor
                        tenantProperties.forEach { (property, floor) ->
                            PropertyItem(
                                property = property,
                                unitNumber = floor, // Pass the specific floor for this property
                                textColor = colors.textColor,
                                backgroundColor = if (isDarkTheme) Color(0xFF2A2A2A) else Color(0xFFF5F7FA)
                            )
                        }
                    }

                    // Permanent Address Section (moved below properties)
                    if (tenant.permanentAddress.isNotBlank()) {
                        SectionTitle(title = "Permanent Address", textColor = colors.textColor)
                        Text(
                            text = tenant.permanentAddress,
                            color = colors.secondaryTextColor,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    Button(
                        onClick = {
                            val propertyAddress = tenantProperties.firstOrNull()?.first?.address
                                ?.takeIf { it.isNotBlank() } ?: tenant.permanentAddress
                            if (propertyAddress.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar("Add a property address before generating a link.") }
                            } else {
                                scope.launch {
                                    try {
                                        val ownerId = FirebaseAuth.getInstance().currentUser?.uid
                                            ?: error("Not signed in")
                                        val deviceToken = FirebaseMessaging.getInstance().token.await()
                                        onboardingLinkLoading = true
                                        viewModel.generateOnboardingFormLink(
                                            ownerId = ownerId,
                                            propertyAddress = propertyAddress,
                                            deviceToken = deviceToken,
                                            baseUrl = BuildConfig.BACKEND_BASE_URL,
                                            tenantId = tenant.id.toString()
                                        )
                                    } catch (_: Exception) {
                                        onboardingLinkLoading = false
                                        scope.launch { snackbarHostState.showSnackbar("Could not generate the tenant link. Try again.") }
                                    }
                                }
                            }
                        },
                        enabled = !onboardingLinkLoading,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text(if (onboardingLinkLoading) "Generating Link…" else "Generate Tenant Link")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) // Reduced padding for tighter spacing

                    // Tabs (Family/Documents)
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = if (isDarkTheme) Color(0xFF1F2937) else Color(0xFFFFFFFF),
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Family", color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else colors.textColor) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Documents", color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else colors.textColor) }
                        )
                    }

                    // Tab Content
                    when (selectedTab) {
                        0 -> FamilyTab(
                            familyMembers = familyMembers,
                            tenantId = tenant.id,
                            viewModel = viewModel,
                            tenant = tenant, // Pass tenant if needed by Add/Edit dialogs
                            availableProperties = allProperties, // Pass all properties
                            onEditFamilyMember = { member ->
                                selectedFamilyMember = member
                                isEditFamilyMemberDialogVisible = true
                            },
                        )
                        1 -> DocumentsTab(
                            documents = documents,
                            tenantId = tenant.id,
                            viewModel = viewModel,
                            tenantName = tenant.name, // Pass tenant name
                            tenantAvatarUri = tenant.avatarUri // Pass avatarUri
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp)) // Add space at the bottom of scrollable content
                }
            }

            // Snackbar Host anchored to the bottom of the Box
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
                // .padding(bottom = 16.dp) // Padding applied to Box already
            )
        } // End Box

        // --- Dialogs ---
        // Edit/Add Family Member Dialog
        if (isEditFamilyMemberDialogVisible) {
            if (selectedFamilyMember != null) {
                EditFamilyMemberDialog(
                    familyMember = selectedFamilyMember!!,
                    onDismiss = { isEditFamilyMemberDialogVisible = false },
                    onUpdateFamilyMember = { updatedMember -> viewModel.updateFamilyInfo(updatedMember); isEditFamilyMemberDialogVisible = false },
                    onDeleteFamilyMember = { memberToDelete -> viewModel.deleteFamilyInfo(memberToDelete); isEditFamilyMemberDialogVisible = false }
                )
            } else {
                AddFamilyMemberDialog(
                    tenantId = tenant.id,
                    onDismiss = { isEditFamilyMemberDialogVisible = false },
                    onAddFamilyMember = { newMember -> viewModel.insertFamilyInfo(newMember); isEditFamilyMemberDialogVisible = false }
                )
            }
        }

        // Delete Tenant Confirmation Dialog
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete Tenant", color = colors.textColor) },
                text = { Text("Are you sure you want to delete ${tenant.name}? This action cannot be undone.", color = colors.textColor) },
                confirmButton = {
                    Button(
                        onClick = {
                            val deletedTenant = tenant // Store for potential undo
                            viewModel.deleteTenant(tenant)
                            showDeleteConfirmation = false
                            onClose() // Close bottom sheet
                            scope.launch { // Show undo snackbar
                                val result = snackbarHostState.showSnackbar(message = "Tenant deleted", actionLabel = "UNDO", duration = SnackbarDuration.Long)
                                if (result == SnackbarResult.ActionPerformed) { viewModel.insertTenant(deletedTenant) } // Restore
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") } }
            )
        }

        // Edit Tenant Dialog (Launched from Edit button)
        if (showEditTenantDialog) {
            EditTenantDialog( // Use the existing EditTenantDialog composable
                tenant = tenant,
                onDismiss = { showEditTenantDialog = false },
                onUpdateTenant = { updatedTenant ->
                    viewModel.updateTenant(updatedTenant)
                    // Optionally: onClose() // Dismiss bottom sheet as well? Or keep it open? Keeping it open for now.
                    scope.launch {
                        delay(500) // Wait for DB operation and refresh
                        showEditTenantDialog = false
                        // Refresh again after dismissal
                        viewModel.refreshTenantData()
                    }
                },
                viewModel = viewModel
            )
        }

        if (showOnboardingLinkDialog && onboardingLink.isNotBlank()) {
            AlertDialog(
                onDismissRequest = { showOnboardingLinkDialog = false },
                title = { Text("Tenant Onboarding Link Ready") },
                text = { Text(onboardingLink) },
                confirmButton = {
                    Button(onClick = {
                        shareOnboardingLinkGeneric(context, onboardingLink)
                        showOnboardingLinkDialog = false
                    }) { Text("Share Link") }
                },
                dismissButton = {
                    TextButton(onClick = { showOnboardingLinkDialog = false }) { Text("Close") }
                }
            )
        }

    } // End ModalBottomSheet
}

@Composable
fun SectionTitle(title: String, textColor: Color) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        color = textColor
    )
}

// Update the PropertyItem function to include theme-aware text colors:
@Composable
fun PropertyItem(
    property: Property,
    unitNumber: String,
    textColor: Color,
    backgroundColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                    imageVector = Icons.Default.Home,
                    contentDescription = "Property",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = property.name,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor
                )
                Text(
                    text = property.address,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Unit: $unitNumber",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun PhoneNumberItem(
    phoneNumber: String,
    label: String = "Phone",
    context: Context
) {
    // Check current theme to set appropriate colors
    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val secondaryTextColor = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Gray

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable {
                // Create intent to dial the number
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                }
                // Verify the intent can be resolved before launching
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else Color(0xFFF3F4F6)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Blue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Phone",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
                Text(
                    text = phoneNumber,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
                Text(
                    text = "Tap to call",
                    fontSize = 12.sp,
                    color = secondaryTextColor
                )
            }
        }
    }
}

/**
 * Individual family member item with icon and description.
 * @param name The name of family member
 * @param description Additional details about family member
 */
@Composable
fun FamilyMemberItem(
    name: String,
    description: String,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF9C27B0).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Person",
                    tint = Color(0xFF9C27B0)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = name,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Add edit button
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Family Member",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Displays an emergency contact with their name, relationship, and phone number.
 * Includes an edit button and supports tap-to-call functionality.
 *
 * @param name Name of the emergency contact
 * @param relation Relationship to the tenant
 * @param phone Phone number of the emergency contact
 * @param onEditClick Callback when the edit button is clicked
 */
@Composable
fun EmergencyContactItem(
    name: String,
    relation: String,
    phone: String,
    address: String?, // <-- Add this parameter
    onEditClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable {
                // Create intent to dial the number
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phone")
                }
                // Verify the intent can be resolved before launching
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Red.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Emergency Contact",
                    tint = Color.Red
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = relation,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = phone,
                    fontSize = 14.sp,
                    color = Color.Red,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Show address if present
                if (!address.isNullOrBlank()) {
                    Text(
                        text = address,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        maxLines = 3, // Allow up to 3 lines for address
                        overflow = TextOverflow.Visible // Show full address
                    )
                }
            }

            // Edit button
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Emergency Contact",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
