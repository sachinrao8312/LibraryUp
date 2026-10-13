package ai.sach.libraryup.ui.screens.authentication

import ai.sach.libraryup.R
import ai.sach.libraryup.ui.common.responsiveness.Dimensions
import ai.sach.libraryup.ui.common.responsiveness.LocalDimensions
import ai.sach.libraryup.ui.theme.AppColors
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn

/**
 * Modern Google Authentication Screen with beautiful Material 3 design
 * Features smooth animations, proper visual hierarchy, and excellent UX
 */
@Composable
fun GoogleAuthScreen(
    authViewModel: AuthViewModel,
    onNavigateToMain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dimensions = LocalDimensions.current
    val configuration = LocalConfiguration.current
    
    // Collect auth states
    val authState by authViewModel.authState.collectAsState()
    val authUiState by authViewModel.authUiState.collectAsState()
    
    // Responsive sizing
    val isSmallScreen = configuration.screenWidthDp < 360
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    // Animation states
    var showContent by remember { mutableStateOf(false) }
    var showButtons by remember { mutableStateOf(false) }
    
    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleGoogleSignInResult(result, authViewModel, context)
    }
    
    // Handle successful authentication
    when (authState) {
        is AuthState.Authenticated, is AuthState.Guest -> {
            onNavigateToMain()
            return
        }
        else -> { /* Continue showing auth screen */ }
    }
    
    // Trigger animations
    LaunchedEffect(Unit) {
        showContent = true
        showButtons = true
    }
    
    // Clean background with subtle gradient
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Skip button, pinned top-right (Blinkit/Zepto style)
        TextButton(
            onClick = { authViewModel.signInAsGuest(context) },
            enabled = !authUiState.isLoading,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.action_skip),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        // Main content - no scrolling, optimized layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (isSmallScreen) 24.dp else 32.dp,
                    vertical = if (isLandscape) 16.dp else 24.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (isLandscape) Arrangement.Center else Arrangement.SpaceBetween
        ) {
            
            // Top section with logo and branding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                if (!isLandscape) {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            
            // Logo and branding with simple fade animation
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(600)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                ModernAppBranding(
                    dimensions = dimensions,
                    isSmallScreen = isSmallScreen
                )
            }
            
                Spacer(modifier = Modifier.height(if (isLandscape) 16.dp else 24.dp))
            }
            
            // Middle section with authentication card
            AnimatedVisibility(
                visible = showButtons,
                enter = fadeIn(animationSpec = tween(600)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                ModernAuthenticationCard(
                    authUiState = authUiState,
                    dimensions = dimensions,
                    isSmallScreen = isSmallScreen,
                    onGoogleSignIn = {
                        val signInIntent = authViewModel.getGoogleSignInIntent()
                        if (signInIntent != null) {
                            googleSignInLauncher.launch(signInIntent)
                        }
                    }
                )
            }
            
            // Bottom section with footer
            if (!isLandscape) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Footer with simple fade
                AnimatedVisibility(
                    visible = showButtons,
                    enter = fadeIn(animationSpec = tween(600)),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    ModernAppFooter(dimensions = dimensions)
                }
            }
        }
    }
}

/**
 * Modern app branding with clean, simplified logo
 */
