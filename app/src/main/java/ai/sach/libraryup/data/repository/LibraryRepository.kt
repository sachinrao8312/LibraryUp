package ai.sach.libraryup.data.repository

import ai.sach.libraryup.R
import ai.sach.libraryup.data.local.LibraryUpDatabase
import ai.sach.libraryup.data.local.dao.LibraryDao
import ai.sach.libraryup.data.local.entity.ActivityLog
import ai.sach.libraryup.data.local.entity.Attendance
import ai.sach.libraryup.data.local.entity.AttendanceMethod
import ai.sach.libraryup.data.local.entity.Expense
import ai.sach.libraryup.data.local.entity.LibraryProfile
import ai.sach.libraryup.data.local.entity.Membership
import ai.sach.libraryup.data.local.entity.MembershipStatus
import ai.sach.libraryup.data.local.entity.Payment
import ai.sach.libraryup.data.local.entity.PaymentMode
import ai.sach.libraryup.data.local.entity.Plan
import ai.sach.libraryup.data.local.entity.Seat
import ai.sach.libraryup.data.local.entity.Shift
import ai.sach.libraryup.data.local.entity.Student
import ai.sach.libraryup.data.local.entity.StudentStatus
import ai.sach.libraryup.domain.BillingCalculator
import ai.sach.libraryup.domain.DueCalculator
import ai.sach.libraryup.domain.DueRules
import ai.sach.libraryup.domain.DueStatus
import ai.sach.libraryup.domain.PeriodSnapshot
import ai.sach.libraryup.domain.ReceiptNumber
import ai.sach.libraryup.domain.SeatBooking
import ai.sach.libraryup.domain.SeatRules
import ai.sach.libraryup.domain.StudentImportRow
import androidx.annotation.StringRes
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.LocalTime

/** A failure the user can fix, carrying a message ready to show. */
class UserError(@StringRes val messageRes: Int) : Exception()

/** One student with their current period resolved. */
data class StudentOverview(
    val student: Student,
    val membership: Membership?,
    val seat: Seat?,
    val shift: Shift?,
    val paid: Long,
    val status: DueStatus?
) {
    val period: PeriodSnapshot?
        get() = membership?.let {
            PeriodSnapshot(it.startDate, it.dueDate, it.fee, it.discount, it.lateFee, paid, student.status == StudentStatus.FROZEN)
        }
    val balance: Long get() = period?.balance ?: 0
}

/** Everything the screens need, recomputed whenever any table changes. */
data class LibraryState(
    val profile: LibraryProfile?,
    val shifts: List<Shift>,
    val seats: List<Seat>,
    val plans: List<Plan>,
    val students: List<StudentOverview>,
    val memberships: List<Membership>,
    val payments: List<Payment>,
    val today: LocalDate
) {
    val activeShifts: List<Shift> get() = shifts.filter { it.active }
    val activeSeats: List<Seat> get() = seats.filter { it.active }
    val activePlans: List<Plan> get() = plans.filter { it.active }
    val shiftsById: Map<Long, Shift> get() = shifts.associateBy { it.id }
    val seatsById: Map<Long, Seat> get() = seats.associateBy { it.id }

    /** Bookings that currently hold seats (students who have not left). */
    val bookings: List<SeatBooking>
        get() = memberships.filter { it.status == MembershipStatus.ACTIVE }
            .mapNotNull { m -> shiftsById[m.shiftId]?.let { m.toBooking(it.window) } }

    val overdueMembershipIds: Set<Long>
        get() = students.filter { it.status == DueStatus.OVERDUE }.mapNotNull { it.membership?.id }.toSet()

    companion object {
        val EMPTY = LibraryState(null, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), LocalDate.now())
    }
}

/**
 * The single place that changes library data. Every operation that touches money or a
 * student runs in a transaction and writes an [ActivityLog] entry, so an owner can always
 * see who changed what (see CLAUDE.md: "every destructive action is undoable or logged").
 */
class LibraryRepository(private val database: LibraryUpDatabase) {

    private val dao: LibraryDao = database.libraryDao()

