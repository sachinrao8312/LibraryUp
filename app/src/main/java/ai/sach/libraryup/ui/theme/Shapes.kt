package ai.sach.libraryup.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Modern LibraryUp shape styles defining corner radii for different UI elements.
 * Uses Material 3 design principles for consistent and contemporary appearance.
 */
val Shapes = Shapes(
    // Small components like buttons, chips, and form fields
    small = RoundedCornerShape(8.dp),
    
    // Medium components like cards, dialogs, and bottom sheets
    medium = RoundedCornerShape(12.dp),
    
    // Large components like modals and full-screen dialogs
    large = RoundedCornerShape(16.dp),
    
    // Extra large components for special cases
    extraLarge = RoundedCornerShape(24.dp)
)