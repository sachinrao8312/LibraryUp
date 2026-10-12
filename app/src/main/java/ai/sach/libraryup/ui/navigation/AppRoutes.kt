package ai.sach.rentup.ui.navigation

/**
 * Navigation route strings, kept free of Compose types so non-UI code - notification
 * routing in particular - can target a screen without a typo-prone string literal.
 *
 * [NavigationItems] builds its bottom-bar entries from these, so there is one definition
 * per route and a wrong route name is a compile error rather than a tap that goes nowhere.
 */
object AppRoutes {
    const val HOME = "home"
    const val PROPERTIES = "properties"
    const val PAYMENT_HISTORY = "payment_history"
    const val SETTINGS = "settings"

    /** Property details takes the property id as a path argument. */
    const val PROPERTY_DETAILS_PATTERN = "propertyDetails/{propertyId}"

    fun propertyDetails(propertyId: String): String = "propertyDetails/$propertyId"
}
