package ai.sach.libraryup.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tenants",
    indices = [
        Index(value = ["mobileNumber"]),
        Index(value = ["propertyId"]),
        Index(value = ["isPaid"]),
        Index(value = ["dueDate"]),
        Index(value = ["name"]),
        Index(value = ["formId"])
    ]
)
data class Tenant(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val status: String = "",
    val isPaid: Boolean = false,
    val mobileNumber: String = "", // Changed to String type for better flexibility with formats
    val dueDate: String = "",
    val propertyId: String = "",
    val floor: String = "",
    val amount: String? = null,
    val avatarUri: String? = null, // This will store the URI to the tenant's image
    val permanentAddress: String = "", // New field for tenant's permanent address
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    // The onboarding form this tenant was created or last updated from. Lets a tap on the
    // "new tenant submission" push notification (which only carries a formId - the local
    // row doesn't exist yet when that notification is built) resolve back to this tenant
    // once the sync worker has imported it.
    val formId: String? = null
)