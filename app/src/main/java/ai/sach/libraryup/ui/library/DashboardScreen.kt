package ai.sach.libraryup.ui.library

import ai.sach.libraryup.R
import ai.sach.libraryup.data.repository.LibraryState
import ai.sach.libraryup.domain.DueStatus
import ai.sach.libraryup.domain.Reports
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.YearMonth

@Composable
fun DashboardScreen(
    state: LibraryState,
    onOpenStudents: (StudentFilter) -> Unit,
    onAddStudent: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenAttendance: () -> Unit,
    onOpenPayments: () -> Unit
) {
    val today = state.today
    val live = state.payments.filter { !it.deleted }
    val todayTotal = live.filter { it.paidOn == today }.sumOf { it.amount }
    val month = YearMonth.from(today)
    val monthTotal = live.filter { YearMonth.from(it.paidOn) == month }.sumOf { it.amount }
    val pending = state.students.sumOf { it.balance }
    val statuses = state.students.mapNotNull { it.status }
    val overdue = statuses.count { it == DueStatus.OVERDUE || it == DueStatus.EXPIRED }
    val dueToday = statuses.count { it == DueStatus.DUE_TODAY || it == DueStatus.UNPAID }
    val expiring = statuses.count { it == DueStatus.EXPIRING_SOON }
    val activeStudents = state.students.count { it.membership != null && it.status != null }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(24.dp))
        Text(state.profile?.name ?: stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
        Text(today.pretty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))

        // Money first: what came in, and what is still owed.
        Card(
            onClick = onOpenPayments,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(stringResource(R.string.dash_month_collection), style = MaterialTheme.typography.labelLarge)
                Text(rupees(monthTotal), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Row {
                    Figure(stringResource(R.string.dash_today_collection), rupees(todayTotal), Modifier.weight(1f))
                    Figure(stringResource(R.string.dash_pending_amount), rupees(pending), Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(stringResource(R.string.dash_overdue), overdue.toString(), Modifier.weight(1f).fillMaxHeight(), DueStatus.OVERDUE.tone()) { onOpenStudents(StudentFilter.OVERDUE) }
            StatCard(stringResource(R.string.dash_due_today), dueToday.toString(), Modifier.weight(1f).fillMaxHeight(), DueStatus.DUE_TODAY.tone()) { onOpenStudents(StudentFilter.DUE) }
            StatCard(stringResource(R.string.dash_expiring), expiring.toString(), Modifier.weight(1f).fillMaxHeight(), DueStatus.EXPIRING_SOON.tone()) { onOpenStudents(StudentFilter.EXPIRING) }
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Shortcut(Icons.Default.PersonAdd, stringResource(R.string.action_add_student), onAddStudent)
            Shortcut(Icons.Default.CurrencyRupee, stringResource(R.string.action_collect_fee)) { onOpenStudents(StudentFilter.DUE) }
            Shortcut(Icons.Default.Campaign, stringResource(R.string.action_send_reminders), onOpenReminders)
            Shortcut(Icons.Default.EventAvailable, stringResource(R.string.action_attendance), onOpenAttendance)
        }

        val seats = state.activeSeats.size
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.dash_occupancy), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.dash_students_count, activeStudents), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (seats == 0 || state.activeShifts.isEmpty()) {
                    Text(stringResource(R.string.dash_no_seats), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                state.activeShifts.takeIf { seats > 0 }.orEmpty().forEach { shift ->
                    val pct = Reports.shiftOccupancyPercent(seats, shift.window, state.bookings, today)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row {
                            Text(shift.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text("$pct%", style = MaterialTheme.typography.labelLarge)
                        }
                        LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        Row(Modifier.padding(vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.dash_privacy_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Figure(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}

/** A round tonal shortcut with a label, as in Google Pay / Files. */
@Composable
private fun Shortcut(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(Modifier.width(76.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(56.dp)) { Icon(icon, contentDescription = null) }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2)
    }
}
