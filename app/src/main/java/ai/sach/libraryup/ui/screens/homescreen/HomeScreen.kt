@file:OptIn(ExperimentalMaterial3Api::class)
package ai.sach.rentup.ui.screens.homescreen

import ai.sach.rentup.RentUpApplication
import ai.sach.rentup.data.local.entity.Tenant
import ai.sach.rentup.data.local.entity.TenantWithMultipleProperties
import ai.sach.rentup.data.viewmodel.TenantViewModel
import ai.sach.rentup.data.viewmodel.TenantViewModelFactory
import ai.sach.rentup.ui.common.composables.BatchDeleteBar
import ai.sach.rentup.ui.common.composables.RentUpSearchBar
import ai.sach.rentup.ui.common.composables.RentUpTopAppBar
import ai.sach.rentup.ui.common.composables.filterTenants
import ai.sach.rentup.ui.common.dialog.AddPropertyForm
import ai.sach.rentup.ui.common.dialog.AddTenantDialog
import ai.sach.rentup.ui.common.responsiveness.rememberDimensions
import ai.sach.rentup.ui.screens.homescreen.composable.EmptyStateGuide
import ai.sach.rentup.ui.screens.homescreen.composable.FilterChips
import ai.sach.rentup.ui.screens.homescreen.composable.TenantCard
import ai.sach.rentup.ui.screens.homescreen.composable.TenantDetailBottomSheet
import ai.sach.rentup.ui.theme.RentUpColors
import ai.sach.rentup.ui.viewmodel.PropertyViewModel
import ai.sach.rentup.ui.viewmodel.PropertyViewModelFactory
import ai.sach.rentup.ui.screens.settingsScreen.composable.SettingsNavRoutes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import kotlinx.coroutines.CoroutineScope
import androidx.navigation.NavController

private fun logDebug(tag: String, message: String) {
    Log.d(tag, message)
}
private fun logError(tag: String, message: String, throwable: Throwable? = null) {
    Log.e(tag, message, throwable)
}

data class HomeScreenUiState(
    val searchQuery: String = "",
    val selectedFilterOption: TenantFilterOption = TenantFilterOption.ALL,
    val selectedSortOption: TenantSortOption = TenantSortOption.DUE_DATE,
    val isSelectionMode: Boolean = false,
    val selectedTenantIds: Set<Long> = emptySet(),
    val showBatchDeleteDialog: Boolean = false,
    val errorMessage: String? = null
)

fun filterAndSortTenants(
    tenants: List<TenantWithMultipleProperties>,
    searchQuery: String,
    filterOption: TenantFilterOption,
    sortOption: TenantSortOption
): List<TenantWithMultipleProperties> {
    val searched = filterTenants(searchQuery, tenants)
    val filtered = if (filterOption != TenantFilterOption.ALL) {
        searched.filter {
            val status = getPaymentStatus(it.tenant.dueDate, it.tenant.isPaid)
            status.name.equals(filterOption.name, ignoreCase = true) ||
                    status.name.equals(filterOption.displayName.replace(" ", "_"), ignoreCase = true)
        }
    } else {
        searched
    }
    return when (sortOption) {
        TenantSortOption.NAME -> filtered.sortedBy { it.tenant.name }
        TenantSortOption.DUE_DATE -> filtered.sortedBy {
            try {
                LocalDate.parse(it.tenant.dueDate)
            } catch (e: Exception) {
                LocalDate.MAX
            }
        }
    }
}

// Payment status enum to categorize different payment statuses
enum class PaymentStatus {
    PAID, DUE, DUE_SOON, OVERDUE
}

// Helper class for storing multiple values
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

enum class TenantSortOption(val displayName: String) {
    NAME("Name"),
    DUE_DATE("Due Date"),
}

enum class TenantFilterOption(val displayName: String) {
    ALL("All"),
    PAID("Paid"),
    DUE("Due"),
    DUE_SOON("Due Soon"),
    OVERDUE("Overdue")
}

