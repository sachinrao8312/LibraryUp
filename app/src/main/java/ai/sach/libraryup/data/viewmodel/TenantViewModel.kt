package ai.sach.rentup.data.viewmodel

import ai.sach.rentup.data.api.GenerateLinkRequest
import ai.sach.rentup.data.api.GenerateLinkResponse
import ai.sach.rentup.data.api.OnboardingApiProvider
import ai.sach.rentup.data.local.entity.EmergencyContact
import ai.sach.rentup.data.local.entity.PaymentHistory
import ai.sach.rentup.data.local.entity.Tenant
import ai.sach.rentup.data.local.entity.TenantDocument
import ai.sach.rentup.data.local.entity.TenantFamilyInfo
import ai.sach.rentup.data.repository.PropertyRepository
import ai.sach.rentup.data.repository.TenantRepository
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


/**
 * ViewModel for managing tenant-related data operations.
 * Provides an abstraction layer between the UI and data repository.
 */
class TenantViewModel(
    private val tenantRepository: TenantRepository,
    private val propertyRepository: PropertyRepository,
) : ViewModel() {
    // Expose tenants with property information
    private val _refreshTrigger = MutableLiveData<Long>()
    val allTenants = _refreshTrigger.switchMap {
        tenantRepository.allTenantsWithProperty.asLiveData()
    }
    
    // Get all properties
    val allProperties = propertyRepository.allProperties.asLiveData()

    // Create a UI model for payment history - moved before init to ensure proper initialization
    private val _allPaymentsWithTenantInfo = MutableStateFlow<List<ai.sach.rentup.ui.screens.historyScreen.PaymentHistoryItem>>(emptyList())
    val allPaymentsWithTenantInfo: StateFlow<List<ai.sach.rentup.ui.screens.historyScreen.PaymentHistoryItem>> = _allPaymentsWithTenantInfo

    // Loading state for payment data
    private val _isPaymentDataLoading = MutableStateFlow(false)
    val isPaymentDataLoading: StateFlow<Boolean> = _isPaymentDataLoading

    // Lazy initialization flag to prevent multiple concurrent initializations
    private var isPaymentDataInitialized = false
    private val initializationMutex = kotlinx.coroutines.sync.Mutex()
    private var paymentDataJob: kotlinx.coroutines.Job? = null

    init {
        // Trigger initial load
        refreshTenantData()
        ensurePaymentDataInitialized() // Use lazy initialization
    }

    // Add reliable refresh method
    fun refreshTenantData() {
        _refreshTrigger.value = System.currentTimeMillis()
    }
    
    // Tenant CRUD operations
    fun insertTenant(tenant: Tenant) = viewModelScope.launch(Dispatchers.IO) {
        val id = tenantRepository.insertTenant(tenant)
        val insertedTenant = tenantRepository.getTenantById(id)
        android.util.Log.d("TenantViewModel", "Inserted tenant: $insertedTenant")
        
        // Invalidate cache to ensure fresh data
        tenantRepository.clearCache()
        
        // Single UI update on main thread - no need for multiple refreshes
        withContext(Dispatchers.Main) {
            refreshTenantData()
        }
    }

    suspend fun insertTenantAndReturnId(tenant: Tenant): Long {
        val id = tenantRepository.insertTenant(tenant)
        val insertedTenant = tenantRepository.getTenantById(id)
        android.util.Log.d("TenantViewModel", "Inserted tenant: $insertedTenant")
        
        // Invalidate cache to ensure fresh data
        tenantRepository.clearCache()
        
        // Single UI update on main thread
        withContext(Dispatchers.Main) {
            refreshTenantData()
        }
        return id
    }

    fun updateTenant(tenant: Tenant) = viewModelScope.launch(Dispatchers.IO) {
        try {
            android.util.Log.d("TenantViewModel", "Updating tenant: ${tenant.id} with isPaid=${tenant.isPaid}, status=${tenant.status}")
            
            // Update the tenant in the repository
            tenantRepository.updateTenant(tenant)
            
            // Get the updated tenant to verify the changes
            val updatedTenant = tenantRepository.getTenantById(tenant.id)
            android.util.Log.d("TenantViewModel", "Updated tenant retrieved: $updatedTenant")
            
            // Invalidate cache to ensure fresh data
            tenantRepository.clearCache()
            
            // Single UI update on main thread - batch all updates together
            withContext(Dispatchers.Main) {
                refreshTenantData()
                // Only refresh payment data if this is a payment-related update
                if (tenant.isPaid) {
                    refreshPaymentData()
                }
            }
            
            android.util.Log.d("TenantViewModel", "Tenant update completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("TenantViewModel", "Error updating tenant ${tenant.id}: ${e.message}", e)
        }
    }

    /**
     * Delete a tenant and all associated data (family members, documents, payments)
     */
    fun deleteTenant(tenant: Tenant) = viewModelScope.launch(Dispatchers.IO) {
        try {
            // First, clean up all related data
            android.util.Log.d("TenantViewModel", "Deleting tenant ${tenant.id} and all associated data")
            
            // 1. Get and delete family members
            val familyMembers = tenantRepository.getFamilyInfoForTenant(tenant.id).first()
            familyMembers.forEach { member ->
                tenantRepository.deleteTenantFamilyInfo(member)
            }
            android.util.Log.d("TenantViewModel", "Deleted ${familyMembers.size} family members for tenant ${tenant.id}")
            
            // 2. Get and delete documents
            val documents = tenantRepository.getDocumentsForTenant(tenant.id).first()
            documents.forEach { document ->
                tenantRepository.deleteTenantDocument(document)
            }
            android.util.Log.d("TenantViewModel", "Deleted ${documents.size} documents for tenant ${tenant.id}")
            
            // 3. Finally delete the tenant
            tenantRepository.deleteTenant(tenant)
            android.util.Log.d("TenantViewModel", "Successfully deleted tenant ${tenant.id}")
            
            // 4. Refresh data on main thread
            withContext(Dispatchers.Main) {
                refreshTenantData()
                refreshPaymentData() // Also refresh payment data to remove deleted tenant's payments
            }
            
            // Add delay to ensure database operation completes
            delay(300)
            
            // Second refresh on main thread
            withContext(Dispatchers.Main) {
                refreshTenantData() // Second refresh to ensure UI updates
            }
        } catch (e: Exception) {
            android.util.Log.e("TenantViewModel", "Error deleting tenant ${tenant.id}: ${e.message}", e)
        }
    }

    /**
     * Delete multiple tenants and all their associated data (cascades via foreign keys)
     */
    fun deleteTenants(tenants: List<Tenant>) = viewModelScope.launch(Dispatchers.IO) {
        try {
            android.util.Log.d("TenantViewModel", "Deleting ${tenants.size} tenants and all associated data")
            tenantRepository.deleteTenants(tenants)
            withContext(Dispatchers.Main) {
                refreshTenantData()
                refreshPaymentData()
            }
            delay(300)
            withContext(Dispatchers.Main) {
                refreshTenantData()
            }
        } catch (e: Exception) {
            android.util.Log.e("TenantViewModel", "Error deleting tenants: ${e.message}", e)
        }
    }

    // Selected tenant for detailed view
    private val _selectedTenantId = MutableLiveData<Long>()

    // Public LiveData exposing tenant family information
    val selectedTenantFamilyInfo = _selectedTenantId.switchMap { tenantId ->
        tenantRepository.getFamilyInfoForTenant(tenantId).asLiveData()
    }

    // Public LiveData exposing tenant documents
    val selectedTenantDocuments = _selectedTenantId.switchMap { tenantId ->
        tenantRepository.getDocumentsForTenant(tenantId).asLiveData()
    }

    // Public LiveData exposing tenant payment history
    val selectedTenantPayments = _selectedTenantId.switchMap { tenantId ->
        tenantRepository.getPaymentHistoryForTenant(tenantId).asLiveData()
    }

    /**
     * Sets the currently selected tenant ID to retrieve related data
     */
    fun selectTenant(tenantId: Long) {
        _selectedTenantId.value = tenantId
    }

    // Family info CRUD operations
    fun insertFamilyInfo(familyInfo: TenantFamilyInfo) = viewModelScope.launch(Dispatchers.IO) {
        tenantRepository.addTenantFamilyInfo(familyInfo)
        withContext(Dispatchers.Main) {
            refreshTenantData()
        }
    }

    fun updateFamilyInfo(familyInfo: TenantFamilyInfo) = viewModelScope.launch(Dispatchers.IO) {
        tenantRepository.updateTenantFamilyInfo(familyInfo)
        withContext(Dispatchers.Main) {
            refreshTenantData()
        }
    }

    fun deleteFamilyInfo(familyInfo: TenantFamilyInfo) = viewModelScope.launch(Dispatchers.IO) {
        tenantRepository.deleteTenantFamilyInfo(familyInfo)
        withContext(Dispatchers.Main) {
            refreshTenantData()
        }
    }

    // Document CRUD operations
    fun insertDocument(document: TenantDocument) = viewModelScope.launch(Dispatchers.IO) {
        tenantRepository.addTenantDocument(document)
        withContext(Dispatchers.Main) {
            refreshTenantData()
        }
    }

    fun updateDocument(document: TenantDocument) = viewModelScope.launch(Dispatchers.IO) {
        // This method does not exist in the repository, but we could implement it if needed
        // For now, we'll delete and insert
        tenantRepository.deleteTenantDocument(document)
        tenantRepository.addTenantDocument(document)
        withContext(Dispatchers.Main) {
            refreshTenantData()
        }
    }

    fun deleteDocument(document: TenantDocument) = viewModelScope.launch(Dispatchers.IO) {
        tenantRepository.deleteTenantDocument(document)
        withContext(Dispatchers.Main) {
            refreshTenantData()
        }
    }

    fun deleteLocalDocument(document: ai.sach.rentup.data.local.entity.TenantDocument) = viewModelScope.launch(Dispatchers.IO) {
        tenantRepository.deleteTenantDocument(document)
        withContext(Dispatchers.Main) {
            refreshTenantData()
        }
    }

    // Payment history CRUD operations
    fun insertPayment(payment: PaymentHistory, tenantId: Long) = viewModelScope.launch(Dispatchers.IO) {
        try {
            android.util.Log.d("TenantViewModel", "Inserting payment for tenant ID: $tenantId, amount: ${payment.amount}")
            
            val tenant = tenantRepository.getTenantById(tenantId)
            if (tenant != null) {
                // Insert the payment
                tenantRepository.addPaymentHistoryItem(payment, tenantId)
                android.util.Log.d("TenantViewModel", "Payment inserted successfully")
                
                // Clear cache to ensure fresh data
                tenantRepository.clearCache()
                
                // Refresh payment data immediately (this is a suspend function that will wait)
                initializePaymentData()
                
                // Switch to main thread for UI updates
                withContext(Dispatchers.Main) {
                    // Refresh tenant data
                    refreshTenantData()
                }
                
                android.util.Log.d("TenantViewModel", "Payment insertion completed successfully")
            } else {
                android.util.Log.e("TenantViewModel", "Failed to insert payment: Tenant with ID $tenantId not found")
            }
        } catch (e: Exception) {
            android.util.Log.e("TenantViewModel", "Exception inserting payment: ${e.message}", e)
        }
    }

    fun updatePayment(payment: PaymentHistory) = viewModelScope.launch(Dispatchers.IO) {
        // This method does not exist in the repository, but we could implement it if needed
        // For now, we'll delete and insert
        // Note: We need a delete method for PaymentHistoryItem
        // tenantRepository.addPaymentHistoryItem(payment)
        withContext(Dispatchers.Main) {
            initializePaymentData() // Ensure payment history is refreshed
        }
    }

    fun deletePayment(payment: PaymentHistory) = viewModelScope.launch(Dispatchers.IO) {
        // This method does not exist in the repository, but we could implement it if needed
        withContext(Dispatchers.Main) {
            initializePaymentData() // Ensure payment history is refreshed
        }
    }

    // Helper function to get family info for a tenant
    fun getFamilyInfoForTenant(tenantId: Long): LiveData<List<TenantFamilyInfo>> {
        return tenantRepository.getFamilyInfoForTenant(tenantId).asLiveData()
    }

    // Helper function to get documents for a tenant
    fun getDocumentsForTenant(tenantId: Long): LiveData<List<TenantDocument>> {
        return tenantRepository.getDocumentsForTenant(tenantId).asLiveData()
    }

    // Helper function to get payments for a tenant
    fun getPaymentsForTenant(tenantId: Long): LiveData<List<PaymentHistory>> {
        return tenantRepository.getPaymentHistoryForTenant(tenantId).asLiveData()
    }

    // Helper functions for filtering tenants
    fun searchTenantsByName(query: String) = viewModelScope.launch(Dispatchers.IO) {
        tenantRepository.searchTenantsByName(query)
    }

    // ===== MULTIPLE PROPERTIES SUPPORT =====
    

    
    /**
     * Get tenants with their multiple properties using the junction table
     * This replaces the comma-separated approach
     */
    fun getTenantsWithMultipleProperties() = tenantRepository.getTenantsWithMultipleProperties().asLiveData()
    

    
    /**
     * Check migration status
     */
    fun checkMigrationStatus(context: Context): Boolean {
        return try {
            val sharedPrefs = context.getSharedPreferences("rentup_migrations", Context.MODE_PRIVATE)
            sharedPrefs.getBoolean("tenant_property_migration_completed", false)
        } catch (e: Exception) {
            android.util.Log.e("TenantViewModel", "Error checking migration status: ${e.message}", e)
            false
        }
    }
    

    
    /**
     * Add a property to a tenant (supports multiple properties per tenant)
     * Uses the NEW NORMALIZED approach with junction table
     */
    fun addPropertyToTenant(
        tenantId: Long,
        propertyId: String,
        floor: String? = null,
        unitNumber: String? = null,
        rentAmount: String? = null
    ) = viewModelScope.launch(Dispatchers.IO) {
        try {
            // Use the NEW normalized approach with junction table
            tenantRepository.addPropertyToTenantNew(tenantId, propertyId, floor, unitNumber, rentAmount)
            withContext(Dispatchers.Main) {
                refreshTenantData()
                refreshPaymentData()
            }
            android.util.Log.d("TenantViewModel", "Successfully added property $propertyId to tenant $tenantId using normalized approach")
        } catch (e: Exception) {
            android.util.Log.e("TenantViewModel", "Error adding property to tenant: ${e.message}", e)
        }
    }
    

    
    /**
     * Remove a property from a tenant
     */
    fun removePropertyFromTenant(tenantId: Long, propertyId: String) = viewModelScope.launch(Dispatchers.IO) {
        try {
            tenantRepository.removePropertyFromTenant(tenantId, propertyId)
            withContext(Dispatchers.Main) {
                refreshTenantData()
            }
            android.util.Log.d("TenantViewModel", "Successfully removed property $propertyId from tenant $tenantId")
        } catch (e: Exception) {
            android.util.Log.e("TenantViewModel", "Error removing property from tenant: ${e.message}", e)
        }
    }
    
    /**
     * Get all properties for a specific tenant using junction table
     */
    fun getPropertiesForTenant(tenantId: Long): LiveData<List<ai.sach.rentup.data.local.entity.Property>> = 
        tenantRepository.getPropertiesForTenant(tenantId).asLiveData()
    
    /**
     * Get all tenants for a specific property using junction table
     */
    fun getTenantsForProperty(propertyId: String): LiveData<List<Tenant>> = 
        tenantRepository.getTenantsForProperty(propertyId).asLiveData()
    
    /**
     * Check if a tenant is assigned to a specific property
     */
    suspend fun isTenantAssignedToProperty(tenantId: Long, propertyId: String): Boolean {
        return try {
            tenantRepository.relationshipExists(tenantId, propertyId)
        } catch (e: Exception) {
            android.util.Log.e("TenantViewModel", "Error checking tenant-property relationship: ${e.message}", e)
            false
        }
    }
    
    /**
     * Get count of active properties for a tenant
     */
    suspend fun getActivePropertyCountForTenant(tenantId: Long): Int {
        return try {
            tenantRepository.getActivePropertyCountForTenant(tenantId)
        } catch (e: Exception) {
            android.util.Log.e("TenantViewModel", "Error getting property count for tenant: ${e.message}", e)
            0
        }
    }
    
    /**
     * Get count of active tenants for a property
     */
    suspend fun getActiveTenantCountForProperty(propertyId: String): Int {
        return try {
            tenantRepository.getActiveTenantCountForProperty(propertyId)
        } catch (e: Exception) {
            android.util.Log.e("TenantViewModel", "Error getting tenant count for property: ${e.message}", e)
            0
        }
    }
    
    /**
     * Update tenant-property relationship details (floor, unit, rent)
     */
    fun updateTenantPropertyDetails(
        tenantId: Long,
        propertyId: String,
        floor: String? = null,
        unitNumber: String? = null,
        rentAmount: String? = null
    ) = viewModelScope.launch(Dispatchers.IO) {
        try {
            tenantRepository.updateTenantPropertyDetails(tenantId, propertyId, floor, unitNumber, rentAmount)
            withContext(Dispatchers.Main) {
                refreshTenantData()
            }
            android.util.Log.d("TenantViewModel", "Successfully updated property details for tenant $tenantId, property $propertyId")
        } catch (e: Exception) {
            android.util.Log.e("TenantViewModel", "Error updating tenant-property details: ${e.message}", e)
        }
    }

    /**
     * Ensures payment data is initialized only once, with thread safety
     */
    fun ensurePaymentDataInitialized() {
        android.util.Log.d("TenantViewModel", "ensurePaymentDataInitialized called - isInitialized: $isPaymentDataInitialized, jobActive: ${paymentDataJob?.isActive}")
        if (!isPaymentDataInitialized && paymentDataJob?.isActive != true) {
            android.util.Log.d("TenantViewModel", "Starting payment data initialization")
            paymentDataJob = viewModelScope.launch(Dispatchers.IO) {
                initializationMutex.withLock {
                    if (!isPaymentDataInitialized) {
                        android.util.Log.d("TenantViewModel", "Executing payment data initialization")
                        initializePaymentData()
                        isPaymentDataInitialized = true
                        android.util.Log.d("TenantViewModel", "Payment data initialization completed")
                    } else {
                        android.util.Log.d("TenantViewModel", "Payment data already initialized, skipping")
                    }
                }
            }
        } else {
            android.util.Log.d("TenantViewModel", "Payment data initialization skipped - already initialized or job active")
        }
    }

    // Initialize payments data with pagination support
    suspend fun initializePaymentData(pageSize: Int = 100) {
        try {
            _isPaymentDataLoading.value = true
            android.util.Log.d("TenantViewModel", "Initializing payment data with pageSize=$pageSize")
            val items = mutableListOf<ai.sach.rentup.ui.screens.historyScreen.PaymentHistoryItem>()

            // Use a one-time collection instead of continuous collection
            val tenantsWithProperty = tenantRepository.allTenantsWithProperty.first()
            // Fetch all properties once for mapping
            val allProperties = propertyRepository.allProperties.first()

            // Handle multi-property tenants correctly
            val processedTenants = mutableSetOf<Long>()

            // Collect all payments for all tenants in parallel with pagination
            kotlinx.coroutines.coroutineScope {
                val deferreds = tenantsWithProperty.map { tenantWithMultipleProperties ->
                    async {
                        try {
                            if (tenantWithMultipleProperties.tenant.id in processedTenants) {
                                return@async emptyList()
                            }
                            processedTenants.add(tenantWithMultipleProperties.tenant.id)

                            val properties = tenantWithMultipleProperties.properties
                            val firstPropertyName = properties.firstOrNull()?.name?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } ?: "Unknown Property"
                            // Only show the first property name, no '+N more property'
                            val payments = tenantRepository.getPaymentHistoryForTenant(tenantWithMultipleProperties.tenant.id).first()

                            payments.take(pageSize).map { payment ->
                                ai.sach.rentup.ui.screens.historyScreen.PaymentHistoryItem(
                                    id = payment.id,
                                    tenantName = tenantWithMultipleProperties.tenant.name,
                                    propertyName = firstPropertyName,
                                    amount = payment.amount,
                                    date = payment.date,
                                    collectedBy = payment.collectedBy,
                                    remark = payment.remark,
                                    avatarUri = tenantWithMultipleProperties.tenant.avatarUri
                                )
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("TenantViewModel", "Error loading payments for tenant ${tenantWithMultipleProperties.tenant.id}: ${e.message}", e)
                            emptyList()
                        }
                    }
                }
                items.addAll(deferreds.flatMap { it.await() })
            }

            // Update the StateFlow directly
            _allPaymentsWithTenantInfo.value = items.sortedByDescending { it.date }
            android.util.Log.d("TenantViewModel", "Payment data initialized with ${items.size} items")
        } catch (e: Exception) {
            android.util.Log.e("TenantViewModel", "Error in initializePaymentData: ${e.message}", e)
            _allPaymentsWithTenantInfo.value = emptyList()
        } finally {
            _isPaymentDataLoading.value = false
        }
    }
    
    /**
     * Refresh payment data when tenant-property relationships change
     */
    fun refreshPaymentData() {
        // Cancel any existing job
        paymentDataJob?.cancel()
        isPaymentDataInitialized = false
        ensurePaymentDataInitialized()
    }

    override fun onCleared() {
        super.onCleared()
        
        try {
            // Cancel any ongoing jobs to prevent memory leaks
            paymentDataJob?.cancel()
            
            // Clear all cached data
            _allPaymentsWithTenantInfo.value = emptyList()
            _isPaymentDataLoading.value = false
            
            // Reset initialization flags
            isPaymentDataInitialized = false
            
            // Clear onboarding link result
            _onboardingLinkResult.value = null
            _firestoreTenantData.value = null
            
            Log.d("TenantViewModel", "ViewModel cleared - all data and jobs cancelled")
        } catch (e: Exception) {
            Log.e("TenantViewModel", "Error in onCleared", e)
        }
    }

    // StateFlow to expose the onboarding link result
    private val _onboardingLinkResult = MutableStateFlow<GenerateLinkResponse?>(null)
    val onboardingLinkResult: StateFlow<GenerateLinkResponse?> = _onboardingLinkResult

    /**
     * Asks the backend for an onboarding form link and publishes the result to
     * [onboardingLinkResult] - `success = false` with a blank url when it can't.
     *
     * There is deliberately no locally made fallback link: only a link the backend
     * registered can be submitted, so a made-up one would reach the tenant as a dead link.
     */
    fun generateOnboardingFormLink(
        ownerId: String,
        propertyAddress: String,
        deviceToken: String,
        baseUrl: String,
        tenantId: String? = null // Optional parameter for updating existing tenants
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = try {
                val idToken = FirebaseAuth.getInstance().currentUser
                    ?.getIdToken(false)?.await()?.token
                    ?: throw IllegalStateException("Not signed in")
                val response = OnboardingApiProvider.create(baseUrl).generateLink(
                    authorization = "Bearer $idToken",
                    request = GenerateLinkRequest(
                        ownerId = ownerId,
                        propertyAddress = propertyAddress,
                        deviceToken = deviceToken,
                        tenantId = tenantId
                    )
                )
                val body = response.body()
                if (response.isSuccessful && body != null && body.url.isNotBlank()) {
                    body
                } else {
                    Log.w("TenantViewModel", "Onboarding link request failed: HTTP ${response.code()}")
                    failedOnboardingLink()
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("TenantViewModel", "Onboarding link request failed", e)
                failedOnboardingLink()
            }
            withContext(Dispatchers.Main) {
                _onboardingLinkResult.value = result
            }
        }
    }

    /** Clears the last result once shown, so the next request - even an identical one - is delivered. */
    fun consumeOnboardingLinkResult() {
        _onboardingLinkResult.value = null
    }

    private fun failedOnboardingLink() =
        GenerateLinkResponse(success = false, formId = "", url = "", expiresAt = "")
    

    // StateFlow to expose fetched Firestore tenant data
    private val _firestoreTenantData = MutableStateFlow<Map<String, Any>?>(null)
    val firestoreTenantData: StateFlow<Map<String, Any>?> = _firestoreTenantData

    private fun safeCastToListOfMaps(data: Any?): List<Map<String, Any>> {
        return (data as? List<*>)?.mapNotNull { it as? Map<*, *> }?.map {
            @Suppress("UNCHECKED_CAST")
            it as Map<String, Any>
        } ?: emptyList()
    }




    fun fetchTenantFromFirestore(formId: String) {
        viewModelScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val db = FirebaseFirestore.getInstance()
            val query = db.collection("tenant-forms")
                .whereEqualTo("formId", formId)
                .whereEqualTo("ownerId", userId)
                .get()
                .await()
            val doc = query.documents.firstOrNull()
            _firestoreTenantData.value = doc?.data
        }
    }

    // Map and save tenant data from Firestore to local database
    // If tenantId is provided, update existing tenant instead of creating a new one
    fun mapAndSaveTenantToLocal(data: Map<String, Any>?) {
        if (data == null) return
        viewModelScope.launch {
            try {
                val existingTenantId = data["tenantId"] as? String
                var tenantId = 0L
                var matchedTenant: Tenant? = null
                if (!existingTenantId.isNullOrBlank()) {
                    tenantId = existingTenantId.toLongOrNull() ?: 0L
                    matchedTenant = tenantRepository.getTenantById(tenantId)
                    if (matchedTenant == null) {
                        android.util.Log.w("TenantViewModel", "No tenant found with tenantId=$existingTenantId, fallback to phone match.")
                    }
                }
                if (matchedTenant == null) {
                    val phone = data["phone"] as? String ?: ""
                    if (phone.isNotBlank()) {
                        val allTenants = tenantRepository.allTenants.first()
                        matchedTenant = allTenants.find {
                            normalizePhone(it.mobileNumber) == normalizePhone(phone)
                        }
                        if (matchedTenant != null) {
                            android.util.Log.w("TenantViewModel", "Fallback match by phone for tenant phone: $phone. Using existing tenantId=${matchedTenant.id}")
                            tenantId = matchedTenant.id
                        }
                    }
                }
                if (matchedTenant != null) {
                    val updatedTenant = matchedTenant.copy(
                        name = data["name"] as? String ?: matchedTenant.name,
                        mobileNumber = data["phone"] as? String ?: matchedTenant.mobileNumber,
                        avatarUri = data["avatarUrl"] as? String ?: matchedTenant.avatarUri,
                        permanentAddress = data["address"] as? String ?: matchedTenant.permanentAddress,
                        emergencyContacts = safeCastToListOfMaps(data["emergencyContacts"]).map {
                            EmergencyContact(
                                name = it["name"] as? String ?: "",
                                phone = it["phone"] as? String ?: "",
                                relationship = it["relationship"] as? String ?: "",
                                address = it["address"] as? String ?: ""
                            )
                        }
                    )
                    tenantRepository.updateTenant(updatedTenant)
                } else {
                    val name = data["name"] as? String ?: ""
                    val phone = data["phone"] as? String ?: ""
                    android.util.Log.w("TenantViewModel", "No matching tenant found for name='$name', phone='$phone'. Skipping insertion to avoid duplicates.")
                    return@launch
                }
                val existingFamilyMembers = tenantRepository.getFamilyInfoForTenant(tenantId).first()
                existingFamilyMembers.forEach { member ->
                    tenantRepository.deleteTenantFamilyInfo(member)
                }
                val familyMembersData = safeCastToListOfMaps(data["familyMembers"])
                familyMembersData.forEach { memberData ->
                    val familyMember = ai.sach.rentup.data.local.entity.TenantFamilyInfo(
                        id = 0,
                        tenantId = tenantId,
                        name = memberData["name"] as? String ?: "",
                        relationship = memberData["relation"] as? String ?: "",
                        age = (memberData["age"] as? Number)?.toInt() ?: 0
                    )
                    tenantRepository.addTenantFamilyInfo(familyMember)
                }
                val emergencyContactsData = safeCastToListOfMaps(data["emergencyContacts"])
                // Get existing family members after clearing (should be empty, but just in case)
                val currentFamilyMembers = tenantRepository.getFamilyInfoForTenant(tenantId).first()
                val existingEmergencyPhones = currentFamilyMembers.filter { it.isEmergencyContact && it.mobileNumber != null }.map { it.mobileNumber!!.trim() }
                val existingEmergencyNames = currentFamilyMembers.filter { it.isEmergencyContact }.map { it.name.trim() }
                emergencyContactsData.forEach { contactData ->
                    val name = contactData["name"] as? String ?: ""
                    val relationship = contactData["relationship"] as? String ?: contactData["relation"] as? String ?: ""
                    val mobileNumber = contactData["phone"] as? String ?: ""
                    val address = contactData["address"] as? String
                    // Avoid duplicate by phone or name
                    if (mobileNumber.trim() !in existingEmergencyPhones && name.trim() !in existingEmergencyNames) {
                        android.util.Log.d("TenantViewModel", "Mapping emergency contact: name=$name, relationship=$relationship, mobileNumber=$mobileNumber, address=$address")
                        val familyInfo = ai.sach.rentup.data.local.entity.TenantFamilyInfo(
                            id = 0,
                            tenantId = tenantId,
                            name = name,
                            relationship = relationship,
                            age = 0,
                            isEmergencyContact = true,
                            mobileNumber = mobileNumber,
                            address = address
                        )
                        tenantRepository.addTenantFamilyInfo(familyInfo)
                    }
                }
                val filesData = safeCastToListOfMaps(data["files"])
                filesData.forEach { fileData ->
                    val fileName = fileData["originalName"] as? String ?: ""
                    val fileUrl = fileData["url"] as? String ?: ""
                    if (fileUrl.isNotEmpty()) {
                        val document = ai.sach.rentup.data.local.entity.TenantDocument(
                            id = 0,
                            tenantId = tenantId,
                            documentType = getDocumentTypeFromFileName(fileName),
                            documentPath = fileUrl,
                            uploadDate = System.currentTimeMillis().toString(),
                            description = fileName,
                            mimeType = getMimeTypeFromFileName(fileName)
                        )
                        tenantRepository.addTenantDocument(document)
                    }
                }
                // --- NEW: Map and save payment history from Firestore ---
                val paymentsData = safeCastToListOfMaps(data["payments"] ?: data["paymentHistory"])
                paymentsData.forEach { paymentMap ->
                    val payment = PaymentHistory(
                        tenantId = 0, // overwritten by addPaymentHistoryItem
                        id = 0,
                        date = paymentMap["date"] as? String ?: "",
                        amount = paymentMap["amount"] as? String ?: "",
                        collectedBy = paymentMap["collectedBy"] as? String ?: "",
                        remark = paymentMap["remark"] as? String
                    )
                    tenantRepository.addPaymentHistoryItem(payment, tenantId)
                }
                // --- END NEW ---
                android.util.Log.d("TenantViewModel", "Successfully processed tenant data with ID: $tenantId")
                refreshTenantData()
            } catch (e: Exception) {
                android.util.Log.e("TenantViewModel", "Error mapping and saving tenant", e)
            }
        }
    }
    
    // --- Normalization Utilities ---
    private fun normalizePhone(phone: String): String {
        return phone.replace("[^0-9]".toRegex(), "")
    }
    private fun normalizeName(name: String): String {
        return name.trim().lowercase()
    }
    
    /**
     * Determine document type from file name
     */
    private fun getDocumentTypeFromFileName(fileName: String): String {
        return when {
            fileName.endsWith(".pdf", ignoreCase = true) -> "PDF"
            fileName.endsWith(".jpg", ignoreCase = true) || 
            fileName.endsWith(".jpeg", ignoreCase = true) || 
            fileName.endsWith(".png", ignoreCase = true) -> "Image"
            fileName.endsWith(".doc", ignoreCase = true) || 
            fileName.endsWith(".docx", ignoreCase = true) -> "Document"
            else -> "Other"
        }
    }
    
    /**
     * Get mime type from file name
     */
    private fun getMimeTypeFromFileName(fileName: String): String {
        return when {
            fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            fileName.endsWith(".jpg", ignoreCase = true) || 
            fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
            fileName.endsWith(".png", ignoreCase = true) -> "image/png"
            fileName.endsWith(".doc", ignoreCase = true) -> "application/msword"
            fileName.endsWith(".docx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            else -> "application/octet-stream"
        }
    }

    suspend fun addPropertiesToTenant(id: Long, propertyIds: List<String>, floors: List<String>) = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("TenantViewModel", "Adding ${propertyIds.size} properties to tenant $id using junction table")
            
            // Perform all database operations in background
            propertyIds.forEachIndexed { index, propertyId ->
                val floor = floors.getOrNull(index)
                tenantRepository.addPropertyToTenantNew(id, propertyId, floor, null, "0.0")
            }
            
            android.util.Log.d("TenantViewModel", "Added ${propertyIds.size} properties to tenant $id using junction table")
            
            // Invalidate cache to ensure fresh data
            tenantRepository.clearCache()
            
            // Single UI update on main thread
            withContext(Dispatchers.Main) {
                refreshTenantData()
            }
        } catch (e: Exception) {
            android.util.Log.e("TenantViewModel", "Error adding properties to tenant: ${e.message}", e)
        }
    }


}

/**
 * Factory class for creating TenantViewModel instances with the required repository dependencies.
 * This is necessary for ViewModel instances that take parameters in their constructor.
 */
class TenantViewModelFactory(
    private val tenantRepository: TenantRepository,
    private val propertyRepository: PropertyRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TenantViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TenantViewModel(tenantRepository, propertyRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
