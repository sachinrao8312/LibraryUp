package ai.sach.libraryup.ui.library

import ai.sach.libraryup.R
import ai.sach.libraryup.data.repository.LibraryState
import ai.sach.libraryup.domain.DueStatus
import ai.sach.libraryup.domain.Reports
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
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

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text(state.profile?.name ?: stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
        Text(today.pretty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(stringResource(R.string.dash_today_collection), rupees(todayTotal), Modifier.weight(1f), onClick = onOpenPayments)
            StatCard(stringResource(R.string.dash_month_collection), rupees(monthTotal), Modifier.weight(1f), onClick = onOpenPayments)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(stringResource(R.string.dash_overdue), overdue.toString(), Modifier.weight(1f), StatusColors.full) { onOpenStudents(StudentFilter.OVERDUE) }
            StatCard(stringResource(R.string.dash_due_today), dueToday.toString(), Modifier.weight(1f), StatusColors.overdue) { onOpenStudents(StudentFilter.DUE) }
            StatCard(stringResource(R.string.dash_expiring), expiring.toString(), Modifier.weight(1f), StatusColors.partial) { onOpenStudents(StudentFilter.EXPIRING) }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(stringResource(R.string.dash_pending_amount), rupees(pending), Modifier.weight(1f)) { onOpenStudents(StudentFilter.DUE) }
            StatCard(stringResource(R.string.dash_active_students), activeStudents.toString(), Modifier.weight(1f)) { onOpenStudents(StudentFilter.ALL) }
        }

        SectionTitle(stringResource(R.string.dash_quick_actions))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            QuickAction(Icons.Default.PersonAdd, stringResource(R.string.action_add_student), onAddStudent, Modifier.weight(1f))
            QuickAction(Icons.Default.CurrencyRupee, stringResource(R.string.action_collect_fee), { onOpenStudents(StudentFilter.DUE) }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            QuickAction(Icons.Default.Campaign, stringResource(R.string.action_send_reminders), onOpenReminders, Modifier.weight(1f))
            QuickAction(Icons.Default.EventAvailable, stringResource(R.string.action_attendance), onOpenAttendance, Modifier.weight(1f))
        }

        SectionTitle(stringResource(R.string.dash_occupancy))
        val seats = state.activeSeats.size
        if (seats == 0 || state.activeShifts.isEmpty()) {
            Text(stringResource(R.string.dash_no_seats), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            state.activeShifts.forEach { shift ->
                val pct = Reports.shiftOccupancyPercent(seats, shift.window, state.bookings, today)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("${shift.name} · ${shift.timeLabel}", style = MaterialTheme.typography.bodyMedium)
                        LinearProgressIndicator(
                            progress = { pct / 100f },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        )
                    }
                    Text("  $pct%", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.dash_privacy_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier) {
    FilledTonalButton(onClick = onClick, modifier = modifier.height(56.dp)) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.padding(4.dp))
        Text(label, maxLines = 2, style = MaterialTheme.typography.labelLarge)
    }
}
