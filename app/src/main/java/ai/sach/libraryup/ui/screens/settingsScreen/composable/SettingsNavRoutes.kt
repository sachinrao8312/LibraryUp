package ai.sach.libraryup.ui.screens.settingsScreen.composable

/**
 * Object containing route constants for all settings-related screens.
 * These constants should be used for navigation to ensure consistency and prevent typos.
 */
object SettingsNavRoutes {

    // Main settings screen
    private const val SETTINGS_MAIN = "settings_main"

    // User profile related
    const val ACCOUNT = "account"
    const val PROFILE_EDIT = "profile_edit" // This seems to be missing or mismatched

    // Security related
    const val SECURITY = "security"
    const val PASSWORD_CHANGE = "security/password"

    // Communication preferences
    const val NOTIFICATIONS = "notifications"

    // App preferences
    const val LANGUAGE = "language"
    const val APP_PREFERENCES = "app_preferences" // New route for App Preferences
    const val BACKUP = "backup"

    // Help and support
    const val HELP = "help"
    const val CONTACT_SUPPORT = "help/contact"
    const val PRIVACY = "privacy"

    /**
     * Navigation routes for the Settings module.
     * Contains constants for all settings-related screen destinations.
     */
    object SettingsNavRoutes {
        // Main settings screens
        const val ACCOUNT = "account"
        const val PROFILE_EDIT = "profile_edit" // This seems to be missing or mismatched
        const val SECURITY = "security"
        const val PASSWORD_CHANGE = "password_change"
        const val NOTIFICATIONS = "notifications"
        const val LANGUAGE = "language"
        const val HELP = "help"
        const val PRIVACY = "privacy"
        const val BACKUP = "backup"
        const val APP_PREFERENCES = "app_preferences"
    }
    // All routes in a list for easy iteration
    val allRoutes = listOf(
        SETTINGS_MAIN,
        ACCOUNT,
        PROFILE_EDIT,
        SECURITY,
        PASSWORD_CHANGE,
        NOTIFICATIONS,
        LANGUAGE,
        APP_PREFERENCES, // Added to the list
        BACKUP,
        HELP,
        CONTACT_SUPPORT,
        PRIVACY
    )
}