/**
 * Main Home Screen composable for the RentUp application.
 * Displays property summaries, tenant information, and includes a modern search bar.
 *
 * @param navController Navigation controller for handling navigation
 * @param isAddTenantFormVisible Controls visibility of the add tenant form
 * @param isAddPropertyFormVisible Controls visibility of the add property form
 * @param onAddTenantFormDismiss Callback to dismiss the add tenant form
 * @param onAddPropertyFormDismiss Callback to dismiss the add property form
 */
@Preview
@Composable
fun HomeScreen(
    navController: NavController? = null,
    viewModel: TenantViewModel = viewModel(factory = TenantViewModelFactory(
        (LocalContext.current.applicationContext as RentUpApplication).tenantRepository,
        (LocalContext.current.applicationContext as RentUpApplication).propertyRepository
    )),
    isAddTenantFormVisible: Boolean = false,
    isAddPropertyFormVisible: Boolean = false,
    onAddTenantFormDismiss: () -> Unit = {},
    onAddPropertyFormDismiss: () -> Unit = {},
    onShowAddPropertyFormRequest: () -> Unit = {},
    onShowAddTenantFormRequest: () -> Unit = {},
    refreshTrigger: Long = 0L,
    initialFilterKey: String? = null,
    onInitialFilterConsumed: () -> Unit = {},
    initialTenantIdToOpen: Long? = null,
    onInitialTenantConsumed: () -> Unit = {},
    onSelectionModeChange: (Boolean) -> Unit = {}
) {

    // Add PropertyViewModel instance
    val context = LocalContext.current
    val application = context.applicationContext as RentUpApplication
    val propertyViewModel: PropertyViewModel = viewModel(
        factory = PropertyViewModelFactory(application.propertyRepository)
    )

    val tenants by viewModel.allTenants.observeAsState(emptyList())
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) Color(0xFF121212) else RentUpColors.BackgroundGray
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var uiState by remember { mutableStateOf(HomeScreenUiState()) }
    val visibleTenantIds = remember(tenants, uiState.searchQuery, uiState.selectedFilterOption, uiState.selectedSortOption) {
        filterAndSortTenants(
            tenants,
            uiState.searchQuery,
            uiState.selectedFilterOption,
            uiState.selectedSortOption
        ).mapTo(mutableSetOf()) { it.tenant.id }
    }
    val allVisibleSelected = visibleTenantIds.isNotEmpty() &&
        visibleTenantIds.all { it in uiState.selectedTenantIds }

    LaunchedEffect(uiState.isSelectionMode) {
        onSelectionModeChange(uiState.isSelectionMode)
    }

    // A rent reminder notification asked for a specific filter chip to already be
    // selected - e.g. tapping an overdue digest should show overdue tenants immediately,
    // not the full unfiltered roster. Applied once per incoming key, then consumed so
    // organic navigation back to Home later does not keep re-imposing it.
    LaunchedEffect(initialFilterKey) {
        val key = initialFilterKey ?: return@LaunchedEffect
        val option = TenantFilterOption.entries.firstOrNull { it.name == key }
        if (option != null) {
            uiState = uiState.copy(selectedFilterOption = option)
        }
        onInitialFilterConsumed()
    }

    // A "new tenant submission" notification tap asked for that tenant's details to open
    // on top of the list. Kept re-checking against tenants rather than consuming
    // immediately: the id was resolved from a database read that could complete a moment
    // before this screen's own tenant list has refreshed to include the same row.
    var notificationTenant by remember { mutableStateOf<Tenant?>(null) }
    LaunchedEffect(initialTenantIdToOpen, tenants) {
        val tenantId = initialTenantIdToOpen ?: return@LaunchedEffect
        val found = tenants.find { it.tenant.id == tenantId }?.tenant ?: return@LaunchedEffect
        viewModel.selectTenant(found.id)
        notificationTenant = found
        onInitialTenantConsumed()
    }

    // Force refresh when composable enters composition
    LaunchedEffect(Unit) {
        viewModel.refreshTenantData()
    }
    


    Scaffold(
        topBar = {
            RentUpTopAppBar(
                title = if (uiState.isSelectionMode) {
                    if (uiState.selectedTenantIds.isEmpty()) "Select tenants" else "${uiState.selectedTenantIds.size} selected"
                } else "RentUp",
                showBackButton = false,
                showDefaultMenu = !uiState.isSelectionMode,
                showAppActions = !uiState.isSelectionMode,
                onNavigateToBackup = if (uiState.isSelectionMode) null else { { navController?.navigate(SettingsNavRoutes.BACKUP) } },
                actions = {
                    if (uiState.isSelectionMode) {
                        TextButton(onClick = {
                            uiState = uiState.copy(
                                selectedTenantIds = if (allVisibleSelected) {
                                    uiState.selectedTenantIds - visibleTenantIds
                                } else {
                                    uiState.selectedTenantIds + visibleTenantIds
                                }
                            )
                        }) {
                            Text(if (allVisibleSelected) "Clear" else "All")
                        }
                        IconButton(onClick = {
                            uiState = uiState.copy(isSelectionMode = false, selectedTenantIds = emptySet())
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                        }
                    } else {
                        IconButton(onClick = {
                            uiState = uiState.copy(isSelectionMode = true)
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Select tenants")
                        }
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                modifier = Modifier.padding(bottom = if (uiState.isSelectionMode) 76.dp else 0.dp)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (uiState.isSelectionMode) 96.dp else 0.dp)
            ) {
                HomeScreenContent(
                    tenants = tenants,
                    onTenantClick = { tenant ->
                        if (uiState.isSelectionMode) {
                            uiState = uiState.copy(
                                selectedTenantIds = if (tenant.id in uiState.selectedTenantIds) {
                                    uiState.selectedTenantIds - tenant.id
                                } else {
                                    uiState.selectedTenantIds + tenant.id
                                }
                            )
                        } else {
                            viewModel.selectTenant(tenant.id)
                        }
                    },
                    onTenantLongClick = { tenant ->
                        uiState = uiState.copy(
                            isSelectionMode = true,
                            selectedTenantIds = uiState.selectedTenantIds + tenant.id
                        )
                    },
                    onDeleteTenant = { tenant -> viewModel.deleteTenant(tenant) },
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    uiState = uiState,
                    onUiStateChange = { uiState = it },
                    onAddPropertyRequest = onShowAddPropertyFormRequest,
                    onAddTenantRequest = onShowAddTenantFormRequest,
                    snackbarHostState = snackbarHostState,
                    coroutineScope = coroutineScope
                )
            }

            // Batch delete bar shown during multi-select mode
            if (uiState.isSelectionMode) {
                BatchDeleteBar(
                    selectedCount = uiState.selectedTenantIds.size,
                    itemLabel = "tenant",
                    onDelete = { uiState = uiState.copy(showBatchDeleteDialog = true) },
                    onCancel = {
                        uiState = uiState.copy(isSelectionMode = false, selectedTenantIds = emptySet())
                    },
                    isDarkTheme = isDarkTheme,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // Batch delete confirmation dialog
            if (uiState.showBatchDeleteDialog) {
                val selectedTenants = tenants.filter { it.tenant.id in uiState.selectedTenantIds }.map { it.tenant }
                if (selectedTenants.isNotEmpty()) {
                    AlertDialog(
                        onDismissRequest = { uiState = uiState.copy(showBatchDeleteDialog = false) },
                        title = { Text("Delete Tenants") },
                        text = {
                            Text("Are you sure you want to delete ${selectedTenants.size} tenants? This will also remove their family members, documents, and payment history.")
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    try {
                                        viewModel.deleteTenants(selectedTenants)
                                        uiState = uiState.copy(
                                            showBatchDeleteDialog = false,
                                            isSelectionMode = false,
                                            selectedTenantIds = emptySet()
                                        )
                                    } catch (e: Exception) {
                                        logError("HomeScreen", "Error deleting tenants: ${e.message}", e)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Error deleting tenants: ${e.message}")
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red
                                )
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { uiState = uiState.copy(showBatchDeleteDialog = false) }) {
                                Text("Cancel")
                            }
                        }
                    )
                } else {
                    uiState = uiState.copy(showBatchDeleteDialog = false)
                }
            }
        }
    }

    // Tenant details opened from a "new tenant submission" notification tap, rather than
    // from tapping a row in the list below - so it renders here, independent of any one
    // TenantCard's own local bottom sheet.
    notificationTenant?.let { tenant ->
        val familyMembers by viewModel.selectedTenantFamilyInfo.observeAsState(emptyList())
        TenantDetailBottomSheet(
            tenant = tenant,
            familyMembers = familyMembers,
            onClose = { notificationTenant = null },
            viewModel = viewModel
        )
    }

    // Handle Add Tenant Form visibility
    if (isAddTenantFormVisible) {
        AddTenantDialog(
            onDismiss = {
                viewModel.refreshTenantData() // Refresh when dialog is dismissed
                onAddTenantFormDismiss()
            },
            onAddTenant = { tenant ->
                viewModel.insertTenantAndReturnId(tenant)
            },
            onAddPropertyRequest = onShowAddPropertyFormRequest
        )
    }

    // Handle Add Property Form visibility (using placeholder for now)
    if (isAddPropertyFormVisible) {
        AddPropertyForm(
            onDismiss = onAddPropertyFormDismiss,
            onAddProperty = { propertyData ->
                try {
                    // Add the property using the ViewModel
                    propertyViewModel.addProperty(
                        name = propertyData["propertyName"] ?: "",
                        address = propertyData["address"] ?: "",
                        totalUnits = propertyData["totalUnits"]?.toIntOrNull() ?: 0,
                        floors = propertyData["floors"]?.toIntOrNull() ?: 0,
                        description = propertyData["notes"],
                        type = null, // Optional: You can add this field to the form later
                        unitRent = null, // Optional: You can add this field to the form later
                        occupancyRate = 0
                    )

                    // Close the form
                    onAddPropertyFormDismiss()
                } catch (e: Exception) {
                    logError("HomeScreen", "Error adding property: ${e.message}", e)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Error adding property: ${e.message}")
                    }
                }
            },
            isDarkTheme = isDarkTheme
        )
    }
}

/**
 * Composable for the main content on the home screen.
 * Integrates a tenant search bar, property summary, filter chips, and tenant list.
 */
@Composable
private fun HomeScreenContent(
    tenants: List<TenantWithMultipleProperties>,
    onTenantClick: (Tenant) -> Unit,
    onTenantLongClick: (Tenant) -> Unit,
    onDeleteTenant: (Tenant) -> Unit,
    viewModel: TenantViewModel,
    isDarkTheme: Boolean,
    uiState: HomeScreenUiState,
    onUiStateChange: (HomeScreenUiState) -> Unit,
    onAddPropertyRequest: () -> Unit,
    onAddTenantRequest: () -> Unit,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp) // Apply consistent horizontal padding to the whole column
    ) {
        RentUpSearchBar(
            query = uiState.searchQuery,
            onQueryChange = { onUiStateChange(uiState.copy(searchQuery = it)) },
            placeholder = "Search tenants by name, property, or phone...",
            modifier = Modifier.padding(vertical = 8.dp), // Only vertical padding
            isDarkTheme = isDarkTheme
        )



        // Always show filter chips for additional tenant filtering (e.g., by payment status)
        FilterChips(
            selectedFilter = uiState.selectedFilterOption,
            onFilterSelected = { onUiStateChange(uiState.copy(selectedFilterOption = it)) },
            isDarkTheme = isDarkTheme
        )

        // Render the tenant list using search, sort, and filter options
        TenantListSection(
            tenants = tenants,
            sortOption = uiState.selectedSortOption,
            filterOption = uiState.selectedFilterOption,
            searchQuery = uiState.searchQuery,
            onTenantClick = onTenantClick,
            onTenantLongClick = onTenantLongClick,
            onDeleteTenant = onDeleteTenant,
            viewModel = viewModel,
            isDarkTheme = isDarkTheme,
            isSelectionMode = uiState.isSelectionMode,
            selectedTenantIds = uiState.selectedTenantIds,
            onAddPropertyRequest = onAddPropertyRequest,
            onAddTenantRequest = onAddTenantRequest
        )
    }
}

