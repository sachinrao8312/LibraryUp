package ai.sach.libraryup.ui.library

import ai.sach.libraryup.R
import ai.sach.libraryup.data.local.entity.Payment
import ai.sach.libraryup.data.local.entity.PaymentMode
import ai.sach.libraryup.data.local.entity.Shift
import ai.sach.libraryup.data.local.entity.Student
import ai.sach.libraryup.data.local.entity.StudentStatus
import ai.sach.libraryup.data.repository.LibraryRepository
import ai.sach.libraryup.data.repository.LibraryState
import ai.sach.libraryup.data.repository.StudentOverview
import ai.sach.libraryup.domain.MessageTemplates
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import java.time.LocalDate

private enum class StudentDialog { NONE, COLLECT, RENEW, CHANGE_SEAT, LATE_FEE, MESSAGE, LEFT, FREEZE, DELETE }

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailScreen(
    state: LibraryState,
    vm: LibraryViewModel,
    studentId: Long,
    onEdit: () -> Unit,
    onAssignSeat: () -> Unit,
    onBack: () -> Unit
) {
    val s = state.students.firstOrNull { it.student.id == studentId }
    if (s == null) {
        Column { LibraryTopBar("", onBack = onBack); EmptyState(stringResource(R.string.empty_student_missing)) }
        return
    }
    val context = LocalContext.current
    val profile = state.profile
    var dialog by rememberSaveable { mutableStateOf(StudentDialog.NONE) }
    var showDeleted by rememberSaveable { mutableStateOf(false) }
    var editPayment by remember { mutableStateOf<Payment?>(null) }
    var viewing by remember { mutableStateOf<String?>(null) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val attendance by remember(studentId) { vm.repository.attendanceFor(studentId) }.collectAsState(initial = emptyList())
    val payments = state.payments.filter { it.studentId == studentId && (showDeleted || !it.deleted) }

    Column(Modifier.fillMaxSize()) {
        LibraryTopBar(stringResource(R.string.section_student), onBack = onBack) {
            IconButton(onClick = { vm.withPro { shareIdCard(context, state.profile, s) } }) {
                Icon(Icons.Default.Badge, contentDescription = stringResource(R.string.action_id_card))
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit)) }
            if (!vm.staff.isOn) IconButton(onClick = { dialog = StudentDialog.DELETE }) {
                Icon(Icons.Default.DeleteForever, contentDescription = stringResource(R.string.action_delete_student))
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                val photo = s.student.photoUri
                Avatar(s.student.name, Modifier.clip(CircleShape).clickable(enabled = photo != null) { viewing = photo }, photo, 88.dp)
                Spacer(Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(s.student.name, style = MaterialTheme.typography.headlineSmall)
                    if (s.student.exam.isNotBlank()) Text(s.student.exam, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    StudentStatusPill(s)
                }
            }
            Row(Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { dial(context, s.student.phone) }, Modifier.weight(1f)) {
                    Icon(Icons.Default.Call, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_call))
                }
                FilledTonalButton(onClick = { dialog = StudentDialog.MESSAGE }, Modifier.weight(1f)) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_whatsapp))
                }
            }
            MembershipCard(s)

            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (s.student.status) {
                    StudentStatus.LEFT -> AssistChip(onClick = onAssignSeat, label = { Text(stringResource(R.string.action_readmit)) })
                    else -> {
                        if (s.membership == null) {
                            AssistChip(onClick = onAssignSeat, label = { Text(stringResource(R.string.action_assign_seat)) })
                        } else {
                            AssistChip(onClick = { dialog = StudentDialog.COLLECT }, label = { Text(stringResource(R.string.action_collect_fee)) })
                            AssistChip(onClick = { dialog = StudentDialog.RENEW }, label = { Text(stringResource(R.string.action_renew)) })
                            AssistChip(onClick = { dialog = StudentDialog.CHANGE_SEAT }, label = { Text(stringResource(R.string.action_change_seat)) })
                            AssistChip(onClick = { dialog = StudentDialog.LATE_FEE }, label = { Text(stringResource(R.string.action_late_fee)) })
                        }
                        AssistChip(
                            onClick = {
                                if (s.student.status == StudentStatus.FROZEN) vm.run(R.string.msg_unfrozen) { unfreeze(studentId, LocalDate.now()) }
                                else dialog = StudentDialog.FREEZE
                            },
                            label = { Text(stringResource(if (s.student.status == StudentStatus.FROZEN) R.string.action_unfreeze else R.string.action_freeze)) }
                        )
                        AssistChip(onClick = { dialog = StudentDialog.LEFT }, label = { Text(stringResource(R.string.action_mark_left)) })
                    }
                }
            }

            SectionTitle(stringResource(R.string.section_details))
            DetailsCard(s.student, onCall = { dial(context, it) })

            SectionTitle(stringResource(R.string.section_documents))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DocumentTile(stringResource(R.string.label_photo), s.student.photoUri, Modifier.weight(1f), onOpen = { viewing = it }, onAdd = onEdit)
                DocumentTile(stringResource(R.string.label_id_proof), s.student.idProofUri, Modifier.weight(1f), onOpen = { viewing = it }, onAdd = onEdit)
            }

            PrimaryTabRow(selectedTabIndex = tab, Modifier.padding(top = 16.dp)) {
                Tab(tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.section_payments)) })
                Tab(tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.section_attendance)) })
            }
            if (tab == 0) {
                FilterChip(
                    selected = showDeleted,
                    onClick = { showDeleted = !showDeleted },
                    label = { Text(stringResource(R.string.filter_show_deleted)) },
                    modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
                )
                if (payments.isEmpty()) Text(stringResource(R.string.empty_payments), color = MaterialTheme.colorScheme.onSurfaceVariant)
                payments.forEach { p ->
                    PaymentRow(
                        p,
                        canChange = !vm.staff.isOn,
                        onEdit = { editPayment = p },
                        onShare = { shareReceiptPdf(context, profile, s, p) },
                        onToggleDeleted = { vm.run(if (p.deleted) R.string.msg_payment_restored else R.string.msg_payment_deleted) { setPaymentDeleted(p.id, !p.deleted) } }
                    )
                    HorizontalDivider()
                }
            } else {
                Spacer(Modifier.height(8.dp))
                if (attendance.isEmpty()) Text(stringResource(R.string.empty_attendance), color = MaterialTheme.colorScheme.onSurfaceVariant)
                attendance.take(14).forEach { a ->
                    Text(
                        "${a.date.pretty()}  ${Shift.hhmm(a.inMinute)} → ${a.outMinute?.let { Shift.hhmm(it) } ?: "—"}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    viewing?.let { uri -> ImageViewer(uri) { viewing = null } }

    editPayment?.let { p ->
        EditPaymentDialog(p, onDismiss = { editPayment = null }) { amount, mode, date, note ->
            editPayment = null
            vm.run(R.string.msg_saved) { updatePayment(p.id, amount, mode, date, note) }
        }
    }

    when (dialog) {
        StudentDialog.NONE -> Unit
        StudentDialog.COLLECT -> CollectFeeDialog(s, state.suggestedLateFee(s), onDismiss = { dialog = StudentDialog.NONE }) { amount, mode, date, note, lateFee ->
            dialog = StudentDialog.NONE
            vm.collect({ collectPayment(studentId, amount, mode, date, note, lateFee) }) { p ->
                val updated = s.copy(
                    membership = s.membership?.let { m -> if (lateFee != null) m.copy(lateFee = lateFee) else m },
                    paid = s.paid + p.amount
                )
                shareReceiptPdf(context, profile, updated, p)
            }
        }
        StudentDialog.DELETE -> ConfirmDialog(
            stringResource(R.string.confirm_delete_student_title),
            stringResource(R.string.confirm_delete_student_text, s.student.name),
            stringResource(R.string.action_delete),
            onConfirm = { vm.run(R.string.msg_deleted, onDone = onBack) { deleteStudent(studentId) } },
            onDismiss = { dialog = StudentDialog.NONE }
        )
        StudentDialog.RENEW -> RenewDialog(state, s, onDismiss = { dialog = StudentDialog.NONE }) { plan, fee, discount, pay ->
            dialog = StudentDialog.NONE
            vm.run(R.string.msg_renewed) { renew(studentId, plan, fee, discount, pay) }
        }
        StudentDialog.CHANGE_SEAT -> ChangeSeatDialog(state, s, onDismiss = { dialog = StudentDialog.NONE }) { seatId, shiftId ->
            dialog = StudentDialog.NONE
            vm.run(R.string.msg_seat_changed) { changeSeat(studentId, seatId, shiftId) }
        }
        StudentDialog.LATE_FEE -> LateFeeDialog(s.membership?.lateFee ?: 0, onDismiss = { dialog = StudentDialog.NONE }) { fee ->
            dialog = StudentDialog.NONE
            s.membership?.let { m -> vm.run(R.string.msg_saved) { setLateFee(m.id, fee) } }
        }
        StudentDialog.MESSAGE -> MessageDialog(onDismiss = { dialog = StudentDialog.NONE }) { kind ->
            dialog = StudentDialog.NONE
            openWhatsApp(context, s.student.phone, buildMessage(profile, kind, s), profile?.whatsappPackage ?: "com.whatsapp")
        }
        StudentDialog.LEFT -> ConfirmDialog(
            stringResource(R.string.confirm_left_title),
            stringResource(R.string.confirm_left_text, s.student.name),
            stringResource(R.string.action_mark_left),
            onConfirm = { vm.run(R.string.msg_marked_left) { markLeft(studentId, LocalDate.now()) } },
            onDismiss = { dialog = StudentDialog.NONE }
        )
        StudentDialog.FREEZE -> ConfirmDialog(
            stringResource(R.string.confirm_freeze_title),
            stringResource(R.string.confirm_freeze_text),
            stringResource(R.string.action_freeze),
            onConfirm = { vm.run(R.string.msg_frozen) { freeze(studentId, LocalDate.now()) } },
            onDismiss = { dialog = StudentDialog.NONE }
        )
    }
}

@Composable
private fun MembershipCard(s: StudentOverview) {
    val m = s.membership
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            if (m == null) {
                Text(stringResource(if (s.student.status == StudentStatus.LEFT) R.string.label_left else R.string.label_no_seat))
                return@Column
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.label_seat_shift, s.seat?.label ?: "-", s.shift?.name ?: "-"),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                s.status?.let { StatusPill(it) }
            }
            Text("${m.planName} · ${m.startDate.pretty()} → ${m.dueDate.minusDays(1).pretty()}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            val p = s.period
            if (p != null) {
                Text(stringResource(R.string.label_fee_breakdown, rupees(m.fee), rupees(m.discount), rupees(m.lateFee)), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.label_paid_balance, rupees(s.paid), rupees(p.balance)),
                    fontWeight = FontWeight.SemiBold, color = if (p.balance > 0) MaterialTheme.colorScheme.error else successColor)
            }
        }
    }
}

