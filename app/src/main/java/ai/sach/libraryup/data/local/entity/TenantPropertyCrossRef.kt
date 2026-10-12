package ai.sach.rentup.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Junction table for many-to-many relationship between Tenant and Property
 * This allows a tenant to rent multiple properties and a property to have multiple tenants
 */
@Entity(
    tableName = "tenant_property_cross_ref",
    primaryKeys = ["tenantId", "propertyId"],
    foreignKeys = [
        ForeignKey(
            entity = Tenant::class,
            parentColumns = ["id"],
            childColumns = ["tenantId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Property::class,
            parentColumns = ["id"],
            childColumns = ["propertyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tenantId"]),
        Index(value = ["propertyId"]),
        Index(value = ["isActive"]),
        Index(value = ["tenantId", "isActive"]),
        Index(value = ["propertyId", "isActive"])
    ]
)
data class TenantPropertyCrossRef(
    val tenantId: Long,
    val propertyId: String,
    val floor: String? = null,
    val unitNumber: String? = null,
    val rentAmount: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val isActive: Boolean = true
)