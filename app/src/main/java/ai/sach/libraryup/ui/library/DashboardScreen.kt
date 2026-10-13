package ai.sach.libraryup.ui.library

import ai.sach.libraryup.R
import ai.sach.libraryup.data.repository.LibraryState
import ai.sach.libraryup.domain.DueStatus
import ai.sach.libraryup.domain.Reports
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
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

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Brand header: library name and today's money, the first thing an owner checks.
        Column(
            Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .clickable(onClick = onOpenPayments)
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp)
        ) {
            val onBrand = MaterialTheme.colorScheme.onPrimary
            Text(state.profile?.name ?: stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, color = onBrand, fontWeight = FontWeight.Bold)
            Text(today.pretty(), style = MaterialTheme.typography.bodyMedium, color = onBrand.copy(alpha = 0.8f))
            Spacer(Modifier.height(20.dp))
            Row {
                HeroFigure(stringResource(R.string.dash_today_collection), rupees(todayTotal), Modifier.weight(1f))
                HeroFigure(stringResource(R.string.dash_month_collection), rupees(monthTotal), Modifier.weight(1f))
            }
        }

        Column(Modifier.padding(16.dp)) {
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(stringResource(R.string.dash_overdue), overdue.toString(), Modifier.weight(1f).fillMaxHeight(), StatusColors.full) { onOpenStudents(StudentFilter.OVERDUE) }
                StatCard(stringResource(R.string.dash_due_today), dueToday.toString(), Modifier.weight(1f).fillMaxHeight(), StatusColors.overdue) { onOpenStudents(StudentFilter.DUE) }
                StatCard(stringResource(R.string.dash_expiring), expiring.toString(), Modifier.weight(1f).fillMaxHeight(), StatusColors.partial) { onOpenStudents(StudentFilter.EXPIRING) }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(stringResource(R.string.dash_pending_amount), rupees(pending), Modifier.weight(1f).fillMaxHeight(), StatusColors.full) { onOpenStudents(StudentFilter.DUE) }
                StatCard(stringResource(R.string.dash_active_students), activeStudents.toString(), Modifier.weight(1f).fillMaxHeight(), MaterialTheme.colorScheme.primary) { onOpenStudents(StudentFilter.ALL) }
            }

            SectionTitle(stringResource(R.string.dash_quick_actions))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction(Icons.Default.PersonAdd, stringResource(R.string.action_add_student), onAddStudent, Modifier.weight(1f))
                QuickAction(Icons.Default.CurrencyRupee, stringResource(R.string.action_collect_fee), { onOpenStudents(StudentFilter.DUE) }, Modifier.weight(1f))
                QuickAction(Icons.Default.Campaign, stringResource(R.string.action_send_reminders), onOpenReminders, Modifier.weight(1f))
                QuickAction(Icons.Default.EventAvailable, stringResource(R.string.action_attendance), onOpenAttendance, Modifier.weight(1f))
            }

            SectionTitle(stringResource(R.string.dash_occupancy))
            val seats = state.activeSeats.size
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    if (seats == 0 || state.activeShifts.isEmpty()) {
                        Text(stringResource(R.string.dash_no_seats), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    state.activeShifts.takeIf { seats > 0 }.orEmpty().forEach { shift ->
                        val pct = Reports.shiftOccupancyPercent(seats, shift.window, state.bookings, today)
                        Row(Modifier.padding(vertical = 6.dp)) {
                            Text(shift.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("  ${shift.timeLabel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            Text("$pct%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { pct / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            trackColor = MaterialTheme.colorScheme.primaryContainer,
                            strokeCap = StrokeCap.Round,
                            drawStopIndicator = {}
                        )
                    }
                }
            }
            Row(Modifier.padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.dash_privacy_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HeroFigure(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
        Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
    }
}

/** Icon tile with a label underneath; four fit in a row on a small phone. */
@Composable
private fun QuickAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier) {
    Column(modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(56.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 2, textAlign = TextAlign.Center)
    }
}
