package ai.sach.libraryup.ui.screens.authentication

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Progress tracker for guest account conversion operations
 * Provides real-time feedback on conversion progress
 */
class ConversionProgressTracker(private val guestUserId: String) {
    
    companion object {
        private const val TAG = "ConversionProgressTracker"
    }
    
    // Progress state
    private val _progress = MutableStateFlow(ConversionProgress())
    val progress: StateFlow<ConversionProgress> = _progress.asStateFlow()
    
    /**
     * Update conversion progress
     * 
     * @param message Current operation description
     * @param percentage Progress percentage (0-100)
     */
    fun updateProgress(message: String, percentage: Int) {
        val clampedPercentage = percentage.coerceIn(0, 100)
        
        _progress.value = ConversionProgress(
            message = message,
            percentage = clampedPercentage,
            isComplete = clampedPercentage >= 100,
            timestamp = System.currentTimeMillis()
        )
        
        Log.d(TAG, "[CONVERSION_PROGRESS] Guest $guestUserId: $message ($clampedPercentage%)")
    }
    
    /**
     * Mark conversion as failed
     * 
     * @param errorMessage Error description
     */
    fun markFailed(errorMessage: String) {
        _progress.value = ConversionProgress(
            message = "Conversion failed: $errorMessage",
            percentage = 0,
            isComplete = false,
            isFailed = true,
            timestamp = System.currentTimeMillis()
        )
        
        Log.e(TAG, "[CONVERSION_FAILED] Guest $guestUserId: $errorMessage")
    }
    
    /**
     * Mark conversion as complete
     */
    fun markComplete() {
        _progress.value = ConversionProgress(
            message = "Conversion completed successfully",
            percentage = 100,
            isComplete = true,
            timestamp = System.currentTimeMillis()
        )
        
        Log.i(TAG, "[CONVERSION_COMPLETE] Guest $guestUserId: Conversion completed successfully")
    }
    
    /**
     * Reset progress tracker
     */
    fun reset() {
        _progress.value = ConversionProgress()
        Log.d(TAG, "[CONVERSION_RESET] Guest $guestUserId: Progress tracker reset")
    }
}

/**
 * Data class representing conversion progress state
 */
data class ConversionProgress(
    val message: String = "Initializing conversion...",
    val percentage: Int = 0,
    val isComplete: Boolean = false,
    val isFailed: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Check if conversion is in progress
     */
    val isInProgress: Boolean
        get() = !isComplete && !isFailed && percentage > 0
    
    /**
     * Get progress as a float between 0.0 and 1.0
     */
    val progressFloat: Float
        get() = percentage / 100f
}