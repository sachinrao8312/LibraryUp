package ai.sach.libraryup.ui.library

import ai.sach.libraryup.LibraryUpApplication
import ai.sach.libraryup.R
import ai.sach.libraryup.data.local.entity.DurationUnit
import ai.sach.libraryup.data.local.entity.LibraryProfile
import ai.sach.libraryup.data.local.entity.Plan
import ai.sach.libraryup.data.local.entity.Seat
import ai.sach.libraryup.data.local.entity.Shift
import ai.sach.libraryup.data.local.entity.StudentStatus
import ai.sach.libraryup.data.backup.BackupManager
import ai.sach.libraryup.data.repository.LibraryState
import ai.sach.libraryup.domain.FeeCycleMode
import ai.sach.libraryup.domain.MessageTemplates
import ai.sach.libraryup.domain.StudentImport
import ai.sach.libraryup.domain.UpiLink
import android.content.Intent
import androidx.compose.foundation.layout.size
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/* ---------------------------------------------------------------- More */

@Composable
fun MoreScreen(
    onReminders: () -> Unit,
    onAttendance: () -> Unit,
    onExpenses: () -> Unit,
    onLibrarySettings: () -> Unit,
    onData: () -> Unit,
    onActivityLog: () -> Unit,
    onBackup: () -> Unit,
    onAppSettings: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        MoreItem(Icons.Default.Campaign, R.string.title_reminders, R.string.more_reminders_desc, onReminders)
        MoreItem(Icons.Default.EventAvailable, R.string.title_attendance, R.string.more_attendance_desc, onAttendance)
        MoreItem(Icons.Default.AccountBalanceWallet, R.string.title_expenses, R.string.more_expenses_desc, onExpenses)
        HorizontalDivider()
        MoreItem(Icons.Default.LocalLibrary, R.string.title_library_settings, R.string.more_library_desc, onLibrarySettings)
        MoreItem(Icons.Default.ImportExport, R.string.title_data, R.string.more_data_desc, onData)
        MoreItem(Icons.Default.CloudUpload, R.string.title_backup, R.string.more_backup_desc, onBackup)
        MoreItem(Icons.Default.History, R.string.title_activity_log, R.string.more_log_desc, onActivityLog)
        HorizontalDivider()
        MoreItem(Icons.Default.Settings, R.string.title_app_settings, R.string.more_settings_desc, onAppSettings)
    }
}

@Composable
private fun MoreItem(icon: ImageVector, title: Int, subtitle: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null)
    }
}

/* ---------------------------------------------------------------- Library settings */

