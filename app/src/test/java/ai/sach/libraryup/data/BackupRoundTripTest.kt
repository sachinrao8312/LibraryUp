package ai.sach.libraryup.data

import ai.sach.libraryup.data.backup.BackupManager
import ai.sach.libraryup.data.backup.BackupPasswordRequiredException
import ai.sach.libraryup.data.local.entity.PaymentMode
import android.app.Application
import android.net.Uri
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate

/** Export → wipe → restore must bring back every table, attendance included. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BackupRoundTripTest {

    private lateinit var lib: TestLibrary
    private lateinit var backup: BackupManager
    private val uri: Uri = Uri.parse("content://test/backup.zip")
    private var bytes = ByteArray(0)

    @Before
    fun setUp() = runBlocking {
        lib = TestLibrary().setUp()
        backup = BackupManager(lib.context, lib.db)
        val resolver = shadowOf(lib.context.contentResolver)
        resolver.registerOutputStreamSupplier(uri) {
            object : ByteArrayOutputStream() {
                override fun close() { super.close(); bytes = toByteArray() }
            }
        }
        resolver.registerInputStreamSupplier(uri) { ByteArrayInputStream(bytes) }

        val asha = lib.admit("Asha", seat = 0, paid = 1000)
        val bina = lib.admit("Bina", seat = 0, shift = lib.evening)
        lib.repo.collectPayment(bina, 400, PaymentMode.UPI, LocalDate.now(), "part")
        lib.repo.checkIn(asha, LocalDate.now())
        lib.repo.checkOut(asha, LocalDate.now())
        lib.repo.checkIn(bina, LocalDate.now())
    }

    @After
    fun tearDown() = lib.close()

    private data class Snapshot(val rows: List<Any?>)

    private suspend fun snapshot() = Snapshot(
        listOf(
            lib.dao.getProfile(), lib.dao.getShifts(), lib.dao.getSeats(), lib.dao.getPlans(),
            lib.dao.getStudents(), lib.dao.getMemberships(), lib.dao.getPayments(),
            lib.dao.getAttendance(), lib.dao.getLog().size
        )
    )

    @Test
    fun `plain backup restores everything`() = runBlocking {
        val before = snapshot()
        val summary = backup.exportTo(uri).getOrThrow()
        assertEquals(2, summary.studentCount)
        assertEquals(2, summary.paymentCount)
        assertEquals(2, summary.attendanceCount)
        assertTrue(bytes.isNotEmpty())

        lib.repo.deleteStudent(lib.dao.getStudents().first().id)
        lib.db.clearAllTables()

        val restored = backup.importFrom(uri).getOrThrow()
        assertEquals(2, restored.attendanceCount)
        assertEquals(before, snapshot())
    }

    @Test
    fun `encrypted backup needs the password or recovery code`() = runBlocking {
        val before = snapshot()
        backup.exportTo(uri, listOf("correct horse", "ABCD-EFGH-JKLM-NPQR")).getOrThrow()
        assertTrue(backup.isEncrypted(uri))
        lib.db.clearAllTables()

        val missing = backup.importFrom(uri).exceptionOrNull()
        assertTrue(missing is BackupPasswordRequiredException && !missing.wrongPassword)
        val wrong = backup.importFrom(uri, "wrong").exceptionOrNull()
        assertTrue(wrong is BackupPasswordRequiredException && wrong.wrongPassword)

        backup.importFrom(uri, "correct horse").getOrThrow()
        assertEquals(before, snapshot())
        lib.db.clearAllTables()
        backup.importFrom(uri, "ABCD-EFGH-JKLM-NPQR").getOrThrow()
        assertEquals(before, snapshot())
    }
}
