package ai.sach.rentup.ui.screens.homescreen.composable

import RentalAmountButtons
import ai.sach.rentup.data.local.entity.PaymentHistory
import ai.sach.rentup.data.local.entity.Tenant
import ai.sach.rentup.data.local.entity.TenantFloors
import ai.sach.rentup.data.viewmodel.TenantViewModel
import ai.sach.rentup.ui.common.responsiveness.rememberDimensions
import ai.sach.rentup.ui.screens.homescreen.PaymentStatus
import ai.sach.rentup.ui.screens.homescreen.Quadruple
import ai.sach.rentup.ui.screens.homescreen.TenantAvatar
import ai.sach.rentup.ui.screens.homescreen.getPaymentStatus
import ai.sach.rentup.ui.theme.RentUpColors
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Optimized function to get status information based on payment status
 */
private fun getStatusInfo(paymentStatus: PaymentStatus, isDarkTheme: Boolean): Quadruple<Color, Color, String, ImageVector> {
    return when (paymentStatus) {
        PaymentStatus.PAID -> {
            val color = Color(0xFF4CAF50)
            Quadruple(
                color,
                if (isDarkTheme) color.copy(alpha = 0.2f) else Color(0xFFE6F9F1),
                "Paid",
                Icons.Default.Check
            )
        }
        PaymentStatus.DUE_SOON -> {
            val color = Color(0xFFF59E0B)
            Quadruple(
                if (isDarkTheme) color.copy(alpha = 0.9f) else color,
                if (isDarkTheme) color.copy(alpha = 0.2f) else Color(0xFFFEF3C7),
                "Due Soon",
                Icons.Default.AccessTime
            )
        }
        PaymentStatus.OVERDUE -> {
            val color = if (isDarkTheme) Color(0xFFFF6B6B) else RentUpColors.Red
            Quadruple(
                color,
                if (isDarkTheme) color.copy(alpha = 0.2f) else Color(0xFFFEE9E9),
                "Overdue",
                Icons.Default.Warning
            )
        }
        PaymentStatus.DUE -> {
            val color = if (isDarkTheme) Color(0xFFB0B0B0) else Color.Gray
            Quadruple(
                color,
                if (isDarkTheme) color.copy(alpha = 0.2f) else Color(0xFFF3F4F6),
                "Due",
                Icons.Default.Schedule
            )
        }
    }
}

/**
 * Optimized status chip component
 */
