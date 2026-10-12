package ai.sach.rentup.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

// TenantFamilyInfo.kt
@Entity(
    tableName = "tenant_family_info",
    foreignKeys = [
        ForeignKey(
            entity = Tenant::class,
            parentColumns = ["id"],
            childColumns = ["tenantId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tenantId")]
)
data class TenantFamilyInfo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: Long,
    val name: String,
    val relationship: String,
    val age: Int,
    val isEmergencyContact: Boolean = false, // New field
    val mobileNumber: String? = null,         // New field (nullable)
    val address: String? = null, // New field for emergency contact's address
    val propertyId: String? = null // New field for property mapping
)
