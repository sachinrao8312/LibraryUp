package ai.sach.rentup.ui.screens.properties

import ai.sach.rentup.RentUpApplication
import ai.sach.rentup.data.local.entity.Property
import ai.sach.rentup.data.viewmodel.TenantViewModel
import ai.sach.rentup.data.viewmodel.TenantViewModelFactory
import ai.sach.rentup.ui.common.composables.BatchDeleteBar
import ai.sach.rentup.ui.common.composables.RentUpSearchBar
import ai.sach.rentup.ui.common.composables.RentUpTopAppBar
import ai.sach.rentup.ui.common.composables.filterProperties
import ai.sach.rentup.ui.common.dialog.AddPropertyForm
import ai.sach.rentup.ui.viewmodel.PropertyViewModel
import ai.sach.rentup.ui.viewmodel.PropertyViewModelFactory
import ai.sach.rentup.ui.screens.settingsScreen.composable.SettingsNavRoutes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch

private fun logDebug(tag: String, message: String) {
    android.util.Log.d(tag, message)
}
private fun logError(tag: String, message: String, throwable: Throwable? = null) {
    android.util.Log.e(tag, message, throwable)
}

data class PropertiesUiState(
    val searchQuery: String = "",
    val sortBy: String = "Name",
    val showDeleteDialogFor: String? = null,
    val isSelectionMode: Boolean = false,
    val selectedPropertyIds: Set<String> = emptySet(),
    val showBatchDeleteDialog: Boolean = false,
    val errorMessage: String? = null
)

fun filterAndSortProperties(
    properties: List<Property>,
    searchQuery: String,
    sortBy: String,
    tenantCountsMap: Map<String, Int>
): List<Property> {
    val filtered = filterProperties(searchQuery, properties)
    return when (sortBy) {
        "Name" -> filtered.sortedBy { it.name }
        "Address" -> filtered.sortedBy { it.address }
        "Units" -> filtered.sortedByDescending { it.totalUnits }
        "Tenants" -> filtered.sortedByDescending { tenantCountsMap[it.id] ?: 0 }
        else -> filtered
    }
}

