package ai.sach.rentup.ui.screens.authentication

// Explicit imports for AuthViewModel and related classes
import ai.sach.rentup.ui.common.responsiveness.LocalDimensions
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn

/**
 * Smart conversion prompt dialog that shows different messages based on user engagement
 */
@Composable
fun SmartConversionPrompt(
    promptType: ConversionPromptType,
    message: String,
    onDismiss: () -> Unit,
    onConvert: () -> Unit,
    modifier: Modifier = Modifier
) {
    val promptConfig = getPromptConfig(promptType)
    val dimensions = LocalDimensions.current
    val configuration = LocalConfiguration.current
    
    // Responsive dialog sizing
    val isSmallScreen = configuration.screenWidthDp < 360
    val dialogPadding = if (isSmallScreen) dimensions.paddingSmall else dimensions.paddingMedium
    val contentPadding = if (isSmallScreen) dimensions.paddingMedium else dimensions.paddingLarge
    val cornerRadius = if (isSmallScreen) dimensions.radiusMedium else dimensions.radiusLarge
    
    val dismissRequest: () -> Unit = if (promptType != ConversionPromptType.MANDATORY) onDismiss else { { } }
    
    Dialog(
        onDismissRequest = dismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = promptType != ConversionPromptType.MANDATORY,
            dismissOnClickOutside = promptType != ConversionPromptType.MANDATORY
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(dialogPadding),
            shape = RoundedCornerShape(cornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = promptConfig.backgroundColor
            )
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with icon and close button (if not mandatory)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = promptConfig.icon,
                        contentDescription = promptConfig.title,
                        tint = promptConfig.iconColor,
                        modifier = Modifier.size(dimensions.iconSizeLarge)
                    )
                    
                    if (promptType != ConversionPromptType.MANDATORY) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(dimensions.iconSizeMedium)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(dimensions.spacingMedium))
                
                // Title
                Text(
                    text = promptConfig.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = dimensions.fontSizeLarge
                    ),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = promptConfig.textColor
                )
                
                Spacer(modifier = Modifier.height(dimensions.spacingSmall))
                
                // Message
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = dimensions.fontSizeMedium
                    ),
                    textAlign = TextAlign.Center,
                    color = promptConfig.textColor.copy(alpha = 0.9f)
                )
                
                Spacer(modifier = Modifier.height(dimensions.spacingLarge))
                
                // Benefits list
                promptConfig.benefits.forEach { benefit ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = dimensions.spacingTiny),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✓",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = dimensions.fontSizeMedium
                            )
                        )
                        Spacer(modifier = Modifier.width(dimensions.spacingSmall))
                        Text(
                            text = benefit,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = dimensions.fontSizeSmall
                            ),
                            color = promptConfig.textColor.copy(alpha = 0.8f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(dimensions.spacingLarge))
                
                // Action buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
                ) {
                    // Primary action button
                    Button(
                        onClick = onConvert,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = promptConfig.primaryButtonColor
                        )
                    ) {
                        Text(
                            text = promptConfig.primaryButtonText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    // Secondary action (if not mandatory)
                    if (promptType != ConversionPromptType.MANDATORY) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Maybe Later",
                                color = promptConfig.textColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Mandatory conversion screen (fullscreen takeover)
 */
@Composable
fun MandatoryConversionScreen(
    daysAsGuest: Int,
    onConvert: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalDimensions.current
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isSmallScreen) dimensions.paddingMedium else dimensions.paddingLarge),
            shape = RoundedCornerShape(dimensions.radiusLarge)
        ) {
            Column(
                modifier = Modifier.padding(
                    if (isSmallScreen) dimensions.paddingLarge else dimensions.paddingLarge * 1.3f
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Warning icon
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(
                        if (isSmallScreen) dimensions.iconSizeLarge * 2f else dimensions.iconSizeLarge * 2.5f
                    )
                )
                
                Spacer(modifier = Modifier.height(dimensions.spacingLarge))
                
                // Title
                Text(
                    text = "Trial Period Ended",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = dimensions.fontSizeXLarge
                    ),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(dimensions.spacingMedium))
                
                // Message
                Text(
                    text = "Your $daysAsGuest-day trial has ended. Create a permanent account to continue using RentUp and keep all your valuable rental data.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Benefits
                val benefits = listOf(
                    "Keep all your properties and tenant data",
                    "Sync across multiple devices",
                    "Automatic cloud backup",
                    "Access from anywhere",
                    "Enhanced security features"
                )
                
                benefits.forEach { benefit ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = benefit,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Convert button
                Button(
                    onClick = onConvert,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Create Account Now",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Data class for prompt configuration
 */
private data class PromptConfig(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconColor: Color,
    val backgroundColor: Color,
    val textColor: Color,
    val primaryButtonColor: Color,
    val primaryButtonText: String,
    val benefits: List<String>
)

/**
 * Get configuration for different prompt types with theme awareness
 */
@Composable
private fun getPromptConfig(promptType: ConversionPromptType): PromptConfig {
    return when (promptType) {
        ConversionPromptType.MANDATORY -> PromptConfig(
            title = "Account Required",
            icon = Icons.Default.Warning,
            iconColor = MaterialTheme.colorScheme.error,
            backgroundColor = MaterialTheme.colorScheme.errorContainer,
            textColor = MaterialTheme.colorScheme.onErrorContainer,
            primaryButtonColor = MaterialTheme.colorScheme.error,
            primaryButtonText = "Create Account",
            benefits = listOf(
                "Keep all your data",
                "Continue using RentUp",
                "Sync across devices"
            )
        )
        ConversionPromptType.DATA_WARNING -> PromptConfig(
            title = "Backup Your Data",
            icon = Icons.Default.Backup,
            iconColor = MaterialTheme.colorScheme.primary,
            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
            textColor = MaterialTheme.colorScheme.onPrimaryContainer,
            primaryButtonColor = MaterialTheme.colorScheme.primary,
            primaryButtonText = "Backup Now",
            benefits = listOf(
                "Never lose your rental data",
                "Access from any device",
                "Automatic cloud sync"
            )
        )
        ConversionPromptType.SMART_BACKUP -> PromptConfig(
            title = "Protect Your Data",
            icon = Icons.Default.Security,
            iconColor = MaterialTheme.colorScheme.secondary,
            backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
            textColor = MaterialTheme.colorScheme.onSecondaryContainer,
            primaryButtonColor = MaterialTheme.colorScheme.secondary,
            primaryButtonText = "Secure Account",
            benefits = listOf(
                "Cloud backup protection",
                "Multi-device access",
                "Enhanced security"
            )
        )
        ConversionPromptType.PROPERTY_SYNC -> PromptConfig(
            title = "Sync Your Properties",
            icon = Icons.Default.Home,
            iconColor = MaterialTheme.colorScheme.tertiary,
            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
            textColor = MaterialTheme.colorScheme.onTertiaryContainer,
            primaryButtonColor = MaterialTheme.colorScheme.tertiary,
            primaryButtonText = "Enable Sync",
            benefits = listOf(
                "Access properties anywhere",
                "Automatic synchronization",
                "Multiple device support"
            )
        )
        ConversionPromptType.TENANT_BACKUP -> PromptConfig(
            title = "Secure Tenant Data",
            icon = Icons.Default.People,
            iconColor = MaterialTheme.colorScheme.primary,
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
            primaryButtonColor = MaterialTheme.colorScheme.primary,
            primaryButtonText = "Backup Data",
            benefits = listOf(
                "Protect tenant information",
                "Safe cloud storage",
                "Easy data recovery"
            )
        )
        ConversionPromptType.GENTLE_HINT -> PromptConfig(
            title = "Upgrade Your Experience",
            icon = Icons.Default.AccountCircle,
            iconColor = MaterialTheme.colorScheme.secondary,
            backgroundColor = MaterialTheme.colorScheme.surface,
            textColor = MaterialTheme.colorScheme.onSurface,
            primaryButtonColor = MaterialTheme.colorScheme.secondary,
            primaryButtonText = "Create Account",
            benefits = listOf(
                "Enhanced app features",
                "Cross-device synchronization",
                "Priority support"
            )
        )
        ConversionPromptType.NONE -> PromptConfig(
            title = "",
            icon = Icons.Default.AccountCircle,
            iconColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.surface,
            textColor = MaterialTheme.colorScheme.onSurface,
            primaryButtonColor = MaterialTheme.colorScheme.primary,
            primaryButtonText = "OK",
            benefits = emptyList()
        )
    }
}

/**
 * Checks if conversion prompts should be shown
 */
@Composable
fun CheckConversionPrompts(
    onPromptRequired: (ConversionPromptType, String) -> Unit
) {
    val guestManager = rememberGuestAccountManager()
    val application = LocalContext.current.applicationContext as ai.sach.rentup.RentUpApplication
    
    // Get data counts for smart triggers
    val propertyViewModel: ai.sach.rentup.ui.viewmodel.PropertyViewModel = viewModel(
        factory = ai.sach.rentup.ui.viewmodel.PropertyViewModelFactory(application.propertyRepository)
    )
    val tenantViewModel: ai.sach.rentup.data.viewmodel.TenantViewModel = viewModel(
        factory = ai.sach.rentup.data.viewmodel.TenantViewModelFactory(
            application.tenantRepository,
            application.propertyRepository
        )
    )
    
    val properties by propertyViewModel.allProperties.observeAsState(initial = emptyList())
    val tenants by tenantViewModel.allTenants.observeAsState(initial = emptyList())
    
    LaunchedEffect(properties, tenants) {
        val propertiesCount = properties.size
        val tenantsCount = tenants.size
        val usageSessions = 5 // Could be tracked separately
        
        if (guestManager.shouldShowConversionPrompt(propertiesCount, tenantsCount, usageSessions)) {
            val promptType = guestManager.getConversionPromptType(propertiesCount, tenantsCount, usageSessions)
            val message = guestManager.getConversionMessage(propertiesCount, tenantsCount, promptType)
            
            onPromptRequired(promptType, message)
            guestManager.markConversionPromptShown()
        }
    }
}

/**
 * Integration composable for main app.
 *
 * No longer called from anywhere. These prompts sold cloud backup, multi-device sync
 * and "enhanced security", none of which the app did - and [ConversionPromptType.MANDATORY] blocked the app entirely after 60
 * days to push a sign-up for it. Backup & Restore in Settings replaced all of it with
 * a local backup file. Do not wire this back up without a working service behind it.
 */
@Composable
fun SmartConversionIntegration(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Create AuthViewModel with proper factory
    val application = context.applicationContext as ai.sach.rentup.RentUpApplication
    val authRepository = remember { AuthRepository(context) }
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(
            application = application,
            authRepository = authRepository
        )
    )
    var showConversionPrompt by remember { mutableStateOf<ConversionPromptType?>(null) }
    var conversionMessage by remember { mutableStateOf("") }
    var showConversionOptions by remember { mutableStateOf(false) }
    
    // Track Google Sign-In errors
    var showGoogleSignInError by remember { mutableStateOf(false) }
    var googleSignInErrorMessage by remember { mutableStateOf("") }
    
    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            // Check if this is our dummy intent with an error
            val isDummy = result.data?.action == "ai.sach.rentup.DUMMY_GOOGLE_SIGN_IN"
            if (isDummy) {
                val errorType = result.data?.getStringExtra("ERROR_TYPE") ?: "UNKNOWN_ERROR"
                val errorMessage = result.data?.getStringExtra("ERROR_MESSAGE")
                
                Log.w("ConversionPrompts", "Received dummy Google Sign-In result: $errorType - $errorMessage")
                
                val userMessage = when (errorType) {
                    "PLAY_SERVICES_UNAVAILABLE" -> "Google Play Services is not available on this device."
                    "INTENT_CREATION_ERROR" -> "Unable to connect to Google services."
                    "CRITICAL_ERROR" -> "A critical error occurred while connecting to Google."
                    "RESOURCE_ID_ERROR" -> "Google Play Services resources are missing or incompatible."
                    "SECURITY_EXCEPTION" -> "Your device has a security configuration that prevents Google Sign-In."
                    else -> "Google Sign-In is not available at this time."
                }
                
                // Show detailed error dialog
                googleSignInErrorMessage = "$userMessage ${errorMessage ?: ""}"
                showGoogleSignInError = true
                return@rememberLauncherForActivityResult
            }
            
            // Normal flow for successful Google Sign-In
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                authViewModel.convertGuestToGoogleAccount(task, context)
                showConversionOptions = false // Close dialog on successful sign-in
            } else {
                // User canceled or failed sign-in
                Log.d("ConversionPrompts", "Google Sign-In was canceled or failed")
                Toast.makeText(context, "Sign-in was canceled", Toast.LENGTH_SHORT).show()
                showConversionOptions = false // Close dialog on cancellation
            }
        } catch (e: Exception) {
            Log.e("ConversionPrompts", "Google conversion failed", e)
            // Show error dialog
            googleSignInErrorMessage = "Account conversion failed: ${e.message}"
            showGoogleSignInError = true
        }
    }
    
    // Check for conversion prompts
    CheckConversionPrompts(onPromptRequired = { promptType, message ->
        showConversionPrompt = promptType
        conversionMessage = message
    })
    
    // Show conversion prompt if needed
    showConversionPrompt?.let { promptType ->
        if (promptType == ConversionPromptType.MANDATORY) {
            val guestManager = rememberGuestAccountManager()
            MandatoryConversionScreen(
                daysAsGuest = guestManager.getDaysAsGuest(),
                onConvert = {
                    showConversionOptions = true
                    showConversionPrompt = null
                }
            )
        } else {
            SmartConversionPrompt(
                promptType = promptType,
                message = conversionMessage,
                onDismiss = {
                    showConversionPrompt = null
                },
                onConvert = {
                    showConversionOptions = true
                    showConversionPrompt = null
                }
            )
        }
    }
    
    // Show Google Sign-In error dialog if needed
    if (showGoogleSignInError) {
        GoogleSignInErrorDialog(
            errorMessage = googleSignInErrorMessage,
            onDismiss = { showGoogleSignInError = false },
            onUseEmail = { /* No email auth */ }
        )
    }
    
    // Show conversion options dialog
    if (showConversionOptions) {
        ConversionOptionsDialog(
            onDismiss = { showConversionOptions = false },
            onGoogleSignIn = {
                try {
                    val signInIntent = authViewModel.getGoogleSignInIntent()
                    if (signInIntent != null) {
                        // Check if this is a dummy intent (error case)
                        if (signInIntent.action == "ai.sach.rentup.DUMMY_GOOGLE_SIGN_IN") {
                            val errorType = signInIntent.getStringExtra("ERROR_TYPE") ?: "UNKNOWN_ERROR"
                            val errorMessage = signInIntent.getStringExtra("ERROR_MESSAGE")
                            Log.w("ConversionPrompts", "Received dummy intent before launch: $errorType - $errorMessage")
                            
                            // Show detailed error dialog
                            googleSignInErrorMessage = when (errorType) {
                                "PLAY_SERVICES_UNAVAILABLE" -> "Google Play Services is not available on this device. ${errorMessage ?: ""}"
                                "INTENT_CREATION_ERROR" -> "Unable to connect to Google services. ${errorMessage ?: ""}"
                                "CRITICAL_ERROR" -> "A critical error occurred while connecting to Google. ${errorMessage ?: ""}"
                                "RESOURCE_ID_ERROR" -> "Google Play Services resources are missing or incompatible. ${errorMessage ?: ""}"
                                "SECURITY_EXCEPTION" -> "Your device has a security configuration that prevents Google Sign-In. ${errorMessage ?: ""}"
                                else -> "Google Sign-In is not available at this time. ${errorMessage ?: ""}"
                            }
                            showGoogleSignInError = true
                            showConversionOptions = false // Only close dialog on error
                        } else {
                            // Normal case - launch the sign-in flow
                            googleSignInLauncher.launch(signInIntent)
                            // Don't close dialog here - let the launcher result handle it
                        }
                    } else {
                        // This shouldn't happen with our changes, but just in case
                        Log.e("ConversionPrompts", "Google Sign-In intent is null")
                        googleSignInErrorMessage = "Google Sign-In is not available. Please try again later."
                        showGoogleSignInError = true
                        showConversionOptions = false // Only close dialog on error
                    }
                } catch (e: Exception) {
                    Log.e("ConversionPrompts", "Error launching Google Sign-In", e)
                    googleSignInErrorMessage = "Unable to connect to Google services: ${e.message}"
                    showGoogleSignInError = true
                    showConversionOptions = false // Only close dialog on error
                }
            },
            onEmailConversion = {
                // No email conversion, just show a message
                showConversionOptions = false
                Toast.makeText(context, "Email conversion is not available", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/**
 * Dialog to choose conversion method (Google only)
 */
@Composable
fun ConversionOptionsDialog(
    onDismiss: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onEmailConversion: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Create Your Account",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Sign in with Google to create your permanent account and sync data across devices",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Google Sign-In Button
                Button(
                    onClick = onGoogleSignIn,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Google",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Continue with Google")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Cancel Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Continue as Guest",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Shows an error dialog when Google Sign-In fails
 */
@Composable
private fun GoogleSignInErrorDialog(
    errorMessage: String,
    onDismiss: () -> Unit,
    onUseEmail: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Google Sign-In Unavailable",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Show a simplified error message to the user
                val simplifiedMessage = when {
                    errorMessage.contains("PLAY_SERVICES_UNAVAILABLE") -> 
                        "Google Play Services is not available on this device."
                    errorMessage.contains("GOOGLE_SIGNIN_DISABLED") ->
                        "Google Sign-In has been disabled due to compatibility issues."
                    errorMessage.contains("RESOURCE_ID_ERROR") ->
                        "Google Play Services resources are missing or incompatible."
                    errorMessage.contains("SECURITY_EXCEPTION") ->
                        "Your device has a security configuration that prevents Google Sign-In."
                    else -> "Unable to connect to Google services at this time."
                }
                
                Text(
                    text = simplifiedMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "You can continue using the app as a guest. Your data will be saved locally on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue as Guest")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Try again later when Google services are available",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}