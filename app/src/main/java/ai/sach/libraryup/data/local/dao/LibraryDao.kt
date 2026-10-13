package ai.sach.libraryup.data.local.dao

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
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Data access for the whole library. Lists are small (hundreds of rows), so screens
 * observe whole tables and the repository joins them in memory; that keeps the SQL
 * simple and every rule in one place (the domain module).
 */
@Dao
interface LibraryDao {

    /* ---------- Profile ---------- */

    @Query("SELECT * FROM library_profile WHERE id = 1")
    fun observeProfile(): Flow<LibraryProfile?>

    @Query("SELECT * FROM library_profile WHERE id = 1")
    suspend fun getProfile(): LibraryProfile?

    @Upsert
    suspend fun upsertProfile(profile: LibraryProfile)

    /* ---------- Shifts, seats, plans ---------- */

    @Query("SELECT * FROM shifts ORDER BY sortOrder, startMinute")
    fun observeShifts(): Flow<List<Shift>>

    @Query("SELECT * FROM shifts ORDER BY sortOrder, startMinute")
    suspend fun getShifts(): List<Shift>

    @Upsert
    suspend fun upsertShift(shift: Shift): Long

    @Query("SELECT * FROM seats ORDER BY block, sortOrder, id")
    fun observeSeats(): Flow<List<Seat>>

    @Query("SELECT * FROM seats ORDER BY block, sortOrder, id")
    suspend fun getSeats(): List<Seat>

    @Upsert
    suspend fun upsertSeat(seat: Seat): Long

    @Insert
    suspend fun insertSeats(seats: List<Seat>): List<Long>

    @Query("SELECT * FROM plans ORDER BY price")
    fun observePlans(): Flow<List<Plan>>

    @Query("SELECT * FROM plans ORDER BY price")
    suspend fun getPlans(): List<Plan>

    @Query("SELECT * FROM plans WHERE id = :id")
    suspend fun getPlan(id: Long): Plan?

    @Upsert
    suspend fun upsertPlan(plan: Plan): Long

    /* ---------- Students ---------- */

    @Query("SELECT * FROM students ORDER BY name COLLATE NOCASE")
    fun observeStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students ORDER BY name COLLATE NOCASE")
    suspend fun getStudents(): List<Student>

    @Query("SELECT * FROM students WHERE id = :id")
    fun observeStudent(id: Long): Flow<Student?>

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun getStudent(id: Long): Student?

    @Insert
    suspend fun insertStudent(student: Student): Long

    @Update
    suspend fun updateStudent(student: Student)

    /** Cascades to the student's memberships, payments and attendance. */
    @Query("DELETE FROM students WHERE id = :id")
    suspend fun deleteStudent(id: Long)

    /* ---------- Memberships ---------- */

    @Query("SELECT * FROM memberships ORDER BY startDate DESC")
    fun observeMemberships(): Flow<List<Membership>>

    @Query("SELECT * FROM memberships")
    suspend fun getMemberships(): List<Membership>

    @Query("SELECT * FROM memberships WHERE id = :id")
    suspend fun getMembership(id: Long): Membership?

    @Query("SELECT * FROM memberships WHERE studentId = :studentId AND status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveMembership(studentId: Long): Membership?

    @Insert
    suspend fun insertMembership(membership: Membership): Long

    @Update
    suspend fun updateMembership(membership: Membership)

    /* ---------- Payments ---------- */

    @Query("SELECT * FROM payments ORDER BY paidOn DESC, id DESC")
    fun observePayments(): Flow<List<Payment>>

    @Query("SELECT * FROM payments")
    suspend fun getPayments(): List<Payment>

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun getPayment(id: Long): Payment?

    @Query("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE membershipId = :membershipId AND deleted = 0")
    suspend fun paidFor(membershipId: Long): Long

    @Insert
    suspend fun insertPayment(payment: Payment): Long

    @Update
    suspend fun updatePayment(payment: Payment)

    /* ---------- Attendance ---------- */

    @Query("SELECT * FROM attendance WHERE date = :date")
    fun observeAttendanceOn(date: LocalDate): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId ORDER BY date DESC LIMIT :limit")
    fun observeAttendanceFor(studentId: Long, limit: Int): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance")
    suspend fun getAttendance(): List<Attendance>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId AND date = :date LIMIT 1")
    suspend fun findAttendance(studentId: Long, date: LocalDate): Attendance?

    @Upsert
    suspend fun upsertAttendance(attendance: Attendance)

    @Query("DELETE FROM attendance WHERE id = :id")
    suspend fun deleteAttendance(id: Long)

    /* ---------- Expenses ---------- */

    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun observeExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses")
    suspend fun getExpenses(): List<Expense>

    @Upsert
    suspend fun upsertExpense(expense: Expense): Long

    /* ---------- Lockers, enquiries ---------- */

    @Query("SELECT * FROM lockers")
    suspend fun getLockers(): List<Locker>

    @Query("SELECT * FROM lockers ORDER BY CAST(number AS INTEGER), number")
    fun observeLockers(): Flow<List<Locker>>

    @Upsert
    suspend fun upsertLocker(locker: Locker): Long

    @Query("DELETE FROM lockers WHERE id = :id")
    suspend fun deleteLocker(id: Long)

    @Query("UPDATE lockers SET studentId = NULL WHERE studentId = :studentId")
    suspend fun freeLockersOf(studentId: Long)

    @Query("SELECT * FROM enquiries")
    suspend fun getEnquiries(): List<Enquiry>

    @Query("SELECT * FROM enquiries ORDER BY createdAt DESC")
    fun observeEnquiries(): Flow<List<Enquiry>>

    @Upsert
    suspend fun upsertEnquiry(enquiry: Enquiry)

    @Query("SELECT COUNT(*) FROM enquiries WHERE status = 'OPEN' AND followUpOn <= :date")
    suspend fun countFollowUpsDue(date: LocalDate): Int

    /* ---------- Activity log ---------- */

    @Insert
    suspend fun insertLog(entry: ActivityLog)

    @Query("SELECT * FROM activity_log ORDER BY at DESC LIMIT :limit")
    fun observeLog(limit: Int): Flow<List<ActivityLog>>

    @Query("SELECT * FROM activity_log")
    suspend fun getLog(): List<ActivityLog>

    /* ---------- Restore ---------- */

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreProfile(rows: List<LibraryProfile>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreShifts(rows: List<Shift>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreSeats(rows: List<Seat>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restorePlans(rows: List<Plan>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreStudents(rows: List<Student>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreMemberships(rows: List<Membership>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restorePayments(rows: List<Payment>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreAttendance(rows: List<Attendance>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreExpenses(rows: List<Expense>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreLockers(rows: List<Locker>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreEnquiries(rows: List<Enquiry>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreLog(rows: List<ActivityLog>)
}
