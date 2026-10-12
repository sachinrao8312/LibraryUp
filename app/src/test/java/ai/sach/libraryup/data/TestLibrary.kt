package ai.sach.libraryup.data

import ai.sach.libraryup.data.local.LibraryUpDatabase
import ai.sach.libraryup.data.local.entity.DurationUnit
import ai.sach.libraryup.data.local.entity.LibraryProfile
import ai.sach.libraryup.data.local.entity.Plan
import ai.sach.libraryup.data.local.entity.Shift
import ai.sach.libraryup.data.local.entity.Student
import ai.sach.libraryup.data.repository.LibraryRepository
import ai.sach.libraryup.data.repository.LibraryRepository.FirstPayment
import ai.sach.libraryup.data.repository.LibraryRepository.NewMembership
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.time.LocalDate

/** An in-memory library with three seats, Morning / Evening / Full day shifts and a monthly plan. */
class TestLibrary {
    val context: Context = ApplicationProvider.getApplicationContext()
    val db: LibraryUpDatabase = Room.inMemoryDatabaseBuilder(context, LibraryUpDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = LibraryRepository(db)
    val dao = db.libraryDao()

    lateinit var morning: Shift
    lateinit var evening: Shift
    lateinit var fullDay: Shift
    lateinit var monthly: Plan
    val seatIds = mutableListOf<Long>()

    suspend fun setUp(): TestLibrary {
        repo.completeSetup(
            LibraryProfile(name = "Test Library", receiptPrefix = "TL"),
            listOf(
                Shift(name = "Morning", startMinute = 6 * 60, endMinute = 14 * 60),
                Shift(name = "Evening", startMinute = 14 * 60, endMinute = 22 * 60),
                Shift(name = "Full day", startMinute = 6 * 60, endMinute = 22 * 60)
            ),
            listOf("A" to 3),
            listOf(Plan(name = "Monthly", durationUnit = DurationUnit.MONTHS, durationValue = 1, price = 1000))
        )
        val shifts = dao.getShifts().associateBy { it.name }
        morning = shifts.getValue("Morning")
        evening = shifts.getValue("Evening")
        fullDay = shifts.getValue("Full day")
        monthly = dao.getPlans().single()
        seatIds += dao.getSeats().sortedBy { it.sortOrder }.map { it.id }
        return this
    }

    suspend fun admit(
        name: String,
        seat: Int = 0,
        shift: Shift = morning,
        start: LocalDate = LocalDate.now(),
        paid: Long = 0,
        phone: String = "98765" + name.hashCode().toString().takeLast(5).padStart(5, '0')
    ): Long = repo.admitStudent(
        Student(name = name, phone = phone),
        NewMembership(seatIds[seat], shift.id, monthly, monthly.name, start, start.plusMonths(1), 1000, 0),
        if (paid > 0) FirstPayment(paid, ai.sach.libraryup.data.local.entity.PaymentMode.CASH) else null
    )

    fun close() = db.close()
}