/** Contact and personal details; phone numbers dial on tap. */
@Composable
private fun DetailsCard(st: Student, onCall: (String) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        DetailRow(Icons.Default.Phone, stringResource(R.string.field_phone), st.phone) { onCall(st.phone) }
        if (st.guardianPhone.isNotBlank()) DetailRow(Icons.Default.SupervisorAccount, stringResource(R.string.field_guardian_phone), st.guardianPhone) { onCall(st.guardianPhone) }
        if (st.fatherName.isNotBlank()) DetailRow(Icons.Default.Person, stringResource(R.string.field_father_name), st.fatherName)
        if (st.address.isNotBlank()) DetailRow(Icons.Default.Home, stringResource(R.string.field_address), st.address)
        DetailRow(Icons.Default.Event, stringResource(R.string.label_joined), st.joinDate.pretty())
        if (st.notes.isNotBlank()) DetailRow(Icons.Default.Info, stringResource(R.string.field_notes), st.notes)
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String, onCall: (() -> Unit)? = null) {
    ListItem(
        modifier = if (onCall != null) Modifier.clickable(onClick = onCall) else Modifier,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        overlineContent = { Text(label) },
        headlineContent = { Text(value) },
        supportingContent = onCall?.let { { Text(stringResource(R.string.hint_tap_to_call)) } }
    )
}