    val profile: Flow<LibraryProfile?> = dao.observeProfile()
    val activityLog: Flow<List<ActivityLog>> = dao.observeLog(200)
    val expenses: Flow<List<Expense>> = dao.observeExpenses()

    fun attendanceOn(date: LocalDate): Flow<List<Attendance>> = dao.observeAttendanceOn(date)
    fun attendanceFor(studentId: Long): Flow<List<Attendance>> = dao.observeAttendanceFor(studentId, 60)

    val state: Flow<LibraryState> = combine(
        combine(dao.observeProfile(), dao.observeShifts(), dao.observeSeats(), dao.observePlans()) { p, sh, se, pl ->
            Setup(p, sh, se, pl)
        },
        dao.observeStudents(),
        dao.observeMemberships(),
        dao.observePayments()
    ) { setup, students, memberships, payments ->
        build(setup, students, memberships, payments, LocalDate.now())
    }

    private data class Setup(val profile: LibraryProfile?, val shifts: List<Shift>, val seats: List<Seat>, val plans: List<Plan>)

    private fun build(
        setup: Setup,
        students: List<Student>,
        memberships: List<Membership>,
        payments: List<Payment>,
        today: LocalDate
    ): LibraryState {
        val rules = DueRules(graceDays = setup.profile?.graceDays ?: DueRules().graceDays)
        val paidByMembership = payments.filter { !it.deleted }.groupBy { it.membershipId }.mapValues { (_, v) -> v.sumOf { it.amount } }
        val activeByStudent = memberships.filter { it.status == MembershipStatus.ACTIVE }.associateBy { it.studentId }
        val seats = setup.seats.associateBy { it.id }
        val shifts = setup.shifts.associateBy { it.id }
        val overviews = students.map { s ->
            val m = activeByStudent[s.id]
            val paid = m?.let { paidByMembership[it.id] } ?: 0
            val status = if (m == null || s.status == StudentStatus.LEFT) null else {
                DueCalculator.status(
                    PeriodSnapshot(m.startDate, m.dueDate, m.fee, m.discount, m.lateFee, paid, s.status == StudentStatus.FROZEN),
                    today,
                    rules
                )
            }
            StudentOverview(s, m, m?.let { seats[it.seatId] }, m?.let { shifts[it.shiftId] }, paid, status)
        }
        return LibraryState(setup.profile, setup.shifts, setup.seats, setup.plans, overviews, memberships, payments, today)
    }

    private suspend fun log(entity: String, id: Long, action: String, summary: String) =
        dao.insertLog(ActivityLog(entity = entity, entityId = id, action = action, summary = summary))

    /* ======================= Setup ======================= */

    suspend fun isSetUp(): Boolean = dao.getProfile() != null

    /** First-run wizard: library, shifts, seats and plans in one go. */
    suspend fun completeSetup(
        profile: LibraryProfile,
        shifts: List<Shift>,
        seatBlocks: List<Pair<String, Int>>,
        plans: List<Plan>
    ) = database.withTransaction {
        dao.upsertProfile(profile.copy(id = LibraryProfile.SINGLETON_ID))
        shifts.forEachIndexed { i, s -> dao.upsertShift(s.copy(id = 0, sortOrder = i)) }
        var n = 0
        val seats = seatBlocks.flatMap { (block, count) ->
            (1..count).map { Seat(label = (++n).toString(), block = block, sortOrder = n) }
        }
        dao.insertSeats(seats)
        plans.forEach { dao.upsertPlan(it.copy(id = 0)) }
        log(ActivityLog.SETUP, 1, ActivityLog.ADD, "Library set up: ${profile.name}, ${seats.size} seats, ${shifts.size} shifts")
    }

    suspend fun saveProfile(profile: LibraryProfile) {
        dao.upsertProfile(profile.copy(id = LibraryProfile.SINGLETON_ID))
    }

    suspend fun saveShift(shift: Shift) { dao.upsertShift(shift) }
    suspend fun savePlan(plan: Plan) { dao.upsertPlan(plan) }
    suspend fun saveSeat(seat: Seat) { dao.upsertSeat(seat) }

