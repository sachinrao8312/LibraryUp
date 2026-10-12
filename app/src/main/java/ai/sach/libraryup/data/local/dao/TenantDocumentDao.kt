package ai.sach.libraryup.data.local.dao

import ai.sach.libraryup.data.local.entity.TenantDocument
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// TenantDocumentDao.kt
@Dao
interface TenantDocumentDao {
    @Insert
    suspend fun insert(document: TenantDocument): Long
    
    @Query("SELECT * FROM tenant_documents WHERE tenantId = :tenantId")
    fun getDocumentsForTenant(tenantId: Long): Flow<List<TenantDocument>>
    
    // Non-Flow version for synchronous access during sync operations
    @Query("SELECT * FROM tenant_documents WHERE tenantId = :tenantId")
    suspend fun getDocumentsForTenantSync(tenantId: Long): List<TenantDocument>

    /** Every row, for backup export. */
    @Query("SELECT * FROM tenant_documents")
    suspend fun getAllSync(): List<TenantDocument>

    /** Bulk insert used by backup restore. */
    @Insert
    suspend fun insertAll(items: List<TenantDocument>)

    @Update
    suspend fun update(document: TenantDocument)
    
    @Delete
    suspend fun delete(document: TenantDocument)

}

