@file:OptIn(ExperimentalMaterial3Api::class)

package ai.sach.libraryup.ui.screens.historyScreen

import ai.sach.libraryup.LibraryUpApplication
import ai.sach.libraryup.data.viewmodel.TenantViewModel
import ai.sach.libraryup.data.viewmodel.TenantViewModelFactory
import ai.sach.libraryup.ui.common.composables.AppSearchBarWithDebounce
import ai.sach.libraryup.ui.common.composables.AppTopAppBar
import ai.sach.libraryup.ui.screens.settingsScreen.appprefernecs.formatWithCurrency
import ai.sach.libraryup.ui.theme.AppColors
import ai.sach.libraryup.ui.screens.settingsScreen.composable.SettingsNavRoutes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.navigation.NavController

private fun logError(tag: String, message: String, throwable: Throwable? = null) {
    android.util.Log.e(tag, message, throwable)
}

data class PaymentHistoryItem(
    val id: Long,
    val tenantName: String,
    val propertyName: String,
    val amount: String,
    val date: String,
    val collectedBy: String,
    val remark: String?,
    val avatarUri: String? = null  // Added new field for tenant avatar URI
)

data class PaymentHistoryUiState(
    val searchQuery: String = "",
    val debouncedQuery: String = "",
    val isFiltersVisible: Boolean = false,
    val dateRangeText: String = "Pick a date range",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val amountRange: ClosedFloatingPointRange<Float> = 0f..2000f
)

fun filterAndSortPayments(
    payments: List<PaymentHistoryItem>,
    query: String,
    startDate: LocalDate?,
    endDate: LocalDate?,
    amountRange: ClosedFloatingPointRange<Float>
): List<PaymentHistoryItem> {
    return payments.filter { payment ->
        val matchesQuery = query.isEmpty() ||
            payment.tenantName.contains(query, ignoreCase = true) ||
            payment.propertyName.contains(query, ignoreCase = true) ||
            payment.date.contains(query, ignoreCase = true)
        val amountValue = payment.amount.replace("$", "").replace(",", "").trim().toFloatOrNull() ?: 0f
        val matchesAmount = amountValue >= amountRange.start && amountValue <= amountRange.endInclusive
        val matchesDate = if (startDate != null && endDate != null) {
            try {
                val paymentDate = try {
                    LocalDate.parse(payment.date)
                } catch (e: Exception) {
                    null
                }
                paymentDate != null &&
                    (paymentDate.isEqual(startDate) || paymentDate.isAfter(startDate)) &&
                    (paymentDate.isEqual(endDate) || paymentDate.isBefore(endDate))
            } catch (e: Exception) {
                false
            }
        } else {
            true
        }
        matchesQuery && matchesAmount && matchesDate
    }.sortedByDescending {
        try {
            LocalDate.parse(it.date)
        } catch (e: Exception) {
            LocalDate.now()
        }
    }
}

