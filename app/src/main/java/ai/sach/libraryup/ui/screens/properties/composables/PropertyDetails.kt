package ai.sach.libraryup.ui.screens.properties.composables

import ai.sach.libraryup.LibraryUpApplication
import ai.sach.libraryup.data.viewmodel.TenantViewModel
import ai.sach.libraryup.data.viewmodel.TenantViewModelFactory
import ai.sach.libraryup.ui.screens.settingsScreen.appprefernecs.formatWithCurrency
import ai.sach.libraryup.ui.screens.settingsScreen.appprefernecs.getCurrencySymbol
import ai.sach.libraryup.ui.theme.AppColors
import ai.sach.libraryup.ui.viewmodel.PropertyViewModel
import ai.sach.libraryup.ui.viewmodel.PropertyViewModelFactory
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.sach.libraryup.ui.common.dialog.PropertyFormFields
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private fun logError(tag: String, message: String, throwable: Throwable? = null) {
    android.util.Log.e(tag, message, throwable)
}

data class PropertyEditUiState(
    val name: String = "",
    val address: String = "",
    val totalUnits: String = "",
    val floors: String = "",
    val description: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailsScreen(
    propertyId: String,
    onBackPressed: () -> Unit = {}
) {
    // Get the application context
    val context = LocalContext.current
    val application = context.applicationContext as LibraryUpApplication

    // Check if dark theme is enabled
    val isDarkTheme = isSystemInDarkTheme()

    // Set colors based on theme
    val backgroundColor = if (isDarkTheme) {
        Color(0xFF121212) // Dark background
    } else {
        Color(0xFFF8F9FA) // Light background
    }

    val textColor = if (isDarkTheme) {
        Color.White
    } else {
        Color.Black
    }

    val secondaryTextColor = if (isDarkTheme) {
        Color(0xFFB0B0B0) // Light gray for dark theme
    } else {
        Color.Gray
    }

    // Initialize ViewModels
    val propertyViewModel: PropertyViewModel = viewModel(
        factory = PropertyViewModelFactory(application.propertyRepository)
    )

    val tenantViewModel: TenantViewModel = viewModel(
        factory = TenantViewModelFactory(
            application.tenantRepository,
            application.propertyRepository
        )
    )

    // Get property details from the database
    val propertyLiveData = propertyViewModel.getPropertyById(propertyId)
    val property by propertyLiveData.observeAsState()

    // Get tenants for this property
    val allTenants by tenantViewModel.allTenants.observeAsState(emptyList())
    val propertyTenants = allTenants.filter { tenantWithMultipleProperties ->
        tenantWithMultipleProperties.properties.any { it.id == propertyId }
    }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    // Use a data class for edit state
    var editState by remember { mutableStateOf(PropertyEditUiState()) }
    var isEditMode by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Update editable fields when property is loaded
    LaunchedEffect(property) {
        try {
            property?.let {
                editState = PropertyEditUiState(
                    name = it.name,
                    address = it.address,
                    totalUnits = it.totalUnits.toString(),
                    floors = it.floors.toString(),
                    description = it.description ?: ""
                )
            }
        } catch (e: Exception) {
            logError("PropertyDetailsScreen", "Error updating fields: ${e.message}", e)
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Error updating fields: ${e.message}")
            }
        }
    }

    // If no property found, show loading or error state
    if (property == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading property details...",
                    color = if (isDarkTheme) Color.White else Color.Black,
                    fontSize = 16.sp
                )
            }
        }
        return
    }


    val appPreferencesRepository = remember {
        (context.applicationContext as LibraryUpApplication).appPreferencesRepository
    }
    val currencyCode by appPreferencesRepository.currency.collectAsState(initial = "INR")
    val currencySymbol = getCurrencySymbol(currencyCode)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Edit Property" else property?.name ?: "",
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (!isEditMode) {
                        // Edit button
                        IconButton(onClick = { isEditMode = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Property",
                                tint = Color.White
                            )
                        }
                    } else {
                        // Save button
                        IconButton(
                            onClick = {
                                property?.let { currentProperty ->
                                    try {
                                        val updatedProperty = currentProperty.copy(
                                            name = editState.name,
                                            address = editState.address,
                                            totalUnits = editState.totalUnits.toIntOrNull() ?: 0,
                                            floors = editState.floors.toIntOrNull() ?: 0,
                                            description = editState.description.ifBlank { null }
                                        )
                                        propertyViewModel.updateProperty(updatedProperty)
                                        isEditMode = false
                                    } catch (e: Exception) {
                                        logError("PropertyDetailsScreen", "Error saving property: ${e.message}", e)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Error saving property: ${e.message}")
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save Changes",
                                tint = Color.White
                            )
                        }
                        // Cancel button
                        IconButton(onClick = {
                            isEditMode = false
                            property?.let {
                                editState = PropertyEditUiState(
                                    name = it.name,
                                    address = it.address,
                                    totalUnits = it.totalUnits.toString(),
                                    floors = it.floors.toString(),
                                    description = it.description ?: ""
                                )
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary  // Use theme primary color
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(backgroundColor)
                .verticalScroll(rememberScrollState())
        ) {
            if (isEditMode) {
                PropertyEditForm(
                    propertyName = editState.name,
                    onPropertyNameChange = { editState = editState.copy(name = it) },
                    propertyNameError = null,
                    address = editState.address,
                    onAddressChange = { editState = editState.copy(address = it) },
                    addressError = null,
                    totalUnits = editState.totalUnits,
                    onTotalUnitsChange = { editState = editState.copy(totalUnits = it) },
                    totalUnitsError = null,
                    floors = editState.floors,
                    onFloorsChange = { editState = editState.copy(floors = it) },
                    floorsError = null,
                    notes = editState.description,
                    onNotesChange = { editState = editState.copy(description = it) },
                    isDarkTheme = isDarkTheme,
                    textColor = textColor,
                    borderColor = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color.Gray,
                    errorColor = Color.Red
                )
            } else {
                // Property address row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = secondaryTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = property?.address ?: "",
                        color = secondaryTextColor,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                // Property statistics section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatCard(
                            title = "Total Units",
                            value = property?.totalUnits?.toString() ?: "0",
                            icon = "🏠",
                            backgroundColor = if (isDarkTheme) Color(0xFF1E3A5F) else Color(0xFFEEF7FF),
                            iconBackgroundColor = Color(0xFF4A9DFF),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp, bottom = 8.dp),
                            textColor = textColor,
                            labelColor = secondaryTextColor
                        )
                        StatCard(
                            title = "Occupancy",
                            value = "${property?.occupancyRate}%",
                            icon = "👥",
                            backgroundColor = if (isDarkTheme) Color(0xFF1C3B2C) else Color(0xFFE6F9F1),
                            iconBackgroundColor = Color(0xFF2ECC71),
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp, bottom = 8.dp),
                            textColor = textColor,
                            labelColor = secondaryTextColor
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatCard(
                            title = "Floors",
                            value = property?.floors?.toString() ?: "0",
                            icon = "🏢",
                            backgroundColor = if (isDarkTheme) Color(0xFF331F47) else Color(0xFFF9EEFF),
                            iconBackgroundColor = Color(0xFFB066FF),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp, top = 8.dp),
                            textColor = textColor,
                            labelColor = secondaryTextColor
                        )
                        StatCard(
                            title = "Due Rent",
                            value = "${calculateDueRent(propertyTenants)}",
                            icon = currencySymbol,
                            backgroundColor = if (isDarkTheme) Color(0xFF3B1F1F) else Color(0xFFFEE9E9),
                            iconBackgroundColor = Color(0xFFFF6B6B),
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp, top = 8.dp),
                            textColor = textColor,
                            labelColor = secondaryTextColor
                        )
                    }
                }

                // Description section (below tabs)
                property?.description?.let { desc ->
                    if (desc.isNotBlank()) {
                        HorizontalDivider(
                            modifier = Modifier
                                .padding(vertical = 16.dp, horizontal = 16.dp)
                                .fillMaxWidth(),
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.2f)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Description",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp),
                                color = textColor
                            )

                            Text(
                                text = desc,
                                fontSize = 14.sp,
                                color = if (isDarkTheme) Color(0xFFD0D0D0) else Color.DarkGray
                            )
                        }
                    }
                }

                // Tab layout for different sections
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = if (isDarkTheme) Color(0xFF1F2937) else AppColors.White,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    // Tenants tab
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = {
                            // Tenants count header
                            Text(
                                text = "Tenants (${propertyTenants.size})",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    )

                    // We could add more tabs here in the future
                    // Tab(
                    //     selected = selectedTabIndex == 1,
                    //     onClick = { selectedTabIndex = 1 },
                    //     text = { Text("Documents") }
                    // )
                }

                // Tab content
                when (selectedTabIndex) {
                    0 -> TenantsTab(
                        propertyTenants = propertyTenants,
                        propertyId = propertyId,
                        isDarkTheme = isDarkTheme,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor
                    )
                    // 1 -> DocumentsTab() // Future tab
                }


            }
        }
    }
}

