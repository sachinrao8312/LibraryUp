package ai.sach.libraryup.data.local.dao

import ai.sach.libraryup.data.local.entity.TenantPropertyCrossRef
import ai.sach.libraryup.data.local.entity.TenantWithMultipleProperties
import ai.sach.libraryup.data.local.entity.TenantPropertyInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TenantPropertyDao {
    
    // Insert tenant-property relationship
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTenantPropertyRelation(crossRef: TenantPropertyCrossRef)
    
    // Insert multiple relationships
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTenantPropertyRelations(crossRefs: List<TenantPropertyCrossRef>)
    
    // Update relationship
    @Update
    suspend fun updateTenantPropertyRelation(crossRef: TenantPropertyCrossRef)
    
    // Delete relationship
    @Delete
    suspend fun deleteTenantPropertyRelation(crossRef: TenantPropertyCrossRef)
    
    // Delete all relationships for a tenant
    @Query("DELETE FROM tenant_property_cross_ref WHERE tenantId = :tenantId")
    suspend fun deleteAllRelationsForTenant(tenantId: Long)
    
    // Delete all relationships for a property
    @Query("DELETE FROM tenant_property_cross_ref WHERE propertyId = :propertyId")
    suspend fun deleteAllRelationsForProperty(propertyId: String)

    /** Every row, for backup export. */
    @Query("SELECT * FROM tenant_property_cross_ref")
    suspend fun getAllSync(): List<TenantPropertyCrossRef>

    // Get all tenants with their properties
    @Transaction
    @Query("SELECT * FROM tenants")
    fun getTenantsWithMultipleProperties(): Flow<List<TenantWithMultipleProperties>>
    
    // Get tenants with their properties in batches to prevent memory issues
    @Transaction
    @Query("SELECT * FROM tenants LIMIT :limit OFFSET :offset")
    fun getTenantsWithMultiplePropertiesBatch(offset: Int, limit: Int): Flow<List<TenantWithMultipleProperties>>
    
    // Get a specific tenant with their properties
    @Transaction
    @Query("SELECT * FROM tenants WHERE id = :tenantId")
    suspend fun getTenantWithProperties(tenantId: Long): TenantWithMultipleProperties?
    
    // Get tenant-property info for UI display
    @Query("""
        SELECT 
            t.id as tenantId,
            t.name as tenantName,
            p.id as propertyId,
            p.name as propertyName,
            p.address as propertyAddress,
            tpc.floor,
            tpc.unitNumber,
            tpc.rentAmount,
            tpc.isActive
        FROM tenants t
        INNER JOIN tenant_property_cross_ref tpc ON t.id = tpc.tenantId
        INNER JOIN properties p ON tpc.propertyId = p.id
        ORDER BY t.name, p.name
    """)
    fun getTenantPropertyInfo(): Flow<List<TenantPropertyInfo>>
    
    // Get properties for a specific tenant
    @Query("""
        SELECT p.id, p.name, p.address, p.totalUnits, p.floors, p.description, p.type, p.unitRent, p.occupancyRate
        FROM properties p
        INNER JOIN tenant_property_cross_ref tpc ON p.id = tpc.propertyId
        WHERE tpc.tenantId = :tenantId AND tpc.isActive = 1
    """)
    fun getPropertiesForTenant(tenantId: Long): Flow<List<ai.sach.libraryup.data.local.entity.Property>>
    
    // Get tenants for a specific property
    @Query("""
        SELECT t.*
        FROM tenants t
        INNER JOIN tenant_property_cross_ref tpc ON t.id = tpc.tenantId
        WHERE tpc.propertyId = :propertyId AND tpc.isActive = 1
    """)
    fun getTenantsForProperty(propertyId: String): Flow<List<ai.sach.libraryup.data.local.entity.Tenant>>
    
    // Check if a tenant-property relationship exists
    @Query("SELECT COUNT(*) FROM tenant_property_cross_ref WHERE tenantId = :tenantId AND propertyId = :propertyId")
    suspend fun relationshipExists(tenantId: Long, propertyId: String): Int
    
    // Get active relationships count for a tenant
    @Query("SELECT COUNT(*) FROM tenant_property_cross_ref WHERE tenantId = :tenantId AND isActive = 1")
    suspend fun getActivePropertyCountForTenant(tenantId: Long): Int
    
    // Get active relationships count for a property
    @Query("SELECT COUNT(*) FROM tenant_property_cross_ref WHERE propertyId = :propertyId AND isActive = 1")
    suspend fun getActiveTenantCountForProperty(propertyId: String): Int

}