@Composable
private fun StatusChip(
    paymentStatus: PaymentStatus,
    daysUntilDue: Int?,
    statusColor: Color,
    statusBgColor: Color,
    statusIcon: ImageVector,
    isDarkTheme: Boolean
) {
    val statusText = when (paymentStatus) {
        PaymentStatus.OVERDUE -> "${abs(daysUntilDue ?: 0)} days"
        PaymentStatus.DUE_SOON -> "Due in ${daysUntilDue ?: 0} days"
        else -> when (paymentStatus) {
            PaymentStatus.PAID -> "Paid"
            PaymentStatus.DUE -> "Due"
            else -> "Unknown"
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = statusBgColor,
        modifier = Modifier
            .heightIn(min = 28.dp, max = 32.dp)
            .widthIn(min = 110.dp, max = 130.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = statusText,
                tint = statusColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = statusColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Optimized info row component
 */
@Composable
private fun InfoRow(
    icon: ImageVector,
    text: String,
    textColor: Color,
    iconSize: Int = 20,
    fontSize: Int = 16
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(iconSize.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = fontSize.sp,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Optimized tenant header section
 */
@Composable
private fun TenantHeader(
    tenant: Tenant,
    propertyName: String,
    statusInfo: Quadruple<Color, Color, String, ImageVector>,
    paymentStatus: PaymentStatus,
    daysUntilDue: Int?,
    memberText: String,
    isOverdue: Boolean,
    hasMultipleProperties: Boolean,
    propertyIds: List<String>,
    isDarkTheme: Boolean
) {
    val (statusColor, statusBgColor, _, statusIcon) = statusInfo
    val primaryTextColor = if (isDarkTheme) Color.White else Color.Black
    val secondaryTextColor = if (isDarkTheme) Color(0xFFB0B0B0) else Color.Gray

    Column(modifier = Modifier.fillMaxWidth()) {
        // Row 1: Tenant name and status chip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tenant.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = primaryTextColor,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 28.dp)
            )

            StatusChip(
                paymentStatus = paymentStatus,
                daysUntilDue = daysUntilDue,
                statusColor = statusColor,
                statusBgColor = statusBgColor,
                statusIcon = statusIcon,
                isDarkTheme = isDarkTheme
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2: Members and due date
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoRow(
                icon = Icons.Default.People,
                text = memberText,
                textColor = secondaryTextColor
            )

            Box(modifier = Modifier.widthIn(min = 120.dp)) {
                InfoRow(
                    icon = Icons.Default.DateRange,
                    text = tenant.dueDate,
                    textColor = if (isOverdue) statusColor else secondaryTextColor
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row 3: Property information
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Property",
                tint = secondaryTextColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = TenantFloors.display(tenant.floor.split(",").firstOrNull().orEmpty())
                    .let { floors -> if (floors.isNotBlank()) "$propertyName, $floors" else propertyName },
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = secondaryTextColor,
                modifier = Modifier.weight(1f)
            )
            
            if (hasMultipleProperties) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "+${propertyIds.size - 1} more property${if (propertyIds.size - 1 == 1) "" else "ies"}",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TenantCard(
    tenant: Tenant,
    propertyName: String,
    onTenantClick: () -> Unit,
    onDeleteTenant: () -> Unit,
    viewModel: TenantViewModel,
    onTenantUpdated: (Tenant) -> Unit,
    onEditTenant: () -> Unit,
    isDarkTheme: Boolean,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onTenantLongClick: () -> Unit = {},
    key: String? = null
) {
    // Track current tenant state to ensure UI updates
    var currentTenant by remember { mutableStateOf(tenant) }
    
    // Update current tenant when the tenant prop changes
    LaunchedEffect(tenant) {
        currentTenant = tenant
    }

    // Helper function to format the remark
    fun getDefaultRemark(): String {
        val month = LocalDate.now().month.name.lowercase()
        return month.replaceFirstChar { it.uppercase() } + " rent payment"
    }

    // Helper function to handle rent collection
    fun handleRentCollection(
        collectedAmount: Double,
        collectedBy: String,
        remark: String
    ) {
        android.util.Log.d("TenantCard", "handleRentCollection called: amount=$collectedAmount, collectedBy=$collectedBy, remark=$remark")
        try {
            if (currentTenant.id > 0) {
                android.util.Log.d("TenantCard", "Inserting payment for tenant ID: ${currentTenant.id}")
                
                // First, insert the payment
                viewModel.insertPayment(
                    PaymentHistory(
                        tenantId = 0,
                        id = 0,
                        date = LocalDate.now().toString(),
                        amount = collectedAmount.toString(),
                        collectedBy = collectedBy,
                        remark = if (remark.isBlank()) getDefaultRemark() else remark
                    ),
                    currentTenant.id
                )

                // Create updated tenant with paid status
                val updatedTenant = currentTenant.copy(
                    isPaid = true,
                    status = "Paid"
                )
                
                android.util.Log.d("TenantCard", "Updating tenant: ${currentTenant.id} to paid status")
                
                // Update the tenant status
                viewModel.updateTenant(updatedTenant)
                
                // Update local state immediately for UI responsiveness
                currentTenant = updatedTenant
                
                // Notify parent component of the update
                android.util.Log.d("TenantCard", "Notifying parent component of tenant update")
                onTenantUpdated(updatedTenant)
                
                android.util.Log.d("TenantCard", "Rent collection completed successfully")
            } else {
                android.util.Log.e("TenantCard", "Invalid tenant ID: ${currentTenant.id}")
            }
        } catch (e: Exception) {
            android.util.Log.e("TenantCard", "Error collecting rent: ${e.message}", e)
        }
    }

    // Optimized state management using stable data class
    val dimensions = rememberDimensions()
    
    // Family members data
    val familyMembers by viewModel.getFamilyInfoForTenant(currentTenant.id).observeAsState(emptyList())
    
    // Optimized state calculations using remember with proper keys
    val memberCount by remember { derivedStateOf { familyMembers.size + 1 } }
    val memberText by remember { derivedStateOf { "$memberCount members" } }

    // Property data
    val propertyIds by remember { derivedStateOf { currentTenant.propertyId.split(",").filter { it.isNotBlank() } } }
    val hasMultipleProperties by remember { derivedStateOf { propertyIds.size > 1 } }

    // Payment status
    val paymentStatus by remember { derivedStateOf { getPaymentStatus(currentTenant.dueDate, currentTenant.isPaid) } }
    val statusInfo by remember { derivedStateOf { getStatusInfo(paymentStatus, isDarkTheme) } }

    // Date calculations
    val now = remember { LocalDate.now() }
    val isOverdue by remember { 
        derivedStateOf { 
            try {
                !currentTenant.isPaid && LocalDate.parse(currentTenant.dueDate).isBefore(now)
            } catch (e: Exception) { false }
        }
    }
    val daysUntilDue by remember { 
        derivedStateOf { 
            try {
                if (!currentTenant.isPaid) {
                    val dueDate = LocalDate.parse(currentTenant.dueDate)
                    ChronoUnit.DAYS.between(now, dueDate).toInt()
                } else null
            } catch (e: Exception) { null }
        }
    }

    // Card styling
    val cardBgColor by remember { derivedStateOf { if (isDarkTheme) Color(0xFF1E1E1E) else RentUpColors.White } }
    val cardBorder by remember { 
        derivedStateOf { 
            if (!currentTenant.isPaid) {
                val daysDue = daysUntilDue
                when {
                    isOverdue -> BorderStroke(1.dp, if (isDarkTheme) Color(0xFFFF6B6B) else Color.Red)
                    daysDue != null && daysDue <= 3 -> BorderStroke(1.dp, if (isDarkTheme) Color(0xFFFFA726) else Color(0xFFFF9800))
                    else -> BorderStroke(1.dp, if (isDarkTheme) Color(0xFF757575) else Color.Gray)
                }
            } else null
        }
    }

    // UI state
    var expanded by remember { mutableStateOf(false) }
    var isBottomSheetVisible by remember { mutableStateOf(false) }
    val payments by viewModel.getPaymentsForTenant(currentTenant.id).observeAsState(emptyList<PaymentHistory>())

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensions.spacingSmall)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = when (paymentStatus) {
                    PaymentStatus.OVERDUE -> dimensions.cardElevationLarge
                    PaymentStatus.DUE_SOON -> dimensions.cardElevationMedium
                    else -> dimensions.cardElevationSmall
                }
            ),
            border = cardBorder
        ) {
            Column(modifier = Modifier.padding(
                horizontal = dimensions.paddingMedium,
                vertical = dimensions.paddingMedium
            )) {
                // Main content area - make only this area clickable
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .combinedClickable(
                            onLongClick = onTenantLongClick,
                            onClick = {
                            if (selectionMode) {
                                onTenantClick()
                            } else {
                                isBottomSheetVisible = true
                                onTenantClick()
                            }
                            }
                        ),
                    verticalAlignment = Alignment.Top
                ) {
                    if (selectionMode) {
                        Checkbox(
                            checked = selected,
                            onCheckedChange = { onTenantClick() },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }

                    TenantAvatar(
                        avatarUri = currentTenant.avatarUri,
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.size(70.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    TenantHeader(
                        tenant = currentTenant,
                        propertyName = propertyName,
                        statusInfo = statusInfo,
                        paymentStatus = paymentStatus,
                        daysUntilDue = daysUntilDue,
                        memberText = memberText,
                        isOverdue = isOverdue,
                        hasMultipleProperties = hasMultipleProperties,
                        propertyIds = propertyIds,
                        isDarkTheme = isDarkTheme
                    )
                }

                // Rent Collection Button
                if (!currentTenant.isPaid && currentTenant.amount != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    RentalAmountButtons(
                        amount = currentTenant.amount!!, // Safe to use !! here since we checked for null above
                        tenantName = currentTenant.name,
                        dueDate = currentTenant.dueDate,
                        tenant = currentTenant,
                        isDarkTheme = isDarkTheme,
                        onRentCollect = { collectedAmount, collectedBy, remark ->
                            handleRentCollection(collectedAmount.toDouble(), collectedBy, remark)
                        }
                    )
                }

                // Payment history sections
                ShowDetailsButton(
                    expanded = expanded,
                    onClick = { expanded = !expanded },
                    isDarkTheme = isDarkTheme
                )

                PaymentHistoryDetails(
                    expanded = expanded,
                    payments = payments,
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }

    // Bottom sheet
    if (isBottomSheetVisible) {
        TenantDetailBottomSheet(
            tenant = currentTenant,
            familyMembers = familyMembers,
            onClose = { isBottomSheetVisible = false },
            viewModel = viewModel
        )
    }
}
