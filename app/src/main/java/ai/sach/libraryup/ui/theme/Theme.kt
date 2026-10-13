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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat

/** Solid tones of the brand colour (no transparency, so cards and chips never look washed out). */
private fun Color.tone(towards: Color, amount: Float) = lerp(this, towards, amount)

private val Amber = Color(0xFFFFB300)

private fun createDarkColorScheme(primary: Color): ColorScheme {
    val bright = primary.tone(Color.White, 0.45f)
    return darkColorScheme(
        primary = bright,
        onPrimary = primary.tone(Color.Black, 0.6f),
        primaryContainer = primary.tone(Color.Black, 0.35f),
        onPrimaryContainer = primary.tone(Color.White, 0.85f),
        secondary = bright,
        secondaryContainer = primary.tone(Color.Black, 0.45f),
        onSecondaryContainer = primary.tone(Color.White, 0.85f),
        tertiary = Amber,
        onTertiary = Color.Black,
        background = Color(0xFF101218),
        onBackground = Color(0xFFE6E8F0),
        surface = Color(0xFF181B23),
        onSurface = Color(0xFFE6E8F0),
        surfaceVariant = Color(0xFF242836),
        onSurfaceVariant = Color(0xFFB9BED0),
        outline = Color(0xFF8A90A6),
        outlineVariant = Color(0xFF353A4C),
        error = Color(0xFFFF8A80),
        onError = Color.Black
    )
}

private fun createLightColorScheme(primary: Color): ColorScheme = lightColorScheme(
    primary = primary,
    onPrimary = Color.White,
    primaryContainer = primary.tone(Color.White, 0.85f),
    onPrimaryContainer = primary.tone(Color.Black, 0.45f),
    secondary = primary,
    onSecondary = Color.White,
    secondaryContainer = primary.tone(Color.White, 0.85f),
    onSecondaryContainer = primary.tone(Color.Black, 0.45f),
    tertiary = Amber,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFFFF0C2),
    onTertiaryContainer = Color(0xFF3D2C00),
    background = Color(0xFFF2F4FA),
    onBackground = Color(0xFF151824),
    surface = Color.White,
    onSurface = Color(0xFF151824),
    surfaceVariant = primary.tone(Color.White, 0.93f),
    onSurfaceVariant = Color(0xFF4A5068),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F8FC),
    surfaceContainer = Color(0xFFF2F4FA),
    surfaceContainerHigh = Color(0xFFECEFF7),
    surfaceContainerHighest = Color(0xFFE6E9F2),
    outline = Color(0xFF767C92),
    outlineVariant = Color(0xFFD5D9E6),
    error = Color(0xFFC62828),
    onError = Color.White,
    errorContainer = Color(0xFFFFE3E0),
    onErrorContainer = Color(0xFF7A0E0E)
)

/**
 * LibraryUp theme that applies the app's color schemes, typography, and shapes.
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
        AppColors.Brand
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