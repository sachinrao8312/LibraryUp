package ai.sach.rentup.data.local.dao

import ai.sach.rentup.data.local.entity.PaymentHistory
import ai.sach.rentup.data.local.entity.Tenant
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Create a data class to hold this info
data class PaymentHistoryWithTenantInfo(
    @Embedded val payment: PaymentHistory,
    val tenantName: String,
    val avatarUri: String?
)

data class PaymentWithTenantInfo(
    @Embedded val payment: PaymentHistory,

    @Relation(
        parentColumn = "tenantId",
        entityColumn = "id"
    )
    val tenant: Tenant
)

// PaymentHistoryDao.kt
@Dao
interface PaymentHistoryDao {
    @Insert
    suspend fun insert(payment: PaymentHistory): Long

    @Transaction
    @Query("SELECT * FROM payment_history ORDER BY date DESC")
    fun getAllPaymentsWithTenantInfo(): Flow<List<PaymentWithTenantInfo>>

    @Query("SELECT * FROM payment_history WHERE tenantId = :tenantId ORDER BY date DESC")
    fun getPaymentsForTenant(tenantId: Long): Flow<List<PaymentHistory>>

    /** Every row, for backup export. */
    @Query("SELECT * FROM payment_history")
    suspend fun getAllSync(): List<PaymentHistory>

    /** Bulk insert used by backup restore. */
    @Insert
    suspend fun insertAll(items: List<PaymentHistory>)

    @Update
    suspend fun update(payment: PaymentHistory)
    
    @Delete
    suspend fun delete(payment: PaymentHistory)

}