package ai.sach.libraryup.ui.screens.settingsScreen.appprefernecs

/**
 * Data class representing a country option.
 * Contains country name, country code, and flag emoji.
 */
data class CountryOption(
    val name: String,
    val code: String,
    val flag: String,
    val currencyCode: String,
    val currencySymbol: String
)

/**
 * Object providing access to supported countries in the application.
 * Each entry includes the country name, code, flag, and currency information.
 */
object SupportedCountries {
    // List of commonly used countries and their details
    val countries = listOf(
        CountryOption("United States", "US", "🇺🇸", "USD", "$"),
        CountryOption("India", "IN", "🇮🇳", "INR", "₹"),
        CountryOption("United Kingdom", "GB", "🇬🇧", "GBP", "£"),
        CountryOption("European Union", "EU", "🇪🇺", "EUR", "€"),
        CountryOption("Canada", "CA", "🇨🇦", "CAD", "CA$"),
        CountryOption("Australia", "AU", "🇦🇺", "AUD", "A$"),
        CountryOption("Japan", "JP", "🇯🇵", "JPY", "¥"),
        CountryOption("China", "CN", "🇨🇳", "CNY", "¥"),
        CountryOption("Russia", "RU", "🇷🇺", "RUB", "₽"),
        CountryOption("Brazil", "BR", "🇧🇷", "BRL", "R$"),
        CountryOption("South Africa", "ZA", "🇿🇦", "ZAR", "R"),
        CountryOption("Singapore", "SG", "🇸🇬", "SGD", "S$"),
        CountryOption("United Arab Emirates", "AE", "🇦🇪", "AED", "د.إ")
    )

    /**
     * Find a country by its code.
     *
     * @param code ISO country code to search for
     * @return The CountryOption matching the code or the default (US) if not found
     */
    fun findByCode(code: String): CountryOption {
        return countries.find { it.code == code } ?: countries.first()
    }
}

/**
 * Data class representing an app theme color.
 * Contains a name, hex value, and color category.
 */
data class ThemeColorOption(
    val name: String,
    val hexColor: String,
    val colorCategory: ColorCategory
)

/**
 * Enum class representing color categories for organizing theme colors.
 */
enum class ColorCategory {
    RED, PINK, PURPLE, BLUE, TEAL, GREEN, YELLOW, ORANGE, BROWN, GRAY
}

/**
 * Object providing access to supported theme colors in the application.
 */
object SupportedThemeColors {
    // List of color options organized by category
    val colors = listOf(
        // Reds
        ThemeColorOption("Red", "#F44336", ColorCategory.RED),
        ThemeColorOption("Light Red", "#EF5350", ColorCategory.RED),
        ThemeColorOption("Deep Red", "#E53935", ColorCategory.RED),
        
        // Pinks
        ThemeColorOption("Pink", "#E91E63", ColorCategory.PINK),
        ThemeColorOption("Light Pink", "#EC407A", ColorCategory.PINK),
        
        // Purples
        ThemeColorOption("Purple", "#9C27B0", ColorCategory.PURPLE),
        ThemeColorOption("Deep Purple", "#673AB7", ColorCategory.PURPLE),
        
        // Blues
        ThemeColorOption("Blue", "#2196F3", ColorCategory.BLUE),
        ThemeColorOption("Light Blue", "#03A9F4", ColorCategory.BLUE),
        ThemeColorOption("Indigo", "#3F51B5", ColorCategory.BLUE),
        
        // Teals/Cyans
        ThemeColorOption("Teal", "#009688", ColorCategory.TEAL),
        ThemeColorOption("Cyan", "#00BCD4", ColorCategory.TEAL),
        
        // Greens
        ThemeColorOption("Green", "#4CAF50", ColorCategory.GREEN),
        ThemeColorOption("Light Green", "#8BC34A", ColorCategory.GREEN),
        ThemeColorOption("LibraryUp Green", "#0F9B4B", ColorCategory.GREEN),
        
        // Yellows
        ThemeColorOption("Yellow", "#FFEB3B", ColorCategory.YELLOW),
        ThemeColorOption("Amber", "#FFC107", ColorCategory.YELLOW),
        
        // Oranges
        ThemeColorOption("Orange", "#FF9800", ColorCategory.ORANGE),
        ThemeColorOption("Deep Orange", "#FF5722", ColorCategory.ORANGE),
        
        // Browns
        ThemeColorOption("Brown", "#795548", ColorCategory.BROWN),
        
        // Grays
        ThemeColorOption("Gray", "#9E9E9E", ColorCategory.GRAY),
        ThemeColorOption("Blue Gray", "#607D8B", ColorCategory.GRAY)
    )

    /**
     * Find a theme color by its hex value.
     *
     * @param hexColor Hex color value to search for
     * @return The ThemeColorOption matching the hex value or the default (LibraryUp Green) if not found
     */
    fun findByHexColor(hexColor: String): ThemeColorOption {
        return colors.find { it.hexColor.equals(hexColor, ignoreCase = true) } 
               ?: colors.find { it.name == "LibraryUp Green" } 
               ?: colors[0]
    }
    
    /**
     * Get colors of a specific category.
     *
     * @param category The category to filter by
     * @return List of ThemeColorOption in the specified category
     */
    fun getColorsByCategory(category: ColorCategory): List<ThemeColorOption> {
        return colors.filter { it.colorCategory == category }
    }
    
    /**
     * Get all color categories that have at least one color.
     *
     * @return List of ColorCategory that have at least one associated color
     */
    fun getAvailableCategories(): List<ColorCategory> {
        return colors.map { it.colorCategory }.distinct()
    }
}