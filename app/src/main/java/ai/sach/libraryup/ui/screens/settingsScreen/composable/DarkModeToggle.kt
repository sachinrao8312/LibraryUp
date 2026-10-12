package ai.sach.libraryup.ui.screens.settingsScreen.composable

import ai.sach.libraryup.R
import ai.sach.libraryup.ui.theme.AppColors
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A toggleable component for switching between dark and light themes.
 *
 * @param themeViewModel The ViewModel that manages theme state
 */
@Composable
fun DarkModeToggle(themeViewModel: ThemeViewModel) {
    // Collect the current theme state
    val isDarkMode by themeViewModel.isDarkMode.collectAsState()

    // Get appropriate colors based on the current theme
    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val descriptionColor = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Gray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { themeViewModel.toggleDarkMode() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon container with a circular background
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFE0E0FE)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.DarkMode,
                contentDescription = stringResource(R.string.settings_dark_mode),
                tint = Color(0xFF673AB7)
            )
        }

        // Text and description
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_dark_mode),
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = textColor
            )

            Text(
                text = stringResource(R.string.settings_dark_mode_desc),
                color = descriptionColor,
                fontSize = 14.sp
            )
        }

        // Toggle switch
        Switch(
            checked = isDarkMode,
            onCheckedChange = { themeViewModel.setDarkMode(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.LightGray
            )
        )
    }
}