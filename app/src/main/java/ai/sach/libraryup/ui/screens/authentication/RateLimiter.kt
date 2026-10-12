package ai.sach.rentup.ui.screens.authentication

class RateLimiter(
    private val maxAttempts: Int = 5,
    private val windowMs: Long = 300000 // 5 minutes
) {
    private var attempts = 0
    private var windowStart = System.currentTimeMillis()
    
    fun allowRequest(): Boolean {
        val now = System.currentTimeMillis()
        
        // Reset window if expired
        if (now - windowStart > windowMs) {
            attempts = 0
            windowStart = now
        }
        
        // Check if under limit
        if (attempts < maxAttempts) {
            attempts++
            return true
        }
        
        return false
    }
    
    fun getRemainingTime(): Long {
        return windowMs - (System.currentTimeMillis() - windowStart)
    }
}