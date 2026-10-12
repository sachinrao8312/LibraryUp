package ai.sach.rentup.data.local.dao

import ai.sach.rentup.data.local.entity.Tenant
import ai.sach.rentup.data.local.entity.TenantWithFullProperty
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TenantDao {
    // Create
    @Insert
    suspend fun insert(tenant: Tenant): Long

    // Read
    @Query("SELECT * FROM tenants")
    fun getAllTenants(): Flow<List<Tenant>>

    // Non-Flow version for synchronous access during sync operations
    @Query("SELECT * FROM tenants")
    suspend fun getAllTenantsSync(): List<Tenant>

    // Paginated loading to prevent memory issues
    @Query("SELECT * FROM tenants LIMIT :limit OFFSET :offset")
    suspend fun getTenantsBatch(offset: Int, limit: Int): List<Tenant>

    @Query("SELECT * FROM tenants WHERE id = :id")
    suspend fun getTenantById(id: Long): Tenant?

    /** Resolves a "new tenant submission" notification tap back to the tenant it created. */
    @Query("SELECT * FROM tenants WHERE formId = :formId LIMIT 1")
    suspend fun getTenantByFormId(formId: String): Tenant?

    @Query("SELECT * FROM tenants WHERE propertyId = :propertyId")
    fun getTenantsByProperty(propertyId: String): Flow<List<Tenant>>

    // New query for searching by mobile number
    @Query("SELECT * FROM tenants WHERE mobileNumber LIKE '%' || :phoneNumber || '%'")
    fun getTenantsByPhoneNumber(phoneNumber: String): Flow<List<Tenant>>

    // Update
    @Update
    suspend fun update(tenant: Tenant)

    // Delete
    @Delete
    suspend fun delete(tenant: Tenant)

    @Query("DELETE FROM tenants WHERE id IN (:tenantIds)")
    suspend fun deleteTenantsByIds(tenantIds: List<Long>)

    /** Wipes every tenant. Family info, documents, payments and property links cascade with it. */
    @Query("DELETE FROM tenants")
    suspend fun deleteAll()

    /** Bulk insert used by backup restore. Each [Tenant.id] is preserved so every foreign
     * key elsewhere in the imported backup (family info, documents, payments, property
     * links) still resolves correctly. */
    @Insert
    suspend fun insertAll(tenants: List<Tenant>)

    // Get tenants with full property information
    @Transaction
    @Query("SELECT * FROM tenants")
    fun getTenantsWithFullProperty(): Flow<List<TenantWithFullProperty>>

    /**
     * Gets the total number of tenants in the database.
     * Used for checking if the database has been populated.
     *
     * @return Count of tenant records
     */
    @Query("SELECT COUNT(*) FROM tenants")
    suspend fun getTenantCount(): Int

}