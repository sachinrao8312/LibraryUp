package ai.sach.rentup.data.local.dao

import ai.sach.rentup.data.local.entity.Property
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PropertyDao {
    @Query("SELECT * FROM properties ORDER BY name ASC")
    fun getAllProperties(): Flow<List<Property>>

    @Query("SELECT * FROM properties ORDER BY name ASC")
    suspend fun getAllPropertiesSync(): List<Property>

    @Query("SELECT * FROM properties WHERE id = :propertyId")
    suspend fun getPropertyById(propertyId: String): Property?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProperty(property: Property)

    @Update
    suspend fun updateProperty(property: Property)

    @Delete
    suspend fun deleteProperty(property: Property)

    @Query("DELETE FROM properties WHERE id = :propertyId")
    suspend fun deletePropertyById(propertyId: String)

    @Query("DELETE FROM properties WHERE id IN (:propertyIds)")
    suspend fun deletePropertiesByIds(propertyIds: List<String>)

    /** Wipes every property. Any remaining tenant-property links cascade with it. */
    @Query("DELETE FROM properties")
    suspend fun deleteAllProperties()

    /** Bulk insert used by backup restore. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllProperties(properties: List<Property>)

    /**
     * Gets the total number of properties in the database.
     * Used for checking if the database has been populated.
     *
     * @return Count of property records
     */
    @Query("SELECT COUNT(*) FROM properties")
    suspend fun getPropertyCount(): Int

}