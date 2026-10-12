package ai.sach.libraryup.ui.screens.authentication

import ai.sach.libraryup.BuildConfig
import ai.sach.libraryup.R
import ai.sach.libraryup.ui.theme.AppColors
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.material3.Icon
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * An enhanced splash screen that displays when the app is first launched.
 * Features multiple animations including logo scaling, text fade-in, and gradient background.
 *
 * @param onSplashFinished Callback triggered when the splash screen animation completes
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // States for controlling animations
    var startLogoAnimation by remember { mutableStateOf(false) }
    var showAppName by remember { mutableStateOf(false) }
    var showTagline by remember { mutableStateOf(false) }

    // Animated values
    val logoScale by animateFloatAsState(
        targetValue = if (startLogoAnimation) 1f else 0.6f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "Logo Scale Animation"
    )

    val logoAlpha by animateFloatAsState(
        targetValue = if (startLogoAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "Logo Alpha Animation"
    )

    // Create a gradient background based on theme
    val isDarkTheme = isSystemInDarkTheme()
    val gradientColors = if (isDarkTheme) {
        listOf(
            Color(0xFF121212),
            Color(0xFF1E1E1E),
            Color(0xFF252525)
        )
    } else {
        listOf(
            Color(0xFFF8F8F8),
            Color(0xFFFFFFFF),
            Color(0xFFE8F5E9) // Light green tint at bottom
        )
    }

    // Animation sequence
    LaunchedEffect(key1 = true) {
        startLogoAnimation = true
        delay(600)
        showAppName = true
        delay(400)
        showTagline = true
        delay(1200)
        onSplashFinished()
    }

    // Splash screen content with animations
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = gradientColors)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // App logo with scaling and fade-in animations
            Box(
                modifier = Modifier
                    .scale(logoScale)
                    .alpha(logoAlpha)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer light green circle
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    AppColors.PrimaryGreen.copy(alpha = 0.3f),
                                    AppColors.PrimaryGreen.copy(alpha = 0.1f),
                                    AppColors.PrimaryGreen.copy(alpha = 0.05f)
                                )
                            )
                        )
                )

                // Logo shadow/glow effect
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                )
                            )
                        )
                )

                // Main logo circle
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    AppColors.PrimaryGreen,
                                    AppColors.PrimaryGreen.copy(alpha = 0.8f)
                                )
                            )
                        )
                        .shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            spotColor = AppColors.PrimaryGreen.copy(alpha = 0.3f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Use the original logo with 2x zoom to crop out rectangular edges
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "RentUp Logo",
                        modifier = Modifier
                            .size(187.dp)
                            .padding(0.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp)) // Increased by 25% from 24dp

            // App name with fade-in and slide-up animation
            AnimatedVisibility(
                visible = showAppName,
                enter = fadeIn(animationSpec = tween(700)) +
                        slideInVertically(
                            initialOffsetY = { 50 },
                            animationSpec = tween(600, easing = FastOutSlowInEasing)
                        )
            ) {
                Text(
                    text = "RentUp",
                    fontSize = 40.sp, // Increased by 25% from 32sp
                    fontWeight = FontWeight.Bold,
                    color = AppColors.PrimaryGreen,
                    style = MaterialTheme.typography.headlineLarge
                )
            }

            Spacer(modifier = Modifier.height(15.dp)) // Increased by 25% from 12dp

            // App tagline with fade-in and slide-up animation
            AnimatedVisibility(
                visible = showTagline,
                enter = fadeIn(animationSpec = tween(700)) +
                        slideInVertically(
                            initialOffsetY = { 30 },
                            animationSpec = tween(500, easing = FastOutSlowInEasing)
                        )
            ) {
                Text(
                    text = "Simplify Rent Management",
                    fontSize = 20.sp, // Increased by 25% from 16sp
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.8f)
                    else Color.Black.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.6.sp, // Slightly increased letterSpacing
                    modifier = Modifier.padding(horizontal = 40.dp) // Increased horizontal padding
                )
            }

            // Version text at bottom with lower opacity
            AnimatedVisibility(
                visible = showTagline,
                enter = fadeIn(animationSpec = tween(1000))
            ) {
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME}",
                    fontSize = 15.sp, // Increased by 25% from 12sp
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 60.dp) // Increased padding by 25%
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    Surface {
        SplashScreen {}
    }
}