package ai.sach.libraryup.data.local.entity

import ai.sach.libraryup.domain.FeeCycleMode
import ai.sach.libraryup.domain.LateFeeRule
import ai.sach.libraryup.domain.MessageTemplates
import ai.sach.libraryup.domain.PlanDuration
import ai.sach.libraryup.domain.SeatBooking
import ai.sach.libraryup.domain.ShiftWindow
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.time.LocalDate

/**
 * Room schema for LibraryUp. Amounts are whole rupees (Long), dates are [LocalDate]
 * stored as epoch days, enums are stored by name.
 *
 * v0.1 runs one library per install; multi-branch is a later feature (see CLAUDE.md),
 * so rows do not carry a library id yet.
 */

/** The library itself and its settings. Always a single row with [id] = 1. */
@Entity(tableName = "library_profile")
data class LibraryProfile(
    @PrimaryKey val id: Long = SINGLETON_ID,
    val name: String,
    val ownerName: String = "",
    val phone: String = "",
    val address: String = "",
    val upiVpa: String = "",
    val feeCycleMode: FeeCycleMode = FeeCycleMode.CALENDAR_MONTH,
    val receiptPrefix: String = "LU",
    /** Sequence number the next receipt will get. */
    val nextReceiptSeq: Long = 1,
    val logoUri: String? = null,
    /** Days after a period starts before an unpaid fee is overdue. */
    val graceDays: Int = 3,
    /** "com.whatsapp" or "com.whatsapp.w4b". */
    val whatsappPackage: String = WHATSAPP,
    /** Language of messages sent to students: "en" or "hi". */
    val messageLanguage: String = "en",
    /** Owner's own wording for each message; null = the built-in template. */
    val templateFeeDue: String? = null,
    val templateExpiring: String? = null,
    val templateWelcome: String? = null,
    val templateHoliday: String? = null,
    /** Automatic late fee: rupees per day after the grace period (0 = off), and its cap (0 = none). */
    val lateFeePerDay: Long = 0,
    val lateFeeMax: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    val lateFeeRule: LateFeeRule get() = LateFeeRule(lateFeePerDay, lateFeeMax)

    /** The owner's template for [kind] if set, else the built-in one in [messageLanguage]. */
    fun template(kind: MessageTemplates.Kind): String {
        val custom = when (kind) {
            MessageTemplates.Kind.FEE_DUE -> templateFeeDue
            MessageTemplates.Kind.EXPIRING -> templateExpiring
            MessageTemplates.Kind.WELCOME -> templateWelcome
            MessageTemplates.Kind.HOLIDAY -> templateHoliday
            MessageTemplates.Kind.RECEIPT -> null
        }
        return custom?.takeIf { it.isNotBlank() } ?: MessageTemplates.default(kind, messageLanguage == "hi")
    }

    companion object {
        const val SINGLETON_ID = 1L
        const val WHATSAPP = "com.whatsapp"
        const val WHATSAPP_BUSINESS = "com.whatsapp.w4b"
    }
}

@Entity(tableName = "shifts")
data class Shift(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Minutes from midnight. */
    val startMinute: Int,
    /** Minutes from midnight, exclusive; 1440 = midnight. May be <= start for night shifts. */
    val endMinute: Int,
    val sortOrder: Int = 0,
    val active: Boolean = true
) {
    val window: ShiftWindow get() = ShiftWindow(startMinute, endMinute)

    val timeLabel: String get() = "${hhmm(startMinute)}–${hhmm(endMinute)}"

    companion object {
        fun hhmm(minute: Int): String {
            val m = minute % ShiftWindow.MINUTES_PER_DAY
            val h = m / 60
            val suffix = if (h < 12) "AM" else "PM"
            val h12 = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
            return if (m % 60 == 0) "$h12 $suffix" else "%d:%02d %s".format(h12, m % 60, suffix)
        }
    }
}

@Entity(tableName = "seats", indices = [Index("block"), Index("label")])
data class Seat(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    /** Block / floor / room name shown as a section on the seat map. */
    val block: String = "",
    val sortOrder: Int = 0,
    val active: Boolean = true,
    val note: String = ""
)

enum class DurationUnit { DAYS, MONTHS }

@Entity(tableName = "plans")
data class Plan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val durationUnit: DurationUnit,
    val durationValue: Int,
    val price: Long,
    /** Plan applies to one shift only; null = any shift. */
    val shiftId: Long? = null,
    val active: Boolean = true
) {
    val duration: PlanDuration
        get() = when (durationUnit) {
            DurationUnit.DAYS -> PlanDuration.Days(durationValue)
            DurationUnit.MONTHS -> PlanDuration.Months(durationValue)
        }
}

enum class StudentStatus { ACTIVE, FROZEN, LEFT }