@Composable
private fun ModernAppBranding(
    dimensions: Dimensions,
    isSmallScreen: Boolean
) {

    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Simplified logo with clean design
        Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer light green circle - reduced size and increased visibility
            Box(
                modifier = Modifier
                    .size(if (isSmallScreen) 70.dp else 87.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                AppColors.Brand.copy(alpha = 0.6f),
                                AppColors.Brand.copy(alpha = 0.4f),
                                AppColors.Brand.copy(alpha = 0.2f)
                            )
                        )
                    )
            )

            // Main logo circle with gradient and white stroke
            Box(
                modifier = Modifier
                    .size(if (isSmallScreen) 100.dp else 125.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                AppColors.Brand,
                                AppColors.Brand.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        spotColor = AppColors.Brand.copy(alpha = 0.3f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // White stroke circle
                Box(
                    modifier = Modifier
                        .size(if (isSmallScreen) 95.dp else 120.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                
                // Inner logo circle
                Box(
                    modifier = Modifier
                        .size(if (isSmallScreen) 90.dp else 115.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    AppColors.Brand,
                                    AppColors.Brand.copy(alpha = 0.8f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Use the original logo with 2x zoom to crop out rectangular edges
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = stringResource(R.string.app_logo_content_description),
                        modifier = Modifier
                            .size(if (isSmallScreen) 125.dp else 156.dp)
                            .padding(0.dp)
                    )
                }
            }
        }
        
        // App name with modern typography
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = if (isSmallScreen) 32.sp else 40.sp
            ),
            fontWeight = FontWeight.Bold,
            color = AppColors.Brand
        )
        
        // Tagline with better spacing
        Text(
            text = stringResource(R.string.welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = if (isSmallScreen) 16.sp else 18.sp
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Modern authentication card with enhanced design
 */
@Composable
private fun ModernAuthenticationCard(
    authUiState: AuthUiState,
    dimensions: Dimensions,
    isSmallScreen: Boolean,
    onGoogleSignIn: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = AppColors.Brand.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isSmallScreen) 24.dp else 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            
            // Header section with better typography
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.auth_welcome_back),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = if (isSmallScreen) 24.sp else 28.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = stringResource(R.string.auth_choose_start),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = if (isSmallScreen) 16.sp else 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
            
            // Google Sign-In Button (Primary)
            ModernGoogleSignInButton(
                onClick = onGoogleSignIn,
                isLoading = authUiState.isLoading,
                isSmallScreen = isSmallScreen
            )

            // Error Message
            if (authUiState.error != null) {
                ModernErrorMessage(
                    message = authUiState.error,
                    isSmallScreen = isSmallScreen
                )
            }
        }
    }
}

/**
 * Modern Google Sign-In button with enhanced styling
 */
@Composable
private fun ModernGoogleSignInButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    isSmallScreen: Boolean
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isSmallScreen) 52.dp else 56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.Brand,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 10.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Google Logo with proper tint and size
                Image(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = stringResource(R.string.auth_google_logo),
                    modifier = Modifier.size(24.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
                )
                
                Text(
                    text = stringResource(R.string.sign_in_with_google),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = if (isSmallScreen) 16.sp else 18.sp
                    ),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Modern error message display
 */
@Composable
private fun ModernErrorMessage(
    message: String,
    isSmallScreen: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.Error.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = stringResource(R.string.cd_error),
                modifier = Modifier.size(20.dp),
                tint = AppColors.Error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = if (isSmallScreen) 14.sp else 16.sp
                ),
                color = AppColors.Error,
                textAlign = TextAlign.Start
            )
        }
    }
}

/**
 * Modern app footer with enhanced design
 */
@Composable
private fun ModernAppFooter(
    dimensions: Dimensions
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Security badges
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SecurityBadge(
                icon = Icons.Default.Verified,
                text = stringResource(R.string.auth_badge_secure),
                color = AppColors.Brand
            )
            SecurityBadge(
                icon = Icons.Default.Security,
                text = stringResource(R.string.auth_badge_private),
                color = AppColors.Info
            )
        }
        
        Text(
            text = stringResource(R.string.auth_footer),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Security badge component
 */
@Composable
private fun SecurityBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(18.dp),
            tint = color
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Handle Google Sign-In result from activity launcher
 */
private fun handleGoogleSignInResult(
    result: androidx.activity.result.ActivityResult,
    authViewModel: AuthViewModel,
    context: Context
) {
    try {
        // Check if this is our dummy intent with an error
        val isDummy = result.data?.action == "ai.sach.libraryup.DUMMY_GOOGLE_SIGN_IN"
        if (isDummy) {
            // Handle Google Play Services errors gracefully
            return
        }
        
        // Normal flow for successful Google Sign-In
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            authViewModel.processGoogleSignIn(task, context)
        }
    } catch (e: Exception) {
        // Handle any unexpected errors
        android.util.Log.e("GoogleAuthScreen", "Error handling Google Sign-In result", e)
    }
}