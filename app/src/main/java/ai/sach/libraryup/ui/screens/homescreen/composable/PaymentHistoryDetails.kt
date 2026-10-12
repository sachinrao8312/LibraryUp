
package ai.sach.rentup.ui.screens.homescreen.composable

import ai.sach.rentup.data.local.entity.PaymentHistory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp


/**
 * Responsive payment history section with proper scrolling and adaptive column widths
 */
@Composable
fun PaymentHistoryDetails(expanded: Boolean, payments: List<PaymentHistory>, isDarkTheme: Boolean) {
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(animationSpec = tween(300)),
        exit = shrinkVertically(animationSpec = tween(300))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // Use BoxWithConstraints to get the available width and calculate responsive column widths
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp) // Limit maximum height
            ) {
                val availableWidth = maxWidth
                val columnSpacing = 16.dp

                // Responsive column widths that adapt to screen size
                val dateWidth = (availableWidth * 0.18f).coerceAtLeast(80.dp)
                val amountWidth = (availableWidth * 0.18f).coerceAtLeast(80.dp)
                val collectorWidth = (availableWidth * 0.28f).coerceAtLeast(100.dp)
                val remarkWidth = (availableWidth * 0.36f).coerceAtLeast(120.dp)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Date",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Gray,
                            modifier = Modifier.width(dateWidth)
                        )
                        Text(
                            text = "Amount",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Gray,
                            modifier = Modifier.width(amountWidth)
                        )
                        Text(
                            text = "Collected By",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Gray,
                            modifier = Modifier.width(collectorWidth)
                        )
                        Text(
                            text = "Remark",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Gray,
                            modifier = Modifier.width(remarkWidth)
                        )
                    }

                    if (payments.isEmpty()) {
                        Text(
                            text = "No payment history available",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textAlign = TextAlign.Center,
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Gray
                        )
                    } else {
                        // Use LazyColumn for better performance with many rows
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 180.dp)
                        ) {
                            // Limit to 5 most recent payments for compact viewing
                            val recentPayments = payments.sortedByDescending { it.date }.take(5)

                            items(recentPayments) { payment ->
                                val backgroundColor = if (isDarkTheme) {
                                    Color(0xFF252525)
                                } else {
                                    if (payments.indexOf(payment) % 2 == 0) Color(0xFFF5F5F5) else Color.White
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(backgroundColor)
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = payment.date,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.width(dateWidth),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isDarkTheme) Color.White else Color.Black
                                    )
                                    Text(
                                        text = payment.amount,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.width(amountWidth),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isDarkTheme) Color.White else Color.Black
                                    )
                                    Text(
                                        text = payment.collectedBy,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.width(collectorWidth),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isDarkTheme) Color.White else Color.Black
                                    )
                                    Text(
                                        text = payment.remark ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.width(remarkWidth),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isDarkTheme) Color.White else Color.Black
                                    )
                                }

                                // Add divider except after the last item
                                if (payment != recentPayments.last()) {
                                    HorizontalDivider(
                                        color = if (isDarkTheme)
                                            Color.White.copy(alpha = 0.05f)
                                        else
                                            Color.Gray.copy(alpha = 0.1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}