@Composable
fun PropertiesScreen(
    propertyId: String? = null,
    isAddPropertyFormVisible: Boolean = false,
    onAddPropertyFormDismiss: () -> Unit = {},
    onAddPropertyClicked: () -> Unit = {},
    navController: NavHostController
) {
    val darkTheme = isSystemInDarkTheme()
    val backgroundColor = MaterialTheme.colorScheme.background
    val context = LocalContext.current
    val application = context.applicationContext as RentUpApplication
    val propertyViewModel: PropertyViewModel = viewModel(
        factory = PropertyViewModelFactory(application.propertyRepository)
    )
    val tenantViewModel: TenantViewModel = viewModel(
        factory = TenantViewModelFactory(
            application.tenantRepository,
            application.propertyRepository
        )
    )
    val properties by propertyViewModel.allProperties.observeAsState(emptyList())
    val allTenants by tenantViewModel.allTenants.observeAsState(emptyList())
    val tenantCountsMap by remember(allTenants) {
        derivedStateOf {
            allTenants.flatMap { tenantWithMultipleProperties ->
                tenantWithMultipleProperties.properties.map { property ->
                    property.id to tenantWithMultipleProperties.tenant.id
                }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, tenantIds) -> tenantIds.distinct().size }
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var uiState by remember { mutableStateOf(PropertiesUiState()) }
    val sortedProperties = filterAndSortProperties(properties, uiState.searchQuery, uiState.sortBy, tenantCountsMap)
    Scaffold(
        topBar = {
            RentUpTopAppBar(
                title = "Properties",
                showDefaultMenu = true,
                onNavigateToBackup = { navController.navigate(SettingsNavRoutes.BACKUP) },
                actions = {
                    if (uiState.isSelectionMode) {
                        IconButton(onClick = {
                            uiState = uiState.copy(isSelectionMode = false, selectedPropertyIds = emptySet())
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                        }
                    } else {
                        IconButton(onClick = {
                            uiState = uiState.copy(isSelectionMode = true)
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Select multiple properties")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    .padding(16.dp)
            ) {
                RentUpSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { uiState = uiState.copy(searchQuery = it) },
                    placeholder = "Search properties by name or address...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    isDarkTheme = darkTheme
                )
                // Property listings
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sortedProperties, key = { it.id }) { property ->
                        val tenantCount = tenantCountsMap[property.id] ?: 0
                        PropertyCard(
                            property = property,
                            onViewDetails = {
                                navController.navigate("propertyDetails/${property.id}")
                            },
                            onDeleteProperty = {
                                uiState = uiState.copy(showDeleteDialogFor = property.id)
                            },
                            isDarkTheme = darkTheme,
                            tenantCount = tenantCount,
                            selectionMode = uiState.isSelectionMode,
                            selected = property.id in uiState.selectedPropertyIds,
                            onToggleSelect = {
                                uiState = uiState.copy(
                                    selectedPropertyIds = if (property.id in uiState.selectedPropertyIds) {
                                        uiState.selectedPropertyIds - property.id
                                    } else {
                                        uiState.selectedPropertyIds + property.id
                                    }
                                )
                            }
                        )
                    }
                }
            }
            // Centralized delete confirmation dialog
            val propertyToDelete = sortedProperties.find { it.id == uiState.showDeleteDialogFor }
            if (propertyToDelete != null) {
                AlertDialog(
                    onDismissRequest = { uiState = uiState.copy(showDeleteDialogFor = null) },
                    title = { Text("Delete Property") },
                    text = {
                        Text("Are you sure you want to delete ${propertyToDelete.name}? This will also remove all tenant associations.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                try {
                                    propertyViewModel.deleteProperty(propertyToDelete)
                                    uiState = uiState.copy(showDeleteDialogFor = null)
                                } catch (e: Exception) {
                                    logError("PropertiesScreen", "Error deleting property: ${e.message}", e)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Error deleting property: ${e.message}")
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
                        TextButton(onClick = { uiState = uiState.copy(showDeleteDialogFor = null) }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            // Batch delete confirmation dialog
            if (uiState.showBatchDeleteDialog) {
                val selectedProperties = sortedProperties.filter { it.id in uiState.selectedPropertyIds }
                if (selectedProperties.isNotEmpty()) {
                    AlertDialog(
                        onDismissRequest = { uiState = uiState.copy(showBatchDeleteDialog = false) },
                        title = { Text("Delete Properties") },
                        text = {
                            Text("Are you sure you want to delete ${selectedProperties.size} properties? This will also remove all tenant associations.")
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    try {
                                        propertyViewModel.deleteProperties(selectedProperties)
                                        uiState = uiState.copy(
                                            showBatchDeleteDialog = false,
                                            isSelectionMode = false,
                                            selectedPropertyIds = emptySet()
                                        )
                                    } catch (e: Exception) {
                                        logError("PropertiesScreen", "Error deleting properties: ${e.message}", e)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Error deleting properties: ${e.message}")
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
            // Floating Action Button for adding properties
            if (!uiState.isSelectionMode) {
                FloatingActionButton(
                    onClick = onAddPropertyClicked,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Property")
                }
            } else {
                BatchDeleteBar(
                    selectedCount = uiState.selectedPropertyIds.size,
                    itemLabel = "property",
                    onDelete = { uiState = uiState.copy(showBatchDeleteDialog = true) },
                    onCancel = {
                        uiState = uiState.copy(isSelectionMode = false, selectedPropertyIds = emptySet())
                    },
                    isDarkTheme = darkTheme,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
        if (isAddPropertyFormVisible) {
            AddPropertyForm(
                onDismiss = onAddPropertyFormDismiss,
                onAddProperty = { propertyData ->
                    try {
                        propertyViewModel.addProperty(
                            name = propertyData["propertyName"] ?: "",
                            address = propertyData["address"] ?: "",
                            totalUnits = propertyData["totalUnits"]?.toIntOrNull() ?: 0,
                            floors = propertyData["floors"]?.toIntOrNull() ?: 0,
                            description = propertyData["notes"],
                            occupancyRate = 0
                        )
                        onAddPropertyFormDismiss()
                    } catch (e: Exception) {
                        logError("PropertiesScreen", "Error adding property: ${e.message}", e)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Error adding property: ${e.message}")
                        }
                    }
                },
                isDarkTheme = darkTheme
            )
        }
    }
}

@Composable
fun PropertyCard(
    property: Property,
    onViewDetails: () -> Unit,
    onDeleteProperty: () -> Unit,
    isDarkTheme: Boolean,
    tenantCount: Int = 0,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onToggleSelect: () -> Unit = {}
) {
    // Optimized color calculations
    val surfaceColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val secondaryTextColor = if (isDarkTheme) Color(0xFFD1D5DB) else Color.Gray

    // Simplified font sizes without complex scaling
    val titleFontSize = 16.sp
    val subtitleFontSize = 14.sp
    val smallTextFontSize = 12.sp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { if (selectionMode) onToggleSelect() else onViewDetails() }),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Property header with name and delete button
            PropertyHeader(
                property = property,
                onDeleteProperty = onDeleteProperty,
                onToggleSelect = onToggleSelect,
                titleFontSize = titleFontSize,
                textColor = textColor,
                isDarkTheme = isDarkTheme,
                selectionMode = selectionMode,
                selected = selected
            )

            // Property address and tenant count
            PropertyInfoRow(
                property = property,
                tenantCount = tenantCount,
                subtitleFontSize = subtitleFontSize,
                secondaryTextColor = secondaryTextColor
            )

            // Property statistics
            PropertyStats(
                property = property,
                textColor = textColor,
                secondaryTextColor = secondaryTextColor,
                subtitleFontSize = subtitleFontSize,
                smallTextFontSize = smallTextFontSize,
                isDarkTheme = isDarkTheme
            )
        }
    }
}

/**
 * Optimized property header component
 */
@Composable
private fun PropertyHeader(
    property: Property,
    onDeleteProperty: () -> Unit,
    onToggleSelect: () -> Unit,
    titleFontSize: androidx.compose.ui.unit.TextUnit,
    textColor: Color,
    isDarkTheme: Boolean,
    selectionMode: Boolean = false,
    selected: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        // Property icon
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isDarkTheme) Color(0xFF1A365D) else Color(0xFFDBEAFE)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Property",
                tint = if (isDarkTheme) Color.White else Color(0xFF16A34A),
                modifier = Modifier.size(20.dp)
            )
        }

        // Property name
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            Text(
                text = property.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = titleFontSize,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Delete button or selection checkbox
        if (selectionMode) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggleSelect() },
                modifier = Modifier.size(28.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isDarkTheme) Color(0xFF374151) else Color(0xFFFEE2E2))
                    .clickable(onClick = onDeleteProperty),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Property",
                    tint = if (isDarkTheme) Color(0xFFEF4444) else Color(0xFFDC2626),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Optimized property info row component
 */
@Composable
private fun PropertyInfoRow(
    property: Property,
    tenantCount: Int,
    subtitleFontSize: androidx.compose.ui.unit.TextUnit,
    secondaryTextColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        // Address
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(2f)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location",
                tint = secondaryTextColor,
                modifier = Modifier.size(16.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            ) {
                Text(
                    text = property.address,
                    color = secondaryTextColor,
                    fontSize = subtitleFontSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Tenant count
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(100.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Tenants",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (tenantCount == 1) "1 tenant" else "$tenantCount tenants",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Optimized property statistics component
 */
@Composable
private fun PropertyStats(
    property: Property,
    textColor: Color,
    secondaryTextColor: Color,
    subtitleFontSize: androidx.compose.ui.unit.TextUnit,
    smallTextFontSize: androidx.compose.ui.unit.TextUnit,
    isDarkTheme: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
    ) {
        // Total Units
        StatItem(
            title = "Total Units",
            value = property.totalUnits.toString(),
            icon = "🏠",
            backgroundColor = if (isDarkTheme) Color(0xFF1E3A8A).copy(alpha = 0.3f) else Color(0xFFDBEAFE),
            iconBackgroundColor = if (isDarkTheme) Color(0xFF3B82F6) else Color(0xFF2563EB),
            textColor = textColor,
            secondaryTextColor = secondaryTextColor,
            subtitleFontSize = subtitleFontSize,
            smallTextFontSize = smallTextFontSize,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Floors
        StatItem(
            title = "Floors",
            value = property.floors.toString(),
            icon = "🏢",
            backgroundColor = if (isDarkTheme) Color(0xFF581C87).copy(alpha = 0.3f) else Color(0xFFF3E8FF),
            iconBackgroundColor = if (isDarkTheme) Color(0xFFA855F7) else Color(0xFF9333EA),
            textColor = textColor,
            secondaryTextColor = secondaryTextColor,
            subtitleFontSize = subtitleFontSize,
            smallTextFontSize = smallTextFontSize,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Optimized stat item component
 */
@Composable
private fun StatItem(
    title: String,
    value: String,
    icon: String,
    backgroundColor: Color,
    iconBackgroundColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    subtitleFontSize: androidx.compose.ui.unit.TextUnit,
    smallTextFontSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = smallTextFontSize,
                color = secondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(iconBackgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        fontSize = 10.sp,
                        color = Color.White
                    )
                }
                Text(
                    text = value,
                    fontWeight = FontWeight.Bold,
                    fontSize = subtitleFontSize,
                    color = textColor,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun AddPropertyBottomSheet(
//    onDismiss: () -> Unit,
//    onAddProperty: (Map<String, String>) -> Unit,
//    isDarkTheme: Boolean
//) {
//    val surfaceColor = if (isDarkTheme) Color(0xFF1F2937) else Color.White
//    val textColor = if (isDarkTheme) Color.White else Color.Black
//    val borderColor = if (isDarkTheme) Color(0xFF374151) else Color(0xFFE5E7EB)
//
//    ModalBottomSheet(
//        onDismissRequest = onDismiss,
//        containerColor = surfaceColor
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp)  // Spacing 4
//                .heightIn(min = 400.dp, max = 700.dp),
//            verticalArrangement = Arrangement.spacedBy(16.dp)  // Spacing 4
//        ) {
//            Text(
//                "Add New Property",
//                style = MaterialTheme.typography.headlineSmall,
//                color = textColor
//            )
//
//            var propertyName by remember { mutableStateOf("") }
//            OutlinedTextField(
//                value = propertyName,
//                onValueChange = { propertyName = it },
//                label = { Text("Property Name") },
//                modifier = Modifier.fillMaxWidth(),
//                shape = RoundedCornerShape(8.dp),  // Medium border radius
//                leadingIcon = {
//                    Icon(
//                        Icons.Default.Home,
//                        contentDescription = "Property",
//                        tint = MaterialTheme.colorScheme.primary
//                    )
//                },
//                colors = OutlinedTextFieldDefaults.colors(
//                    focusedBorderColor = MaterialTheme.colorScheme.primary,
//                    unfocusedBorderColor = borderColor,
//                    cursorColor = MaterialTheme.colorScheme.primary,
//                    focusedTextColor = textColor,
//                    unfocusedTextColor = textColor
//                )
//            )
//
//            var address by remember { mutableStateOf("") }
//            OutlinedTextField(
//                value = address,
//                onValueChange = { address = it },
//                label = { Text("Address") },
//                modifier = Modifier.fillMaxWidth(),
//                shape = RoundedCornerShape(8.dp),  // Medium border radius
//                leadingIcon = {
//                    Icon(
//                        Icons.Default.LocationOn,
//                        contentDescription = "Address",
//                        tint = MaterialTheme.colorScheme.primary
//                    )
//                },
//                colors = OutlinedTextFieldDefaults.colors(
//                    focusedBorderColor = MaterialTheme.colorScheme.primary,
//                    unfocusedBorderColor = borderColor,
//                    cursorColor = MaterialTheme.colorScheme.primary,
//                )
//            )
//
//            var totalUnits by remember { mutableStateOf("") }
//            OutlinedTextField(
//                value = totalUnits,
//                onValueChange = { totalUnits = it },
//                label = { Text("Total Units") },
//                modifier = Modifier.fillMaxWidth(),
//                shape = RoundedCornerShape(8.dp),  // Medium border radius
//                leadingIcon = {
//                    Icon(
//                        Icons.Default.Apartment,
//                        contentDescription = "Units",
//                        tint = MaterialTheme.colorScheme.primary
//                    )
//                },
//                colors = OutlinedTextFieldDefaults.colors(
//                    focusedBorderColor = MaterialTheme.colorScheme.primary,
//                    unfocusedBorderColor = borderColor,
//                    cursorColor = MaterialTheme.colorScheme.primary,
//                )
//            )
//
//            var floors by remember { mutableStateOf("") }
//            OutlinedTextField(
//                value = floors,
//                onValueChange = { floors = it },
//                label = { Text("Floors") },
//                modifier = Modifier.fillMaxWidth(),
//                shape = RoundedCornerShape(8.dp),  // Medium border radius
//                leadingIcon = {
//                    Icon(
//                        Icons.Default.Layers,
//                        contentDescription = "Floors",
//                        tint = MaterialTheme.colorScheme.primary
//                    )
//                },
//                colors = OutlinedTextFieldDefaults.colors(
//                    focusedBorderColor = MaterialTheme.colorScheme.primary,
//                    unfocusedBorderColor = borderColor,
//                    cursorColor = MaterialTheme.colorScheme.primary,
//                )
//            )
//
//            var notes by remember { mutableStateOf("") }
//            OutlinedTextField(
//                value = notes,
//                onValueChange = { notes = it },
//                label = { Text("Notes") },
//                placeholder = { Text("Additional information about the property") },
//                modifier = Modifier.fillMaxWidth(),
//                shape = RoundedCornerShape(8.dp),  // Medium border radius
//                leadingIcon = {
//                    Icon(
//                        Icons.AutoMirrored.Filled.Notes,
//                        contentDescription = "Notes",
//                        tint = MaterialTheme.colorScheme.primary
//                    )
//                },
//                minLines = 3,
//                colors = OutlinedTextFieldDefaults.colors(
//                    focusedBorderColor = MaterialTheme.colorScheme.primary,
//                    unfocusedBorderColor = borderColor,
//                    cursorColor = MaterialTheme.colorScheme.primary,
//                )
//            )
//
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.End
//            ) {
//                TextButton(
//                    onClick = onDismiss,
//                    colors = ButtonDefaults.textButtonColors(
//                        contentColor = MaterialTheme.colorScheme.primary
//                    )
//                ) {
//                    Text("Cancel")
//                }
//                Spacer(modifier = Modifier.width(8.dp))  // Spacing 2
//                Button(
//                    onClick = {
//                        onAddProperty(
//                            mapOf(
//                                "propertyName" to propertyName,
//                                "address" to address,
//                                "totalUnits" to totalUnits,
//                                "floors" to floors,
//                                "notes" to notes
//                            )
//                        )
//                    },
//                    colors = ButtonDefaults.buttonColors(
//                        containerColor = MaterialTheme.colorScheme.primary,
//                        contentColor = Color.White
//                    ),
//                    shape = CircleShape  // Full circle shape for primary buttons
//                ) {
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Icon(
//                            Icons.Default.Add,
//                            contentDescription = "Add",
//                            modifier = Modifier.size(16.dp)  // Small icon
//                        )
//                        Spacer(modifier = Modifier.width(8.dp))  // Spacing 2
//                        Text("Add Property")
//                    }
//                }
//            }
//        }
//    }
//}
