package ai.sach.libraryup.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * Material 3 colour scheme generated from the brand seed #283593 (the launcher icon) with
 * Material Color Utilities, tonal-spot variant, 2021 spec: the same output as Material Theme
 * Builder. Regenerate with tools/gen_scheme.py rather than hand-editing.
 */

internal val LightScheme = lightColorScheme(
    primary = Color(0xFF535A92),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDFE0FF),
    onPrimaryContainer = Color(0xFF3B4279),
    inversePrimary = Color(0xFFBCC2FF),
    secondary = Color(0xFF5B5D72),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E0F9),
    onSecondaryContainer = Color(0xFF434559),
    tertiary = Color(0xFF77536C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD7F0),
    onTertiaryContainer = Color(0xFF5E3C54),
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE3E1EC),
    onSurfaceVariant = Color(0xFF46464F),
    surfaceTint = Color(0xFF535A92),
    inverseSurface = Color(0xFF303036),
    inverseOnSurface = Color(0xFFF2EFF7),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    outline = Color(0xFF777680),
    outlineVariant = Color(0xFFC7C5D0),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFBF8FF),
    surfaceContainer = Color(0xFFEFEDF4),
    surfaceContainerHigh = Color(0xFFE9E7EF),
    surfaceContainerHighest = Color(0xFFE4E1E9),
    surfaceContainerLow = Color(0xFFF5F2FA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFDBD9E0),
)

internal val DarkScheme = darkColorScheme(
    primary = Color(0xFFBCC2FF),
    onPrimary = Color(0xFF242B61),
    primaryContainer = Color(0xFF3B4279),
    onPrimaryContainer = Color(0xFFDFE0FF),
    inversePrimary = Color(0xFF535A92),
    secondary = Color(0xFFC4C5DD),
    onSecondary = Color(0xFF2D2F42),
    secondaryContainer = Color(0xFF434559),
    onSecondaryContainer = Color(0xFFE0E0F9),
    tertiary = Color(0xFFE6BAD6),
    onTertiary = Color(0xFF45263D),
    tertiaryContainer = Color(0xFF5E3C54),
    onTertiaryContainer = Color(0xFFFFD7F0),
    background = Color(0xFF131318),
    onBackground = Color(0xFFE4E1E9),
    surface = Color(0xFF131318),
    onSurface = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF46464F),
    onSurfaceVariant = Color(0xFFC7C5D0),
    surfaceTint = Color(0xFFBCC2FF),
    inverseSurface = Color(0xFFE4E1E9),
    inverseOnSurface = Color(0xFF303036),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF90909A),
    outlineVariant = Color(0xFF46464F),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF39393F),
    surfaceContainer = Color(0xFF1F1F25),
    surfaceContainerHigh = Color(0xFF29292F),
    surfaceContainerHighest = Color(0xFF34343A),
    surfaceContainerLow = Color(0xFF1B1B21),
    surfaceContainerLowest = Color(0xFF0D0E13),
    surfaceDim = Color(0xFF131318),
)

/** A Material custom colour: the colour, text on it, its container, and text on the container. */
@Immutable
data class ToneSet(val color: Color, val onColor: Color, val container: Color, val onContainer: Color)

/** Status colours harmonized with the seed (Theme Builder "custom colours"). Errors use colorScheme.error. */
@Immutable
data class StatusColorScheme(val success: ToneSet, val warning: ToneSet)

internal val LightStatus = StatusColorScheme(
    success = ToneSet(Color(0xFF006D43), Color(0xFFFFFFFF), Color(0xFF92F7BC), Color(0xFF002111)),
    warning = ToneSet(Color(0xFF914C00), Color(0xFFFFFFFF), Color(0xFFFFDCC3), Color(0xFF2F1500))
)

internal val DarkStatus = StatusColorScheme(
    success = ToneSet(Color(0xFF76DAA1), Color(0xFF003920), Color(0xFF005231), Color(0xFF92F7BC)),
    warning = ToneSet(Color(0xFFFFB77F), Color(0xFF4E2600), Color(0xFF6E3900), Color(0xFFFFDCC3))
)

val LocalStatusColors = staticCompositionLocalOf { LightStatus }

/** Fixed brand colours for the inherited sign-in screens and shared images (ID card, poster). */
object AppColors {
    val Brand = Color(0xFF535A92)
    val BrandLight = Color(0xFFDFE0FF)
    val BrandDark = Color(0xFF3B4279)

    // Functional colors
    val Success = Color(0xFF22C55E)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)
    val Info = Color(0xFF3B82F6)

    // UI colors - Light Theme
    val BackgroundGray = Color(0xFFF5F5F5)
    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)
    val Gray = Color(0xFF9CA3AF)
    val LightGray = Color(0xFFE5E7EB)
    val LightBackground = Color(0xFFF3F4F6)

    // UI colors - Dark Theme
    val DarkBackground = Color(0xFF121212)
    val DarkSurface = Color(0xFF1F2937)
    val DarkMuted = Color(0xFF374151)
    val DarkBorder = Color(0xFF374151)

    // Red color
    val Red = Color(0xFFDC2626)
}