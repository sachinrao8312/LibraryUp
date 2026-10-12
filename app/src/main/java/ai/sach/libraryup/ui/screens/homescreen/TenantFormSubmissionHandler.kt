package ai.sach.libraryup.ui.screens.homescreen

import ai.sach.libraryup.data.repository.TenantRepository
import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.cancel

/**
 * Imports tenant form submissions from Firestore on demand.
 *
 * Push-triggered imports are handled by
 * [ai.sach.libraryup.service.TenantSubmissionSyncWorker], the single consumer of a
 * submission push; this class covers the manual and on-resume refreshes.
 */
class TenantFormSubmissionHandler @Inject constructor(
    private val context: Context,
    private val tenantRepository: TenantRepository
) : DefaultLifecycleObserver {
    
    companion object {
        private const val TAG = "TenantFormHandler"
    }
    
    // Coroutine scope for background processing
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    /**
     * Process tenant form submissions from Firestore
     */
    private fun processTenantFormSubmissions() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Processing tenant form submissions...")
                val result = tenantRepository.processTenantFormSubmissions()
                
                result.fold(
                    onSuccess = { count ->
                        Log.d(TAG, "Successfully processed $count tenant form submissions")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error processing tenant form submissions", error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception during tenant form processing", e)
            }
        }
    }
    
    /**
     * Manually trigger processing of tenant form submissions
     */
    fun refreshTenantSubmissions() {
        processTenantFormSubmissions()
    }

    /**
     * Cancel the coroutine scope to prevent memory leaks
     */
    fun cancel() {
        coroutineScope.cancel()
        Log.d(TAG, "TenantFormSubmissionHandler coroutine scope cancelled")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        cancel()
    }
}