package ai.sach.rentup.data.repository

import ai.sach.rentup.data.local.dao.PropertyDao
import ai.sach.rentup.data.local.entity.Property
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PropertyRepository @Inject constructor(
    private val propertyDao: PropertyDao
) {

    val allProperties: Flow<List<Property>> = propertyDao.getAllProperties()

    /**
     * Get all properties synchronously for data migration
     */
    suspend fun getAllPropertiesSync(): List<Property> {
        return withContext(Dispatchers.IO) {
            propertyDao.getAllPropertiesSync()
        }
    }

    suspend fun getPropertyById(id: String): Property? {
        return propertyDao.getPropertyById(id)
    }

    suspend fun insertProperty(property: Property) {
        propertyDao.insertProperty(property)
    }

    suspend fun updateProperty(property: Property) {
        propertyDao.updateProperty(property)
    }

    suspend fun deleteProperty(property: Property) {
        propertyDao.deleteProperty(property)
    }

    suspend fun deletePropertyById(id: String) {
        propertyDao.deletePropertyById(id)
    }

    suspend fun deletePropertiesByIds(ids: List<String>) {
        propertyDao.deletePropertiesByIds(ids)
    }

    // Helper function to generate a unique ID for new properties
    fun generatePropertyId(): String {
        return UUID.randomUUID().toString()
    }
}