/**
 * Displays a summary of property statistics.
 */
@Composable
private fun PropertySummarySection(tenants: List<TenantWithMultipleProperties>, isDarkTheme: Boolean) {
    // Calculate summary statistics
    val totalUnits = tenants.size
    val totalDue = tenants.filter { !it.tenant.isPaid }
        .sumOf { it.tenant.amount?.replace("$", "")?.toDoubleOrNull() ?: 0.0 }
    val occupancyRate = if (totalUnits > 0) 100 else 0

    val cardBgColor = if (isDarkTheme) {
        Color(0xFF1E1E1E)
    } else {
        RentUpColors.White
    }
    val textColor = if (isDarkTheme) {
        Color.White
    } else {
        Color.Black
    }
    val secondaryTextColor = if (isDarkTheme) {
        Color(0xFFB0B0B0)
    } else {
        Color.Gray
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBgColor, RoundedCornerShape(8.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PropertySummaryItem("Total Units", totalUnits.toString(), textColor, secondaryTextColor)
            PropertySummaryItem("Total Due", "$$totalDue", textColor, secondaryTextColor)
            PropertySummaryItem("Occupancy", "$occupancyRate%", textColor, secondaryTextColor)
        }
    }
}

@Composable
private fun PropertySummaryItem(
    title: String,
    value: String,
    primaryColor: Color,
    secondaryColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = primaryColor
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = secondaryColor
        )
    }
}

