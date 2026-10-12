package ai.sach.libraryup.ui.screens.authentication
import ai.sach.libraryup.ui.theme.AppColors
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.delay
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.MutableState

/**
 * Enhanced Biometric Lock Screen that can be used both on app startup and in the security settings.
 *
 * @param activity FragmentActivity required for biometric prompt
 * @param biometricAuthManager BiometricAuthManager instance or null (will be created if null)
 * @param onAuthenticationSuccess Callback for successful authentication
 * @param onSkipAuthentication Callback for when user skips authentication
 * @param showSkipOption Whether to show the skip option button
 * @param showPinOption Whether to show the PIN option button
 * @param onUsePinInstead Callback for when user wants to use PIN instead
 * @param title Custom title for the screen
 */
@Composable
fun BiometricLockScreen(
    activity: FragmentActivity,
    biometricAuthManager: BiometricAuthManager? = null,
    onAuthenticationSuccess: () -> Unit,
    onSkipAuthentication: () -> Unit = {},
    showSkipOption: Boolean = false,
    showPinOption: Boolean = true,
    onUsePinInstead: (() -> Unit)? = null,
    title: String = "RentUp"
) {
    val context = LocalContext.current
    val authManager = biometricAuthManager ?: remember { BiometricAuthManager(context) }

    data class BiometricState(
        val errorMessage: String? = null,
        val isAuthenticating: Boolean = false,
        val showPinOption: Boolean = false,
        val authAttempts: Int = 0,
        val showLoadingAnimation: Boolean = false,
        val hasTriggered: Boolean = false
    )
    val state: MutableState<BiometricState> = remember { mutableStateOf(BiometricState()) }

    val isAvailable by authManager.isBiometricAvailable.collectAsState()
    val isEnabled by authManager.isBiometricEnabled.collectAsState()

    // Helper to trigger authentication only once
    fun triggerAuthenticationOnce() {
        if (!state.value.hasTriggered && isEnabled) {
            state.value = state.value.copy(hasTriggered = true)
            if (isAvailable && !state.value.isAuthenticating) {
                state.value = state.value.copy(isAuthenticating = true, showLoadingAnimation = true)
                authManager.showBiometricPrompt(
                    activity = activity,
                    title = "Verify your identity",
                    subtitle = "Use your fingerprint to unlock RentUp",
                    onSuccess = {
                        state.value = state.value.copy(isAuthenticating = false, showLoadingAnimation = false)
                        onAuthenticationSuccess()
                    },
                    onError = { error ->
                        state.value = state.value.copy(
                            isAuthenticating = false,
                            showLoadingAnimation = false,
                            errorMessage = error,
                            authAttempts = state.value.authAttempts + 1,
                            showPinOption = state.value.authAttempts + 1 >= 2
                        )
                    },
                    onCancel = {
                        state.value = state.value.copy(
                            isAuthenticating = false,
                            showLoadingAnimation = false,
                            showPinOption = true
                        )
                    }
                )
            } else if (!isAvailable) {
                state.value = state.value.copy(errorMessage = "Biometric authentication is not available on this device", showPinOption = true)
            } else if (!isEnabled) {
                onSkipAuthentication()
            }
        }
    }

    // Trigger authentication only once per screen entry
    LaunchedEffect(isEnabled) {
        triggerAuthenticationOnce()
    }

    // Main lock screen UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // App logo or title
            Text(
                text = title,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.PrimaryGreen
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Biometric authentication section
            if (isEnabled && isAvailable) {
                // Fingerprint icon with animation
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            color = if (state.value.errorMessage == null)
                                AppColors.PrimaryGreen.copy(alpha = 0.1f)
                            else
                                MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        )
                        .clickable(enabled = !state.value.isAuthenticating) { triggerAuthenticationOnce() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Fingerprint",
                        tint = if (state.value.errorMessage == null)
                            AppColors.PrimaryGreen
                        else
                            MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )

                    // Show loading animation when authenticating
                    if (state.value.showLoadingAnimation) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Circular progress indicator around the icon
                            CircularLoadingIndicator(
                                color = AppColors.PrimaryGreen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Instruction text
            Text(
                text = if (state.value.errorMessage == null)
                    "Touch the fingerprint sensor to unlock"
                else
                    "Authentication failed. Try again or use another method.",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                color = if (state.value.errorMessage == null)
                    MaterialTheme.colorScheme.onBackground
                else
                    MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error message if any
            state.value.errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Progress indicator for authenticating state
            if (state.value.isAuthenticating) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .padding(vertical = 8.dp),
                    color = AppColors.PrimaryGreen
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // PIN option button (shown when biometric fails or is cancelled)
            if (state.value.showPinOption && onUsePinInstead != null) {
                OutlinedButton(
                    onClick = onUsePinInstead,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Use PIN Instead",
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Retry button
            if (state.value.errorMessage != null) {
                Button(
                    onClick = { state.value = state.value.copy(errorMessage = null); triggerAuthenticationOnce() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.PrimaryGreen
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Try Again",
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Skip button
            if (showSkipOption) {
                TextButton(
                    onClick = onSkipAuthentication,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Skip for Now",
                        color = AppColors.PrimaryGreen,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

/**
 * A circular loading indicator for biometric authentication
 */
@Composable
fun CircularLoadingIndicator(
    color: Color = AppColors.PrimaryGreen
) {
    Box(
        modifier = Modifier
            .size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            modifier = Modifier.size(110.dp),
            color = color,
            strokeWidth = 2.dp
        )
    }
}