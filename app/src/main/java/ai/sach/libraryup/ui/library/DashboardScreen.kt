package ai.sach.libraryup.ui.library

import ai.sach.libraryup.R
import ai.sach.libraryup.data.local.entity.ActivityLog
import ai.sach.libraryup.data.repository.LibraryState
import ai.sach.libraryup.domain.DueStatus
import ai.sach.libraryup.domain.Reports
import android.text.format.DateUtils
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    state: LibraryState,
    onOpenStudents: (StudentFilter) -> Unit,
    onAddStudent: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenAttendance: () -> Unit,
    onOpenPayments: () -> Unit,
    recent: List<ActivityLog> = emptyList(),
    onOpenLog: () -> Unit = {}
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
        val libraryName = state.profile?.name ?: stringResource(R.string.app_name)
        val owner = state.profile?.ownerName?.trim()?.substringBefore(' ').orEmpty()
        val greeting = when (LocalTime.now().hour) { in 0..11 -> R.string.greet_morning; in 12..16 -> R.string.greet_afternoon; else -> R.string.greet_evening }
        Text(if (owner.isEmpty()) libraryName else stringResource(greeting, owner), style = MaterialTheme.typography.headlineMedium)
        Text(
            if (owner.isEmpty()) today.pretty() else "$libraryName · ${today.pretty()}",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
                trend(live.filter { YearMonth.from(it.paidOn) == month.minusMonths(1) && it.paidOn.dayOfMonth <= today.dayOfMonth }.sumOf { it.amount }, monthTotal)?.let {
                    Text(stringResource(R.string.dash_trend, it), style = MaterialTheme.typography.labelLarge)
                }
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

        Spacer(Modifier.height(12.dp))
        RevenueCard(month, (5 downTo 0).map { month.minusMonths(it.toLong()) }.map { m -> m to live.filter { YearMonth.from(it.paidOn) == m }.sumOf { it.amount } })
        if (recent.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            RecentActivityCard(recent, onOpenLog)
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

/** "↑ 12%" against last month up to the same date; null when there is nothing to compare. */
private fun trend(before: Long, now: Long): String? {
    if (before <= 0) return null
    val pct = ((now - before) * 100 / before).toInt()
    return (if (pct >= 0) "↑ " else "↓ ") + "${kotlin.math.abs(pct)}%"
}

/** Fees collected per month. The current month is solid; tap any bar to read its amount. */
@Composable
private fun RevenueCard(current: YearMonth, months: List<Pair<YearMonth, Long>>) {
    var picked by remember(current) { mutableStateOf(current) }
    val max = months.maxOf { it.second }.coerceAtLeast(1)
    val label = DateTimeFormatter.ofPattern("MMM", LocalConfiguration.current.locales[0])
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.dash_revenue_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.dash_revenue_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(picked.format(label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(rupees(months.first { it.first == picked }.second), style = MaterialTheme.typography.titleMedium)
                }
            }
            Row(Modifier.fillMaxWidth().height(140.dp).padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                months.forEach { (m, total) ->
                    val amount = rupees(total)
                    Column(
                        // Screen readers hear "Oct ₹6,500" for each bar.
                        Modifier.weight(1f).fillMaxHeight().clickable { picked = m }.semantics(mergeDescendants = true) { contentDescription = amount },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            Modifier.fillMaxWidth(0.7f).fillMaxHeight((total.toFloat() / max).coerceAtLeast(0.02f) * 0.8f)
                                .background(
                                    if (m == picked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                )
                        )
                        Text(m.format(label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Row {
                Text(stringResource(R.string.dash_revenue_avg), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(rupees(months.sumOf { it.second } / months.size), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

/** The last few entries of the activity log: who paid, who joined, what changed. */
@Composable
private fun RecentActivityCard(recent: List<ActivityLog>, onOpenLog: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.dash_recent), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onOpenLog) { Text(stringResource(R.string.action_view_all)) }
        }
        recent.forEach { e ->
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = {
                    Icon(
                        when (e.entity) { ActivityLog.PAYMENT -> Icons.Default.CurrencyRupee; ActivityLog.STUDENT -> Icons.Default.Person; else -> Icons.Default.History },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, CircleShape).padding(8.dp)
                    )
                },
                headlineContent = { Text(e.summary, maxLines = 2) },
                supportingContent = { Text(DateUtils.getRelativeTimeSpanString(e.at).toString()) }
            )
        }
    }
}
