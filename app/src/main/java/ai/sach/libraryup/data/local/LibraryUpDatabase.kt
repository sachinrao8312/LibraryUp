package ai.sach.libraryup.data.local

import ai.sach.libraryup.data.local.dao.PaymentHistoryDao
import ai.sach.libraryup.data.local.dao.PropertyDao
import ai.sach.libraryup.data.local.dao.TenantDao
import ai.sach.libraryup.data.local.dao.TenantDocumentDao
import ai.sach.libraryup.data.local.dao.TenantFamilyInfoDao
import ai.sach.libraryup.data.local.dao.NotificationLogDao
import ai.sach.libraryup.data.local.dao.TenantPropertyDao
import ai.sach.libraryup.data.local.entity.NotificationLog
import ai.sach.libraryup.data.local.entity.PaymentHistory
import ai.sach.libraryup.data.local.entity.Tenant
import ai.sach.libraryup.data.local.entity.TenantDocument
import ai.sach.libraryup.data.local.entity.TenantFamilyInfo
import ai.sach.libraryup.data.local.entity.TenantPropertyCrossRef
import ai.sach.libraryup.data.local.entity.Property
import ai.sach.libraryup.data.local.entity.Converters // Import the Converters class
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters // Import TypeConverters annotation
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Tenant::class,
        TenantFamilyInfo::class,
        TenantDocument::class,
        PaymentHistory::class,
        Property::class,
        TenantPropertyCrossRef::class,
        NotificationLog::class
    ],
    version = 8, // Incremented version to 8 to record which form a tenant came from
    exportSchema = false
)
@TypeConverters(Converters::class) // Add TypeConverters annotation
abstract class LibraryUpDatabase : RoomDatabase() {
    abstract fun tenantDao(): TenantDao
    abstract fun tenantFamilyInfoDao(): TenantFamilyInfoDao
    abstract fun tenantDocumentDao(): TenantDocumentDao
    abstract fun paymentHistoryDao(): PaymentHistoryDao
    abstract fun propertyDao(): PropertyDao
    abstract fun tenantPropertyDao(): TenantPropertyDao
    abstract fun notificationLogDao(): NotificationLogDao

    companion object {
        @Volatile
        private var INSTANCE: LibraryUpDatabase? = null

        // Migration from version 1 to 2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add permanentAddress to tenants
                database.execSQL("ALTER TABLE tenants ADD COLUMN permanentAddress TEXT NOT NULL DEFAULT ''")
                // Add address to tenant_family_info
                database.execSQL("ALTER TABLE tenant_family_info ADD COLUMN address TEXT")
            }
        }

        // Migration from version 2 to 3: Add emergencyContacts to tenants table
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tenants ADD COLUMN emergencyContacts TEXT NOT NULL DEFAULT '[]'")
            }
        }

        // Migration from version 3 to 4: Add propertyId to tenant_family_info table
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tenant_family_info ADD COLUMN propertyId TEXT")
            }
        }

        // Migration from version 4 to 5: Create tenant_property_cross_ref junction table
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the junction table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS tenant_property_cross_ref (
                        tenantId INTEGER NOT NULL,
                        propertyId TEXT NOT NULL,
                        floor TEXT,
                        unitNumber TEXT,
                        rentAmount TEXT,
                        startDate TEXT,
                        endDate TEXT,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        PRIMARY KEY(tenantId, propertyId),
                        FOREIGN KEY(tenantId) REFERENCES tenants(id) ON DELETE CASCADE,
                        FOREIGN KEY(propertyId) REFERENCES properties(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // Create indices for better performance
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tenant_property_cross_ref_tenantId ON tenant_property_cross_ref(tenantId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tenant_property_cross_ref_propertyId ON tenant_property_cross_ref(propertyId)")
            }
        }

        // Migration from version 5 to 6: Add performance indexes to existing tables
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add indexes to tenants table for better query performance
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tenants_mobileNumber ON tenants(mobileNumber)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tenants_propertyId ON tenants(propertyId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tenants_isPaid ON tenants(isPaid)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tenants_dueDate ON tenants(dueDate)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tenants_name ON tenants(name)")
                
                // Add indexes to properties table for better query performance
                database.execSQL("CREATE INDEX IF NOT EXISTS index_properties_name ON properties(name)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_properties_address ON properties(address)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_properties_type ON properties(type)")
                
                // Add additional indexes to tenant_property_cross_ref for better performance
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tenant_property_cross_ref_isActive ON tenant_property_cross_ref(isActive)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tenant_property_cross_ref_tenantId_isActive ON tenant_property_cross_ref(tenantId, isActive)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tenant_property_cross_ref_propertyId_isActive ON tenant_property_cross_ref(propertyId, isActive)")
            }
        }


        // Migration from version 6 to 7: Create the rent reminder delivery ledger.
        // This is what makes the daily reminder sweep idempotent - it records which
        // reminder was delivered for which tenant in which billing cycle.
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS notification_log (
                        tenantId INTEGER NOT NULL,
                        cycleKey TEXT NOT NULL,
                        kind TEXT NOT NULL,
                        lastSentOn TEXT NOT NULL,
                        sendCount INTEGER NOT NULL,
                        PRIMARY KEY(tenantId, cycleKey, kind),
                        FOREIGN KEY(tenantId) REFERENCES tenants(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                database.execSQL("CREATE INDEX IF NOT EXISTS index_notification_log_tenantId ON notification_log(tenantId)")
            }
        }

        // Migration from version 7 to 8: record which onboarding form a tenant came from,
        // so a tap on that submission's notification can find the tenant it created.
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tenants ADD COLUMN formId TEXT")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tenants_formId ON tenants(formId)")
            }
        }

        fun getDatabase(context: Context): LibraryUpDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LibraryUpDatabase::class.java,
                    "rentup_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8) // Register all migrations including the reminder ledger
                    .fallbackToDestructiveMigrationOnDowngrade() // Allow destructive migration for development
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}