package ai.sach.libraryup.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Represents a relationship between Tenant and Property entities.
 * This is used when we want to get a tenant with its full property data.
 */
data class TenantWithFullProperty(
    @Embedded val tenant: Tenant,
    @Relation(
        parentColumn = "propertyId",
        entityColumn = "id"
    )
    val property: Property
)