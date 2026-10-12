package ai.sach.libraryup.data.backup

import ai.sach.libraryup.data.local.entity.ActivityLog
import ai.sach.libraryup.data.local.entity.Attendance
import ai.sach.libraryup.data.local.entity.Enquiry
import ai.sach.libraryup.data.local.entity.Expense
import ai.sach.libraryup.data.local.entity.LibraryProfile
import ai.sach.libraryup.data.local.entity.Locker
import ai.sach.libraryup.data.local.entity.Membership
import ai.sach.libraryup.data.local.entity.Payment
import ai.sach.libraryup.data.local.entity.Plan
import ai.sach.libraryup.data.local.entity.Seat
import ai.sach.libraryup.data.local.entity.Shift
import ai.sach.libraryup.data.local.entity.Student

/**
 * The `backup.json` entry inside a LibraryUp backup zip: every row of every table.
 * Reuses the Room entities directly; they are plain data classes.
 *
 * [app] identifies which app wrote the file, so a RentUp (or any sibling app) backup is
 * refused with a clear message instead of being half-read.
 *
 * Student photo and ID-proof URIs in here are either blank or a
 * `libraryup-backup:attachments/...` marker naming an entry in the same zip.
 */
data class BackupEnvelope(
    val app: String = APP_ID,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val appVersionName: String,
    val exportedAt: String,
    val profile: List<LibraryProfile>,
    val shifts: List<Shift>,
    val seats: List<Seat>,
    val plans: List<Plan>,
    val students: List<Student>,
    val memberships: List<Membership>,
    val payments: List<Payment>,
    val attendance: List<Attendance>,
    val expenses: List<Expense>,
    val lockers: List<Locker>,
    val enquiries: List<Enquiry>,
    val activityLog: List<ActivityLog>
) {
    companion object {
        const val APP_ID = "libraryup"
        const val CURRENT_SCHEMA_VERSION = 1
    }
}
