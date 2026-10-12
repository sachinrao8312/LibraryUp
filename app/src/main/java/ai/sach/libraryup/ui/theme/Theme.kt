package ai.sach.libraryup.ui.theme

import ai.sach.libraryup.ui.screens.settingsScreen.appprefernecs.AppPreferencesRepository
import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat

/**
 * Creates a dark color scheme with the provided primary color.
 *
 * @param primaryColor The primary color to use in the color scheme
 * @return A dark color scheme with the customized primary color
 */
private fun createDarkColorScheme(primaryColor: Color): ColorScheme {
    return darkColorScheme(
        primary = primaryColor,
        onPrimary = Color.White,
        primaryContainer = primaryColor.copy(alpha = 0.2f),
        onPrimaryContainer = Color.White,
        secondary = primaryColor.copy(alpha = 0.8f),
        onSecondary = Color.White,
        secondaryContainer = primaryColor.copy(alpha = 0.15f),
        onSecondaryContainer = Color.White,
        tertiary = primaryColor.copy(alpha = 0.6f),
        onTertiary = Color.White,
        tertiaryContainer = primaryColor.copy(alpha = 0.1f),
        onTertiaryContainer = Color.White,
        background = Color(0xFF0A0A0A),
        onBackground = Color.White,
        surface = Color(0xFF1A1A1A),
        onSurface = Color.White,
        surfaceVariant = Color(0xFF2A2A2A),
        onSurfaceVariant = Color.White.copy(alpha = 0.8f),
        outline = Color.White.copy(alpha = 0.2f),
        outlineVariant = Color.White.copy(alpha = 0.1f),
        error = Color(0xFFF44336),
        onError = Color.White,
        errorContainer = Color(0xFFB71C1C),
        onErrorContainer = Color.White,
        inverseSurface = Color.White,
        inverseOnSurface = Color.Black,
        inversePrimary = primaryColor.copy(alpha = 0.8f),
        scrim = Color.Black.copy(alpha = 0.32f)
    )
}

/**
 * Creates a light color scheme with the provided primary color.
 *
 * @param primaryColor The primary color to use in the color scheme
 * @return A light color scheme with the customized primary color
 */
private fun createLightColorScheme(primaryColor: Color): ColorScheme {
    return lightColorScheme(
        primary = primaryColor,
        onPrimary = Color.White,
        primaryContainer = primaryColor.copy(alpha = 0.12f),
        onPrimaryContainer = primaryColor.copy(alpha = 0.9f),
        secondary = primaryColor.copy(alpha = 0.8f),
        onSecondary = Color.White,
        secondaryContainer = primaryColor.copy(alpha = 0.1f),
        onSecondaryContainer = primaryColor.copy(alpha = 0.8f),
        tertiary = primaryColor.copy(alpha = 0.6f),
        onTertiary = Color.White,
        tertiaryContainer = primaryColor.copy(alpha = 0.08f),
        onTertiaryContainer = primaryColor.copy(alpha = 0.7f),
        background = Color(0xFFFAFAFA),
        onBackground = Color(0xFF1C1B1F),
        surface = Color.White,
        onSurface = Color(0xFF1C1B1F),
        surfaceVariant = Color(0xFFF3F4F6),
        onSurfaceVariant = Color(0xFF49454F),
        outline = Color(0xFF79747E),
        outlineVariant = Color(0xFFCAC4D0),
        error = Color(0xFFD32F2F),
        onError = Color.White,
        errorContainer = Color(0xFFFFEBEE),
        onErrorContainer = Color(0xFFB71C1C),
        inverseSurface = Color(0xFF313033),
        inverseOnSurface = Color(0xFFF4EFF4),
        inversePrimary = primaryColor.copy(alpha = 0.8f),
        scrim = Color.Black.copy(alpha = 0.32f)
    )
}

/**
 * RentUp theme that applies the app's color schemes, typography, and shapes.
 * Respects the system's dark/light theme settings and user's theme color preference.
 *
 * @param darkTheme Whether to use dark theme
 * @param dynamicColor Whether to use dynamic color adaptation on Android 12+
 * @param content The content to be styled with this theme
 */
@Composable
fun LibraryUpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Get current density
    val density = LocalDensity.current

    // Create fixed density that ignores system font scaling
    val fixedDensity = Density(
        density = density.density,
        fontScale = 1.0f // Force 1.0 scale
    )

    // Get the context
    val context = LocalContext.current

    // Get the user's theme color preference
    val appPreferencesRepository = AppPreferencesRepository(context)
    val primaryColorHex by appPreferencesRepository.primaryColor
        .collectAsState(initial = AppPreferencesRepository.DEFAULT_PRIMARY_COLOR)

    // Parse the color hex string to a Color object
    val primaryColor = try {
        Color(android.graphics.Color.parseColor(primaryColorHex))
    } catch (e: Exception) {
        AppColors.PrimaryGreen
    }

    // Dynamic color is available on Android 12+
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> createDarkColorScheme(primaryColor)
        else -> createLightColorScheme(primaryColor)
    }

    // Apply theme to system UI (status bar, navigation bar)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Apply the status bar appearance using WindowCompat for edge-to-edge
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
            
            // Set status bar and navigation bar colors to transparent for edge-to-edge design
            // Note: These properties are deprecated but still functional for edge-to-edge support
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
        }
    }

    // Apply fixed density to the theme
    CompositionLocalProvider(
        LocalDensity provides fixedDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography, // Use our enhanced typography
            shapes = Shapes, // Use our custom shapes
            content = content
        )
    }
}