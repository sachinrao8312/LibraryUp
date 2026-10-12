package ai.sach.rentup.data.firestore.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore access for tenant onboarding forms - the only landlord data that goes through
 * Firestore. Tenants and properties live in the local Room database and are no longer
 * uploaded (earlier versions mirrored them to `users/{uid}/tenants` and `/properties`).
 */
@Singleton
class FirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TAG = "FirestoreRepository"
        private const val COLLECTION_TENANT_FORMS = "tenant-forms"
    }

    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid
            ?: throw SecurityException("User must be authenticated to access Firestore")
    }

    private fun getUserTenantFormsCollection() =
        firestore.collection(COLLECTION_TENANT_FORMS)
            .whereEqualTo("ownerId", getCurrentUserId())

    /**
     * Retrieves all tenant form submissions for the current user
     */
    suspend fun getAllTenantFormSubmissions(): Result<List<Map<String, Any>>> = try {
        val querySnapshot = getUserTenantFormsCollection()
            .get()
            .await()

        val submissions = querySnapshot.documents.mapNotNull { doc ->
            doc.data?.plus("id" to doc.id) // Include the document ID
        }

        Log.d(TAG, "Retrieved ${submissions.size} tenant form submissions")
        Result.success(submissions)
    } catch (e: Exception) {
        Log.e(TAG, "Error retrieving tenant form submissions", e)
        Result.failure(e)
    }

    /**
     * Marks a tenant form submission as processed
     */
    suspend fun markTenantFormAsProcessed(formId: String, updateData: Map<String, Any>): Result<Unit> = try {
        firestore.collection(COLLECTION_TENANT_FORMS)
            .document(formId)
            .update(updateData)
            .await()

        Log.d(TAG, "Marked form $formId as processed")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error marking form as processed: $formId", e)
        Result.failure(e)
    }
}