    /** Adds [count] seats to [block], numbered after the highest numeric label so far. */
    suspend fun addSeats(block: String, count: Int) = database.withTransaction {
        val existing = dao.getSeats()
        val start = existing.mapNotNull { it.label.toIntOrNull() }.maxOrNull() ?: 0
        dao.insertSeats((1..count).map { Seat(label = (start + it).toString(), block = block, sortOrder = start + it) })
    }

    /* ======================= Students ======================= */

    data class NewMembership(
        val seatId: Long,
        val shiftId: Long,
        val plan: Plan?,
        val planName: String,
        val startDate: LocalDate,
        val dueDate: LocalDate,
        val fee: Long,
        val discount: Long
    )

    data class FirstPayment(val amount: Long, val mode: PaymentMode)

    /** Admission: student + membership + optional first payment, atomically. */
    suspend fun admitStudent(student: Student, membership: NewMembership, firstPayment: FirstPayment?): Long =
        database.withTransaction {
            val studentId = dao.insertStudent(student.copy(id = 0))
            val membershipId = insertMembershipChecked(studentId, membership)
            log(ActivityLog.STUDENT, studentId, ActivityLog.ADD, "Admitted ${student.name}")
            if (firstPayment != null && firstPayment.amount > 0) {
                insertPayment(studentId, membershipId, firstPayment.amount, firstPayment.mode, membership.startDate, "")
            }
            studentId
        }

    suspend fun updateStudent(student: Student) = database.withTransaction {
        dao.updateStudent(student.copy(updatedAt = System.currentTimeMillis()))
        log(ActivityLog.STUDENT, student.id, ActivityLog.EDIT, "Edited ${student.name}")
    }

    private suspend fun insertMembershipChecked(studentId: Long, m: NewMembership, replacingId: Long = 0): Long {
        requireSeatFree(m.seatId, m.shiftId, m.startDate, ignoreMembershipId = replacingId)
        return dao.insertMembership(
            Membership(
                studentId = studentId,
                seatId = m.seatId,
                shiftId = m.shiftId,
                planId = m.plan?.id,
                planName = m.planName,
                startDate = m.startDate,
                dueDate = m.dueDate,
                fee = m.fee,
                discount = m.discount
            )
        )
    }

    private suspend fun requireSeatFree(seatId: Long, shiftId: Long, from: LocalDate, ignoreMembershipId: Long) {
        val shifts = dao.getShifts().associateBy { it.id }
        val window = shifts[shiftId]?.window ?: throw UserError(R.string.error_shift_missing)
        val existing = dao.getMemberships()
            .filter { it.status == MembershipStatus.ACTIVE && it.id != ignoreMembershipId }
            .mapNotNull { m -> shifts[m.shiftId]?.let { m.toBooking(it.window) } }
        val candidate = SeatBooking(ignoreMembershipId, seatId, shiftId, window, from, Membership.OPEN_END)
        if (!SeatRules.isFree(candidate, existing)) throw UserError(R.string.error_seat_taken)
    }

    /** Next period on the same seat and shift, starting at the current due date. */
    suspend fun renew(studentId: Long, plan: Plan, fee: Long, discount: Long, payment: FirstPayment?) =
        database.withTransaction {
            val current = dao.getActiveMembership(studentId) ?: throw UserError(R.string.error_no_membership)
            val mode = dao.getProfile()?.feeCycleMode ?: ai.sach.libraryup.domain.FeeCycleMode.CALENDAR_MONTH
            val start = current.dueDate
            val newId = dao.insertMembership(
                current.copy(
                    id = 0,
                    planId = plan.id,
                    planName = plan.name,
                    startDate = start,
                    dueDate = BillingCalculator.nextDueDate(start, plan.duration, mode),
                    fee = fee,
                    discount = discount,
                    lateFee = 0,
                    createdAt = System.currentTimeMillis()
                )
            )
            dao.updateMembership(current.copy(status = MembershipStatus.ENDED, endedOn = start))
            log(ActivityLog.MEMBERSHIP, newId, ActivityLog.ADD, "Renewed ${plan.name} from $start")
            if (payment != null && payment.amount > 0) insertPayment(studentId, newId, payment.amount, payment.mode, LocalDate.now(), "")
        }

