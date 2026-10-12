package ai.sach.rentup.data.local.dao

import ai.sach.rentup.data.local.entity.TenantFamilyInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// TenantFamilyInfoDao.kt
@Dao
interface TenantFamilyInfoDao {
    @Insert
    suspend fun insert(familyInfo: TenantFamilyInfo): Long

    @Query("SELECT * FROM tenant_family_info WHERE tenantId = :tenantId")
    fun getFamilyInfoForTenant(tenantId: Long): Flow<List<TenantFamilyInfo>>
    
    // Added synchronous version for use during processing
    @Query("SELECT * FROM tenant_family_info WHERE tenantId = :tenantId")
    suspend fun getFamilyInfoForTenantSync(tenantId: Long): List<TenantFamilyInfo>

    /** Every row, for backup export. */
    @Query("SELECT * FROM tenant_family_info")
    suspend fun getAllSync(): List<TenantFamilyInfo>

    /** Bulk insert used by backup restore. */
    @Insert
    suspend fun insertAll(items: List<TenantFamilyInfo>)

    @Update
    suspend fun update(familyInfo: TenantFamilyInfo)
    
    @Delete
    suspend fun delete(familyInfo: TenantFamilyInfo)

}