/**
 * Determines payment status based on due date and payment status.
 */
fun getPaymentStatus(dueDateStr: String, isPaid: Boolean): PaymentStatus {
    if (isPaid) return PaymentStatus.PAID

    try {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dueDate = try {
            LocalDate.parse(dueDateStr, formatter)
        } catch (e: Exception) {
            LocalDate.now() // Default to today if date is invalid
        }
        val today = LocalDate.now()
        val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate)

        return when {
            dueDate.isBefore(today) -> PaymentStatus.OVERDUE
            daysUntilDue <= 5 -> PaymentStatus.DUE_SOON
            else -> PaymentStatus.DUE
        }
    } catch (e: Exception) {
        return PaymentStatus.DUE
    }
}

/**
 * Displays the list of tenants using search, sort, and filter criteria.
 *
 * @param tenants The list of tenants with their associated property details.
 * @param sortOption The selected sorting method.
 * @param filterOption The selected filtering method based on payment status.
 * @param searchQuery The search query string for tenant names, property names, or mobile numbers.
 * @param onTenantClick Callback when a tenant card is clicked.
 * @param onDeleteTenant Callback when the delete action is triggered for a tenant.
 * @param viewModel The ViewModel providing access to tenant data and actions.
 * @param isDarkTheme Flag indicating if the dark theme is active.
 */
