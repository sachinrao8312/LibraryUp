package ai.sach.rentup.ui.screens.authentication

import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Comprehensive account collision detection and resolution system
 * Handles conflicts between guest accounts and Google accounts with data preservation
 */
class AccountCollisionHandler(
    private val authRepository: AuthRepository
) {
    
    companion object {
        private const val TAG = "AccountCollisionHandler"
    }
    
    /**
     * Detailed collision analysis result
     */
    data class CollisionAnalysis(
        val hasCollision: Boolean,
        val collisionType: AuthRepository.ConflictType?,
        val googleAccountExists: Boolean,
        val guestHasData: Boolean,
        val googleAccountEmail: String?,
        val recommendedAction: RecommendedAction,
        val riskLevel: RiskLevel,
        val dataImpact: DataImpact
    )
    
    /**
     * Recommended actions for collision resolution
     */
    enum class RecommendedAction {
        PROCEED_WITH_CONVERSION,
        SIGN_IN_EXISTING,
        SHOW_MERGE_OPTIONS,
        SHOW_ACCOUNT_EXISTS_DIALOG,
        REQUIRE_USER_CHOICE
    }
    
    /**
     * Risk levels for different collision scenarios
     */
    enum class RiskLevel {
        LOW,      // No data loss risk
        MEDIUM,   // Some data might be affected
        HIGH      // Significant data loss risk
    }
    
    /**
     * Data impact assessment
     */
    data class DataImpact(
        val guestDataAtRisk: Boolean,
        val googleDataAtRisk: Boolean,
        val canMergeData: Boolean,
        val requiresUserDecision: Boolean,
        val impactDescription: String
    )
    
    /**
     * Collision resolution strategy
     */
    data class ResolutionStrategy(
        val strategy: StrategyType,
        val requiresUserInput: Boolean,
        val preservesGuestData: Boolean,
        val preservesGoogleData: Boolean,
        val description: String
    )
    
    enum class StrategyType {
        AUTO_CONVERT,
        AUTO_SIGN_IN,
        MERGE_DATA,
        USER_CHOICE,
        CREATE_NEW
    }
    
    /**
     * Analyze potential account collisions before attempting conversion
     * 
     * @param task Google Sign-In task result
     * @return Detailed collision analysis
     */
    suspend fun analyzeCollision(task: Task<GoogleSignInAccount>): CollisionAnalysis = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting collision analysis")
            
            // Validate preconditions
            val currentUser = authRepository.getCurrentFirebaseUser()
            if (currentUser == null || !currentUser.isAnonymous) {
                return@withContext CollisionAnalysis(
                    hasCollision = false,
                    collisionType = null,
                    googleAccountExists = false,
                    guestHasData = false,
                    googleAccountEmail = null,
                    recommendedAction = RecommendedAction.PROCEED_WITH_CONVERSION,
                    riskLevel = RiskLevel.LOW,
                    dataImpact = DataImpact(
                        guestDataAtRisk = false,
                        googleDataAtRisk = false,
                        canMergeData = false,
                        requiresUserDecision = false,
                        impactDescription = "No collision detected - safe to proceed"
                    )
                )
            }
            
            // Get Google account information
            val googleAuthService = getGoogleAuthService()
            val account = googleAuthService.processSignInResult(task)
            val email = account?.email
            
            if (email == null) {
                Log.w(TAG, "Cannot analyze collision - Google account email is null")
                return@withContext createErrorAnalysis("Google account email unavailable")
            }
            
            // Check if Google account exists
            val googleAccountExists = authRepository.checkGoogleAccountExists(email)
            Log.d(TAG, "Google account exists: $googleAccountExists")
            
            // Check if guest has data
            val guestHasData = authRepository.hasGuestCreatedAccount()
            Log.d(TAG, "Guest has data: $guestHasData")
            
            // Determine collision type and recommended action
            val (collisionType, recommendedAction, riskLevel) = determineCollisionType(
                googleAccountExists, guestHasData
            )
            
            // Assess data impact
            val dataImpact = assessDataImpact(googleAccountExists, guestHasData, collisionType)
            
            Log.d(TAG, "Collision analysis complete - Type: $collisionType, Action: $recommendedAction, Risk: $riskLevel")
            
            return@withContext CollisionAnalysis(
                hasCollision = collisionType != null,
                collisionType = collisionType,
                googleAccountExists = googleAccountExists,
                guestHasData = guestHasData,
                googleAccountEmail = email,
                recommendedAction = recommendedAction,
                riskLevel = riskLevel,
                dataImpact = dataImpact
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during collision analysis", e)
            return@withContext createErrorAnalysis("Analysis failed: ${e.message}")
        }
    }
    
    /**
     * Resolve account collision based on user choice
     * 
     * @param analysis The collision analysis result
     * @param userChoice The user's chosen resolution strategy
     * @param task Google Sign-In task result
     * @return Result of the resolution attempt
     */
    suspend fun resolveCollision(
        analysis: CollisionAnalysis,
        userChoice: ResolutionStrategy,
        task: Task<GoogleSignInAccount>
    ): AuthRepository.ConversionResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Resolving collision with strategy: ${userChoice.strategy}")
            
            return@withContext when (userChoice.strategy) {
                StrategyType.AUTO_CONVERT -> {
                    // Proceed with normal conversion
                    authRepository.convertGuestToGoogleAccount(task, skipValidation = true)
                }
                
                StrategyType.AUTO_SIGN_IN -> {
                    // Sign in with existing Google account
                    val result = authRepository.signInWithExistingGoogleAccount(task)
                    result.fold(
                        onSuccess = { user -> AuthRepository.ConversionResult.Success(user) },
                        onFailure = { exception -> AuthRepository.ConversionResult.Failure(exception as Exception) }
                    )
                }
                
                StrategyType.MERGE_DATA -> {
                    // Perform data merge during conversion
                    performDataMerge(task, analysis)
                }
                
                StrategyType.CREATE_NEW -> {
                    // Create completely new account (guest data will be lost)
                    createNewAccount(task)
                }
                
                StrategyType.USER_CHOICE -> {
                    // This should not happen at resolution time
                    AuthRepository.ConversionResult.Failure(
                        Exception("User choice required - resolution strategy not specified")
                    )
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving collision", e)
            AuthRepository.ConversionResult.Failure(e)
        }
    }
    
    /**
     * Get recommended resolution strategies for a collision
     * 
     * @param analysis The collision analysis result
     * @return List of possible resolution strategies
     */
    fun getResolutionStrategies(analysis: CollisionAnalysis): List<ResolutionStrategy> {
        return when (analysis.collisionType) {
            AuthRepository.ConflictType.GOOGLE_ACCOUNT_EXISTS -> listOf(
                ResolutionStrategy(
                    strategy = StrategyType.AUTO_SIGN_IN,
                    requiresUserInput = false,
                    preservesGuestData = false,
                    preservesGoogleData = true,
                    description = "Sign in with existing Google account (guest data will be lost)"
                ),
                ResolutionStrategy(
                    strategy = StrategyType.CREATE_NEW,
                    requiresUserInput = true,
                    preservesGuestData = false,
                    preservesGoogleData = true,
                    description = "Create new account with different email"
                )
            )
            
            AuthRepository.ConflictType.GUEST_DATA_EXISTS -> listOf(
                ResolutionStrategy(
                    strategy = StrategyType.AUTO_CONVERT,
                    requiresUserInput = false,
                    preservesGuestData = true,
                    preservesGoogleData = true,
                    description = "Convert guest account to Google account (preserves all data)"
                ),
                ResolutionStrategy(
                    strategy = StrategyType.CREATE_NEW,
                    requiresUserInput = true,
                    preservesGuestData = false,
                    preservesGoogleData = true,
                    description = "Start fresh with new Google account (guest data will be lost)"
                )
            )
            
            AuthRepository.ConflictType.BOTH_HAVE_DATA -> listOf(
                ResolutionStrategy(
                    strategy = StrategyType.MERGE_DATA,
                    requiresUserInput = true,
                    preservesGuestData = true,
                    preservesGoogleData = true,
                    description = "Merge guest data with existing Google account"
                ),
                ResolutionStrategy(
                    strategy = StrategyType.AUTO_SIGN_IN,
                    requiresUserInput = true,
                    preservesGuestData = false,
                    preservesGoogleData = true,
                    description = "Use existing Google account (guest data will be lost)"
                ),
                ResolutionStrategy(
                    strategy = StrategyType.CREATE_NEW,
                    requiresUserInput = true,
                    preservesGuestData = false,
                    preservesGoogleData = true,
                    description = "Create completely new account (guest data will be lost)"
                )
            )
            
            null -> listOf(
                ResolutionStrategy(
                    strategy = StrategyType.AUTO_CONVERT,
                    requiresUserInput = false,
                    preservesGuestData = true,
                    preservesGoogleData = true,
                    description = "Safe to proceed with conversion"
                )
            )
        }
    }
    
    /**
     * Determine collision type and recommended action
     */
    private fun determineCollisionType(
        googleAccountExists: Boolean,
        guestHasData: Boolean
    ): Triple<AuthRepository.ConflictType?, RecommendedAction, RiskLevel> {
        return when {
            googleAccountExists && guestHasData -> Triple(
                AuthRepository.ConflictType.BOTH_HAVE_DATA,
                RecommendedAction.REQUIRE_USER_CHOICE,
                RiskLevel.HIGH
            )
            googleAccountExists -> Triple(
                AuthRepository.ConflictType.GOOGLE_ACCOUNT_EXISTS,
                RecommendedAction.SHOW_ACCOUNT_EXISTS_DIALOG,
                RiskLevel.MEDIUM
            )
            guestHasData -> Triple(
                AuthRepository.ConflictType.GUEST_DATA_EXISTS,
                RecommendedAction.SHOW_MERGE_OPTIONS,
                RiskLevel.LOW
            )
            else -> Triple(
                null,
                RecommendedAction.PROCEED_WITH_CONVERSION,
                RiskLevel.LOW
            )
        }
    }
    
    /**
     * Assess the potential data impact of collision resolution
     */
    private fun assessDataImpact(
        googleAccountExists: Boolean,
        guestHasData: Boolean,
        collisionType: AuthRepository.ConflictType?
    ): DataImpact {
        return when (collisionType) {
            AuthRepository.ConflictType.BOTH_HAVE_DATA -> DataImpact(
                guestDataAtRisk = true,
                googleDataAtRisk = false,
                canMergeData = true,
                requiresUserDecision = true,
                impactDescription = "Both accounts have data. Merging is possible but requires user decision."
            )
            
            AuthRepository.ConflictType.GOOGLE_ACCOUNT_EXISTS -> DataImpact(
                guestDataAtRisk = true,
                googleDataAtRisk = false,
                canMergeData = false,
                requiresUserDecision = true,
                impactDescription = "Guest data will be lost if signing in with existing Google account."
            )
            
            AuthRepository.ConflictType.GUEST_DATA_EXISTS -> DataImpact(
                guestDataAtRisk = false,
                googleDataAtRisk = false,
                canMergeData = true,
                requiresUserDecision = false,
                impactDescription = "Guest data can be safely converted to Google account."
            )
            
            null -> DataImpact(
                guestDataAtRisk = false,
                googleDataAtRisk = false,
                canMergeData = false,
                requiresUserDecision = false,
                impactDescription = "No data at risk - safe to proceed."
            )
        }
    }
    
    /**
     * Perform data merge during conversion
     */
    private suspend fun performDataMerge(
        task: Task<GoogleSignInAccount>,
        analysis: CollisionAnalysis
    ): AuthRepository.ConversionResult {
        Log.d(TAG, "Performing data merge for collision resolution")
        
        // Use the enhanced conversion method with merge capability
        return authRepository.convertGuestToGoogleAccount(task, skipValidation = true)
    }
    
    /**
     * Create a completely new account
     */
    private suspend fun createNewAccount(
        task: Task<GoogleSignInAccount>
    ): AuthRepository.ConversionResult {
        Log.d(TAG, "Creating new account (guest data will be lost)")
        
        try {
            // Sign out current guest user
            authRepository.signOut()
            
            // Sign in with Google to create new account
            val result = authRepository.signInWithGoogle(task)
            return result.fold(
                onSuccess = { user -> AuthRepository.ConversionResult.Success(user) },
                onFailure = { exception -> AuthRepository.ConversionResult.Failure(exception as Exception) }
            )
        } catch (e: Exception) {
            return AuthRepository.ConversionResult.Failure(e)
        }
    }
    
    /**
     * Create error analysis result
     */
    private fun createErrorAnalysis(errorMessage: String): CollisionAnalysis {
        return CollisionAnalysis(
            hasCollision = false,
            collisionType = null,
            googleAccountExists = false,
            guestHasData = false,
            googleAccountEmail = null,
            recommendedAction = RecommendedAction.PROCEED_WITH_CONVERSION,
            riskLevel = RiskLevel.LOW,
            dataImpact = DataImpact(
                guestDataAtRisk = false,
                googleDataAtRisk = false,
                canMergeData = false,
                requiresUserDecision = false,
                impactDescription = errorMessage
            )
        )
    }
    
    /**
     * Get GoogleAuthService instance (using reflection to access private field)
     */
    private fun getGoogleAuthService(): GoogleAuthService {
        // Remove reflection usage - this is a hidden API that causes warnings
        // Instead, we'll use the public method from AuthRepository
        return authRepository.getGoogleAuthServiceInstance()
    }
}