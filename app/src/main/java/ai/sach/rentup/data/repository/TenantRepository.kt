package ai.sach.rentup.data.repository

import ai.sach.rentup.data.firestore.model.TenantSubmission
import ai.sach.rentup.data.firestore.repository.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import ai.sach.rentup.data.local.dao.PaymentHistoryDao
import ai.sach.rentup.data.local.dao.TenantDao
import ai.sach.rentup.data.local.RentUpDatabase
import ai.sach.rentup.data.local.dao.TenantDocumentDao
import ai.sach.rentup.data.local.dao.TenantFamilyInfoDao
import ai.sach.rentup.data.local.dao.TenantPropertyDao
import ai.sach.rentup.data.local.entity.EmergencyContact
import ai.sach.rentup.data.local.entity.PaymentHistory
import ai.sach.rentup.data.local.entity.Tenant
import ai.sach.rentup.data.local.entity.TenantDocument
import ai.sach.rentup.data.local.entity.TenantFamilyInfo
import ai.sach.rentup.data.local.entity.TenantPropertyCrossRef
import ai.sach.rentup.data.local.entity.TenantWithFullProperty
import ai.sach.rentup.data.local.entity.TenantWithMultipleProperties
import ai.sach.rentup.utils.InputValidator
import ai.sach.rentup.ui.screens.homescreen.documents.downloadFileToLocalStorage
import ai.sach.rentup.ui.screens.settingsScreen.notifications.NotificationHelper
import ai.sach.rentup.ui.screens.settingsScreen.notifications.NotificationPreferencesManager
import ai.sach.rentup.ui.screens.settingsScreen.notifications.ReminderScheduler
import android.content.Context
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for tenant operations.
 * Handles CRUD operations for tenants and related entities.
 */