    suspend fun changeSeat(studentId: Long, seatId: Long, shiftId: Long) = database.withTransaction {
        val current = dao.getActiveMembership(studentId) ?: throw UserError(R.string.error_no_membership)
        requireSeatFree(seatId, shiftId, later(LocalDate.now(), current.startDate), ignoreMembershipId = current.id)
        dao.updateMembership(current.copy(seatId = seatId, shiftId = shiftId))
        log(ActivityLog.MEMBERSHIP, current.id, ActivityLog.EDIT, "Seat/shift changed (seat ${current.seatId}→$seatId, shift ${current.shiftId}→$shiftId)")
    }

    suspend fun setLateFee(membershipId: Long, lateFee: Long) = database.withTransaction {
        val m = dao.getMembership(membershipId) ?: return@withTransaction
        dao.updateMembership(m.copy(lateFee = lateFee.coerceAtLeast(0)))
        log(ActivityLog.MEMBERSHIP, m.id, ActivityLog.EDIT, "Late fee ₹${m.lateFee} → ₹$lateFee")
    }

    suspend fun freeze(studentId: Long, on: LocalDate) = database.withTransaction {
        val s = dao.getStudent(studentId) ?: return@withTransaction
        if (s.status != StudentStatus.ACTIVE) return@withTransaction
        dao.updateStudent(s.copy(status = StudentStatus.FROZEN, frozenSince = on, updatedAt = System.currentTimeMillis()))
        log(ActivityLog.STUDENT, s.id, ActivityLog.EDIT, "Froze ${s.name} from $on")
    }

    /** Ends a freeze and gives back the paused days on the current period. */
    suspend fun unfreeze(studentId: Long, on: LocalDate) = database.withTransaction {
        val s = dao.getStudent(studentId) ?: return@withTransaction
        val since = s.frozenSince ?: on
        dao.getActiveMembership(studentId)?.let { m ->
            dao.updateMembership(m.copy(dueDate = BillingCalculator.extendForFreeze(m.dueDate, since, later(on, since))))
        }
        dao.updateStudent(s.copy(status = StudentStatus.ACTIVE, frozenSince = null, updatedAt = System.currentTimeMillis()))
        log(ActivityLog.STUDENT, s.id, ActivityLog.EDIT, "Unfroze ${s.name} on $on")
    }

    /** Student leaves: the seat is released today; history stays. */
    suspend fun markLeft(studentId: Long, on: LocalDate) = database.withTransaction {
        val s = dao.getStudent(studentId) ?: return@withTransaction
        dao.getActiveMembership(studentId)?.let { m ->
            val endedOn = if (on.isAfter(m.startDate)) on else m.startDate.plusDays(1)
            dao.updateMembership(m.copy(status = MembershipStatus.ENDED, endedOn = endedOn))
        }
        dao.updateStudent(s.copy(status = StudentStatus.LEFT, frozenSince = null, updatedAt = System.currentTimeMillis()))
        log(ActivityLog.STUDENT, s.id, ActivityLog.EDIT, "${s.name} left on $on")
    }

    /** A student who left comes back: new membership on a free seat. */
    suspend fun readmit(studentId: Long, membership: NewMembership, payment: FirstPayment?) = database.withTransaction {
        val s = dao.getStudent(studentId) ?: return@withTransaction
        val membershipId = insertMembershipChecked(studentId, membership)
        dao.updateStudent(s.copy(status = StudentStatus.ACTIVE, updatedAt = System.currentTimeMillis()))
        log(ActivityLog.STUDENT, s.id, ActivityLog.EDIT, "Re-admitted ${s.name}")
        if (payment != null && payment.amount > 0) insertPayment(studentId, membershipId, payment.amount, payment.mode, membership.startDate, "")
    }

    /* ======================= Payments ======================= */

    suspend fun collectPayment(studentId: Long, amount: Long, mode: PaymentMode, paidOn: LocalDate, note: String): Payment =
        database.withTransaction {
            if (amount <= 0) throw UserError(R.string.error_amount_positive)
            val m = dao.getActiveMembership(studentId) ?: throw UserError(R.string.error_no_membership)
            insertPayment(studentId, m.id, amount, mode, paidOn, note)
        }

