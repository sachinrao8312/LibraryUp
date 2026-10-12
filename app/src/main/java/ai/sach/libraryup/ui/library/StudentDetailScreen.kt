package ai.sach.libraryup.ui.library

import ai.sach.libraryup.R
import ai.sach.libraryup.data.local.entity.Payment
import ai.sach.libraryup.data.local.entity.PaymentMode
import ai.sach.libraryup.data.local.entity.Shift
import ai.sach.libraryup.data.local.entity.StudentStatus
import ai.sach.libraryup.data.repository.LibraryRepository
import ai.sach.libraryup.data.repository.LibraryState
import ai.sach.libraryup.data.repository.StudentOverview
import ai.sach.libraryup.domain.MessageTemplates
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.time.LocalDate

private enum class StudentDialog { NONE, COLLECT, RENEW, CHANGE_SEAT, LATE_FEE, MESSAGE, LEFT, FREEZE }

@OptIn(ExperimentalLayoutApi::class)
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
    val attendance by remember(studentId) { vm.repository.attendanceFor(studentId) }.collectAsState(initial = emptyList())
    val payments = state.payments.filter { it.studentId == studentId && (showDeleted || !it.deleted) }

    Column(Modifier.fillMaxSize()) {
        LibraryTopBar(s.student.name, onBack = onBack) {
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit)) }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (s.student.photoUri != null) {
                    AsyncImage(
                        model = s.student.photoUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(64.dp).clip(CircleShape)
                    )
                    Spacer(Modifier.padding(6.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(s.student.phone, style = MaterialTheme.typography.bodyLarge)
                    if (s.student.fatherName.isNotBlank()) Text(stringResource(R.string.label_father, s.student.fatherName), style = MaterialTheme.typography.bodySmall)
                    if (s.student.exam.isNotBlank()) Text(s.student.exam, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { dial(context, s.student.phone) }) { Icon(Icons.Default.Call, contentDescription = stringResource(R.string.action_call)) }
                IconButton(onClick = { dialog = StudentDialog.MESSAGE }) { Icon(Icons.Default.Chat, contentDescription = stringResource(R.string.action_whatsapp)) }
            }
            if (s.student.address.isNotBlank()) Text(s.student.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(12.dp))
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle(stringResource(R.string.section_payments), Modifier.weight(1f))
                FilterChip(selected = showDeleted, onClick = { showDeleted = !showDeleted }, label = { Text(stringResource(R.string.filter_show_deleted)) })
            }
            if (payments.isEmpty()) Text(stringResource(R.string.empty_payments), color = MaterialTheme.colorScheme.onSurfaceVariant)
            payments.forEach { p ->
                PaymentRow(
                    p,
                    onShare = { shareReceiptPdf(context, profile, s, p) },
                    onToggleDeleted = { vm.run(if (p.deleted) R.string.msg_payment_restored else R.string.msg_payment_deleted) { setPaymentDeleted(p.id, !p.deleted) } }
                )
                HorizontalDivider()
            }

            SectionTitle(stringResource(R.string.section_attendance))
            if (attendance.isEmpty()) Text(stringResource(R.string.empty_attendance), color = MaterialTheme.colorScheme.onSurfaceVariant)
            attendance.take(14).forEach { a ->
                Text(
                    "${a.date.pretty()}  ${Shift.hhmm(a.inMinute)} → ${a.outMinute?.let { Shift.hhmm(it) } ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    when (dialog) {
        StudentDialog.NONE -> Unit
        StudentDialog.COLLECT -> CollectFeeDialog(s, onDismiss = { dialog = StudentDialog.NONE }) { amount, mode, date, note ->
            dialog = StudentDialog.NONE
            vm.collect({ collectPayment(studentId, amount, mode, date, note) }) { p -> shareReceiptPdf(context, profile, s.copy(paid = s.paid + p.amount), p) }
        }
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
                    fontWeight = FontWeight.SemiBold, color = if (p.balance > 0) StatusColors.full else StatusColors.paid)
            }
        }
    }
}

@Composable
private fun PaymentRow(p: Payment, onShare: () -> Unit, onToggleDeleted: () -> Unit) {
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
        if (!p.deleted) IconButton(onClick = onShare) { Icon(Icons.Default.Share, contentDescription = stringResource(R.string.action_share_receipt)) }
        IconButton(onClick = onToggleDeleted) {
            Icon(if (p.deleted) Icons.Default.Restore else Icons.Default.Delete,
                contentDescription = stringResource(if (p.deleted) R.string.action_restore else R.string.action_delete))
        }
    }
}

/* ---------------------------------------------------------------- dialogs */

@Composable
fun CollectFeeDialog(s: StudentOverview, onDismiss: () -> Unit, onSave: (Long, PaymentMode, LocalDate, String) -> Unit) {
    var amount by rememberSaveable { mutableStateOf(s.balance.takeIf { it > 0 }?.toString() ?: "") }
    var mode by rememberSaveable { mutableStateOf(PaymentMode.CASH) }
    var dateEpoch by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    var note by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_collect_fee)) },
        text = {
            Column {
                Text(stringResource(R.string.label_balance, rupees(s.balance)), style = MaterialTheme.typography.bodySmall)
                AmountInput(amount, { amount = it }, stringResource(R.string.field_amount_received))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentMode.values().forEach { m -> FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(stringResource(m.label())) }) }
                }
                DateField(stringResource(R.string.field_date), LocalDate.ofEpochDay(dateEpoch), { dateEpoch = it.toEpochDay() })
                TextInput(note, { note = it }, stringResource(R.string.field_note))
            }
        },
        confirmButton = {
            Button(onClick = { onSave(amount.toAmount(), mode, LocalDate.ofEpochDay(dateEpoch), note.trim()) }, enabled = amount.toAmount() > 0) {
                Text(stringResource(R.string.action_save_receipt))
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
