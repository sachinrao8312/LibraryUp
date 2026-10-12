package ai.sach.rentup.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "properties",
    indices = [
        Index(value = ["name"]),
        Index(value = ["address"]),
        Index(value = ["type"])
    ]
)
data class Property(
    @PrimaryKey
    val id: String,
    val name: String,
    val address: String,
    val totalUnits: Int,
    val floors: Int,
    val description: String? = null, // Optional
    val type: String? = null, // Optional
    val unitRent: Int? = null, // Optional
    // Additional fields needed for UI display
    val occupancyRate: Int = 0
)