@Singleton
class TenantRepository @Inject constructor(
    private val database: RentUpDatabase,
    private val tenantDao: TenantDao,
    private val tenantFamilyInfoDao: TenantFamilyInfoDao,
    private val tenantDocumentDao: TenantDocumentDao,
    private val paymentHistoryDao: PaymentHistoryDao,
    private val tenantPropertyDao: TenantPropertyDao,
    private val propertyRepository: PropertyRepository? = null,
    private val firestoreRepository: FirestoreRepository? = null,
    private val notificationHelper: NotificationHelper,
    private val notificationPreferences: NotificationPreferencesManager,
    private val reminderScheduler: ReminderScheduler,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "TenantRepository"
    }

    // Get all tenants
    val allTenants: Flow<List<Tenant>> = tenantDao.getAllTenants()

    // Cache for tenant-property data to avoid repeated queries
    private var cachedTenantsWithMultipleProperties: List<TenantWithMultipleProperties>? = null
    private var lastCacheTime: Long = 0
    private val cacheValidityDuration = 300_000L // 5 minutes cache (increased from 30 seconds)
    
    // Thread-safe cache management
    private val cacheLock = kotlinx.coroutines.sync.Mutex()
    
    // Pre-compiled regex patterns to avoid repeated compilation
    private val phoneRegex = "[^0-9]".toRegex()
    private val spaceRegex = " ".toRegex()
    
    // Expose tenants with property information using pagination
    val allTenantsWithProperty: Flow<List<TenantWithMultipleProperties>> = flow {
        try {
            // Thread-safe cache check
            cacheLock.withLock {
                val currentTime = System.currentTimeMillis()
                if (cachedTenantsWithMultipleProperties != null && (currentTime - lastCacheTime) < cacheValidityDuration) {
                    emit(cachedTenantsWithMultipleProperties!!)
                    return@flow
                }
            }

            // Only log when actually loading from database
            val isFirstLoad = cachedTenantsWithMultipleProperties == null
            if (isFirstLoad) {
                Log.d(TAG, "Loading tenants with multiple properties from junction table using pagination")
            }
            
            // Use IO dispatcher for all database operations
            val result = withContext(Dispatchers.IO) {
                try {
                    // Load tenants in batches
                    val allTenantsWithProperties = mutableListOf<TenantWithMultipleProperties>()
                    var offset = 0
                    val batchSize = 10
                    
                    while (true) {
                        // Get a batch of tenants with their properties
                        val batch = kotlinx.coroutines.withTimeoutOrNull(5000) { // 5 second timeout per batch
                            tenantPropertyDao.getTenantsWithMultiplePropertiesBatch(offset, batchSize).first()
                        }
                        
                        if (batch == null) {
                            Log.w(TAG, "Tenant batch query timed out at offset $offset")
                            break
                        }
                        
                        if (batch.isEmpty()) {
                            break // No more data
                        }
                        
                        allTenantsWithProperties.addAll(batch)
                        offset += batchSize
                        
                        // Only log on first load
                        if (isFirstLoad) {
                            Log.d(TAG, "Loaded batch: ${batch.size} tenants, total so far: ${allTenantsWithProperties.size}")
                        }
                    }
                    
                    // Only log counts on first load
                    if (isFirstLoad) {
                        Log.d(TAG, "Found ${allTenantsWithProperties.size} tenants with multiple properties in total")
                    }
                    
                    allTenantsWithProperties
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading tenant data: ${e.message}", e)
                    // Return empty list on error to prevent crashes
                    emptyList()
                }
            }
            
            // Thread-safe cache update and emit
            cacheLock.withLock {
                cachedTenantsWithMultipleProperties = result
                lastCacheTime = System.currentTimeMillis()
                
                // Only log final size on first load or when size changes significantly
                if (isFirstLoad || kotlin.math.abs(result.size - (cachedTenantsWithMultipleProperties?.size ?: 0)) > 1) {
                    Log.d(TAG, "Final tenant list size: ${result.size}")
                }
            }
            
            emit(result)
            
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            // Only log real errors, not expected cancellations
            Log.e(TAG, "Critical error in allTenantsWithProperty: ${e.message}", e)
            // Emit empty list to prevent app crashes for other exceptions
            emit(emptyList())
        }
    }

    // Method to invalidate cache when data changes - thread-safe
    private suspend fun invalidateCache() {
        cacheLock.withLock {
            cachedTenantsWithMultipleProperties = null
            lastCacheTime = 0
        }
    }

    /**
     * Makes sure the daily reminder sweep is armed (or stops it when reminders are off).
     *
     * This deliberately schedules nothing per tenant and sends nothing: deciding what is
     * owed is [ai.sach.rentup.ui.screens.settingsScreen.notifications.DailyReminderWorker]'s
     * job, once a day, against live rows. Data writes only need to guarantee the sweep
     * exists, so this is cheap and idempotent no matter how often it is called.
     */
    suspend fun refreshRentReminders() {
        if (notificationPreferences.areRentRemindersEnabled()) {
            reminderScheduler.ensureScheduled()
        } else {
            reminderScheduler.cancel()
        }
    }
    


    // Public method to clear cache - thread-safe
    suspend fun clearCache() {
        invalidateCache()
        Log.d(TAG, "Cache cleared")
    }
    
    // Synchronous version for non-suspend contexts
    fun clearCacheSync() {
        cachedTenantsWithMultipleProperties = null
        lastCacheTime = 0
        Log.d(TAG, "Cache cleared synchronously")
    }

    // ========== DATA MIGRATION UTILITIES ==========
    
    /**
     * Migrate tenant-property relationships using optimized batch processing
     * to prevent memory issues and GC blocking
     */
    suspend fun migrateTenantPropertyRelationships() {
        try {
            Log.d(TAG, "Starting optimized tenant-property relationship migration")
            
            withContext(Dispatchers.IO) {
                // Process in smaller batches
                val batchSize = 8
                var offset = 0
                var totalProcessed = 0
                var totalRelationshipsCreated = 0
                
                while (true) {
                    // Get tenants in batches
                    val tenantBatch = tenantDao.getTenantsBatch(offset, batchSize)
                    if (tenantBatch.isEmpty()) {
                        break // No more tenants to process
                    }
                    
                    Log.d(TAG, "Processing batch ${offset / batchSize + 1}: ${tenantBatch.size} tenants")
                    
                    // Get all properties once (they're usually smaller in number)
                    val allProperties = propertyRepository?.getAllPropertiesSync() ?: emptyList()
                    
                    val relationshipsToCreate = mutableListOf<TenantPropertyCrossRef>()
                    
                    tenantBatch.forEach { tenant ->
                        try {
                            // Check if tenant already has relationships
                            val existingRelationships = tenantPropertyDao.getTenantPropertyInfo().first()
                            val hasRelationships = existingRelationships.any { it.tenantId == tenant.id }
                            
                            if (!hasRelationships) {
                                // Try to establish relationship from existing propertyId field
                                if (tenant.propertyId.isNotBlank()) {
                                    val propertyIds = tenant.propertyId.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                    
                                    propertyIds.forEach { propertyId ->
                                        // Check if property exists
                                        val propertyExists = allProperties.any { it.id == propertyId }
                                        
                                        if (propertyExists) {
                                            relationshipsToCreate.add(
                                                TenantPropertyCrossRef(
                                                    tenantId = tenant.id,
                                                    propertyId = propertyId,
                                                    floor = tenant.floor,
                                                    rentAmount = tenant.amount,
                                                    isActive = true
                                                )
                                            )
                                        } else {
                                            Log.w(TAG, "Property $propertyId not found for tenant ${tenant.id}")
                                        }
                                    }
                                } else {
                                    Log.d(TAG, "Tenant ${tenant.id} has no propertyId, skipping relationship creation")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing tenant ${tenant.id}: ${e.message}")
                        }
                    }
                    
                    // Insert relationships in smaller batches
                    if (relationshipsToCreate.isNotEmpty()) {
                        relationshipsToCreate.chunked(10).forEach { batch ->
                            try {
                                tenantPropertyDao.insertTenantPropertyRelations(batch)
                                totalRelationshipsCreated += batch.size
                                Log.d(TAG, "Inserted ${batch.size} tenant-property relationships")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error inserting relationship batch: ${e.message}")
                            }
                        }
                    }
                    
                    totalProcessed += tenantBatch.size
                    offset += batchSize
                }
                
                // Clear cache to reflect new relationships
                invalidateCache()
                
                Log.d(TAG, "Migration completed: $totalProcessed tenants processed, $totalRelationshipsCreated relationships created")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during tenant-property relationship migration: ${e.message}", e)
        }
    }
    
    /**
     * Validate tenant-property relationships with optimized batch processing
     * to prevent memory issues and GC blocking
     */
    suspend fun validateTenantPropertyRelationships(): Map<String, Any> {
        return withContext(Dispatchers.IO) {
            try {
                val batchSize = 15
                var offset = 0
                var totalTenants = 0
                var tenantsWithoutRelationships = 0
                val orphanedRelationships = mutableListOf<Long>()
                
                // Get all properties once (usually smaller dataset)
                val allProperties = propertyRepository?.getAllPropertiesSync() ?: emptyList()
                val allPropertyIds = allProperties.map { it.id }.toSet()
                
                // Get all relationships once
                val relationships = tenantPropertyDao.getTenantPropertyInfo().first()
                val tenantIdsWithRelationships = relationships.map { it.tenantId }.toSet()
                
                // Process tenants in batches
                while (true) {
                    val tenantBatch = tenantDao.getTenantsBatch(offset, batchSize)
                    if (tenantBatch.isEmpty()) {
                        break
                    }
                    
                    totalTenants += tenantBatch.size
                    
                    // Check for tenants without relationships
                    tenantBatch.forEach { tenant ->
                        if (!tenantIdsWithRelationships.contains(tenant.id)) {
                            tenantsWithoutRelationships++
                        }
                    }
                    
                    offset += batchSize
                }
                
                // Check for orphaned relationships
                relationships.forEach { relationship ->
                    if (!allPropertyIds.contains(relationship.propertyId)) {
                        orphanedRelationships.add(relationship.tenantId)
                    }
                }
                
                val validationResults = mutableMapOf<String, Any>()
                validationResults["totalTenants"] = totalTenants
                validationResults["totalProperties"] = allProperties.size
                validationResults["totalRelationships"] = relationships.size
                validationResults["tenantsWithoutRelationships"] = tenantsWithoutRelationships
                validationResults["orphanedRelationships"] = orphanedRelationships.size
                
                Log.d(TAG, "Validation completed: $totalTenants tenants, ${allProperties.size} properties, ${relationships.size} relationships")
                Log.d(TAG, "Issues found: $tenantsWithoutRelationships tenants without relationships, ${orphanedRelationships.size} orphaned relationships")
                
                validationResults
            } catch (e: Exception) {
                Log.e(TAG, "Error during validation: ${e.message}", e)
                mapOf("error" to (e.message ?: "Unknown error"))
            }
        }
    }
    
    /**
     * Clean up orphaned relationships
     */
    suspend fun cleanupOrphanedRelationships() {
        try {
            Log.d(TAG, "Starting orphaned relationship cleanup")
            
            withContext(Dispatchers.IO) {
                val allTenants = tenantDao.getAllTenantsSync()
                val allProperties = propertyRepository?.getAllPropertiesSync() ?: emptyList()
                val relationships = tenantPropertyDao.getTenantPropertyInfo().first()
                
                val orphanedRelationships = relationships.filter { relationship ->
                    !allTenants.any { it.id == relationship.tenantId } ||
                    !allProperties.any { it.id == relationship.propertyId }
                }
                
                if (orphanedRelationships.isNotEmpty()) {
                    Log.d(TAG, "Found ${orphanedRelationships.size} orphaned relationships to clean up")
                    
                    orphanedRelationships.forEach { relationship ->
                        tenantPropertyDao.deleteTenantPropertyRelation(
                            TenantPropertyCrossRef(
                                tenantId = relationship.tenantId,
                                propertyId = relationship.propertyId
                            )
                        )
                    }
                    
                    // Clear cache
                    invalidateCache()
                    
                    Log.d(TAG, "Cleanup completed: ${orphanedRelationships.size} orphaned relationships removed")
                } else {
                    Log.d(TAG, "No orphaned relationships found")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during orphaned relationship cleanup: ${e.message}", e)
        }
    }

    // Get all tenants with full property details
    val allTenantsWithFullProperty: Flow<List<TenantWithFullProperty>> = tenantDao.getTenantsWithFullProperty()

    // --- Normalization Utilities ---
    private fun normalizePhone(phone: String): String {
        return phone.replace(phoneRegex, "")
    }
    private fun normalizeName(name: String): String {
        return name.trim().lowercase()
    }

    /**
     * Process tenant form submissions from Firestore and convert to local entities
     * Enhanced with better error handling and performance optimizations
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun processTenantFormSubmissions(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Skip if firestoreRepository is null (dependency injection fallback)
            if (firestoreRepository == null) {
                Log.w(TAG, "Firestore repository is null, skipping form submission processing")
                return@withContext Result.success(0)
            }
            
            // Check if user is authenticated (not guest) before attempting Firestore operations
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            if (currentUser == null || currentUser.isAnonymous) {
                Log.w(TAG, "User is not authenticated or is a guest user, skipping tenant form submission processing")
                return@withContext Result.success(0)
            }
            
            // Network connectivity will be checked at the application level
            // Repository should focus on data operations, not network checks
            
            // Fetch all tenant form submissions
            val submissionsResult = firestoreRepository.getAllTenantFormSubmissions()
            
            if (submissionsResult.isFailure) {
                Log.e(TAG, "Failed to fetch tenant form submissions", submissionsResult.exceptionOrNull())
                return@withContext Result.failure(
                    submissionsResult.exceptionOrNull() ?: Exception("Unknown error fetching submissions")
                )
            }
            
            val rawSubmissions = submissionsResult.getOrNull() ?: emptyList()

            // Imported submissions stay in Firestore forever. Without this filter
            // every sync re-imported every tenant the owner ever onboarded.
            val submissions = rawSubmissions
                .map { TenantSubmission.from(it) }
                .filterNot { it.processed }

            Log.d(TAG, "Processing ${submissions.size} of ${rawSubmissions.size} tenant form submissions")

            // Count of successfully processed submissions
            var processedCount = 0

            // Imported this pass, new tenant or update alike: both mean someone
            // filled in the form, so both are announced.
            val imported = mutableListOf<TenantSubmission>()

            // Process submissions in smaller batches
            val chunkSize = 3
            submissions.chunked(chunkSize).forEach { submissionBatch ->
                submissionBatch.forEach { submission ->
                    try {
                        val tenantIdStr = submission.tenantId
                        val phone = submission.phone
                        var matchedTenant: Tenant? = null
                        var tenantId: Long = 0L

                        // Search for existing tenant in batches to prevent memory issues
                        if (!tenantIdStr.isNullOrBlank()) {
                            // Try to find by tenantId first
                            tenantId = tenantIdStr.toLongOrNull() ?: 0L
                            if (tenantId > 0) {
                                matchedTenant = tenantDao.getTenantById(tenantId)
                            }
                        }

                        // If not found by tenantId, search by phone in batches
                        if (matchedTenant == null && phone.isNotBlank()) {
                            var searchOffset = 0
                            val searchBatchSize = 20
                            while (true) {
                                val searchBatch = tenantDao.getTenantsBatch(searchOffset, searchBatchSize)
                                if (searchBatch.isEmpty()) break

                                matchedTenant = searchBatch.find {
                                    normalizePhone(it.mobileNumber) == normalizePhone(phone)
                                }

                                if (matchedTenant != null) {
                                    Log.w(TAG, "Fallback match by phone for tenant phone: $phone. Using existing tenantId=${matchedTenant.id}")
                                    tenantId = matchedTenant.id
                                    break
                                }

                                searchOffset += searchBatchSize
                                kotlinx.coroutines.delay(5) // Small delay between search batches
                            }
                        }

                        // Process the submission based on whether we found a matching tenant
                        if (matchedTenant != null) {
                            // Update existing tenant
                            Log.d(TAG, "Updating existing tenant: ${matchedTenant.id}")
                            val updatedTenant = updateExistingTenantFromSubmission(matchedTenant, submission)
                            updateTenant(updatedTenant)
                            // Always process family members and documents for existing tenants
                            processFamilyMembers(
                                matchedTenant.id,
                                submission.familyMembers,
                                submission.emergencyContacts
                            )
                            processDocuments(matchedTenant.id, submission.files)
                            processedCount++ // Count updates as successes
                            imported += submission
                            Log.d(TAG, "Updated tenant in DB: ${matchedTenant.id}")
                        } else {
                            // Insert new tenant
                            val newTenant = convertSubmissionToTenant(submission)
                            val newTenantId = insertTenant(newTenant)

                            // Process property assignment using junction table approach
                            val propertyAddress = submission.propertyAddress
                            if (propertyAddress.isNotBlank()) {
                                try {
                                    val propertyId = findPropertyIdByAddress(propertyAddress)
                                    if (propertyId != null) {
                                        // Add property to tenant using junction table
                                        addPropertyToTenant(
                                            tenantId = newTenantId,
                                            propertyId = propertyId,
                                            floor = submission.floor,
                                            unitNumber = submission.unitNumber,
                                            rentAmount = submission.rentAmount
                                        )
                                        Log.d(TAG, "Added property $propertyId to tenant $newTenantId via junction table")
                                    } else {
                                        Log.w(TAG, "Could not find property for address: $propertyAddress")
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error processing property assignment for tenant $newTenantId", e)
                                }
                            }

                            // Process family members and documents for new tenant
                            processFamilyMembers(
                                newTenantId,
                                submission.familyMembers,
                                submission.emergencyContacts
                            )
                            processDocuments(newTenantId, submission.files)
                            processedCount++
                            imported += submission
                            Log.d(TAG, "Inserted new tenant: $newTenantId")
                        }

                        // Mark form as processed
                        try {
                            val formId = submission.formId
                            if (formId != null) {
                                firestoreRepository.markTenantFormAsProcessed(
                                    formId,
                                    mapOf(
                                        "processed" to true,
                                        "processedAt" to System.currentTimeMillis(),
                                        "processedBy" to "android_app"
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error marking form as processed", e)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing submission: ${e.message}", e)
                    }
                }
            }

            notifyNewSubmissions(imported)

            Log.d(TAG, "Completed processing tenant submissions. Success: $processedCount, Total: ${submissions.size}")
            Result.success(processedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error in processTenantFormSubmissions", e)
            Result.failure(e)
        }
    }
    
    /**
     * Downloads a tenant's photo into app storage while its R2 signed URL is still valid
     * (it expires in 7 days) and returns a file:// URI TenantAvatar's Coil AsyncImage can
     * load directly. Falls back to the remote URL on failure, so the avatar still loads
     * for as long as that link happens to remain valid.
     */
    private suspend fun downloadAvatarLocally(avatarUrl: String?, tenantName: String): String? {
        if (avatarUrl.isNullOrBlank()) return null
        return try {
            val safeName = "avatar_${tenantName}_${System.currentTimeMillis()}"
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
            val file = downloadFileToLocalStorage(context, avatarUrl, safeName)
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            Log.w(TAG, "Could not download avatar locally for $tenantName, keeping remote URL", e)
            avatarUrl
        }
    }

    /**
     * Converts a normalised submission to a Tenant entity.
     */
    private suspend fun convertSubmissionToTenant(submission: TenantSubmission): Tenant {
        Log.d(TAG, "Tenant avatar URL: ${submission.avatarUrl}")

        return Tenant(
            id = 0, // Room will auto-generate ID
            name = submission.name,
            status = submission.status,
            isPaid = false, // Default payment status
            mobileNumber = submission.phone,
            dueDate = "", // Can be set later
            propertyId = "", // No longer used - properties are stored in junction table
            floor = "", // No longer used - floor info is stored in junction table
            amount = null, // Can be set later
            avatarUri = downloadAvatarLocally(submission.avatarUrl, submission.name),
            permanentAddress = submission.address,
            emergencyContacts = submission.toEmergencyContacts(),
            formId = submission.formId
        )
    }

    /**
     * Tells the owner a tenant filled in their form.
     *
     * The push that carries a submission raises this too, but a submission also turns
     * up on a manual or on-resume sync - when the push was dropped, or the app was
     * offline. Either way the owner hears once: form ids already announced, by this or
     * by the messaging service, are remembered.
     */
    private fun notifyNewSubmissions(submissions: List<TenantSubmission>) {
        val unannounced = submissions.filterNot { submission ->
            submission.formId?.let { notificationPreferences.wasOnboardingNotificationPosted(it) } == true
        }
        if (unannounced.isEmpty()) return

        try {
            if (notificationPreferences.areTenantUpdatesEnabled()) {
                notificationHelper.showNewTenantNotification(
                    count = unannounced.size,
                    // Only a single submission can point the tap at one form.
                    formId = unannounced.singleOrNull()?.formId
                )
            }
            // Record the announcement either way, so an opt-out doesn't queue a stale
            // burst the day the toggle is flipped back on.
            unannounced.forEach { it.formId?.let(notificationPreferences::markOnboardingNotificationPosted) }
            Log.d(TAG, "Processed ${unannounced.size} new tenant submission(s)")
        } catch (e: Exception) {
            Log.e(TAG, "Could not post new tenant notification", e)
        }
    }

    /**
     * Find property ID by address or return null if not found (non-suspend version)
     */
    private fun findPropertyIdByAddressSync(address: String): String? {
        if (address.isBlank()) return null
        
        try {
            // Since we can't use suspend functions here, use a simpler approach 
            // that doesn't require database access
            return "unknown_property"  // Default for now, will be updated later
        } catch (e: Exception) {
            Log.e(TAG, "Error finding property by address: $address", e)
            return null
        }
    }
    
    /**
     * Find property ID by address or return null if not found (suspend version)
     */
    private suspend fun findPropertyIdByAddress(address: String): String? {
        if (address.isBlank()) return null
        
        try {
            // Get properties in batches
            var offset = 0
            val batchSize = 25
            while (true) {
                val properties = propertyRepository?.getAllPropertiesSync()?.drop(offset)?.take(batchSize) ?: emptyList()
                if (properties.isEmpty()) break
                
                // Find property with matching address (case insensitive, ignoring spaces)
                val matchingProperty = properties.find { property ->
                    val normalizedPropertyAddress = property.address.replace(spaceRegex, "").lowercase()
                    val normalizedAddress = address.replace(spaceRegex, "").lowercase()
                    normalizedPropertyAddress.contains(normalizedAddress) || normalizedAddress.contains(normalizedPropertyAddress)
                }
                
                if (matchingProperty != null) {
                    return matchingProperty.id
                }
                
                offset += batchSize
            }
            
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error finding property by address: $address", e)
            return null
        }
    }

    /**
     * Fills in an existing tenant from a submission, keeping what the submission does
     * not carry. The property assignment is not touched - that lives in the junction
     * table.
     */
    private suspend fun updateExistingTenantFromSubmission(
        existingTenant: Tenant,
        submission: TenantSubmission
    ): Tenant {
        submission.avatarUrl?.let { Log.d(TAG, "Updating tenant with avatar URL: $it") }

        val emergencyContacts = submission.toEmergencyContacts()
            .ifEmpty { existingTenant.emergencyContacts }

        val avatarUri = submission.avatarUrl
            ?.let { downloadAvatarLocally(it, submission.name) }
            ?: existingTenant.avatarUri

        return existingTenant.copy(
            mobileNumber = submission.phone.ifBlank { existingTenant.mobileNumber },
            permanentAddress = submission.address.ifBlank { existingTenant.permanentAddress },
            avatarUri = avatarUri,
            emergencyContacts = emergencyContacts,
            formId = submission.formId ?: existingTenant.formId
        )
    }

    /**
     * Maps a submission's emergency contacts onto the entity the tenant row embeds.
     */
    private fun TenantSubmission.toEmergencyContacts(): List<EmergencyContact> =
        emergencyContacts.mapNotNull { contact ->
            val contactName = contact["name"] as? String ?: return@mapNotNull null
            EmergencyContact(
                name = contactName,
                phone = contact["phone"] as? String ?: "",
                relationship = contact["relationship"] as? String ?: "",
                address = contact["address"] as? String ?: ""
            )
        }

    /**
     * Process family members from submission
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun processFamilyMembers(tenantId: Long, familyMembers: List<Map<String, Any>>, emergencyContacts: List<Map<String, Any>>? = null) {
        val allMembers = mutableListOf<Map<String, Any>>()
        allMembers.addAll(familyMembers)
        emergencyContacts?.forEach { ec ->
            val ecMap = ec.toMutableMap()
            ecMap["isEmergencyContact"] = true
            allMembers.add(ecMap)
        }
        if (allMembers.isNotEmpty()) {
            // Clear existing family members to avoid duplicates
            val existingFamilyMembers = tenantFamilyInfoDao.getFamilyInfoForTenantSync(tenantId)
            existingFamilyMembers.forEach { member ->
                tenantFamilyInfoDao.delete(member)
            }
            allMembers.forEach { member ->
                val name = member["name"] as? String ?: member["memberName"] as? String ?: ""
                val relationship = member["relation"] as? String ?: member["relationship"] as? String ?: member["relationshipType"] as? String ?: ""
                val age = member["age"] as? String ?: member["memberAge"] as? String ?: ""
                val mobileNumber = member["phone"] as? String ?: member["mobileNumber"] as? String
                val isEmergencyContact = member["isEmergencyContact"] as? Boolean ?: false
                val address = member["address"] as? String
                val propertyId = member["propertyId"] as? String
                val familyInfo = TenantFamilyInfo(
                    id = 0,
                    tenantId = tenantId,
                    name = name,
                    relationship = relationship,
                    age = age.toIntOrNull() ?: 0,
                    isEmergencyContact = isEmergencyContact,
                    mobileNumber = mobileNumber,
                    address = address,
                    propertyId = propertyId
                )
                if (name.isNotBlank()) {
                    tenantFamilyInfoDao.insert(familyInfo)
                    Log.d(TAG, "Added family member $name for tenant $tenantId (isEmergencyContact=$isEmergencyContact)")
                }
            }
        }
        // If allMembers is empty, do nothing (keep existing)
    }

    /**
     * Process documents from form submission
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun processDocuments(tenantId: Long, documents: List<Map<String, Any>>) {
        if (documents.isNotEmpty()) {
            val existingDocuments = tenantDocumentDao.getDocumentsForTenantSync(tenantId)
            // Process files array based on the exact structure provided
            documents.forEach { document ->
                try {
                    // Log the document for debugging
                    Log.d(TAG, "Processing document: $document")
                    val fileName = document["originalName"] as? String ?: ""
                    val url = document["url"] as? String ?: ""
                    val docType = document["type"] as? String ?: getDocumentType(fileName)
                    val mimeType = getMimeType(fileName)
                    val exists = existingDocuments.any {
                        it.documentPath == url ||
                        (it.description == fileName && fileName.isNotBlank())
                    }
                    if (!exists && url.isNotBlank()) {
                        // The URL is an R2 signed link that expires in 7 days - download it
                        // into app storage now, while it's still valid, rather than leaving
                        // this record pointing at a link that will start failing on its own.
                        // downloadAndOpenDocument (DocumentsTab) already knows how to open a
                        // local path directly, so falling back to the remote URL on failure
                        // just means the tenant-manual retry path there still applies.
                        val storedPath = try {
                            val safeName = fileName.ifBlank { url.substringAfterLast('/') }
                            downloadFileToLocalStorage(context, url, safeName).absolutePath
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not download document $fileName locally for tenant $tenantId, keeping remote URL", e)
                            url
                        }
                        val tenantDocument = TenantDocument(
                            id = 0,
                            tenantId = tenantId,
                            documentType = docType,
                            uploadDate = System.currentTimeMillis().toString(),
                            documentPath = storedPath,
                            description = fileName,
                            mimeType = mimeType
                        )
                        tenantDocumentDao.insert(tenantDocument)
                        Log.d(TAG, "Added document $fileName ($storedPath) for tenant $tenantId")
                    } else if (exists) {
                        Log.d(TAG, "Skipped duplicate document $fileName for tenant $tenantId")
                    } else if (url.isBlank()) {
                        Log.w(TAG, "Skipped document $fileName with empty URL for tenant $tenantId")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing document for tenant $tenantId", e)
                    // Continue with other documents despite errors
                }
            }
        }
        // If documents is empty, do nothing (keep existing)
    }
    
    /**
     * Determine document type from file name
     */
    private fun getDocumentType(fileName: String): String {
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
    private fun getMimeType(fileName: String): String {
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
    
    /**
     * Mark a submission as processed in Firestore
     */
    private suspend fun markSubmissionAsProcessed(submissionId: String) {
        if (submissionId.isBlank() || firestoreRepository == null) return
        
        try {
            val updateData = mapOf(
                "processed" to true,
                "processedAt" to System.currentTimeMillis()
            )
            firestoreRepository.markTenantFormAsProcessed(submissionId, updateData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark submission $submissionId as processed", e)
        }
    }

    /**
     * Insert a tenant and create tenant-property relationships
     */
    suspend fun insertTenant(tenant: Tenant): Long = withContext(Dispatchers.IO) {
        try {
            // Insert the tenant first
            val tenantId = database.withTransaction {
                tenantDao.insert(tenant).also {
                    createTenantPropertyRelationships(it, tenant.propertyId, tenant.floor, tenant.amount)
                }
            }
            
            // Invalidate cache to ensure fresh data
            invalidateCache()
            refreshRentReminders()
            
            Log.d(TAG, "Tenant inserted successfully with ID: $tenantId")
            tenantId
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting tenant", e)
            throw e
        }
    }

    /**
     * Update a tenant and maintain tenant-property relationships
     */
    suspend fun updateTenant(tenant: Tenant) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Updating tenant: ${tenant.id} with isPaid=${tenant.isPaid}, status=${tenant.status}")
            
            val previousTenant = tenantDao.getTenantById(tenant.id)
            database.withTransaction {
                tenantDao.update(tenant)
                updateTenantPropertyRelationships(tenant.id, tenant.propertyId, tenant.floor, tenant.amount)
            }
            
            // Invalidate cache to ensure fresh data
            invalidateCache()
            refreshRentReminders()
            if (previousTenant?.isPaid == false && tenant.isPaid && notificationPreferences.arePaymentConfirmationsEnabled()) {
                notificationHelper.sendPaymentConfirmationNotification(tenant, tenant.amount.orEmpty())
            }
            
            Log.d(TAG, "Tenant updated successfully: ${tenant.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating tenant", e)
            throw e
        }
    }

    /**
     * Create tenant-property relationships in the junction table
     */
    private suspend fun createTenantPropertyRelationships(
        tenantId: Long,
        propertyIds: String,
        floors: String,
        amount: String?
    ) {
        try {
            val propertyIdList = propertyIds.split(",").filter { it.isNotBlank() }
            val floorList = floors.split(",").filter { it.isNotBlank() }
            
            // Validate that all properties exist before creating relationships
            val validCrossRefs = mutableListOf<TenantPropertyCrossRef>()
            
            for ((index, propertyId) in propertyIdList.withIndex()) {
                val trimmedPropertyId = propertyId.trim()
                
                // Check if the property exists in the database
                val propertyExists = try {
                    val property = propertyRepository?.getPropertyById(trimmedPropertyId)
                    property != null
                } catch (e: Exception) {
                    Log.w(TAG, "Error checking property existence for ID: $trimmedPropertyId", e)
                    false
                }
                
                if (propertyExists) {
                    val floor = if (index < floorList.size) floorList[index] else ""
                    val crossRef = TenantPropertyCrossRef(
                        tenantId = tenantId,
                        propertyId = trimmedPropertyId,
                        floor = floor.trim().takeIf { it.isNotBlank() },
                        rentAmount = amount,
                        isActive = true
                    )
                    validCrossRefs.add(crossRef)
                    Log.d(TAG, "Valid property found for tenant $tenantId: $trimmedPropertyId")
                } else {
                    Log.w(TAG, "Property not found in database, attempting to create placeholder: $trimmedPropertyId")
                    
                    // Try to create a placeholder property if PropertyRepository is available
                    val placeholderCreated = try {
                        propertyRepository?.let { repo ->
                            val placeholderProperty = ai.sach.rentup.data.local.entity.Property(
                                id = trimmedPropertyId,
                                name = "Property $trimmedPropertyId",
                                address = "Address not available",
                                totalUnits = 1,
                                floors = 1
                            )
                            repo.insertProperty(placeholderProperty)
                            Log.d(TAG, "Created placeholder property: $trimmedPropertyId")
                            true
                        } ?: false
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to create placeholder property: $trimmedPropertyId", e)
                        false
                    }
                    
                    if (placeholderCreated) {
                        // Now try to create the relationship
                        val floor = if (index < floorList.size) floorList[index] else ""
                        val crossRef = TenantPropertyCrossRef(
                            tenantId = tenantId,
                            propertyId = trimmedPropertyId,
                            floor = floor.trim().takeIf { it.isNotBlank() },
                            rentAmount = amount,
                            isActive = true
                        )
                        validCrossRefs.add(crossRef)
                        Log.d(TAG, "Created relationship with placeholder property for tenant $tenantId: $trimmedPropertyId")
                    } else {
                        Log.w(TAG, "Skipping relationship for missing property: $trimmedPropertyId")
                    }
                }
            }
            
            if (validCrossRefs.isNotEmpty()) {
                try {
                    tenantPropertyDao.insertTenantPropertyRelations(validCrossRefs)
                    Log.d(TAG, "Created ${validCrossRefs.size} tenant-property relationships for tenant $tenantId")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to insert tenant-property relationships for tenant $tenantId", e)
                    throw e
                }
            } else {
                Log.w(TAG, "No valid properties found for tenant $tenantId, skipping relationship creation")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating tenant-property relationships for tenant $tenantId", e)
            throw e
        }
    }

    /**
     * Update tenant-property relationships in the junction table
     */
    private suspend fun updateTenantPropertyRelationships(
        tenantId: Long,
        propertyIds: String,
        floors: String,
        amount: String?
    ) {
        try {
            // First, delete existing relationships for this tenant
            try {
                tenantPropertyDao.deleteAllRelationsForTenant(tenantId)
                Log.d(TAG, "Deleted existing relationships for tenant $tenantId")
            } catch (e: Exception) {
                Log.w(TAG, "Error deleting existing relationships for tenant $tenantId", e)
                throw e
            }
            
            // Then create new relationships
            createTenantPropertyRelationships(tenantId, propertyIds, floors, amount)
            
            Log.d(TAG, "Updated tenant-property relationships for tenant $tenantId")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating tenant-property relationships for tenant $tenantId", e)
            throw e
        }
    }

// Delete a tenant
    suspend fun deleteTenant(tenant: Tenant): Int {
        tenantDao.delete(tenant)
        invalidateCache() // Invalidate cache when data changes
        refreshRentReminders()
        return 1
    }

    // Delete multiple tenants at once (related data cascades via foreign keys)
    suspend fun deleteTenants(tenants: List<Tenant>) {
        if (tenants.isEmpty()) return
        tenantDao.deleteTenantsByIds(tenants.map { it.id })
        invalidateCache()
        refreshRentReminders()
    }

    // Get tenant by ID
    suspend fun getTenantById(id: Long): Tenant? {
        return tenantDao.getTenantById(id)
    }

    /** Resolves a "new tenant submission" notification tap back to the tenant it created. */
    suspend fun getTenantByFormId(formId: String): Tenant? {
        return tenantDao.getTenantByFormId(formId)
    }

    // Get tenant with property by tenant ID
    fun getTenantWithPropertyById(tenantId: Long): Flow<TenantWithMultipleProperties?> {
        return flow {
            allTenantsWithProperty.collect { tenants ->
                val matchingTenant = tenants.find { it.tenant.id == tenantId }
                emit(matchingTenant)
            }
        }
    }

    // Get tenant with full property details by tenant ID
    fun getTenantWithFullPropertyById(tenantId: Long): Flow<TenantWithFullProperty?> {
        // This is not directly supported by the DAO, so we need to filter the results
        return flow {
            tenantDao.getTenantsWithFullProperty().collect { tenants ->
                val matchingTenant = tenants.find { it.tenant.id == tenantId }
                emit(matchingTenant)
            }
        }
    }

    // Search tenants by name
    fun searchTenantsByName(query: String): Flow<List<TenantWithMultipleProperties>> {
        return flow {
            allTenantsWithProperty.collect { tenants ->
                val filteredTenants = tenants.filter { 
                    it.tenant.name.contains(query, ignoreCase = true) 
                }
                emit(filteredTenants)
            }
        }
    }

    // Add family info for a tenant
    suspend fun addTenantFamilyInfo(tenantFamilyInfo: TenantFamilyInfo): Long {
        return tenantFamilyInfoDao.insert(tenantFamilyInfo)
    }

    // Get family info for a tenant
    fun getFamilyInfoForTenant(tenantId: Long): Flow<List<TenantFamilyInfo>> {
        return tenantFamilyInfoDao.getFamilyInfoForTenant(tenantId)
    }

    // Delete family info
    suspend fun deleteTenantFamilyInfo(tenantFamilyInfo: TenantFamilyInfo): Int {
        tenantFamilyInfoDao.delete(tenantFamilyInfo)
        return 1
    }

    // Update family info
    suspend fun updateTenantFamilyInfo(tenantFamilyInfo: TenantFamilyInfo): Int {
        tenantFamilyInfoDao.update(tenantFamilyInfo)
        return 1
    }

    // Add document for a tenant
    suspend fun addTenantDocument(tenantDocument: TenantDocument): Long {
        return tenantDocumentDao.insert(tenantDocument)
    }

    // Get documents for a tenant
    fun getDocumentsForTenant(tenantId: Long): Flow<List<TenantDocument>> {
        return tenantDocumentDao.getDocumentsForTenant(tenantId)
    }

    // Delete document
    suspend fun deleteTenantDocument(tenantDocument: TenantDocument): Int {
        tenantDocumentDao.delete(tenantDocument)
        return 1
    }

    // Add payment history item
    suspend fun addPaymentHistoryItem(paymentHistoryItem: PaymentHistory, tenantId: Long): Long {
        return paymentHistoryDao.insert(paymentHistoryItem.copy(tenantId = tenantId))
    }

    // Get payment history for a tenant
    fun getPaymentHistoryForTenant(tenantId: Long): Flow<List<PaymentHistory>> {
        return paymentHistoryDao.getPaymentsForTenant(tenantId)
    }

    // Update payment status
    suspend fun updatePaymentStatus(tenantId: Long, isPaid: Boolean): Int {
        val tenant = tenantDao.getTenantById(tenantId)
        return tenant?.let {
            val updatedTenant = it.copy(isPaid = isPaid)
            updateTenant(updatedTenant)
            1
        } ?: 0
    }

    // ===== NEW METHODS FOR MULTIPLE PROPERTIES SUPPORT =====
    
    /**
     * Add a property to a tenant (many-to-many relationship)
     * Uses ONLY the junction table for proper normalized many-to-many relationship
     * NO MORE comma-separated values in tenant table
     */
    suspend fun addPropertyToTenant(
        tenantId: Long,
        propertyId: String,
        floor: String? = null,
        unitNumber: String? = null,
        rentAmount: String? = null
    ) = withContext(Dispatchers.IO) {
        try {
            // Check if tenant exists
            val tenant = tenantDao.getTenantById(tenantId)
            if (tenant != null) {
                // Check if relationship already exists
                val exists = tenantPropertyDao.relationshipExists(tenantId, propertyId)
                
                if (exists <= 0) {
                    // Create new relationship in junction table ONLY
                    val crossRef = TenantPropertyCrossRef(
                        tenantId = tenantId,
                        propertyId = propertyId,
                        floor = floor,
                        unitNumber = unitNumber,
                        rentAmount = rentAmount ?: tenant.amount,
                        isActive = true
                    )
                    
                    tenantPropertyDao.insertTenantPropertyRelation(crossRef)
                    Log.d(TAG, "Added property $propertyId to tenant $tenantId using normalized junction table approach")
                } else {
                    Log.d(TAG, "Property $propertyId already assigned to tenant $tenantId")
                }
            } else {
                Log.e(TAG, "Tenant $tenantId not found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding property to tenant: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Remove a property from a tenant
     * Uses ONLY the junction table for proper normalized many-to-many relationship
     * NO MORE comma-separated values in tenant table
     */
    suspend fun removePropertyFromTenant(tenantId: Long, propertyId: String) = withContext(Dispatchers.IO) {
        try {
            // Check if tenant exists
            val tenant = tenantDao.getTenantById(tenantId)
            if (tenant != null) {
                // Check if relationship exists in junction table
                val exists = tenantPropertyDao.relationshipExists(tenantId, propertyId)
                
                if (exists > 0) {
                    // Delete relationship from junction table ONLY
                    val crossRef = TenantPropertyCrossRef(
                        tenantId = tenantId,
                        propertyId = propertyId
                    )
                    tenantPropertyDao.deleteTenantPropertyRelation(crossRef)
                    Log.d(TAG, "Removed property $propertyId from tenant $tenantId using normalized junction table approach")
                } else {
                    Log.w(TAG, "Property $propertyId not found for tenant $tenantId")
                }
            } else {
                Log.e(TAG, "Tenant with ID $tenantId not found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing property from tenant: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Get all properties for a specific tenant using the junction table
     */
    fun getPropertiesForTenant(tenantId: Long): Flow<List<ai.sach.rentup.data.local.entity.Property>> {
        return tenantPropertyDao.getPropertiesForTenant(tenantId)
    }
    

    
    /**
     * Get all tenants for a specific property
     */
    fun getTenantsForProperty(propertyId: String): Flow<List<Tenant>> {
        return tenantPropertyDao.getTenantsForProperty(propertyId)
    }
    
    /**
     * Check if a tenant-property relationship exists
     */
    suspend fun relationshipExists(tenantId: Long, propertyId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val tenant = tenantDao.getTenantById(tenantId)
            return@withContext tenant?.propertyId?.split(",")?.map { it.trim() }?.contains(propertyId) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking relationship for tenant $tenantId and property $propertyId: ${e.message}", e)
            return@withContext false
        }
    }
    
    /**
     * Get count of active properties for a tenant
     */
    suspend fun getActivePropertyCountForTenant(tenantId: Long): Int = withContext(Dispatchers.IO) {
        try {
            val tenant = tenantDao.getTenantById(tenantId)
            return@withContext tenant?.propertyId?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.size ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting property count for tenant $tenantId: ${e.message}", e)
            return@withContext 0
        }
    }
    
    /**
     * Get count of active tenants for a property
     */
    suspend fun getActiveTenantCountForProperty(propertyId: String): Int = withContext(Dispatchers.IO) {
        try {
            var count = 0
            var offset = 0
            val batchSize = 50
            while (true) {
                val tenantBatch = tenantDao.getTenantsBatch(offset, batchSize)
                if (tenantBatch.isEmpty()) break
                
                count += tenantBatch.count { tenant ->
                    tenant.propertyId.split(",").map { it.trim() }.contains(propertyId)
                }
                
                offset += batchSize
                kotlinx.coroutines.delay(10) // Small delay to prevent overwhelming
            }
            return@withContext count
        } catch (e: Exception) {
            Log.e(TAG, "Error getting tenant count for property $propertyId: ${e.message}", e)
            return@withContext 0
        }
    }
    
    /**
     * Get the TenantDao for direct access (used for migration)
     */
    fun getTenantDao() = tenantDao
    
    /**
     * Get the TenantPropertyDao for direct access (used for migration)
     */
    fun getTenantPropertyDao() = tenantPropertyDao
    

    
    /**
     * Update tenant-property relationship details
     */
    suspend fun updateTenantPropertyDetails(
        tenantId: Long,
        propertyId: String,
        floor: String? = null,
        unitNumber: String? = null,
        rentAmount: String? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val tenant = tenantDao.getTenantById(tenantId)
            if (tenant != null) {
                val propertyIds = tenant.propertyId.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
                val floors = tenant.floor.split(",").map { it.trim() }.toMutableList()
                
                val propertyIndex = propertyIds.indexOf(propertyId)
                if (propertyIndex != -1) {
                    // Update floor if provided
                    if (floor != null) {
                        // Ensure floors list is large enough
                        while (floors.size <= propertyIndex) {
                            floors.add("")
                        }
                        floors[propertyIndex] = floor
                    }
                    
                    // Update rent amount if provided
                    val updatedTenant = tenant.copy(
                        floor = floors.joinToString(","),
                        amount = rentAmount ?: tenant.amount
                    )
                    
                    tenantDao.update(updatedTenant)
                    Log.d(TAG, "Updated details for tenant $tenantId, property $propertyId")
                } else {
                    Log.w(TAG, "Property $propertyId not found for tenant $tenantId")
                }
            } else {
                Log.e(TAG, "Tenant with ID $tenantId not found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating tenant-property details: ${e.message}", e)
            throw e
        }
    }

    // ===== JUNCTION TABLE METHODS (REPLACING COMMA-SEPARATED APPROACH) =====
    
    /**
     * Add a property to a tenant using the junction table (NEW APPROACH)
     */
    suspend fun addPropertyToTenantNew(
        tenantId: Long,
        propertyId: String,
        floor: String? = null,
        unitNumber: String? = null,
        rentAmount: String? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val crossRef = TenantPropertyCrossRef(
                tenantId = tenantId,
                propertyId = propertyId,
                floor = floor,
                unitNumber = unitNumber,
                rentAmount = rentAmount,
                isActive = true
            )
            
            tenantPropertyDao.insertTenantPropertyRelation(crossRef)
            
            // Invalidate cache to ensure fresh data
            invalidateCache()
            
            Log.d(TAG, "Added property $propertyId to tenant $tenantId using junction table")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding property to tenant using junction table: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Remove a property from a tenant using the junction table (NEW APPROACH)
     */
    suspend fun removePropertyFromTenantNew(tenantId: Long, propertyId: String) = withContext(Dispatchers.IO) {
        try {
            val crossRef = TenantPropertyCrossRef(
                tenantId = tenantId,
                propertyId = propertyId,
                isActive = false
            )
            
            tenantPropertyDao.deleteTenantPropertyRelation(crossRef)
            
            // Invalidate cache to ensure fresh data
            invalidateCache()
            
            Log.d(TAG, "Removed property $propertyId from tenant $tenantId using junction table")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing property from tenant using junction table: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Check if a tenant-property relationship exists using the junction table
     */
    suspend fun relationshipExistsNew(tenantId: Long, propertyId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            return@withContext tenantPropertyDao.relationshipExists(tenantId, propertyId) > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error checking relationship using junction table: ${e.message}", e)
            return@withContext false
        }
    }
    
    /**
     * Get count of active properties for a tenant using the junction table
     */
    suspend fun getActivePropertyCountForTenantNew(tenantId: Long): Int = withContext(Dispatchers.IO) {
        try {
            return@withContext tenantPropertyDao.getActivePropertyCountForTenant(tenantId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting property count using junction table: ${e.message}", e)
            return@withContext 0
        }
    }
    
    /**
     * Get count of active tenants for a property using the junction table
     */
    suspend fun getActiveTenantCountForPropertyNew(propertyId: String): Int = withContext(Dispatchers.IO) {
        try {
            return@withContext tenantPropertyDao.getActiveTenantCountForProperty(propertyId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting tenant count using junction table: ${e.message}", e)
            return@withContext 0
        }
    }
    
    /**
     * Get all tenants with their multiple properties using the junction table
     */
    fun getTenantsWithMultipleProperties(): Flow<List<TenantWithMultipleProperties>> {
        return tenantPropertyDao.getTenantsWithMultipleProperties()
    }
}