@Composable
fun EmptyPaymentHistoryState(isDarkTheme: Boolean, searchQuery: String, startDate: LocalDate?, endDate: LocalDate?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "No Results",
            tint = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Gray,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No payment records found",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.DarkGray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (searchQuery.isNotEmpty() || startDate != null || endDate != null)
                "Try adjusting your search or filters"
            else
                "Payment records will appear here",
            fontSize = 16.sp,
            color = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Main screen for displaying payment history records with filtering capabilities
 * Designed with larger UI elements and clearer organization for better usability
 */
@Composable
fun PaymentHistoryScreen(navController: NavController? = null) {
    val context = LocalContext.current
    val viewModel: TenantViewModel = viewModel(
        factory = TenantViewModelFactory(
            (context.applicationContext as LibraryUpApplication).tenantRepository,
            (context.applicationContext as LibraryUpApplication).propertyRepository
        )
    )
    LaunchedEffect(Unit) {
        viewModel.ensurePaymentDataInitialized()
    }
    val paymentItems by viewModel.allPaymentsWithTenantInfo.collectAsState()
    val isPaymentDataLoading by viewModel.isPaymentDataLoading.collectAsState()
    val tenants by viewModel.allTenants.observeAsState(emptyList())
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) Color(0xFF121212) else AppColors.BackgroundGray
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    // Centralized UI state - optimized to prevent unnecessary recompositions
    var uiState by remember {
        mutableStateOf(
            PaymentHistoryUiState(
                amountRange = 0f..2000f // Default range, will be updated when data loads
            )
        )
    }
    
    // Calculate true min/max from all payment amounts (not filtered)
    val trueAmountRange = remember(paymentItems) {
        if (paymentItems.isNotEmpty()) {
            val amounts = paymentItems.map {
                it.amount.replace("$", "").replace(",", "").trim().toFloatOrNull() ?: 0f
            }
            val minAmount = amounts.minOrNull() ?: 0f
            val maxAmount = amounts.maxOrNull() ?: 2000f
            minAmount..(maxAmount + 100f)
        } else {
            0f..2000f
        }
    }
    
    // Update amount range when payment items change
    LaunchedEffect(paymentItems) {
        if (paymentItems.isNotEmpty()) {
            uiState = uiState.copy(
                amountRange = trueAmountRange
            )
        }
    }
    val filteredPayments = filterAndSortPayments(
        paymentItems,
        uiState.debouncedQuery,
        uiState.startDate,
        uiState.endDate,
        uiState.amountRange
    )
    Scaffold(
        topBar = {
            AppTopAppBar(
                title = "Payment History",
                showDefaultMenu = true,
                onNavigateToBackup = { navController?.navigate(SettingsNavRoutes.BACKUP) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize()
                .background(backgroundColor)
                .padding(horizontal = 16.dp)
        ) {
            AppSearchBarWithDebounce(
                query = uiState.searchQuery,
                onQueryChange = { uiState = uiState.copy(searchQuery = it) },
                onDebouncedQueryChange = { uiState = uiState.copy(debouncedQuery = it) },
                debounceTime = 300L,
                placeholder = "Search by tenant, property or date...",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                isDarkTheme = isDarkTheme
            )
            Button(
                onClick = { uiState = uiState.copy(isFiltersVisible = !uiState.isFiltersVisible) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.FilterList,
                        contentDescription = "Filter",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Filter Payments",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (uiState.isFiltersVisible) {
                FiltersSection(
                    dateRangeText = uiState.dateRangeText,
                    startDate = uiState.startDate,
                    endDate = uiState.endDate,
                    onDateRangeSelected = { start, end ->
                        uiState = uiState.copy(startDate = start, endDate = end, dateRangeText = if (start != null && end != null) {
                            "${start.format(DateTimeFormatter.ISO_LOCAL_DATE)} to ${end.format(DateTimeFormatter.ISO_LOCAL_DATE)}"
                        } else {
                            "Pick a date range"
                        })
                    },
                    amountRange = uiState.amountRange,
                    onAmountRangeChange = { uiState = uiState.copy(amountRange = it) },
                    sliderMin = trueAmountRange.start,
                    sliderMax = trueAmountRange.endInclusive,
                    onApply = {},
                    onDismiss = { uiState = uiState.copy(isFiltersVisible = false) },
                    isDarkTheme = isDarkTheme,
                    allPayments = paymentItems.toList(),
                    onResetFilters = {
                        uiState = uiState.copy(
                            startDate = null,
                            endDate = null,
                            dateRangeText = "Pick a date range",
                            amountRange = trueAmountRange,
                            searchQuery = "",
                            debouncedQuery = ""
                        )
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (isPaymentDataLoading) {
                // Show loading indicator
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading payment history...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color.DarkGray,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Text(
                    text = "Showing ${filteredPayments.size} payment records",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color.DarkGray,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredPayments) { payment ->
                        EnhancedPaymentHistoryCard(
                            payment = payment,
                            isDarkTheme = isDarkTheme
                        )
                    }
                    if (filteredPayments.isEmpty()) {
                        item {
                            EmptyPaymentHistoryState(
                                isDarkTheme = isDarkTheme,
                                searchQuery = uiState.searchQuery,
                                startDate = uiState.startDate,
                                endDate = uiState.endDate
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun FiltersSection(
    dateRangeText: String,
    startDate: LocalDate?,
    endDate: LocalDate?,
    onDateRangeSelected: (LocalDate?, LocalDate?) -> Unit,
    amountRange: ClosedFloatingPointRange<Float>,
    onAmountRangeChange: (ClosedFloatingPointRange<Float>) -> Unit,
    sliderMin: Float,
    sliderMax: Float,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean,
    allPayments: List<PaymentHistoryItem>,
    onResetFilters: () -> Unit
) {
    val cardBackgroundColor = if (isDarkTheme) {
        Color(0xFF2A2A2A)
    } else {
        Color.White
    }

    val textColor = if (isDarkTheme) Color.White else Color.Black

    // Date picker states
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Separate date states for start and end dates
    var tempStartDate by remember { mutableStateOf(startDate) }
    var tempEndDate by remember { mutableStateOf(endDate) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(
            containerColor = cardBackgroundColor
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Date Range section with larger elements
            Text(
                "Date Range",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = textColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Start Date Button - larger and more clear
            Button(
                onClick = { showStartDatePicker = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkTheme) Color(0xFF333333) else Color(0xFFF3F4F6)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp), // Taller button for easier tapping
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Start Date",
                        tint = if (isDarkTheme) Color.White else Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = tempStartDate?.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")) ?: "Select Start Date",
                        color = if (isDarkTheme) Color.White else Color.Black,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // End Date Button - larger and more clear
            Button(
                onClick = { showEndDatePicker = true },
                enabled = tempStartDate != null, // Only enable if start date is selected
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkTheme) Color(0xFF333333) else Color(0xFFF3F4F6),
                    disabledContainerColor = if (isDarkTheme) Color(0xFF222222) else Color(0xFFE0E0E0)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp), // Taller button for easier tapping
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(
                        Icons.Default.Event,
                        contentDescription = "End Date",
                        tint = if (isDarkTheme) Color.White else Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = tempEndDate?.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")) ?: "Select End Date",
                        color = if (isDarkTheme) Color.White else Color.Black,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Amount Range Filter
            Text(
                "Payment Amount Range",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = textColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Amount range display
            Text(
                text = "Min: ${formatWithCurrency(amountRange.start.toInt().toString())} - Max: ${formatWithCurrency(
                    amountRange.endInclusive.toInt().toString()
                )}",
                fontSize = 16.sp,
                color = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color.DarkGray
            )

            Spacer(modifier = Modifier.height(12.dp))

            // RangeSlider with more visible track and thumb
            RangeSlider(
                value = amountRange,
                onValueChange = onAmountRangeChange,
                valueRange = sliderMin..sliderMax, // Dynamic range
                steps = ((sliderMax - sliderMin) / 100f).toInt() - 1, // $100 steps
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = if (isDarkTheme) Color.DarkGray else Color.LightGray,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Filter action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Reset filters button
                OutlinedButton(
                    onClick = {
                        onResetFilters()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isDarkTheme) Color.White else Color.Black
                    ),
                    border = BorderStroke(1.dp, if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color.Gray),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset Filters", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Apply filters button
                Button(
                    onClick = {
                        // Apply the date range selection
                        onDateRangeSelected(tempStartDate, tempEndDate)
                        onApply()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apply Filters", fontSize = 16.sp)
                }
            }
        }
    }

    // Show Date Pickers when needed
    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            onDateSelected = { selectedDate ->
                tempStartDate = selectedDate
                // Reset end date if it's before the new start date
                if (tempEndDate != null && tempEndDate!!.isBefore(selectedDate)) {
                    tempEndDate = null
                }
                showStartDatePicker = false
                // Enable end date picker after selecting start date
                if (tempEndDate == null) {
                    showEndDatePicker = true
                }
            },
            initialSelectedDate = tempStartDate,
            isDarkTheme = isDarkTheme
        )
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            onDateSelected = { selectedDate ->
                tempEndDate = selectedDate
                showEndDatePicker = false
                // Call the callback function with the selected range
                onDateRangeSelected(tempStartDate, selectedDate)
            },
            initialSelectedDate = tempEndDate,
            minDate = tempStartDate, // End date must be after start date
            isDarkTheme = isDarkTheme
        )
    }
}


/**
 * Custom date picker dialog with larger, easier to use controls
 */
@Composable
fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    initialSelectedDate: LocalDate? = null,
    minDate: LocalDate? = null,
    isDarkTheme: Boolean
) {
    val backgroundColor = if (isDarkTheme) Color(0xFF2A2A2A) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black

    // State for the selected date
    var selectedDate by remember { mutableStateOf(initialSelectedDate ?: LocalDate.now()) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                // If there's a minimum date, check that the selected date is after it
                if (minDate != null) {
                    val date = Instant.ofEpochMilli(utcTimeMillis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    return !date.isBefore(minDate)
                }
                return true
            }
        }
    )

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .wrapContentSize()
            ) {
                Text(
                    text = "Select Date",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(8.dp),
                    color = textColor
                )

                // Add the actual date picker
                DatePicker(
                    state = datePickerState,
                    colors = DatePickerDefaults.colors(
                        selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                        todayContentColor = MaterialTheme.colorScheme.primary,
                        todayDateBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismissRequest
                    ) {
                        Text(
                            "Cancel",
                            fontSize = 16.sp,
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val localDate = Instant.ofEpochMilli(millis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                onDateSelected(localDate)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            "OK",
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}