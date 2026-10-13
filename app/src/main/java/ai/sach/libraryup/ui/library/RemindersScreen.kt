package ai.sach.libraryup.ui.library

import ai.sach.libraryup.R
import ai.sach.libraryup.data.local.entity.Shift
import ai.sach.libraryup.data.local.entity.StudentStatus
import ai.sach.libraryup.data.repository.LibraryState
import ai.sach.libraryup.data.repository.StudentOverview
import ai.sach.libraryup.domain.DueStatus
import ai.sach.libraryup.data.repository.LibraryRepository.ScanResult
import ai.sach.libraryup.domain.StudentQr
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material.icons.filled.QrCodeScanner
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import ai.sach.libraryup.domain.MessageTemplates
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate

/**
 * Bulk WhatsApp reminders from the owner's own phone: no message credits. Android does
 * not let an app send WhatsApp messages by itself, so this is a queue: tap "Send", WhatsApp
 * opens with the message typed, send it, come back, tap "Send next".
 */
private enum class ReminderQueue { DUE, EXPIRING, HOLIDAY }

@Composable
fun RemindersScreen(state: LibraryState, onBack: () -> Unit) {
    val context = LocalContext.current
    var mode by rememberSaveable { mutableStateOf(ReminderQueue.DUE) }
    var holidayEpoch by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(1).toEpochDay()) }
    var sent by remember { mutableStateOf(listOf<Long>()) }
    val queue: List<StudentOverview> = state.students.filter {
        when (mode) {
            ReminderQueue.DUE -> it.status == DueStatus.OVERDUE || it.status == DueStatus.EXPIRED || it.status == DueStatus.DUE_TODAY || it.status == DueStatus.UNPAID
            ReminderQueue.EXPIRING -> it.status == DueStatus.EXPIRING_SOON
            // Everyone currently studying here, including frozen students who will come back.
            ReminderQueue.HOLIDAY -> it.membership != null && it.student.status != StudentStatus.LEFT
        }
    }.sortedBy { if (mode == ReminderQueue.HOLIDAY) 0 else it.status?.ordinal ?: Int.MAX_VALUE }
    val next = queue.firstOrNull { it.student.id !in sent }
    val kind = when (mode) {
        ReminderQueue.DUE -> MessageTemplates.Kind.FEE_DUE
        ReminderQueue.EXPIRING -> MessageTemplates.Kind.EXPIRING
        ReminderQueue.HOLIDAY -> MessageTemplates.Kind.HOLIDAY
    }
    val pkg = state.profile?.whatsappPackage ?: "com.whatsapp"

    fun send(s: StudentOverview) {
        sent = sent + s.student.id
        val extra = if (mode == ReminderQueue.HOLIDAY) mapOf(MessageTemplates.DUE_DATE to LocalDate.ofEpochDay(holidayEpoch).pretty()) else emptyMap()
        openWhatsApp(context, s.student.phone, buildMessage(state.profile, kind, s, extra = extra), pkg)
    }

    Column(Modifier.fillMaxSize()) {
        LibraryTopBar(stringResource(R.string.title_reminders), onBack = onBack)
        ChipRow {
            FilterChip(selected = mode == ReminderQueue.DUE, onClick = { mode = ReminderQueue.DUE; sent = emptyList() }, label = { Text(stringResource(R.string.reminders_fee_due)) })
            FilterChip(selected = mode == ReminderQueue.EXPIRING, onClick = { mode = ReminderQueue.EXPIRING; sent = emptyList() }, label = { Text(stringResource(R.string.reminders_expiring)) })
            FilterChip(selected = mode == ReminderQueue.HOLIDAY, onClick = { mode = ReminderQueue.HOLIDAY; sent = emptyList() }, label = { Text(stringResource(R.string.reminders_holiday)) })
        }
        Column(Modifier.padding(16.dp)) {
            if (mode == ReminderQueue.HOLIDAY) {
                DateField(stringResource(R.string.field_holiday_date), LocalDate.ofEpochDay(holidayEpoch), { holidayEpoch = it.toEpochDay() })
            }
            Text(stringResource(R.string.reminders_progress, sent.count { id -> queue.any { it.student.id == id } }, queue.size))
            Spacer(Modifier.height(8.dp))
            if (next != null) {
                Button(onClick = { send(next) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.reminders_send_next, next.student.name))
                }
            } else if (queue.isNotEmpty()) {
                Text(stringResource(R.string.reminders_done), color = StatusColors.paid)
            }
            Text(
                stringResource(R.string.reminders_how),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        if (queue.isEmpty()) EmptyState(stringResource(R.string.empty_reminders))
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(queue, key = { it.student.id }) { s ->
                Row(
                    Modifier.fillMaxWidth().clickable { send(s) }.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(s.student.name, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.label_seat_shift, s.seat?.label ?: "-", s.shift?.name ?: "-"), style = MaterialTheme.typography.bodySmall)
                    }
                    if (s.student.id in sent) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusColors.paid)
                    else s.status?.let { StatusPill(it) }
                }
                HorizontalDivider()
            }
        }
    }
}

