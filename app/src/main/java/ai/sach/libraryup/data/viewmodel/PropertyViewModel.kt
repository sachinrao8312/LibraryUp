package ai.sach.libraryup.ui.viewmodel

import ai.sach.libraryup.data.local.entity.Property
import ai.sach.libraryup.data.repository.PropertyRepository
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PropertyViewModel(private val repository: PropertyRepository) : ViewModel() {

    // All properties as a Flow
    val allProperties = repository.allProperties.asLiveData()

    // Function to add a new property
    fun addProperty(
        name: String,
        address: String,
        totalUnits: Int,
        floors: Int,
        description: String? = null,
        type: String? = null,
        unitRent: Int? = null,
        occupancyRate: Int = 0
    ) {
        val property = Property(
            id = repository.generatePropertyId(),
            name = name,
            address = address,
            totalUnits = totalUnits,
            floors = floors,
            description = description,
            type = type,
            unitRent = unitRent,
            occupancyRate = occupancyRate
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.insertProperty(property)
        }
    }

    // Function to update an existing property
    fun updateProperty(property: Property) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateProperty(property)
        }
    }

    // Function to delete a property
    fun deleteProperty(property: Property) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProperty(property)
        }
    }

    // Function to delete multiple properties at once
    fun deleteProperties(properties: List<Property>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePropertiesByIds(properties.map { it.id })
        }
    }

    // Function to get a property by ID
    fun getPropertyById(id: String): LiveData<Property?> {
        val result = MutableLiveData<Property?>()
        viewModelScope.launch(Dispatchers.IO) {
            val property = repository.getPropertyById(id)
            withContext(Dispatchers.Main) {
                result.value = property
            }
        }
        return result
    }
}

// Factory to create the ViewModel with dependencies
class PropertyViewModelFactory(private val repository: PropertyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PropertyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PropertyViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}