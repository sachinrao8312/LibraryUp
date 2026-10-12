package ai.sach.libraryup.ui.screens.settingsScreen.appprefernecs

import ai.sach.libraryup.ui.common.composables.AppTopAppBar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch

/**
 * App Preferences Screen that allows users to customize the appearance and behavior of the app.
 * Currently supports theme color selection and country/currency settings.
 *
 * @param navController Navigation controller for handling screen navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPreferencesScreen(
    navController: NavController,
    viewModel: AppPreferencesViewModel = viewModel(factory = AppPreferencesViewModelFactory(LocalContext.current))
) {
    // Get current theme preferences
    val isDarkTheme = isSystemInDarkTheme()
    val primaryColorHex by viewModel.primaryColor.collectAsState(initial = AppPreferencesRepository.DEFAULT_PRIMARY_COLOR)
    val showProperties by viewModel.showProperties.collectAsState(initial = AppPreferencesRepository.DEFAULT_SHOW_PROPERTIES)
    val currentCurrency by viewModel.currency.collectAsState(initial = AppPreferencesRepository.DEFAULT_CURRENCY)
    val currentCountry by viewModel.country.collectAsState(initial = AppPreferencesRepository.DEFAULT_COUNTRY)

    // State for color picker dialog
    var showColorPicker by remember { mutableStateOf(false) }
    var showCurrencyPicker by remember { mutableStateOf(false) }

    // Set text and background colors based on theme
    val backgroundColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFF8F9FA)
    val cardBackgroundColor = if (isDarkTheme) Color(0xFF1F2937) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val secondaryTextColor = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Gray

    // Parse current color
    val primaryColor = try {
        Color(android.graphics.Color.parseColor(primaryColorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    // Coroutine scope for saving preferences
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            AppTopAppBar(
                title = "App Preferences",
                showBackButton = true,
                onBackPressed = { navController.popBackStack() },
                showDefaultMenu = true,
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header text
            Text(
                text = "Customize how the app looks and behaves",
                modifier = Modifier.padding(16.dp),
                color = secondaryTextColor,
                fontSize = 16.sp
            )

            // Theme Color Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Theme Color",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Current color display
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showColorPicker = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Color preview
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(primaryColor)
                                .border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // Color name
                        Text(
                            text = getColorName(primaryColorHex),
                            fontSize = 16.sp,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Select Color",
                            tint = secondaryTextColor
                        )
                    }
                }
            }

            // Country & Currency Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Country & Currency",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Currency selector
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDarkTheme) Color(0xFF2A2A2A) else Color(0xFFF5F5F5))
                            .clickable { showCurrencyPicker = true }
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Country flag
                            getFlagEmoji(currentCountry)?.let { flag ->
                                Text(
                                    text = flag,
                                    fontSize = 20.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = getCountryName(currentCountry),
                                    color = textColor,
                                    fontWeight = FontWeight.Medium
                                )

                                Text(
                                    text = "$currentCurrency ${getCurrencyName(currentCurrency)}",
                                    color = secondaryTextColor,
                                    fontSize = 14.sp
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Select Currency",
                                tint = secondaryTextColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Changes currency symbol throughout the app",
                        color = secondaryTextColor,
                        fontSize = 14.sp
                    )
                }
            }

            // Additional UI Settings
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Additional Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Show Properties toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Show Properties Screen",
                                color = textColor,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )

                            Text(
                                text = "Display Properties tab in navigation bar",
                                color = secondaryTextColor,
                                fontSize = 14.sp
                            )
                        }

                        Switch(
                            checked = showProperties,
                            onCheckedChange = {
                                scope.launch {
                                    viewModel.setShowProperties(it)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = primaryColor,
                                uncheckedThumbColor = Color.White
                            )
                        )
                    }
                }
            }
        }
    }

    // Color Picker Dialog
    if (showColorPicker) {
        ColorPickerDialog(
            onDismiss = { showColorPicker = false },
            onColorSelected = { colorHex ->
                scope.launch {
                    viewModel.setPrimaryColor(colorHex)
                }
            },
            currentColorHex = primaryColorHex,
        )
    }

    // Currency Picker
    if (showCurrencyPicker) {
        CurrencyPickerDialog(
            onDismiss = { showCurrencyPicker = false },
            onCurrencySelected = { country, currency ->
                scope.launch {
                    viewModel.setCountry(country)
                    viewModel.setCurrency(currency)
                }
            },
            currentCountry = currentCountry,
            currentCurrency = currentCurrency,
            isDarkTheme = isDarkTheme
        )
    }
}

/**
 * Component for displaying and selecting a theme color.
 *
 * @param currentColor Current selected color hex string
 * @param onColorSelected Callback when a new color is selected
 * @param isDarkTheme Whether the app is in dark mode
 */