@Composable
fun LibrarySettingsScreen(state: LibraryState, vm: LibraryViewModel, onBack: () -> Unit) {
    val p = state.profile ?: return
    var name by rememberSaveable { mutableStateOf(p.name) }
    var ownerName by rememberSaveable { mutableStateOf(p.ownerName) }
    var phone by rememberSaveable { mutableStateOf(p.phone) }
    var address by rememberSaveable { mutableStateOf(p.address) }
    var upi by rememberSaveable { mutableStateOf(p.upiVpa) }
    var prefix by rememberSaveable { mutableStateOf(p.receiptPrefix) }
    var grace by rememberSaveable { mutableStateOf(p.graceDays.toString()) }
    var calendarMonth by rememberSaveable { mutableStateOf(p.feeCycleMode == FeeCycleMode.CALENDAR_MONTH) }
    var business by rememberSaveable { mutableStateOf(p.whatsappPackage == LibraryProfile.WHATSAPP_BUSINESS) }
    var hindi by rememberSaveable { mutableStateOf(p.messageLanguage == "hi") }
    var lateFeePerDay by rememberSaveable { mutableStateOf(if (p.lateFeePerDay > 0) p.lateFeePerDay.toString() else "") }
    var lateFeeMax by rememberSaveable { mutableStateOf(if (p.lateFeeMax > 0) p.lateFeeMax.toString() else "") }
    var tFeeDue by rememberSaveable { mutableStateOf(p.templateFeeDue.orEmpty()) }
    var tExpiring by rememberSaveable { mutableStateOf(p.templateExpiring.orEmpty()) }
    var tWelcome by rememberSaveable { mutableStateOf(p.templateWelcome.orEmpty()) }
    var tHoliday by rememberSaveable { mutableStateOf(p.templateHoliday.orEmpty()) }
    var editShift by remember { mutableStateOf<Shift?>(null) }
    var editPlan by remember { mutableStateOf<Plan?>(null) }
    var editSeat by remember { mutableStateOf<Seat?>(null) }
    var showAddSeats by remember { mutableStateOf(false) }
    var logo by rememberSaveable { mutableStateOf(p.logoUri) }
    val context = LocalContext.current
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            logo = uri.toString()
        }
    }
    val upiError = upi.isNotBlank() && !UpiLink.isValidVpa(upi)

    Column(Modifier.fillMaxSize()) {
        LibraryTopBar(stringResource(R.string.title_library_settings), onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp)) {
            SectionTitle(stringResource(R.string.setup_step_library))
            TextInput(name, { name = it }, stringResource(R.string.field_library_name))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                logo?.let { AsyncImage(model = it, contentDescription = null, modifier = Modifier.size(48.dp)) }
                OutlinedButton(onClick = { logoPicker.launch(arrayOf("image/*")) }) {
                    Text(stringResource(if (logo == null) R.string.action_add_logo else R.string.action_change_logo))
                }
                if (logo != null) TextButton(onClick = { logo = null }) { Text(stringResource(R.string.action_remove)) }
            }
            TextInput(ownerName, { ownerName = it }, stringResource(R.string.field_owner_name))
            TextInput(phone, { phone = it }, stringResource(R.string.field_phone), keyboardType = KeyboardType.Phone)
            TextInput(address, { address = it }, stringResource(R.string.field_address), singleLine = false)
            TextInput(upi, { upi = it.trim() }, stringResource(R.string.field_upi_id), isError = upiError, supportingText = stringResource(R.string.hint_upi))
            TextInput(prefix, { prefix = it.filter(Char::isLetterOrDigit).take(6).uppercase() }, stringResource(R.string.field_receipt_prefix))
            TextInput(grace, { grace = it.filter(Char::isDigit).take(2) }, stringResource(R.string.field_grace_days), keyboardType = KeyboardType.Number)
            SectionTitle(stringResource(R.string.field_fee_cycle))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = calendarMonth, onClick = { calendarMonth = true }, label = { Text(stringResource(R.string.fee_cycle_calendar)) })
                FilterChip(selected = !calendarMonth, onClick = { calendarMonth = false }, label = { Text(stringResource(R.string.fee_cycle_30)) })
            }
            SwitchRow(stringResource(R.string.field_whatsapp_business), business) { business = it }
            SwitchRow(stringResource(R.string.field_messages_hindi), hindi) { hindi = it }

            SectionTitle(stringResource(R.string.section_late_fee))
            Text(stringResource(R.string.late_fee_hint), style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AmountInput(lateFeePerDay, { lateFeePerDay = it }, stringResource(R.string.field_late_fee_per_day), Modifier.weight(1f))
                AmountInput(lateFeeMax, { lateFeeMax = it }, stringResource(R.string.field_late_fee_max), Modifier.weight(1f))
            }

            SectionTitle(stringResource(R.string.section_templates))
            Text(stringResource(R.string.templates_hint), style = MaterialTheme.typography.bodySmall)
            val lang = hindi
            TemplateField(tFeeDue, { tFeeDue = it }, stringResource(R.string.template_fee_due), MessageTemplates.default(MessageTemplates.Kind.FEE_DUE, lang))
            TemplateField(tExpiring, { tExpiring = it }, stringResource(R.string.template_expiring), MessageTemplates.default(MessageTemplates.Kind.EXPIRING, lang))
            TemplateField(tWelcome, { tWelcome = it }, stringResource(R.string.template_welcome), MessageTemplates.default(MessageTemplates.Kind.WELCOME, lang))
            TemplateField(tHoliday, { tHoliday = it }, stringResource(R.string.template_holiday), MessageTemplates.default(MessageTemplates.Kind.HOLIDAY, lang))

            Button(
                enabled = name.isNotBlank() && !upiError,
                onClick = {
                    vm.run(R.string.msg_saved) {
                        saveProfile(
                            p.copy(
                                name = name.trim(), logoUri = logo, ownerName = ownerName.trim(), phone = phone.trim(), address = address.trim(),
                                upiVpa = upi.trim(), receiptPrefix = prefix.ifBlank { "LU" }, graceDays = grace.toIntOrNull() ?: 3,
                                feeCycleMode = if (calendarMonth) FeeCycleMode.CALENDAR_MONTH else FeeCycleMode.FIXED_DAYS,
                                whatsappPackage = if (business) LibraryProfile.WHATSAPP_BUSINESS else LibraryProfile.WHATSAPP,
                                messageLanguage = if (hindi) "hi" else "en",
                                lateFeePerDay = lateFeePerDay.toAmount(),
                                lateFeeMax = lateFeeMax.toAmount(),
                                templateFeeDue = tFeeDue.trim().ifBlank { null },
                                templateExpiring = tExpiring.trim().ifBlank { null },
                                templateWelcome = tWelcome.trim().ifBlank { null },
                                templateHoliday = tHoliday.trim().ifBlank { null }
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) { Text(stringResource(R.string.action_save)) }

            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle(stringResource(R.string.section_shifts), Modifier.weight(1f))
                TextButton(onClick = { editShift = Shift(name = "", startMinute = 6 * 60, endMinute = 12 * 60, sortOrder = state.shifts.size) }) {
                    Text(stringResource(R.string.action_add))
                }
            }
            state.shifts.forEach { s ->
                ListLine("${s.name} · ${s.timeLabel}", inactive = !s.active) { editShift = s }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle(stringResource(R.string.section_plans), Modifier.weight(1f))
                TextButton(onClick = { editPlan = Plan(name = "", durationUnit = DurationUnit.MONTHS, durationValue = 1, price = 0) }) {
                    Text(stringResource(R.string.action_add))
                }
            }
            state.plans.forEach { pl ->
                val unit = stringResource(if (pl.durationUnit == DurationUnit.DAYS) R.string.unit_days else R.string.unit_months)
                ListLine("${pl.name} · ${pl.durationValue} $unit · ${rupees(pl.price)}", inactive = !pl.active) { editPlan = pl }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle(stringResource(R.string.section_seats_count, state.seats.count { it.active }), Modifier.weight(1f))
                TextButton(onClick = { showAddSeats = true }) { Text(stringResource(R.string.action_add)) }
            }
            Text(stringResource(R.string.seats_edit_hint), style = MaterialTheme.typography.bodySmall)
            state.seats.groupBy { it.block }.forEach { (block, seats) ->
                Text(block.ifBlank { stringResource(R.string.label_main_hall) }, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                Text(
                    seats.joinToString("  ") { if (it.active) it.label else "(${it.label})" },
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedButton(onClick = { editSeat = seats.first() }, modifier = Modifier.padding(top = 4.dp)) {
                    Text(stringResource(R.string.action_edit_seat))
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    editShift?.let { s -> ShiftDialog(s, onDismiss = { editShift = null }) { vm.run(R.string.msg_saved) { saveShift(it) }; editShift = null } }
    editPlan?.let { pl -> PlanDialog(pl, state.shifts, onDismiss = { editPlan = null }) { vm.run(R.string.msg_saved) { savePlan(it) }; editPlan = null } }
    editSeat?.let { se -> SeatDialog(state.seats, se, onDismiss = { editSeat = null }) { vm.run(R.string.msg_saved) { saveSeat(it) }; editSeat = null } }
    if (showAddSeats) AddSeatsDialog(onDismiss = { showAddSeats = false }) { block, count -> vm.run(R.string.msg_saved) { addSeats(block, count) }; showAddSeats = false }
}

/** A message template; blank means "use the built-in one", which is shown as the placeholder. */
@Composable
private fun TemplateField(value: String, onChange: (String) -> Unit, label: String, default: String) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text(default, style = MaterialTheme.typography.bodySmall) },
        minLines = 2,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

@Composable
private fun ListLine(text: String, inactive: Boolean, onClick: () -> Unit) {
    Text(
        text + if (inactive) "  · ${stringResource(R.string.label_inactive)}" else "",
        color = if (inactive) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp)
    )
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** "06:30" ↔ minutes. Accepts 6, 6:30, 18:00, 24:00. */
private fun parseTime(text: String): Int? {
    val parts = text.trim().split(":", ".")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    if (h !in 0..24 || m !in 0..59 || (h == 24 && m != 0)) return null
    return h * 60 + m
}

private fun formatTime(minute: Int): String = "%02d:%02d".format(minute / 60, minute % 60)

@Composable
private fun ShiftDialog(shift: Shift, onDismiss: () -> Unit, onSave: (Shift) -> Unit) {
    var name by rememberSaveable { mutableStateOf(shift.name) }
    var start by rememberSaveable { mutableStateOf(formatTime(shift.startMinute)) }
    var end by rememberSaveable { mutableStateOf(formatTime(shift.endMinute)) }
    var active by rememberSaveable { mutableStateOf(shift.active) }
    val s = parseTime(start)
    val e = parseTime(end)
    val valid = name.isNotBlank() && s != null && e != null && s < 24 * 60 && s != e
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.section_shifts)) },
        text = {
            Column {
                TextInput(name, { name = it }, stringResource(R.string.field_name))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextInput(start, { start = it }, stringResource(R.string.field_start_time), Modifier.weight(1f))
                    TextInput(end, { end = it }, stringResource(R.string.field_end_time), Modifier.weight(1f))
                }
                Text(stringResource(R.string.hint_time_format), style = MaterialTheme.typography.bodySmall)
                SwitchRow(stringResource(R.string.label_active), active) { active = it }
            }
        },
        confirmButton = {
            Button(enabled = valid, onClick = { onSave(shift.copy(name = name.trim(), startMinute = s ?: 0, endMinute = e ?: 0, active = active)) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun PlanDialog(plan: Plan, shifts: List<Shift>, onDismiss: () -> Unit, onSave: (Plan) -> Unit) {
    var shiftId by rememberSaveable { mutableStateOf(plan.shiftId) }
    val anyShift = stringResource(R.string.label_any_shift)
    var name by rememberSaveable { mutableStateOf(plan.name) }
    var months by rememberSaveable { mutableStateOf(plan.durationUnit == DurationUnit.MONTHS) }
    var length by rememberSaveable { mutableStateOf(plan.durationValue.toString()) }
    var price by rememberSaveable { mutableStateOf(if (plan.price > 0) plan.price.toString() else "") }
    var active by rememberSaveable { mutableStateOf(plan.active) }
    val len = length.toIntOrNull() ?: 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.section_plans)) },
        text = {
            Column {
                TextInput(name, { name = it }, stringResource(R.string.field_name))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = months, onClick = { months = true }, label = { Text(stringResource(R.string.unit_months)) })
                    FilterChip(selected = !months, onClick = { months = false }, label = { Text(stringResource(R.string.unit_days)) })
                }
                TextInput(length, { length = it.filter(Char::isDigit).take(3) }, stringResource(R.string.field_duration), keyboardType = KeyboardType.Number)
                AmountInput(price, { price = it }, stringResource(R.string.field_price))
                val shiftOptions = listOf<Pair<Long?, String>>(null to anyShift) + shifts.map { it.id to it.name }
                Picker(
                    stringResource(R.string.field_shift),
                    shiftOptions,
                    shiftOptions.firstOrNull { it.first == shiftId },
                    { it.second },
                    { shiftId = it.first }
                )
                SwitchRow(stringResource(R.string.label_active), active) { active = it }
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank() && len > 0 && price.toAmount() > 0, onClick = {
                onSave(plan.copy(name = name.trim(), durationUnit = if (months) DurationUnit.MONTHS else DurationUnit.DAYS,
                    durationValue = len, price = price.toAmount(), shiftId = shiftId, active = active))
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

/** Rename, move to another block, or switch off one seat. */
@Composable
private fun SeatDialog(all: List<Seat>, initial: Seat, onDismiss: () -> Unit, onSave: (Seat) -> Unit) {
    var seatId by rememberSaveable { mutableStateOf(initial.id) }
    val seat = all.firstOrNull { it.id == seatId } ?: initial
    var label by remember(seatId) { mutableStateOf(seat.label) }
    var block by remember(seatId) { mutableStateOf(seat.block) }
    var active by remember(seatId) { mutableStateOf(seat.active) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_edit_seat)) },
        text = {
            Column {
                Picker(stringResource(R.string.field_seat), all, seat, { if (it.block.isBlank()) it.label else "${it.label} · ${it.block}" }, { seatId = it.id })
                TextInput(label, { label = it.take(8) }, stringResource(R.string.field_seat_label))
                TextInput(block, { block = it }, stringResource(R.string.field_block_name))
                SwitchRow(stringResource(R.string.label_active), active) { active = it }
            }
        },
        confirmButton = {
            Button(enabled = label.isNotBlank(), onClick = { onSave(seat.copy(label = label.trim(), block = block.trim(), active = active)) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun AddSeatsDialog(onDismiss: () -> Unit, onSave: (String, Int) -> Unit) {
    var count by rememberSaveable { mutableStateOf("10") }
    var block by rememberSaveable { mutableStateOf("") }
    val n = count.toIntOrNull() ?: 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_add_seats)) },
        text = {
            Column {
                TextInput(count, { count = it.filter(Char::isDigit).take(3) }, stringResource(R.string.field_seat_count), keyboardType = KeyboardType.Number)
                TextInput(block, { block = it }, stringResource(R.string.field_block_name))
            }
        },
        confirmButton = { Button(enabled = n in 1..500, onClick = { onSave(block.trim(), n) }) { Text(stringResource(R.string.action_add)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

/* ---------------------------------------------------------------- Import / export */

@Composable
fun DataScreen(state: LibraryState, vm: LibraryViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val backup = (context.applicationContext as LibraryUpApplication).backupManager
    val scope = rememberCoroutineScope()
    val date = LocalDate.now().toString()

    val studentsCsv = rememberCsvExport(backup, vm) {
        listOf("name", "phone", "father_name", "address", "exam", "guardian_phone", "status", "seat", "shift", "plan", "start", "due", "fee", "paid", "balance") to
            state.students.map { s ->
                listOf(
                    s.student.name, s.student.phone, s.student.fatherName, s.student.address, s.student.exam, s.student.guardianPhone,
                    s.student.status.name, s.seat?.label.orEmpty(), s.shift?.name.orEmpty(), s.membership?.planName.orEmpty(),
                    s.membership?.startDate?.toString().orEmpty(), s.membership?.dueDate?.toString().orEmpty(),
                    s.membership?.let { (it.fee - it.discount + it.lateFee).toString() }.orEmpty(), s.paid.toString(), s.balance.toString()
                )
            }
    }
    val paymentsCsv = rememberCsvExport(backup, vm) {
        val names = state.students.associate { it.student.id to it.student.name }
        listOf("receipt_no", "date", "student", "amount", "mode", "note", "deleted") to
            state.payments.map { p ->
                listOf(p.receiptNo, p.paidOn.toString(), names[p.studentId].orEmpty(), p.amount.toString(), p.mode.name, p.note, p.deleted.toString())
            }
    }
    val templateCsv = rememberCsvExport(backup, vm) { StudentImport.HEADER to listOf(listOf("Asha Sharma", "9876543210", "Ramesh Sharma", "Lane 4, Patna", "UPSC", "9876500000")) }
    var importReport by remember { mutableStateOf<String?>(null) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            backup.readText(uri).onSuccess { text ->
                val result = StudentImport.parse(text)
                val added = vm.repository.importStudents(result.rows)
                importReport = context.getString(R.string.import_report, added, result.rows.size - added, result.errors.size) +
                    if (result.errors.isNotEmpty()) "\n\n" + result.errors.take(10).joinToString("\n") else ""
            }.onFailure { vm.message(R.string.error_generic, it.message ?: it.javaClass.simpleName) }
        }
    }

    Column(Modifier.fillMaxSize()) {
        LibraryTopBar(stringResource(R.string.title_data), onBack = onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(stringResource(R.string.section_export))
            Text(stringResource(R.string.export_hint), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { studentsCsv.launch("libraryup-students-$date.csv") }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_export_students))
            }
            OutlinedButton(onClick = { paymentsCsv.launch("libraryup-payments-$date.csv") }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_export_payments))
            }
            SectionTitle(stringResource(R.string.section_import))
            Text(stringResource(R.string.import_hint), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { templateCsv.launch("libraryup-import-template.csv") }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_download_template))
            }
            Button(onClick = { importer.launch(arrayOf("text/*", "text/csv", "text/comma-separated-values", "application/vnd.ms-excel")) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_import_students))
            }
        }
    }

    importReport?.let { report ->
        AlertDialog(
            onDismissRequest = { importReport = null },
            title = { Text(stringResource(R.string.section_import)) },
            text = { Text(report) },
            confirmButton = { TextButton(onClick = { importReport = null }) { Text(stringResource(R.string.action_ok)) } }
        )
    }
}

