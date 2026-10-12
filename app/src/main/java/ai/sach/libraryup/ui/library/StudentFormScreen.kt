package ai.sach.libraryup.ui.library

import ai.sach.libraryup.R
import ai.sach.libraryup.data.local.entity.PaymentMode
import ai.sach.libraryup.data.local.entity.Plan
import ai.sach.libraryup.data.local.entity.Seat
import ai.sach.libraryup.data.local.entity.Shift
import ai.sach.libraryup.data.local.entity.Student
import ai.sach.libraryup.data.repository.LibraryRepository
import ai.sach.libraryup.data.repository.LibraryState
import ai.sach.libraryup.domain.BillingCalculator
import ai.sach.libraryup.domain.FeeCycleMode
import ai.sach.libraryup.domain.PhoneNumbers
import ai.sach.libraryup.domain.SeatBooking
import ai.sach.libraryup.domain.SeatRules
import ai.sach.libraryup.data.local.entity.Membership
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate

/** Seats free for [shift] from [from], given the library's current bookings. */
fun freeSeatsFor(state: LibraryState, shift: Shift?, from: LocalDate, ignoreMembershipId: Long = -1): List<Seat> {
    if (shift == null) return emptyList()
    val bookings = state.bookings
    return state.activeSeats.filter { seat ->
        SeatRules.isFree(SeatBooking(ignoreMembershipId, seat.id, shift.id, shift.window, from, Membership.OPEN_END), bookings)
    }
}

fun planLabel(plan: Plan): String = "${plan.name} · ${rupees(plan.price)}"

/**
 * Admission form (new student, or re-admission of one who left) and the edit form for
 * an existing student's details. Seat, shift and plan are part of admission only; seat
 * changes for current students go through the student screen.
 */