@Composable
private fun TenantListSection(
    tenants: List<TenantWithMultipleProperties>,
    sortOption: TenantSortOption,
    filterOption: TenantFilterOption,
    searchQuery: String,
    onTenantClick: (Tenant) -> Unit,
    onTenantLongClick: (Tenant) -> Unit,
    onDeleteTenant: (Tenant) -> Unit,
    viewModel: TenantViewModel,
    isDarkTheme: Boolean,
    isSelectionMode: Boolean = false,
    selectedTenantIds: Set<Long> = emptySet(),
    onAddPropertyRequest: () -> Unit,
    onAddTenantRequest: () -> Unit
) {
    // --- Data Fetching ---
    val allProperties by viewModel.allProperties.observeAsState(emptyList())
    // --- End Data Fetching ---

    // --- Filtering Logic ---
    // Apply search query filter - replace existing filtering with our function
    val searchedTenants = remember(searchQuery, tenants) {
        filterTenants(searchQuery, tenants)
    }

    // Apply status filter
    val filteredTenants = if (filterOption != TenantFilterOption.ALL) {
        searchedTenants.filter {
            val status = getPaymentStatus(it.tenant.dueDate, it.tenant.isPaid)
            status.name.equals(filterOption.name, ignoreCase = true) ||
                    status.name.equals(filterOption.displayName.replace(" ", "_"), ignoreCase = true)
        }
    } else {
        searchedTenants
    }
    // --- End Filtering Logic ---

    // --- Sorting Logic ---
    val sortedTenants = remember(filteredTenants, sortOption) {
        when (sortOption) {
            TenantSortOption.NAME -> filteredTenants.sortedBy { it.tenant.name }
            TenantSortOption.DUE_DATE -> filteredTenants.sortedBy {
                try {
                    LocalDate.parse(it.tenant.dueDate)
                } catch (e: Exception) {
                    LocalDate.MAX
                }
            }
        }
    }
    // --- End Sorting Logic ---

    // --- List Display ---
    LazyColumn(
        modifier = Modifier.padding(top = 8.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        // Display message if list is empty after filtering/sorting
        if (sortedTenants.isEmpty()) {
            item {
                // Check if we have any tenants at all (not just filtered out)
                if (tenants.isEmpty()) {
                    // Show the empty state guide
                    EmptyStateGuide(
                        hasProperties = allProperties.isNotEmpty(),
                        onAddPropertyClick = onAddPropertyRequest,
                        onAddTenantClick = onAddTenantRequest
                    )
                } else {
                    // Show filtered empty message
                    Text(
                        text = "No tenants match the current criteria.",
                        modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Display tenant cards
            items(sortedTenants, key = { "${it.tenant.id}" }) { tenantWithMultipleProperties ->
                val tenant = tenantWithMultipleProperties.tenant
                val properties = tenantWithMultipleProperties.properties

                // Get property names from the properties list
                val propertyNames = properties.map { it.name }
                val firstPropertyName = propertyNames.firstOrNull() ?: "No Property"
                val moreCount = propertyNames.size - 1

                TenantCard(
                    tenant = tenant,
                    propertyName = if (moreCount > 0) "$firstPropertyName (+$moreCount more)" else firstPropertyName,
                    onTenantClick = { onTenantClick(tenant) },
                    onTenantLongClick = { onTenantLongClick(tenant) },
                    onDeleteTenant = { onDeleteTenant(tenant) },
                    viewModel = viewModel,
                    onTenantUpdated = { updatedTenant -> viewModel.updateTenant(updatedTenant) },
                    onEditTenant = {
                        // Navigation logic to edit tenant screen goes here
                    },
                    isDarkTheme = isDarkTheme,
                    selectionMode = isSelectionMode,
                    selected = tenant.id in selectedTenantIds,
                    key = "${tenant.id}_${tenant.isPaid}_${tenant.status}" // Force recomposition when tenant status changes
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
    // --- End List Display ---
}




/**
 * Enhanced tenant avatar component with responsive sizing
 */
@Composable
fun TenantAvatar(
    modifier: Modifier,
    avatarUri: String?,
    isDarkTheme: Boolean = false,
) {
    // Get responsive dimensions
    val dimensions = rememberDimensions()

    val avatarBackgroundColor = if (isDarkTheme) {
        Color(0xFF333333) // Darker gray for dark theme
    } else {
        Color(0xFFE0E0E0) // Light gray for light theme
    }

    val iconTint = if (isDarkTheme) {
        Color(0xFFAAAAAA) // Lighter gray for icon in dark theme
    } else {
        Color.Gray
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(avatarBackgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUri != null) {
            // Load and display the avatar image
            AsyncImage(
                model = avatarUri.toUri(),
                contentDescription = "Tenant Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Calculate icon size based on the avatar container modifier
            // Use dimensions for a responsive size relative to container
            val avatarSize = (modifier as? Modifier)?.let {
                // Attempt to extract size from modifier, default to medium avatar size
                dimensions.avatarSizeLarge * 0.45f
            } ?: dimensions.iconSizeMedium

            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Avatar",
                tint = iconTint,
                modifier = Modifier.size(avatarSize)
            )
        }
    }
}