@Composable
fun TenantsTab(
    propertyTenants: List<ai.sach.libraryup.data.local.entity.TenantWithMultipleProperties>,
    propertyId: String,
    isDarkTheme: Boolean,
    textColor: Color,
    secondaryTextColor: Color
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {

        // Table header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Name",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = secondaryTextColor,
                modifier = Modifier.weight(2f)
            )
            Text(
                text = "Unit",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = secondaryTextColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Rent",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = secondaryTextColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Status",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = secondaryTextColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Due Date",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = secondaryTextColor,
                modifier = Modifier.weight(1.5f)
            )
        }

        // Table divider
        HorizontalDivider(
            color = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.2f)
        )

        // If no tenants, show empty state
        if (propertyTenants.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "No Tenants",
                        tint = secondaryTextColor,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "No tenants found for this property",
                        color = secondaryTextColor,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Add tenants from the home screen",
                        color = secondaryTextColor.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            // Tenant rows
            propertyTenants.forEach { tenantWithMultipleProperties ->
                val tenant = tenantWithMultipleProperties.tenant

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tenant.name,
                        fontSize = 14.sp,
                        color = textColor,
                        modifier = Modifier.weight(2f)
                    )
                    Text(
                        text = ai.sach.libraryup.data.local.entity.TenantFloors.display(
                            ai.sach.libraryup.data.local.entity.TenantFloors.entryFor(tenant, propertyId)
                        ),
                        fontSize = 14.sp,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatWithCurrency(tenant.amount ?: "-"),
                        fontSize = 14.sp,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )

                    // Status chip
                    val statusColor = if (tenant.isPaid) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Red
                    }
                    val statusBgColor = if (tenant.isPaid) {
                        if (isDarkTheme) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color(0xFFE6F9F1)
                    } else {
                        if (isDarkTheme) Color.Red.copy(alpha = 0.2f) else Color(0xFFFEE9E9)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(statusBgColor)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tenant.isPaid) "Paid" else "Due",
                            fontSize = 12.sp,
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = tenant.dueDate,
                        fontSize = 14.sp,
                        color = textColor,
                        modifier = Modifier.weight(1.5f)
                    )
                }

                // Row divider
                HorizontalDivider(
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Gray.copy(alpha = 0.1f)
                )
            }
        }
    }
}

