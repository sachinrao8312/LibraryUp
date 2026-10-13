package ai.sach.libraryup.data.local

import ai.sach.libraryup.data.local.dao.LibraryDao
import ai.sach.libraryup.data.local.entity.ActivityLog
import ai.sach.libraryup.data.local.entity.Attendance
import ai.sach.libraryup.data.local.entity.Converters
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
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * LibraryUp's local database. Still at version 1: nothing has shipped, so v1 is edited in
 * place until the first Play release (uninstall old test builds after a schema change).
 * After the first release every schema change needs a real Migration and a test against
 * the exported schema in app/schemas; never fall back to destructive migration, it would
 * wipe a library's records.
 */
@Database(
    entities = [
        LibraryProfile::class,
        Shift::class,
        Seat::class,
        Plan::class,
        Student::class,
        Membership::class,
        Payment::class,
        Attendance::class,
        Expense::class,
        Locker::class,
        Enquiry::class,
        ActivityLog::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class LibraryUpDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao

    companion object {
        const val FILE_NAME = "libraryup.db"

        @Volatile
        private var INSTANCE: LibraryUpDatabase? = null

        fun getDatabase(context: Context): LibraryUpDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LibraryUpDatabase::class.java,
                    FILE_NAME
                ).build().also { INSTANCE = it }
            }
    }
}
