package ai.sach.rentup.ui.common.responsiveness

import ai.sach.rentup.ui.common.composables.WindowSizeClass
import ai.sach.rentup.ui.common.composables.rememberWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Responsive dimensions helper class that provides dimension values that scale
 * based on the screen size.
 *
 * This is a data class to make it immutable and more efficient with recompositions.
 */
@Immutable
data class Dimensions(
    // Screen size class
    val windowSizeClass: WindowSizeClass,
    val isLandscape: Boolean,

    // Padding values
    val paddingSmall: Dp,
    val paddingMedium: Dp,
    val paddingLarge: Dp,

    // Spacing values
    val spacingTiny: Dp,
    val spacingSmall: Dp,
    val spacingMedium: Dp,
    val spacingLarge: Dp,

    // Avatar sizes
    val avatarSizeSmall: Dp,
    val avatarSizeMedium: Dp,
    val avatarSizeLarge: Dp,

    // Font sizes
    val fontSizeSmall: TextUnit,
    val fontSizeMedium: TextUnit,
    val fontSizeLarge: TextUnit,
    val fontSizeXLarge: TextUnit,

    // Card elevations
    val cardElevationSmall: Dp,
    val cardElevationMedium: Dp,
    val cardElevationLarge: Dp,

    // Border radius values
    val radiusSmall: Dp,
    val radiusMedium: Dp,
    val radiusLarge: Dp,

    // Card border width
    val cardBorderWidth: Dp,

    // Icon sizes
    val iconSizeSmall: Dp,
    val iconSizeMedium: Dp,
    val iconSizeLarge: Dp,

    // Button sizes
    val buttonHeight: Dp,
    val buttonHeightSmall: Dp,

    // List/column widths
    val maxContentWidthCompact: Dp,
    val maxContentWidthMedium: Dp,
    val maxContentWidthExpanded: Dp,

    // Grid cell minimum size
    val gridCellMinWidth: Dp,

    // Maximum height for scrollable items
    val maxListHeight: Dp,

    // Computed property
    val contentWidth: Dp
)

/**
 * CompositionLocal to provide dimensions throughout the app
 */
val LocalDimensions = compositionLocalOf {
    // Default dimensions for when the LocalDimensions is accessed outside the DimensionsProvider
    createDimensions(WindowSizeClass(600.dp), false)
}

/**
 * Factory function to create a Dimensions object based on window size and orientation
 */