/** Daily attendance: tap In when a student arrives and Out when they leave. */
@Composable
fun AttendanceScreen(state: LibraryState, vm: LibraryViewModel, onBack: () -> Unit) {
    var dateEpoch by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    val date = LocalDate.ofEpochDay(dateEpoch)
    val records by remember(dateEpoch) { vm.repository.attendanceOn(date) }.collectAsState(initial = emptyList())
    val byStudent = records.associateBy { it.studentId }
    var shiftFilter by rememberSaveable { mutableStateOf<Long?>(null) }
    val students = state.students
        .filter { it.membership != null && it.student.status == StudentStatus.ACTIVE }
        .filter { shiftFilter == null || it.shift?.id == shiftFilter }
        .sortedBy { it.seat?.label?.toIntOrNull() ?: Int.MAX_VALUE }
    val isToday = date == LocalDate.now()
    val scanPrompt = stringResource(R.string.scan_prompt)
    val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
        val text = result.contents ?: return@rememberLauncherForActivityResult
        val student = StudentQr.decode(text)?.let { id -> state.students.firstOrNull { it.student.id == id }?.student }
        if (student == null) vm.message(R.string.scan_unknown)
        else vm.run {
            val res = when (scanAttendance(student.id, LocalDate.now())) {
                ScanResult.IN -> R.string.scan_in
                ScanResult.OUT -> R.string.scan_out
                ScanResult.ALREADY -> R.string.scan_already
                ScanResult.NOT_ACTIVE -> R.string.scan_not_active
            }
            vm.message(res, student.name)
        }
    }

    Column(Modifier.fillMaxSize()) {
        LibraryTopBar(stringResource(R.string.title_attendance), onBack = onBack)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
            IconButton(onClick = { dateEpoch -= 1 }) { Icon(Icons.Default.ChevronLeft, contentDescription = null) }
            Text(date.pretty(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.attendance_present, records.size, students.size), style = MaterialTheme.typography.bodyMedium)
            if (isToday) IconButton(onClick = { scanner.launch(ScanOptions().setPrompt(scanPrompt).setOrientationLocked(false)) }) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = stringResource(R.string.action_scan))
            }
            IconButton(onClick = { dateEpoch += 1 }, enabled = !isToday) { Icon(Icons.Default.ChevronRight, contentDescription = null) }
        }
        ChipRow {
            FilterChip(selected = shiftFilter == null, onClick = { shiftFilter = null }, label = { Text(stringResource(R.string.filter_all_shifts)) })
            state.activeShifts.forEach { sh -> FilterChip(selected = shiftFilter == sh.id, onClick = { shiftFilter = sh.id }, label = { Text(sh.name) }) }
        }
        if (students.isEmpty()) EmptyState(stringResource(R.string.empty_students))
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(students, key = { it.student.id }) { s ->
                val a = byStudent[s.student.id]
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(s.student.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(R.string.label_seat_shift, s.seat?.label ?: "-", s.shift?.name ?: "-") +
                                (a?.let { "  ·  ${Shift.hhmm(it.inMinute)} → ${it.outMinute?.let { m -> Shift.hhmm(m) } ?: "…"}" } ?: ""),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    when {
                        a == null -> FilledTonalButton(onClick = { vm.run { checkIn(s.student.id, date) } }) { Text(stringResource(R.string.action_in)) }
                        a.outMinute == null -> OutlinedButton(onClick = { vm.run { checkOut(s.student.id, date) } }) { Text(stringResource(R.string.action_out)) }
                        else -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusColors.paid)
                    }
                    if (a != null) {
                        IconButton(onClick = { vm.run { undoAttendance(s.student.id, date) } }) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.action_undo))
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
