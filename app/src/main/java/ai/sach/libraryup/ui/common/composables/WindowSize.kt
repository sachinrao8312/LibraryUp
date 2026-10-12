package ai.sach.libraryup.ui.common.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Window width class for responsive layouts. */
class WindowSizeClass(private val windowWidth: Dp) {
    val isCompactWidth: Boolean get() = windowWidth < 600.dp
    val isMediumWidth: Boolean get() = windowWidth >= 600.dp && windowWidth < 840.dp
    val isExpandedWidth: Boolean get() = windowWidth >= 840.dp
}

@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val width = LocalConfiguration.current.screenWidthDp.dp
    return remember(width) { WindowSizeClass(width) }
}