// Calculate total due rent for the property
private fun calculateDueRent(tenants: List<ai.sach.libraryup.data.local.entity.TenantWithMultipleProperties>): Int {
    return tenants
        .filter { !it.tenant.isPaid }
        .sumOf { tenantWithMultipleProperties ->
            try {
                tenantWithMultipleProperties.tenant.amount?.replace("$", "")?.replace(",", "")?.trim()?.toIntOrNull() ?: 0
            } catch (e: Exception) {
                // If parsing fails, return 0 to prevent crashes
                0
            }
        }
}

@Composable
fun PropertyEditForm(
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
    PropertyFormFields(
        propertyName = propertyName,
        onPropertyNameChange = onPropertyNameChange,
        propertyNameError = propertyNameError,
        address = address,
        onAddressChange = onAddressChange,
        addressError = addressError,
        totalUnits = totalUnits,
        onTotalUnitsChange = onTotalUnitsChange,
        totalUnitsError = totalUnitsError,
        floors = floors,
        onFloorsChange = onFloorsChange,
        floorsError = floorsError,
        notes = notes,
        onNotesChange = onNotesChange,
        isDarkTheme = isDarkTheme,
        textColor = textColor,
        borderColor = borderColor,
        errorColor = errorColor
    )
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: String,
    backgroundColor: Color,
    iconBackgroundColor: Color,
    modifier: Modifier = Modifier,
    textColor: Color,
    labelColor: Color
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = labelColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon with background
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconBackgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                // Value
                Text(
                    text = value,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 8.dp),
                    color = textColor
                )
            }
        }
    }
}