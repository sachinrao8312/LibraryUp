package ai.sach.rentup.ui.common.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bottom bar shown during multi-select mode with the selected count and delete action.
 */
@Composable
fun BatchDeleteBar(
    selectedCount: Int,
    itemLabel: String,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val barColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(barColor, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (selectedCount == 0) "Tap tenants to select" else "$selectedCount $itemLabel selected",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onDelete,
                    enabled = selectedCount > 0
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete selected",
                        tint = if (selectedCount > 0) Color.Red else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Delete",
                        color = if (selectedCount > 0) Color.Red else Color.Gray,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                }
            }
        }
    }
}
