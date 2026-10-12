package ai.sach.libraryup.data

import ai.sach.libraryup.R
import ai.sach.libraryup.data.local.entity.ActivityLog
import ai.sach.libraryup.data.local.entity.MembershipStatus
import ai.sach.libraryup.data.local.entity.PaymentMode
import ai.sach.libraryup.data.local.entity.StudentStatus
import ai.sach.libraryup.data.repository.UserError
import ai.sach.libraryup.domain.StudentImportRow
import android.app.Application
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LibraryRepositoryTest {

    private lateinit var lib: TestLibrary

    @Before
    fun setUp() = runBlocking { lib = TestLibrary().setUp(); Unit }

    @After
    fun tearDown() = lib.close()

    private suspend fun expectError(messageRes: Int, block: suspend () -> Unit) {
        try {
            block()
            fail("expected UserError")
        } catch (e: UserError) {
            assertEquals(messageRes, e.messageRes)
        }
    }

    @Test
    fun `one seat hosts morning and evening students but not an overlapping full day`() = runBlocking {
        lib.admit("Asha", seat = 0, shift = lib.morning)
        lib.admit("Bina", seat = 0, shift = lib.evening)
        expectError(R.string.error_seat_taken) { lib.admit("Chandu", seat = 0, shift = lib.fullDay) }
        // The failed admission rolled back: no orphan student.
        assertEquals(listOf("Asha", "Bina"), lib.dao.getStudents().map { it.name }.sorted())
    }

    @Test
    fun `renew starts the next period on the old due date, same seat and shift`() = runBlocking {
        val start = LocalDate.of(2026, 1, 31)
        val id = lib.admit("Asha", start = start)
        lib.repo.renew(id, lib.monthly, 1000, 100, null)

        val all = lib.dao.getMemberships().filter { it.studentId == id }.sortedBy { it.id }
        assertEquals(2, all.size)
        assertEquals(MembershipStatus.ENDED, all[0].status)
        val next = all[1]
        assertEquals(MembershipStatus.ACTIVE, next.status)
        assertEquals(LocalDate.of(2026, 2, 28), next.startDate)
        assertEquals(LocalDate.of(2026, 3, 28), next.dueDate)
        assertEquals(all[0].seatId, next.seatId)
        assertEquals(all[0].shiftId, next.shiftId)
        assertEquals(100, next.discount)
    }

    @Test
    fun `change seat refuses a taken seat and moves to a free one`() = runBlocking {
        val asha = lib.admit("Asha", seat = 0)
        lib.admit("Bina", seat = 1)
        expectError(R.string.error_seat_taken) { lib.repo.changeSeat(asha, lib.seatIds[1], lib.morning.id) }
        lib.repo.changeSeat(asha, lib.seatIds[2], lib.morning.id)
        assertEquals(lib.seatIds[2], lib.dao.getActiveMembership(asha)!!.seatId)
        // Same seat, different shift is fine too.
        lib.repo.changeSeat(asha, lib.seatIds[1], lib.evening.id)
        assertEquals(lib.evening.id, lib.dao.getActiveMembership(asha)!!.shiftId)
    }

    @Test
    fun `unfreeze gives back the paused days`() = runBlocking {
        val start = LocalDate.of(2026, 3, 1)
        val id = lib.admit("Asha", start = start)
        val due = lib.dao.getActiveMembership(id)!!.dueDate
        lib.repo.freeze(id, LocalDate.of(2026, 3, 10))
        assertEquals(StudentStatus.FROZEN, lib.dao.getStudent(id)!!.status)
        lib.repo.unfreeze(id, LocalDate.of(2026, 3, 20))
        assertEquals(StudentStatus.ACTIVE, lib.dao.getStudent(id)!!.status)
        assertNull(lib.dao.getStudent(id)!!.frozenSince)
        assertEquals(due.plusDays(10), lib.dao.getActiveMembership(id)!!.dueDate)
    }

    @Test
    fun `a student who left frees the seat`() = runBlocking {
        val asha = lib.admit("Asha", seat = 0, start = LocalDate.now().minusDays(5))
        lib.repo.markLeft(asha, LocalDate.now())
        assertEquals(StudentStatus.LEFT, lib.dao.getStudent(asha)!!.status)
        assertNull(lib.dao.getActiveMembership(asha))
        lib.admit("Bina", seat = 0)
        val state = lib.repo.state.first()
        assertNull(state.students.single { it.student.id == asha }.status)
    }

    @Test
    fun `receipts are numbered in sequence and keep their number when edited`() = runBlocking {
        val id = lib.admit("Asha", paid = 500)
        val second = lib.repo.collectPayment(id, 500, PaymentMode.UPI, LocalDate.now(), "")
        val payments = lib.dao.getPayments().sortedBy { it.id }
        assertEquals(2, payments.size)
        assertNotEquals(payments[0].receiptNo, payments[1].receiptNo)
        assertEquals(3L, lib.dao.getProfile()!!.nextReceiptSeq)

        lib.repo.updatePayment(second.id, 600, PaymentMode.CASH, LocalDate.now(), "corrected")
        val edited = lib.dao.getPayment(second.id)!!
        assertEquals(600L, edited.amount)
        assertEquals(PaymentMode.CASH, edited.mode)
        assertEquals(second.receiptNo, edited.receiptNo)
        assertTrue(lib.dao.getLog().any { it.entity == ActivityLog.PAYMENT && it.action == ActivityLog.EDIT })
    }

    @Test
    fun `deleted payments stop counting but can be restored`() = runBlocking {
        val id = lib.admit("Asha", paid = 1000)
        val p = lib.dao.getPayments().single()
        lib.repo.setPaymentDeleted(p.id, true)
        assertEquals(0L, lib.repo.state.first().students.single().paid)
        lib.repo.setPaymentDeleted(p.id, false)
        assertEquals(1000L, lib.repo.state.first().students.single { it.student.id == id }.paid)
    }

    @Test
    fun `collecting with a late fee updates the period`() = runBlocking {
        val id = lib.admit("Asha")
        lib.repo.collectPayment(id, 1050, PaymentMode.CASH, LocalDate.now(), "", lateFee = 50)
        assertEquals(50L, lib.dao.getActiveMembership(id)!!.lateFee)
        expectError(R.string.error_amount_positive) { lib.repo.collectPayment(id, 0, PaymentMode.CASH, LocalDate.now(), "") }
    }

    @Test
    fun `deleting a student removes their records and logs it`() = runBlocking {
        val id = lib.admit("Asha", paid = 700)
        lib.repo.checkIn(id, LocalDate.now())
        lib.repo.deleteStudent(id)
        assertNull(lib.dao.getStudent(id))
        assertTrue(lib.dao.getMemberships().none { it.studentId == id })
        assertTrue(lib.dao.getPayments().none { it.studentId == id })
        assertTrue(lib.dao.getAttendance().none { it.studentId == id })
        val entry = lib.dao.getLog().single { it.entity == ActivityLog.STUDENT && it.action == ActivityLog.DELETE }
        assertTrue(entry.summary, entry.summary.contains("₹700"))
    }

    @Test
    fun `attendance undo clears out time first, then the check-in`() = runBlocking {
        val id = lib.admit("Asha")
        val today = LocalDate.now()
        lib.repo.checkIn(id, today)
        lib.repo.checkOut(id, today)
        lib.repo.undoAttendance(id, today)
        val a = lib.dao.findAttendance(id, today)!!
        assertNull(a.outMinute)
        lib.repo.undoAttendance(id, today)
        assertNull(lib.dao.findAttendance(id, today))
    }

    @Test
    fun `duplicate seat labels in a block are refused`() = runBlocking {
        val seat = lib.dao.getSeats().first { it.id == lib.seatIds[1] }
        expectError(R.string.error_seat_label_taken) { lib.repo.saveSeat(seat.copy(label = "1")) }
        lib.repo.saveSeat(seat.copy(label = "1", block = "B"))
    }

    @Test
    fun `import skips phones that already exist`() = runBlocking {
        lib.admit("Asha", phone = "9876500001")
        val added = lib.repo.importStudents(
            listOf(
                row(2, "Asha again", "+91 98765 00001"),
                row(3, "Bina", "9876500002")
            )
        )
        assertEquals(1, added)
    }

    private fun row(line: Int, name: String, phone: String) =
        StudentImportRow(line = line, name = name, phone = phone, fatherName = "", address = "", exam = "", guardianPhone = "")
}
