package ai.sach.libraryup.screens

import ai.sach.libraryup.billing.ProManager
import ai.sach.libraryup.data.TestLibrary
import ai.sach.libraryup.data.local.entity.DurationUnit
import ai.sach.libraryup.data.local.entity.Enquiry
import ai.sach.libraryup.data.local.entity.Expense
import ai.sach.libraryup.data.local.entity.LibraryProfile
import ai.sach.libraryup.data.local.entity.Locker
import ai.sach.libraryup.data.local.entity.PaymentMode
import ai.sach.libraryup.data.local.entity.Plan
import ai.sach.libraryup.data.local.entity.Shift
import ai.sach.libraryup.data.local.entity.Student
import ai.sach.libraryup.data.repository.LibraryRepository.FirstPayment
import ai.sach.libraryup.data.repository.LibraryRepository.NewMembership
import ai.sach.libraryup.ui.library.AttendanceScreen
import ai.sach.libraryup.ui.library.DashboardScreen
import ai.sach.libraryup.ui.library.EnquiriesScreen
import ai.sach.libraryup.ui.library.ExpensesScreen
import ai.sach.libraryup.ui.library.LibrarySettingsScreen
import ai.sach.libraryup.ui.library.LibraryViewModel
import ai.sach.libraryup.ui.library.LockersScreen
import ai.sach.libraryup.ui.library.MoreScreen
import ai.sach.libraryup.ui.library.PaymentsScreen
import ai.sach.libraryup.ui.library.ProScreen
import ai.sach.libraryup.ui.library.RemindersScreen
import ai.sach.libraryup.ui.library.SeatMapScreen
import ai.sach.libraryup.ui.library.StaffMode
import ai.sach.libraryup.ui.library.StudentDetailScreen
import ai.sach.libraryup.ui.library.StudentFilter
import ai.sach.libraryup.ui.library.StudentFormScreen
import ai.sach.libraryup.ui.library.StudentsScreen
import ai.sach.libraryup.ui.library.idCard
import ai.sach.libraryup.ui.library.vacancyPoster
import ai.sach.libraryup.ui.theme.LibraryUpTheme
import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/**
 * Renders the main screens with a realistic demo library into app/build/outputs/roborazzi/.
 * CI publishes them (with the Room schema) to the `screenshots` branch for review.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h860dp-xxhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val lib = TestLibrary()
    private val out = File("build/outputs/roborazzi").apply { mkdirs() }
    private var screen by mutableStateOf<@Composable () -> Unit>({})
    private var hindi by mutableStateOf(false)

    @After
    fun tearDown() = lib.close()

    @Test
    fun screens() = runBlocking {
        seed()
        val vm = LibraryViewModel(lib.repo, StaffMode(lib.context), ProManager(lib.context))
        val state = lib.repo.state.first()
        val asha = state.students.first { it.student.name == "Aarav Singh" }
        compose.setContent {
            val base = LocalContext.current
            val config = Configuration(base.resources.configuration).apply { if (hindi) setLocale(Locale("hi")) }
            CompositionLocalProvider(LocalContext provides base.createConfigurationContext(config), LocalConfiguration provides config) {
                LibraryUpTheme(darkTheme = false) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { screen() }
                }
            }
        }

        shot("01_dashboard") { DashboardScreen(state, {}, {}, {}, {}, {}) }
        shot("02_seat_map") { SeatMapScreen(state, {}, { _, _ -> }, { it() }) }
        shot("03_students") { StudentsScreen(state, StudentFilter.ALL, {}, {}) }
        shot("04_student_detail") { StudentDetailScreen(state, vm, asha.student.id, {}, {}, {}) }
        shot("05_admission_form") { StudentFormScreen(state, vm, null, lib.seatIds[5], lib.evening.id, {}, {}) }
        shot("06_payments") { PaymentsScreen(state) {} }
        shot("07_reminders") { RemindersScreen(state) {} }
        shot("08_attendance") { AttendanceScreen(state, vm) {} }
        shot("09_enquiries") { EnquiriesScreen(state, vm, {}, {}) }
        shot("10_lockers") { LockersScreen(state, vm) {} }
        shot("11_expenses") { ExpensesScreen(state, vm) {} }
        shot("12_more") { MoreScreen(vm, "backup") {} }
        shot("13_pro") { ProScreen(state, vm) {} }
        shot("14_library_settings") { LibrarySettingsScreen(state, vm) {} }
        save("15_id_card", idCard(lib.context, state.profile, asha))
        save("16_vacancy_poster", vacancyPoster(lib.context, state))
        hindi = true
        shot("17_dashboard_hindi") { DashboardScreen(state, {}, {}, {}, {}, {}) }
    }

    /** Shows [content], lets Room flows deliver their first rows, then captures the screen. */
    private fun shot(name: String, content: @Composable () -> Unit) {
        screen = { key(name) { content() } }
        repeat(3) { Thread.sleep(200); compose.waitForIdle() }
        compose.onRoot().captureRoboImage(File(out, "$name.png").path)
    }

    private fun save(name: String, bitmap: Bitmap) =
        File(out, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

    private suspend fun seed() {
        val today = LocalDate.now()
        lib.repo.completeSetup(
            LibraryProfile(
                name = "Gyan Deep Library", ownerName = "Ravi Sharma", phone = "98765 43210",
                address = "B-12, Mukherjee Nagar, Delhi", upiVpa = "gyandeep@okaxis", receiptPrefix = "GD",
                lateFeePerDay = 10, lateFeeMax = 200
            ),
            listOf(
                Shift(name = "Morning", startMinute = 6 * 60, endMinute = 14 * 60),
                Shift(name = "Evening", startMinute = 14 * 60, endMinute = 22 * 60),
                Shift(name = "Full day", startMinute = 6 * 60, endMinute = 22 * 60)
            ),
            listOf("Hall A" to 24, "AC Cabins" to 8),
            listOf(
                Plan(name = "Monthly", durationUnit = DurationUnit.MONTHS, durationValue = 1, price = 1000),
                Plan(name = "Quarterly", durationUnit = DurationUnit.MONTHS, durationValue = 3, price = 2700)
            )
        )
        lib.loadIds()
        // name, exam, father, seat index, shift, days since start, paid
        val students = listOf(
            Row("Aarav Singh", "UPSC CSE", "Mahesh Singh", 0, lib.fullDay, 10, 1500),
            Row("Priya Verma", "SSC CGL", "Anil Verma", 1, lib.morning, 20, 0),
            Row("Rohit Kumar", "IBPS PO", "Suresh Kumar", 1, lib.evening, 27, 1000),
            Row("Neha Gupta", "NEET", "Rajesh Gupta", 2, lib.morning, 0, 0),
            Row("Ankit Yadav", "UPSC CSE", "Ramesh Yadav", 3, lib.fullDay, 5, 1500),
            Row("Sneha Mishra", "JEE Main", "Alok Mishra", 4, lib.evening, 15, 500),
            Row("Vikas Pandey", "BPSC", "Dinesh Pandey", 5, lib.morning, 26, 1000),
            Row("Pooja Rawat", "UPSC CSE", "Mohan Rawat", 24, lib.fullDay, 12, 1500),
            Row("Rahul Meena", "RAS", "Kailash Meena", 25, lib.morning, 35, 0),
            Row("Kavita Joshi", "SSC CHSL", "Prakash Joshi", 6, lib.evening, 3, 1000),
            Row("Sandeep Chauhan", "RRB NTPC", "Vijay Chauhan", 7, lib.morning, 8, 1000),
            Row("Anjali Tiwari", "CTET", "Shyam Tiwari", 8, lib.evening, 18, 1000)
        )
        students.forEachIndexed { i, r ->
            val start = today.minusDays(r.daysAgo.toLong())
            val fee = if (r.shift == lib.fullDay) 1500L else 1000L
            val id = lib.repo.admitStudent(
                Student(name = r.name, phone = "98${(10000000 + i * 7919).toString().take(8)}", fatherName = r.father, exam = r.exam, joinDate = start),
                NewMembership(lib.seatIds[r.seat], r.shift.id, lib.monthly, "Monthly", start, start.plusMonths(1), fee, 0),
                if (r.paid > 0) FirstPayment(r.paid, if (i % 2 == 0) PaymentMode.UPI else PaymentMode.CASH) else null
            )
            if (i % 3 != 1) lib.repo.checkIn(id, today)
        }
        lib.repo.saveEnquiry(Enquiry(name = "Manish Rathore", phone = "9812345678", wantedShiftId = lib.evening.id, followUpOn = today, note = "Coming Sunday"))
        lib.repo.saveEnquiry(Enquiry(name = "Ritu Sharma", phone = "9898989898", wantedShiftId = lib.morning.id, followUpOn = today.plusDays(2)))
        lib.repo.saveEnquiry(Enquiry(name = "Deepak Saini", phone = "9123456780", followUpOn = today.minusDays(1), note = "Wants AC cabin"))
        val ids = lib.dao.getStudents().map { it.id }
        lib.repo.saveLocker(Locker(number = "L1", fee = 200, studentId = ids[0]))
        lib.repo.saveLocker(Locker(number = "L2", fee = 200, studentId = ids[4]))
        lib.repo.saveLocker(Locker(number = "L3", fee = 200))
        lib.repo.saveLocker(Locker(number = "L4", fee = 200))
        listOf("Rent" to 15000L, "Electricity" to 3200L, "Internet" to 999L, "Staff salary" to 8000L).forEach { (c, a) ->
            lib.repo.saveExpense(Expense(category = c, amount = a, date = today.withDayOfMonth(1)))
        }
    }

    private data class Row(
        val name: String, val exam: String, val father: String, val seat: Int,
        val shift: Shift, val daysAgo: Int, val paid: Long
    )
}
