package ai.sach.libraryup.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * Data class representing a tenant with all their associated properties
 * This supports the many-to-many relationship between tenants and properties
 */
data class TenantWithMultipleProperties(
    @Embedded val tenant: Tenant,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = TenantPropertyCrossRef::class,
            parentColumn = "tenantId",
            entityColumn = "propertyId"
        )
    )
    val properties: List<Property>
)

/**
 * Simplified data class for UI display
 */
data class TenantPropertyInfo(
    val tenantId: Long,
    val tenantName: String,
    val propertyId: String,
    val propertyName: String,
    val propertyAddress: String,
    val floor: String?,
    val unitNumber: String?,
    val rentAmount: String?,
    val isActive: Boolean
)