    /** Must run inside a transaction: takes the next receipt number. */
    private suspend fun insertPayment(studentId: Long, membershipId: Long, amount: Long, mode: PaymentMode, paidOn: LocalDate, note: String): Payment {
        val profile = dao.getProfile() ?: throw UserError(R.string.error_not_set_up)
        val receiptNo = ReceiptNumber.format(profile.receiptPrefix, profile.nextReceiptSeq, paidOn)
        dao.upsertProfile(profile.copy(nextReceiptSeq = profile.nextReceiptSeq + 1))
        val payment = Payment(studentId = studentId, membershipId = membershipId, amount = amount, mode = mode, paidOn = paidOn, receiptNo = receiptNo, note = note)
        val id = dao.insertPayment(payment)
        log(ActivityLog.PAYMENT, id, ActivityLog.ADD, "Received ₹$amount ($mode), receipt $receiptNo")
        return payment.copy(id = id)
    }

    suspend fun setPaymentDeleted(paymentId: Long, deleted: Boolean) = database.withTransaction {
        val p = dao.getPayment(paymentId) ?: return@withTransaction
        dao.updatePayment(p.copy(deleted = deleted))
        log(
            ActivityLog.PAYMENT, p.id,
            if (deleted) ActivityLog.DELETE else ActivityLog.RESTORE,
            "${if (deleted) "Deleted" else "Restored"} ₹${p.amount} receipt ${p.receiptNo}"
        )
    }

    /* ======================= Attendance ======================= */

    suspend fun checkIn(studentId: Long, date: LocalDate, method: AttendanceMethod = AttendanceMethod.MANUAL) {
        if (dao.findAttendance(studentId, date) != null) return
        dao.upsertAttendance(Attendance(studentId = studentId, date = date, inMinute = nowMinute(), method = method))
    }

    suspend fun checkOut(studentId: Long, date: LocalDate) {
        val a = dao.findAttendance(studentId, date) ?: return
        dao.upsertAttendance(a.copy(outMinute = nowMinute()))
    }

    suspend fun clearAttendance(studentId: Long, date: LocalDate) {
        val a = dao.findAttendance(studentId, date) ?: return
        // Undo a wrong tap: keep the row but drop the out time, or remove the in time by
        // re-marking. Rows are cheap; a full delete is not needed for v0.1.
        dao.upsertAttendance(a.copy(outMinute = null))
    }

    private fun later(a: LocalDate, b: LocalDate): LocalDate = if (a.isAfter(b)) a else b

    private fun nowMinute(): Int = LocalTime.now().let { it.hour * 60 + it.minute }

    /* ======================= Expenses ======================= */

    suspend fun saveExpense(expense: Expense) = database.withTransaction {
        val id = dao.upsertExpense(expense)
        val realId = if (expense.id != 0L) expense.id else id
        log(ActivityLog.EXPENSE, realId, if (expense.id == 0L) ActivityLog.ADD else ActivityLog.EDIT, "${expense.category} ₹${expense.amount}")
    }

    suspend fun setExpenseDeleted(expense: Expense, deleted: Boolean) = database.withTransaction {
        dao.upsertExpense(expense.copy(deleted = deleted))
        log(ActivityLog.EXPENSE, expense.id, if (deleted) ActivityLog.DELETE else ActivityLog.RESTORE, "${expense.category} ₹${expense.amount}")
    }

    /* ======================= Import ======================= */

    /** Adds imported students without seats; the owner assigns seats afterwards. */
    suspend fun importStudents(rows: List<StudentImportRow>): Int = database.withTransaction {
        val existingPhones = dao.getStudents().map { it.phone.filter(Char::isDigit).takeLast(10) }.toSet()
        var added = 0
        rows.filter { it.phone.filter(Char::isDigit).takeLast(10) !in existingPhones }.forEach { r ->
            val id = dao.insertStudent(
                Student(name = r.name, phone = r.phone, fatherName = r.fatherName, address = r.address, exam = r.exam, guardianPhone = r.guardianPhone)
            )
            log(ActivityLog.STUDENT, id, ActivityLog.ADD, "Imported ${r.name}")
            added++
        }
        added
    }
}
