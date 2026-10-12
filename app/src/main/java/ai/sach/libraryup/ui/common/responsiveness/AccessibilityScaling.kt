package ai.sach.libraryup.ui.common.responsiveness

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Data class to hold accessibility scaling information with limits
 * to prevent UI breakage with very large font sizes
 */
data class AccessibilityScaling(
    val fontScale: Float,
    val contentScale: Float,
    val isLargeFont: Boolean,
    val isDarkMode: Boolean
)

// CompositionLocal to provide accessibility scaling information throughout the app
val LocalAccessibilityScaling = compositionLocalOf {
    AccessibilityScaling(
        fontScale = 1.0f,
        contentScale = 1.0f,
        isLargeFont = false,
        isDarkMode = false
    )
}

/**
 * Detects and provides system accessibility settings like font scale
 * with limits to prevent UI breakage
 */
// Modify AccessibilityScalingProvider.kt
@Composable
fun AccessibilityScalingProvider(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current

    // FIXED VALUES: Ignore system settings completely
    val fixedFontScale = 1.0f // Always use 1.0 scale
    val fixedContentScale = 1.0f // Always use 1.0 scale

    // Create our accessibility scaling object with fixed values
    val accessibilityScaling = remember {
        AccessibilityScaling(
            fontScale = fixedFontScale,
            contentScale = fixedContentScale,
            isLargeFont = false, // Always false
            isDarkMode = isInDarkMode(context) // Only keep dark mode detection
        )
    }

    // Create a fixed density that ignores system font scaling
    val density = LocalDensity.current
    val scaledDensity = remember(density) {
        Density(
            density = density.density, // Keep screen density
            fontScale = fixedFontScale // Force 1.0 font scale
        )
    }

    // Provide the fixed values to the composition
    CompositionLocalProvider(
        LocalAccessibilityScaling provides accessibilityScaling,
        LocalDensity provides scaledDensity
    ) {
        Box(modifier = modifier, content = content)
    }
}

/**
 * Check if the device is in dark mode
 */
private fun isInDarkMode(context: Context): Boolean {
    return when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
        Configuration.UI_MODE_NIGHT_YES -> true
        else -> false
    }
}