/** Photo or ID proof thumbnail; opens full screen, or the edit form when missing. */
@Composable
private fun DocumentTile(label: String, uri: String?, modifier: Modifier, onOpen: (String) -> Unit, onAdd: () -> Unit) {
    Column(modifier) {
        Surface(
            onClick = { if (uri != null) onOpen(uri) else onAdd() },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                if (uri != null) AsyncImage(uri, label, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun ImageViewer(uri: String, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AsyncImage(uri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            IconButton(onClick = onClose, Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close), tint = Color.White)
            }
        }
    }
}

@Composable
private fun PaymentRow(p: Payment, canChange: Boolean, onEdit: () -> Unit, onShare: () -> Unit, onToggleDeleted: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                "${rupees(p.amount)} · ${stringResource(p.mode.label())}",
                fontWeight = FontWeight.SemiBold,
                color = if (p.deleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            Text("${p.receiptNo} · ${p.paidOn.pretty()}" + if (p.deleted) " · ${stringResource(R.string.label_deleted)}" else "",
                style = MaterialTheme.typography.bodySmall)
        }
        if (canChange && !p.deleted) IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit)) }
        if (!p.deleted) IconButton(onClick = onShare) { Icon(Icons.Default.Share, contentDescription = stringResource(R.string.action_share_receipt)) }
        if (canChange) IconButton(onClick = onToggleDeleted) {
            Icon(if (p.deleted) Icons.Default.Restore else Icons.Default.Delete,
                contentDescription = stringResource(if (p.deleted) R.string.action_restore else R.string.action_delete))
        }
    }
}