@Composable
fun ColorPickerDialog(
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit,
    currentColorHex: String,
) {
    var selectedBaseColor by remember { mutableStateOf(getBaseColor(currentColorHex)) }
    var selectedShadeColorHex by remember { mutableStateOf(currentColorHex) }
    var showingShades by remember { mutableStateOf(false) }

    // Define base colors (main hues)
    val baseColors = remember {
        listOf(
            // Row 1
            "#FF3B30", "#FF2D55", "#AF52DE", "#5856D6", "#007AFF",
            // Row 2
            "#00C7BE", "#32ADE6", "#28CD41", "#8BC34A", "#CDDC39",
            // Row 3
            "#FFCC00", "#FF9500", "#FF7D0A", "#E64A19", "#A0522D",
            // Row 4
            "#A9A9A9", "#607D8B"
        )
    }

    // Generate shades for selected base color
    val colorShades = remember(selectedBaseColor) {
        generateColorShadesForIOS(selectedBaseColor)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1C1C1E) // iOS-style dark background
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Color grid
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Column {
                        AnimatedVisibility(
                            visible = showingShades,
                            enter = fadeIn() + slideInHorizontally { it },
                            exit = fadeOut() + slideOutHorizontally { it }
                        ) {
                            // Back button for shade view
                            Box(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .align(Alignment.Start)
                                    .clickable { showingShades = false }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Colors",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Grid layout for colors
                        val colorsToShow = if (showingShades) colorShades else baseColors
                        val columns = if (showingShades) 5 else 5 // Adjust grid columns based on view

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(colorsToShow) { colorHex ->
                                val isSelected = if (showingShades) {
                                    colorHex == selectedShadeColorHex
                                } else {
                                    colorHex == selectedBaseColor
                                }

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(colorHex)))
                                        .clickable {
                                            if (showingShades) {
                                                selectedShadeColorHex = colorHex
                                            } else {
                                                selectedBaseColor = colorHex
                                                selectedShadeColorHex = colorHex
                                                showingShades = true
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = { onDismiss() }
                    ) {
                        Text("Cancel", color = Color.White)
                    }

                    TextButton(
                        onClick = {
                            onColorSelected(selectedShadeColorHex)
                            onDismiss()
                        }
                    ) {
                        Text("Done", color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Generate iOS-style color shades - more tints and shades per color.
 */
fun generateColorShadesForIOS(baseColorHex: String): List<String> {
    try {
        val color = android.graphics.Color.parseColor(baseColorHex)
        val red = android.graphics.Color.red(color)
        val green = android.graphics.Color.green(color)
        val blue = android.graphics.Color.blue(color)

        val hsl = FloatArray(3)
        android.graphics.Color.RGBToHSV(red, green, blue, hsl)

        // Get base HSV values
        val hue = hsl[0]
        val saturation = hsl[1]
        val value = hsl[2]

        // Create an array to hold our color variants
        val colorShades = mutableListOf<String>()

        // Add white to very light tints
        colorShades.add("#FFFFFF") // White

        // Light tints - decrease saturation and increase value
        for (i in 9 downTo 5) {
            val factor = i / 10f
            val newSat = saturation * factor
            val newVal = value + (1f - value) * (1 - factor)

            val hsv = floatArrayOf(hue, newSat, newVal)
            val shadeColor = android.graphics.Color.HSVToColor(hsv)
            colorShades.add(String.format("#%06X", 0xFFFFFF and shadeColor))
        }

        // Base color and variations
        colorShades.add(baseColorHex) // Base color

        // Shades - decrease value and maintain saturation
        for (i in 8 downTo 4 step 2) {
            val factor = i / 10f
            val hsv = floatArrayOf(hue, saturation, value * factor)
            val shadeColor = android.graphics.Color.HSVToColor(hsv)
            colorShades.add(String.format("#%06X", 0xFFFFFF and shadeColor))
        }

        return colorShades
    } catch (e: Exception) {
        // Fallback for invalid color
        return listOf(
            "#FFFFFF", "#F5F5F5", "#E0E0E0", "#BDBDBD",
            baseColorHex,
            "#757575", "#616161", "#424242", "#212121"
        )
    }
}

@Composable
fun CurrencyPickerDialog(
    onDismiss: () -> Unit,
    onCurrencySelected: (String, String) -> Unit,
    currentCountry: String,
    currentCurrency: String,
    isDarkTheme: Boolean
) {
    // Background colors based on theme
    val dialogBackgroundColor = if (isDarkTheme) Color(0xFF212121) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val secondaryTextColor = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Gray

    // List of supported countries and currencies
    val supportedCurrencies = remember {
        listOf(
            Triple("IN", "₹", "INR"), // India
            Triple("US", "$", "USD"), // United States
            Triple("GB", "£", "GBP"), // United Kingdom
            Triple("EU", "€", "EUR"), // Euro
            Triple("JP", "¥", "JPY"), // Japan
            Triple("CA", "$", "CAD"), // Canada
            Triple("AU", "$", "AUD")  // Australia
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = dialogBackgroundColor
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Select Currency",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(supportedCurrencies) { (country, symbol, currency) ->
                        val isSelected = country == currentCountry && currency == currentCurrency

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCurrencySelected(country, currency)
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Country flag
                            getFlagEmoji(country)?.let { flag ->
                                Text(
                                    text = flag,
                                    fontSize = 24.sp,
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = getCountryName(country),
                                    color = textColor,
                                    fontWeight = FontWeight.Medium
                                )

                                Text(
                                    text = "$symbol $currency",
                                    color = secondaryTextColor,
                                    fontSize = 14.sp
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper function to get a human-readable name for a color hex value.
 */
fun getColorName(colorHex: String): String {
    // Map common hex values to readable names
    return when (colorHex.uppercase()) {
        "#F44336" -> "Red"
        "#E91E63" -> "Pink"
        "#9C27B0" -> "Purple"
        "#673AB7" -> "Deep Purple"
        "#3F51B5" -> "Indigo"
        "#2196F3" -> "Blue"
        "#03A9F4" -> "Light Blue"
        "#00BCD4" -> "Cyan"
        "#009688" -> "Teal"
        "#4CAF50" -> "Green"
        "#8BC34A" -> "Light Green"
        "#CDDC39" -> "Lime"
        "#FFEB3B" -> "Yellow"
        "#FFC107" -> "Amber"
        "#FF9800" -> "Orange"
        "#FF5722" -> "Deep Orange"
        "#795548" -> "Brown"
        "#9E9E9E" -> "Gray"
        "#607D8B" -> "Blue Gray"
        "#0F9B4B" -> "RentUp Green"
        // Specialized color shades
        "#D32F2F" -> "Red 700"
        "#C2185B" -> "Pink 700"
        "#7B1FA2" -> "Purple 700"
        "#512DA8" -> "Deep Purple 700"
        "#303F9F" -> "Indigo 700"
        "#1976D2" -> "Blue 700"
        "#0288D1" -> "Light Blue 700"
        "#0097A7" -> "Cyan 700"
        "#00796B" -> "Teal 700"
        "#388E3C" -> "Green 700"
        "#689F38" -> "Light Green 700"
        "#AFB42B" -> "Lime 700"
        "#FBC02D" -> "Yellow 700"
        "#FFA000" -> "Amber 700"
        "#F57C00" -> "Orange 700"
        "#E64A19" -> "Deep Orange 700"
        "#5D4037" -> "Brown 700"
        "#616161" -> "Gray 700"
        "#455A64" -> "Blue Gray 700"
        // Fallback
        else -> {
            try {
                val color = Color(android.graphics.Color.parseColor(colorHex))
                getClosestNamedColor(color)
            } catch (e: Exception) {
                "Custom"
            }
        }
    }
}

/**
 * Helper function to get the base color from a color hex value.
 */
fun getBaseColor(colorHex: String): String {
    // Map to base colors
    return when {
        colorHex.startsWith("#F4") || colorHex.startsWith("#E5") -> "#F44336" // Red
        colorHex.startsWith("#E9") || colorHex.startsWith("#D8") -> "#E91E63" // Pink
        colorHex.startsWith("#9C") || colorHex.startsWith("#8E") -> "#9C27B0" // Purple
        colorHex.startsWith("#67") || colorHex.startsWith("#5E") -> "#673AB7" // Deep Purple
        colorHex.startsWith("#3F") || colorHex.startsWith("#35") -> "#3F51B5" // Indigo
        colorHex.startsWith("#21") || colorHex.startsWith("#1E") -> "#2196F3" // Blue
        colorHex.startsWith("#03") || colorHex.startsWith("#02") -> "#03A9F4" // Light Blue
        colorHex.startsWith("#00B") || colorHex.startsWith("#00A") -> "#00BCD4" // Cyan
        colorHex.startsWith("#009") || colorHex.startsWith("#008") -> "#009688" // Teal
        colorHex.startsWith("#4C") || colorHex.startsWith("#43") -> "#4CAF50" // Green
        colorHex.startsWith("#8B") || colorHex.startsWith("#7C") -> "#8BC34A" // Light Green
        colorHex.startsWith("#CD") || colorHex.startsWith("#BA") -> "#CDDC39" // Lime
        colorHex.startsWith("#FF") && colorHex.contains("EB") -> "#FFEB3B" // Yellow
        colorHex.startsWith("#FF") && colorHex.contains("C1") -> "#FFC107" // Amber
        colorHex.startsWith("#FF") && colorHex.contains("98") -> "#FF9800" // Orange
        colorHex.startsWith("#FF") && colorHex.contains("57") -> "#FF5722" // Deep Orange
        colorHex.startsWith("#79") || colorHex.startsWith("#6D") -> "#795548" // Brown
        colorHex.startsWith("#9E") || colorHex.startsWith("#8E") -> "#9E9E9E" // Gray
        colorHex.startsWith("#60") || colorHex.startsWith("#54") -> "#607D8B" // Blue Gray
        colorHex.startsWith("#0F") || colorHex.startsWith("#10") -> "#0F9B4B" // RentUp Green
        else -> "#4CAF50" // Default to Green
    }
}

/**
 * Helper function to get the closest named color for a given color.
 */
fun getClosestNamedColor(color: Color): String {
    val red = (color.red * 255).toInt()
    val green = (color.green * 255).toInt()
    val blue = (color.blue * 255).toInt()

    // Simple heuristic to determine the color name
    return when {
        red > 200 && green < 100 && blue < 100 -> "Red"
        red > 200 && green > 200 && blue < 100 -> "Yellow"
        red < 100 && green > 140 && blue < 100 -> "Green"
        red < 100 && green < 150 && blue > 200 -> "Blue"
        red > 200 && green > 100 && blue < 50 -> "Orange"
        red > 150 && green < 100 && blue > 150 -> "Purple"
        red > 150 && green < 70 && blue < 70 -> "Maroon"
        red < 70 && green > 100 && blue > 100 -> "Teal"
        red > 200 && green > 150 && blue > 200 -> "Pink"
        red < 100 && green < 100 && blue < 100 -> "Black"
        red > 200 && green > 200 && blue > 200 -> "White"
        Math.abs(red - green) < 30 && Math.abs(red - blue) < 30 -> "Gray"
        else -> "Custom"
    }
}

/**
 * Helper function to get an emoji flag for a country code.
 */
fun getFlagEmoji(countryCode: String): String? {
    return try {
        // Country code should be 2 letters
        if (countryCode.length != 2) return null

        val firstLetter = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6
        val secondLetter = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6

        String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
    } catch (e: Exception) {
        null
    }
}

/**
 * Helper function to get a country name from a country code.
 */
fun getCountryName(countryCode: String): String {
    return when (countryCode) {
        "IN" -> "India"
        "US" -> "United States"
        "GB" -> "United Kingdom"
        "EU" -> "European Union"
        "JP" -> "Japan"
        "CA" -> "Canada"
        "AU" -> "Australia"
        else -> "Unknown"
    }
}

/**
 * Helper function to get a currency name from a currency code.
 */
fun getCurrencyName(currencyCode: String): String {
    return when (currencyCode) {
        "INR" -> "Indian Rupee"
        "USD" -> "US Dollar"
        "GBP" -> "British Pound"
        "EUR" -> "Euro"
        "JPY" -> "Japanese Yen"
        "CAD" -> "Canadian Dollar"
        "AUD" -> "Australian Dollar"
        else -> ""
    }
}