private fun createDimensions(windowSizeClass: WindowSizeClass, isLandscape: Boolean): Dimensions {
    // Padding values that adapt to screen size
    val paddingSmall = when {
        windowSizeClass.isExpandedWidth -> 12.dp
        windowSizeClass.isMediumWidth -> 8.dp
        else -> 4.dp
    }

    val paddingMedium = when {
        windowSizeClass.isExpandedWidth -> 24.dp
        windowSizeClass.isMediumWidth -> 16.dp
        else -> 12.dp
    }

    val paddingLarge = when {
        windowSizeClass.isExpandedWidth -> 32.dp
        windowSizeClass.isMediumWidth -> 24.dp
        else -> 16.dp
    }

    // Spacing values for consistent layout gaps
    val spacingTiny = when {
        windowSizeClass.isExpandedWidth -> 6.dp
        else -> 4.dp
    }

    val spacingSmall = when {
        windowSizeClass.isExpandedWidth -> 10.dp
        windowSizeClass.isMediumWidth -> 8.dp
        else -> 6.dp
    }

    val spacingMedium = when {
        windowSizeClass.isExpandedWidth -> 16.dp
        windowSizeClass.isMediumWidth -> 12.dp
        else -> 8.dp
    }

    val spacingLarge = when {
        windowSizeClass.isExpandedWidth -> 24.dp
        windowSizeClass.isMediumWidth -> 18.dp
        else -> 12.dp
    }

    // Component size scales
    val avatarSizeSmall = when {
        windowSizeClass.isExpandedWidth -> 60.dp
        windowSizeClass.isMediumWidth -> 50.dp
        else -> 40.dp
    }

    val avatarSizeMedium = when {
        windowSizeClass.isExpandedWidth -> 80.dp
        windowSizeClass.isMediumWidth -> 65.dp
        else -> 50.dp
    }

    val avatarSizeLarge = when {
        windowSizeClass.isExpandedWidth -> 100.dp
        windowSizeClass.isMediumWidth -> 80.dp
        else -> 60.dp
    }

    // Font sizes that adapt to screen size
    val fontSizeSmall = when {
        windowSizeClass.isExpandedWidth -> 14.sp
        else -> 12.sp
    }

    val fontSizeMedium = when {
        windowSizeClass.isExpandedWidth -> 16.sp
        else -> 14.sp
    }

    val fontSizeLarge = when {
        windowSizeClass.isExpandedWidth -> 22.sp
        windowSizeClass.isMediumWidth -> 18.sp
        else -> 16.sp
    }

    val fontSizeXLarge = when {
        windowSizeClass.isExpandedWidth -> 28.sp
        windowSizeClass.isMediumWidth -> 24.sp
        else -> 20.sp
    }

    // Card elevation that adapts to screen size
    val cardElevationSmall = when {
        windowSizeClass.isExpandedWidth -> 3.dp
        else -> 2.dp
    }

    val cardElevationMedium = when {
        windowSizeClass.isExpandedWidth -> 5.dp
        else -> 4.dp
    }

    val cardElevationLarge = when {
        windowSizeClass.isExpandedWidth -> 8.dp
        windowSizeClass.isMediumWidth -> 6.dp
        else -> 4.dp
    }

    // Border radius values
    val radiusSmall = when {
        windowSizeClass.isExpandedWidth -> 8.dp
        else -> 6.dp
    }

    val radiusMedium = when {
        windowSizeClass.isExpandedWidth -> 14.dp
        windowSizeClass.isMediumWidth -> 12.dp
        else -> 8.dp
    }

    val radiusLarge = when {
        windowSizeClass.isExpandedWidth -> 20.dp
        windowSizeClass.isMediumWidth -> 16.dp
        else -> 12.dp
    }

    // Card border width
    val cardBorderWidth = when {
        windowSizeClass.isExpandedWidth -> 2.dp
        else -> 1.dp
    }

    // Icon sizes
    val iconSizeSmall = when {
        windowSizeClass.isExpandedWidth -> 18.dp
        else -> 16.dp
    }

    val iconSizeMedium = when {
        windowSizeClass.isExpandedWidth -> 24.dp
        windowSizeClass.isMediumWidth -> 22.dp
        else -> 20.dp
    }

    val iconSizeLarge = when {
        windowSizeClass.isExpandedWidth -> 32.dp
        windowSizeClass.isMediumWidth -> 28.dp
        else -> 24.dp
    }

    // Button sizes
    val buttonHeight = when {
        windowSizeClass.isExpandedWidth -> 54.dp
        windowSizeClass.isMediumWidth -> 48.dp
        else -> 42.dp
    }

    val buttonHeightSmall = when {
        windowSizeClass.isExpandedWidth -> 44.dp
        windowSizeClass.isMediumWidth -> 38.dp
        else -> 32.dp
    }

    // Column/list widths
    val maxContentWidthCompact = 600.dp
    val maxContentWidthMedium = 840.dp
    val maxContentWidthExpanded = 1200.dp

    // Grid cell minimum size
    val gridCellMinWidth = when {
        windowSizeClass.isExpandedWidth -> 320.dp
        windowSizeClass.isMediumWidth -> 280.dp
        else -> 240.dp
    }

    // Maximum height for scrollable items
    val maxListHeight = when {
        windowSizeClass.isExpandedWidth -> 400.dp
        windowSizeClass.isMediumWidth -> 300.dp
        else -> 240.dp
    }

    // Get appropriate content width based on window size class
    val contentWidth = when {
        windowSizeClass.isExpandedWidth -> maxContentWidthExpanded
        windowSizeClass.isMediumWidth -> maxContentWidthMedium
        else -> maxContentWidthCompact
    }

    return Dimensions(
        windowSizeClass = windowSizeClass,
        isLandscape = isLandscape,
        paddingSmall = paddingSmall,
        paddingMedium = paddingMedium,
        paddingLarge = paddingLarge,
        spacingTiny = spacingTiny,
        spacingSmall = spacingSmall,
        spacingMedium = spacingMedium,
        spacingLarge = spacingLarge,
        avatarSizeSmall = avatarSizeSmall,
        avatarSizeMedium = avatarSizeMedium,
        avatarSizeLarge = avatarSizeLarge,
        fontSizeSmall = fontSizeSmall,
        fontSizeMedium = fontSizeMedium,
        fontSizeLarge = fontSizeLarge,
        fontSizeXLarge = fontSizeXLarge,
        cardElevationSmall = cardElevationSmall,
        cardElevationMedium = cardElevationMedium,
        cardElevationLarge = cardElevationLarge,
        radiusSmall = radiusSmall,
        radiusMedium = radiusMedium,
        radiusLarge = radiusLarge,
        cardBorderWidth = cardBorderWidth,
        iconSizeSmall = iconSizeSmall,
        iconSizeMedium = iconSizeMedium,
        iconSizeLarge = iconSizeLarge,
        buttonHeight = buttonHeight,
        buttonHeightSmall = buttonHeightSmall,
        maxContentWidthCompact = maxContentWidthCompact,
        maxContentWidthMedium = maxContentWidthMedium,
        maxContentWidthExpanded = maxContentWidthExpanded,
        gridCellMinWidth = gridCellMinWidth,
        maxListHeight = maxListHeight,
        contentWidth = contentWidth
    )
}

/**
 * Composable to provide dimension values that adjust to screen size.
 * This should be called near the top of your app's composition.
 */
@Composable
fun DimensionsProvider(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val windowSizeClass = rememberWindowSizeClass()
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Create dimensions instance based on current screen size
    val dimensions = remember(windowSizeClass, isLandscape) {
        createDimensions(windowSizeClass, isLandscape)
    }

    // Provide the dimensions to the entire composition
    CompositionLocalProvider(LocalDimensions provides dimensions) {
        content()
    }
}

/**
 * Composable helper to get dimensions in the composition
 */
@Composable
fun rememberDimensions(): Dimensions {
    return LocalDimensions.current
}