/* ---------------------------------------------------------------- dialogs */

@Composable
fun CollectFeeDialog(
    s: StudentOverview,
    suggestedLateFee: Long,
    onDismiss: () -> Unit,
    onSave: (amount: Long, mode: PaymentMode, date: LocalDate, note: String, lateFee: Long?) -> Unit
) {
    val currentLateFee = s.membership?.lateFee ?: 0L
    val extra = (suggestedLateFee - currentLateFee).coerceAtLeast(0)
    var addLateFee by rememberSaveable { mutableStateOf(extra > 0) }
    val balance = s.balance + if (addLateFee) extra else 0L
    var amount by rememberSaveable(addLateFee) { mutableStateOf(balance.takeIf { it > 0 }?.toString() ?: "") }
    var mode by rememberSaveable { mutableStateOf(PaymentMode.CASH) }
    var dateEpoch by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    var note by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_collect_fee)) },
        text = {
            Column {
                Text(stringResource(R.string.label_balance, rupees(balance)), style = MaterialTheme.typography.bodySmall)
                if (extra > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = addLateFee, onCheckedChange = { addLateFee = it })
                        Text(stringResource(R.string.label_add_late_fee, rupees(extra)), style = MaterialTheme.typography.bodySmall)
                    }
                }
                AmountInput(amount, { amount = it }, stringResource(R.string.field_amount_received))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentMode.values().forEach { m -> FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(stringResource(m.label())) }) }
                }
                DateField(stringResource(R.string.field_date), LocalDate.ofEpochDay(dateEpoch), { dateEpoch = it.toEpochDay() })
                TextInput(note, { note = it }, stringResource(R.string.field_note))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(amount.toAmount(), mode, LocalDate.ofEpochDay(dateEpoch), note.trim(), if (addLateFee && extra > 0) suggestedLateFee else null)
                },
                enabled = amount.toAmount() > 0
            ) { Text(stringResource(R.string.action_save_receipt)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

/** Fixes a payment entered wrongly. The receipt number does not change. */
@Composable
private fun EditPaymentDialog(p: Payment, onDismiss: () -> Unit, onSave: (Long, PaymentMode, LocalDate, String) -> Unit) {
    var amount by rememberSaveable { mutableStateOf(p.amount.toString()) }
    var mode by rememberSaveable { mutableStateOf(p.mode) }
    var dateEpoch by rememberSaveable { mutableStateOf(p.paidOn.toEpochDay()) }
    var note by rememberSaveable { mutableStateOf(p.note) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_edit_payment, p.receiptNo)) },
        text = {
            Column {
                AmountInput(amount, { amount = it }, stringResource(R.string.field_amount))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentMode.values().forEach { m -> FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(stringResource(m.label())) }) }
                }
                DateField(stringResource(R.string.field_date), LocalDate.ofEpochDay(dateEpoch), { dateEpoch = it.toEpochDay() })
                TextInput(note, { note = it }, stringResource(R.string.field_note))
            }
        },
        confirmButton = {
            Button(enabled = amount.toAmount() > 0, onClick = { onSave(amount.toAmount(), mode, LocalDate.ofEpochDay(dateEpoch), note.trim()) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun RenewDialog(
    state: LibraryState,
    s: StudentOverview,
    onDismiss: () -> Unit,
    onSave: (ai.sach.libraryup.data.local.entity.Plan, Long, Long, LibraryRepository.FirstPayment?) -> Unit
) {
    val plans = state.activePlans.filter { it.shiftId == null || it.shiftId == s.membership?.shiftId }
    var planId by rememberSaveable { mutableStateOf(s.membership?.planId?.takeIf { id -> plans.any { it.id == id } } ?: plans.firstOrNull()?.id) }
    val plan = plans.firstOrNull { it.id == planId }
    var fee by rememberSaveable { mutableStateOf((plan?.price ?: s.membership?.fee ?: 0L).toString()) }
    var discount by rememberSaveable { mutableStateOf("") }
    var paid by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(PaymentMode.CASH) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_renew)) },
        text = {
            Column {
                s.membership?.let { Text(stringResource(R.string.label_renew_from, it.dueDate.pretty()), style = MaterialTheme.typography.bodySmall) }
                Picker(stringResource(R.string.field_plan), plans, plan, ::planLabel, { planId = it.id; fee = it.price.toString() })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AmountInput(fee, { fee = it }, stringResource(R.string.field_fee), Modifier.weight(1f))
                    AmountInput(discount, { discount = it }, stringResource(R.string.field_discount), Modifier.weight(1f))
                }
                AmountInput(paid, { paid = it }, stringResource(R.string.field_amount_received))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentMode.values().forEach { m -> FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(stringResource(m.label())) }) }
                }
            }
        },
        confirmButton = {
            Button(enabled = plan != null, onClick = {
                plan?.let { onSave(it, fee.toAmount(), discount.toAmount(), paid.toAmount().takeIf { a -> a > 0 }?.let { a -> LibraryRepository.FirstPayment(a, mode) }) }
            }) { Text(stringResource(R.string.action_renew)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun ChangeSeatDialog(state: LibraryState, s: StudentOverview, onDismiss: () -> Unit, onSave: (Long, Long) -> Unit) {
    var shiftId by rememberSaveable { mutableStateOf(s.membership?.shiftId) }
    var seatId by rememberSaveable { mutableStateOf<Long?>(null) }
    val shift = state.shiftsById[shiftId]
    val free = freeSeatsFor(state, shift, state.today, ignoreMembershipId = s.membership?.id ?: -1)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_change_seat)) },
        text = {
            Column {
                Picker(stringResource(R.string.field_shift), state.activeShifts, shift, { "${it.name} · ${it.timeLabel}" }, { shiftId = it.id; seatId = null })
                Picker(stringResource(R.string.field_seat_free_count, free.size), free, free.firstOrNull { it.id == seatId },
                    { if (it.block.isBlank()) it.label else "${it.label} · ${it.block}" }, { seatId = it.id })
            }
        },
        confirmButton = {
            Button(enabled = shift != null && seatId != null, onClick = { val se = seatId; if (shift != null && se != null) onSave(se, shift.id) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun LateFeeDialog(current: Long, onDismiss: () -> Unit, onSave: (Long) -> Unit) {
    var fee by rememberSaveable { mutableStateOf(if (current > 0) current.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_late_fee)) },
        text = { AmountInput(fee, { fee = it }, stringResource(R.string.field_late_fee)) },
        confirmButton = { Button(onClick = { onSave(fee.toAmount()) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun MessageDialog(onDismiss: () -> Unit, onPick: (MessageTemplates.Kind) -> Unit) {
    val kinds = listOf(
        MessageTemplates.Kind.FEE_DUE to R.string.template_fee_due,
        MessageTemplates.Kind.EXPIRING to R.string.template_expiring,
        MessageTemplates.Kind.WELCOME to R.string.template_welcome
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_whatsapp)) },
        text = {
            Column {
                kinds.forEach { (kind, label) ->
                    Text(
                        stringResource(label),
                        modifier = Modifier.fillMaxWidth().clickable { onPick(kind) }.padding(vertical = 12.dp)
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}