/** A "Save as" launcher that writes the CSV produced by [rows] to the chosen file. */
@Composable
private fun rememberCsvExport(
    backup: BackupManager,
    vm: LibraryViewModel,
    rows: () -> Pair<List<String>, List<List<String>>>
): androidx.activity.compose.ManagedActivityResultLauncher<String, android.net.Uri?> {
    val scope = rememberCoroutineScope()
    return rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BackupManager.MIME_TYPE_CSV)) { uri ->
        if (uri != null) scope.launch {
            val (header, data) = rows()
            backup.writeCsv(uri, header, data)
                .onSuccess { vm.message(R.string.msg_exported, it.toString()) }
                .onFailure { vm.message(R.string.error_generic, it.message ?: it.javaClass.simpleName) }
        }
    }
}

/* ---------------------------------------------------------------- Activity log */

private val logTime = DateTimeFormatter.ofPattern("dd MMM, hh:mm a")

@Composable
fun ActivityLogScreen(vm: LibraryViewModel, onBack: () -> Unit) {
    val log by remember { vm.repository.activityLog }.collectAsState(initial = emptyList())
    Column(Modifier.fillMaxSize()) {
        LibraryTopBar(stringResource(R.string.title_activity_log), onBack = onBack)
        if (log.isEmpty()) EmptyState(stringResource(R.string.empty_log))
        LazyColumn {
            items(log, key = { it.id }) { e ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(e.summary)
                    Text(
                        Instant.ofEpochMilli(e.at).atZone(ZoneId.systemDefault()).format(logTime) + " · ${e.entity} · ${e.action}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider()
            }
        }
    }
}

@Suppress("unused")
private fun StudentStatus.isCurrent() = this != StudentStatus.LEFT