@Composable
fun StudentFormScreen(
    state: LibraryState,
    vm: LibraryViewModel,
    studentId: Long?,
    presetSeatId: Long?,
    presetShiftId: Long?,
    onDone: (Long?) -> Unit,
    onBack: () -> Unit
) {
    val existing = studentId?.let { id -> state.students.firstOrNull { it.student.id == id } }
    val admitting = existing == null || existing.membership == null

    var name by rememberSaveable { mutableStateOf(existing?.student?.name ?: "") }
    var phone by rememberSaveable { mutableStateOf(existing?.student?.phone ?: "") }
    var fatherName by rememberSaveable { mutableStateOf(existing?.student?.fatherName ?: "") }
    var guardianPhone by rememberSaveable { mutableStateOf(existing?.student?.guardianPhone ?: "") }
    var address by rememberSaveable { mutableStateOf(existing?.student?.address ?: "") }
    var exam by rememberSaveable { mutableStateOf(existing?.student?.exam ?: "") }
    var notes by rememberSaveable { mutableStateOf(existing?.student?.notes ?: "") }
    var photoUri by rememberSaveable { mutableStateOf(existing?.student?.photoUri) }
    var idProofUri by rememberSaveable { mutableStateOf(existing?.student?.idProofUri) }

    var shiftId by rememberSaveable { mutableStateOf(presetShiftId ?: state.activeShifts.firstOrNull()?.id) }
    var seatId by rememberSaveable { mutableStateOf(presetSeatId) }
    var planId by rememberSaveable { mutableStateOf<Long?>(null) }
    var startEpoch by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    var fee by rememberSaveable { mutableStateOf("") }
    var discount by rememberSaveable { mutableStateOf("") }
    var firstPayment by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(PaymentMode.CASH) }
    var showErrors by remember { mutableStateOf(false) }

    val context = LocalContext.current
    fun persist(uri: Uri) = runCatching {
        context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { persist(uri); photoUri = uri.toString() }
    }
    val idPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { persist(uri); idProofUri = uri.toString() }
    }

    val shift = state.shiftsById[shiftId]
    val start = LocalDate.ofEpochDay(startEpoch)
    val plans = state.activePlans.filter { it.shiftId == null || it.shiftId == shiftId }
    val plan = plans.firstOrNull { it.id == planId }
    LaunchedEffect(shiftId, plans.size) {
        if (plan == null && plans.isNotEmpty()) {
            planId = plans.first().id
            fee = plans.first().price.toString()
        }
    }
    val freeSeats = freeSeatsFor(state, shift, start)
    LaunchedEffect(shiftId) {
        if (seatId != null && freeSeats.none { it.id == seatId }) seatId = null
    }
    val mode0 = state.profile?.feeCycleMode ?: FeeCycleMode.CALENDAR_MONTH
    val due = plan?.let { BillingCalculator.nextDueDate(start, it.duration, mode0) }

    val nameError = name.isBlank()
    val phoneError = PhoneNumbers.forWhatsApp(phone) == null
    val admissionError = admitting && (shift == null || seatId == null || plan == null)

    Column(Modifier.fillMaxSize()) {
        LibraryTopBar(
            stringResource(
                when {
                    existing == null -> R.string.title_add_student
                    admitting -> R.string.title_readmit_student
                    else -> R.string.title_edit_student
                }
            ),
            onBack = onBack
        )
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp)
        ) {
            SectionTitle(stringResource(R.string.section_student))
            TextInput(name, { name = it }, stringResource(R.string.field_name), isError = showErrors && nameError)
            TextInput(phone, { phone = it }, stringResource(R.string.field_phone), keyboardType = KeyboardType.Phone,
                isError = showErrors && phoneError, supportingText = if (showErrors && phoneError) stringResource(R.string.error_phone_invalid) else null)
            TextInput(fatherName, { fatherName = it }, stringResource(R.string.field_father_name))
            TextInput(guardianPhone, { guardianPhone = it }, stringResource(R.string.field_guardian_phone), keyboardType = KeyboardType.Phone)
            TextInput(address, { address = it }, stringResource(R.string.field_address), singleLine = false)
            TextInput(exam, { exam = it }, stringResource(R.string.field_exam))
            TextInput(notes, { notes = it }, stringResource(R.string.field_notes), singleLine = false)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { photoPicker.launch(arrayOf("image/*")) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(if (photoUri == null) R.string.action_add_photo else R.string.action_change_photo))
                }
                OutlinedButton(onClick = { idPicker.launch(arrayOf("image/*", "application/pdf")) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(if (idProofUri == null) R.string.action_add_id_proof else R.string.action_change_id_proof))
                }
            }

            if (admitting) {
                SectionTitle(stringResource(R.string.section_admission))
                Picker(stringResource(R.string.field_shift), state.activeShifts, shift, { "${it.name} · ${it.timeLabel}" }, { shiftId = it.id })
                Picker(
                    stringResource(R.string.field_seat_free_count, freeSeats.size),
                    freeSeats,
                    freeSeats.firstOrNull { it.id == seatId },
                    { if (it.block.isBlank()) it.label else "${it.label} · ${it.block}" },
                    { seatId = it.id }
                )
                Picker(stringResource(R.string.field_plan), plans, plan, ::planLabel, { planId = it.id; fee = it.price.toString() })
                DateField(stringResource(R.string.field_start_date), start, { startEpoch = it.toEpochDay() })
                due?.let {
                    Text(
                        stringResource(R.string.label_valid_till, it.minusDays(1).pretty()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AmountInput(fee, { fee = it }, stringResource(R.string.field_fee), Modifier.weight(1f))
                    AmountInput(discount, { discount = it }, stringResource(R.string.field_discount), Modifier.weight(1f))
                }
                SectionTitle(stringResource(R.string.section_first_payment))
                AmountInput(firstPayment, { firstPayment = it }, stringResource(R.string.field_amount_received))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentMode.values().forEach { m ->
                        FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(stringResource(m.label())) })
                    }
                }
                if (showErrors && admissionError) {
                    Text(stringResource(R.string.error_admission_incomplete), color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    showErrors = true
                    if (nameError || phoneError || admissionError) return@Button
                    val student = (existing?.student ?: Student(name = "", phone = "")).copy(
                        name = name.trim(), phone = phone.trim(), fatherName = fatherName.trim(),
                        guardianPhone = guardianPhone.trim(), address = address.trim(), exam = exam.trim(),
                        notes = notes.trim(), photoUri = photoUri, idProofUri = idProofUri
                    )
                    val membership = if (admitting && plan != null && shift != null && seatId != null && due != null) {
                        LibraryRepository.NewMembership(seatId!!, shift.id, plan, plan.name, start, due, fee.toAmount(), discount.toAmount())
                    } else null
                    val pay = firstPayment.toAmount().takeIf { it > 0 }?.let { LibraryRepository.FirstPayment(it, mode) }
                    when {
                        existing == null && membership != null ->
                            vm.run(R.string.msg_student_added, onDone = { onDone(null) }) { admitStudent(student, membership, pay) }
                        existing != null && membership != null ->
                            vm.run(R.string.msg_student_saved, onDone = { onDone(existing.student.id) }) {
                                updateStudent(student); readmit(existing.student.id, membership, pay)
                            }
                        existing != null ->
                            vm.run(R.string.msg_student_saved, onDone = { onDone(existing.student.id) }) { updateStudent(student) }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text(stringResource(R.string.action_save)) }
            Spacer(Modifier.height(32.dp))
        }
    }
}

fun PaymentMode.label(): Int = when (this) {
    PaymentMode.CASH -> R.string.mode_cash
    PaymentMode.UPI -> R.string.mode_upi
    PaymentMode.OTHER -> R.string.mode_other
}