@Entity(tableName = "students", indices = [Index("name"), Index("phone"), Index("status")])
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val guardianPhone: String = "",
    val fatherName: String = "",
    val address: String = "",
    val exam: String = "",
    val photoUri: String? = null,
    val idProofUri: String? = null,
    val joinDate: LocalDate = LocalDate.now(),
    val notes: String = "",
    val status: StudentStatus = StudentStatus.ACTIVE,
    /** Set while [status] is FROZEN. */
    val frozenSince: LocalDate? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class MembershipStatus {
    /** The student's current period. At most one per student. */
    ACTIVE,

    /** Replaced by a renewal, or the student left. */
    ENDED
}

/**
 * One billing period of a student on a seat in a shift. Renewing creates the next
 * membership on the same seat and shift; changing seat or shift edits the active one.
 */
@Entity(
    tableName = "memberships",
    foreignKeys = [
        ForeignKey(entity = Student::class, parentColumns = ["id"], childColumns = ["studentId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Seat::class, parentColumns = ["id"], childColumns = ["seatId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Shift::class, parentColumns = ["id"], childColumns = ["shiftId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("studentId"), Index("seatId"), Index("shiftId"), Index("status")]
)
data class Membership(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val seatId: Long,
    val shiftId: Long,
    val planId: Long?,
    val planName: String,
    val startDate: LocalDate,
    /** First day not covered: the next due date. */
    val dueDate: LocalDate,
    val fee: Long,
    val discount: Long = 0,
    val lateFee: Long = 0,
    val status: MembershipStatus = MembershipStatus.ACTIVE,
    /** Day the seat was released (student left or renewal took over). */
    val endedOn: LocalDate? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * The seat-holding range. An active membership keeps the seat until the student
     * leaves or renews, even after the due date - owners hold a seat while chasing a fee.
     */
    fun toBooking(window: ShiftWindow): SeatBooking {
        val until = endedOn ?: if (status == MembershipStatus.ACTIVE) OPEN_END else dueDate
        val safeUntil = if (until.isAfter(startDate)) until else startDate.plusDays(1)
        return SeatBooking(id, seatId, shiftId, window, startDate, safeUntil)
    }

    companion object {
        val OPEN_END: LocalDate = LocalDate.of(9999, 1, 1)
    }
}

enum class PaymentMode { CASH, UPI, OTHER }

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(entity = Student::class, parentColumns = ["id"], childColumns = ["studentId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Membership::class, parentColumns = ["id"], childColumns = ["membershipId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("studentId"), Index("membershipId"), Index("paidOn"), Index(value = ["receiptNo"], unique = true)]
)
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val membershipId: Long,
    val amount: Long,
    val mode: PaymentMode,
    val paidOn: LocalDate,
    val receiptNo: String,
    val note: String = "",
    /** Soft delete: deleted payments stay recoverable and visible in the activity log. */
    val deleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class AttendanceMethod { MANUAL, QR }

@Entity(
    tableName = "attendance",
    foreignKeys = [ForeignKey(entity = Student::class, parentColumns = ["id"], childColumns = ["studentId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["studentId", "date"], unique = true), Index("date")]
)
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val date: LocalDate,
    val inMinute: Int,
    val outMinute: Int? = null,
    val method: AttendanceMethod = AttendanceMethod.MANUAL
)

@Entity(tableName = "expenses", indices = [Index("date")])
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val amount: Long,
    val date: LocalDate,
    val mode: PaymentMode = PaymentMode.CASH,
    val note: String = "",
    val deleted: Boolean = false
)

@Entity(tableName = "lockers")
data class Locker(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val fee: Long = 0,
    val studentId: Long? = null
)

enum class EnquiryStatus { OPEN, CONVERTED, LOST }

@Entity(tableName = "enquiries", indices = [Index("followUpOn"), Index("status")])
data class Enquiry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val wantedShiftId: Long? = null,
    val followUpOn: LocalDate? = null,
    val note: String = "",
    val status: EnquiryStatus = EnquiryStatus.OPEN,
    val convertedStudentId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/** Append-only record of every change to money and students. */
@Entity(tableName = "activity_log", indices = [Index("at"), Index(value = ["entity", "entityId"])])
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Long = System.currentTimeMillis(),
    val entity: String,
    val entityId: Long,
    val action: String,
    val summary: String
) {
    companion object {
        const val STUDENT = "student"
        const val MEMBERSHIP = "membership"
        const val PAYMENT = "payment"
        const val EXPENSE = "expense"
        const val LOCKER = "locker"
        const val SETUP = "setup"

        const val ADD = "add"
        const val EDIT = "edit"
        const val DELETE = "delete"
        const val RESTORE = "restore"
    }
}

class Converters {
    @TypeConverter fun fromDate(value: LocalDate?): Long? = value?.toEpochDay()
    @TypeConverter fun toDate(value: Long?): LocalDate? = value?.let { LocalDate.ofEpochDay(it) }
}
