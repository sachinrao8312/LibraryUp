package ai.sach.libraryup

import ai.sach.libraryup.ui.common.composables.BottomNavigationBar
import ai.sach.libraryup.ui.common.composables.ExpandableFAB
import ai.sach.libraryup.ui.common.composables.rememberWindowSizeClass
import ai.sach.libraryup.ui.common.responsiveness.AccessibilityScalingProvider
import ai.sach.libraryup.ui.common.responsiveness.DimensionsProvider
import ai.sach.libraryup.service.FcmTokenSyncWorker
import ai.sach.libraryup.ui.navigation.AppRoutes
import ai.sach.libraryup.ui.navigation.NavigationItems
import ai.sach.libraryup.ui.screens.authentication.AuthRepository
import ai.sach.libraryup.ui.screens.authentication.AuthViewModelFactory
import ai.sach.libraryup.ui.screens.authentication.BiometricAuthManager
import ai.sach.libraryup.ui.screens.authentication.BiometricLockScreen
import ai.sach.libraryup.ui.screens.authentication.GoogleAuthScreen
import ai.sach.libraryup.ui.screens.authentication.SplashScreen
import ai.sach.libraryup.ui.screens.historyScreen.PaymentHistoryScreen
import ai.sach.libraryup.ui.screens.homescreen.HomeScreen
import ai.sach.libraryup.ui.screens.homescreen.TenantFormSubmissionHandler
import ai.sach.libraryup.ui.screens.properties.PropertiesScreen
import ai.sach.libraryup.ui.screens.properties.composables.PropertyDetailsScreen
import ai.sach.libraryup.ui.screens.settingsScreen.SettingsScreen
import ai.sach.libraryup.ui.screens.settingsScreen.appprefernecs.AppPreferencesScreen
import ai.sach.libraryup.ui.screens.settingsScreen.composable.AccountScreen
import ai.sach.libraryup.ui.screens.settingsScreen.composable.BackupRestoreScreen
import ai.sach.libraryup.ui.screens.settingsScreen.composable.HelpAndSupportScreen
import ai.sach.libraryup.ui.screens.settingsScreen.composable.LanguageScreen
import ai.sach.libraryup.ui.screens.settingsScreen.composable.NotificationsScreen
import ai.sach.libraryup.ui.screens.settingsScreen.composable.PasswordChangeScreen
import ai.sach.libraryup.ui.screens.settingsScreen.composable.PinLockScreen
import ai.sach.libraryup.ui.screens.settingsScreen.composable.PrivacyPolicyScreen
import ai.sach.libraryup.ui.screens.settingsScreen.composable.SecurityScreen
import ai.sach.libraryup.ui.screens.settingsScreen.composable.SettingsNavRoutes
import ai.sach.libraryup.ui.screens.settingsScreen.notifications.NotificationIntent
import ai.sach.libraryup.ui.screens.settingsScreen.notifications.PendingNotificationTarget
import ai.sach.libraryup.ui.theme.LibraryUpTheme
import android.Manifest
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * App state for authentication flow
 */
enum class AppState {
    Loading,        // Initial loading and auth state check
    Unauthenticated, // Show GoogleAuthScreen
    BiometricLock,  // Show biometric authentication
    Ready           // Main app ready to show
}

/**
 * Main Activity class for the RentUp application.
 * This serves as the entry point for the app and configures the main UI structure.
 * Now includes improved authentication flow and integration.
 */
/**
 * Main Activity class for the RentUp application.
 * This serves as the entry point for the app and configures the main UI structure.
 * Now includes improved authentication flow with unified BiometricAuthManager.
 */
@RequiresApi(Build.VERSION_CODES.N)
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    // Inject the BiometricAuthManager
    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    // Inject TenantFormSubmissionHandler
    @Inject
    lateinit var tenantFormSubmissionHandler: TenantFormSubmissionHandler

    // Lazily create a safe version that falls back to manual creation if injection fails
    private val bioAuthManager: BiometricAuthManager by lazy {
        if (::biometricAuthManager.isInitialized) {
            biometricAuthManager
        } else {
            // Fallback if injection fails
            Log.w(TAG, "BiometricAuthManager injection failed, creating manually")
            BiometricAuthManager(applicationContext)
        }
    }

    // Notification permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, initialize notification system
            initializeNotifications()
        }
    }

    /**
     * Route (and, for rent reminders, the tenant-list chip to pre-select) requested by a
     * notification tap, consumed once navigation is ready.
     */
    private val pendingNotificationTarget = MutableStateFlow<PendingNotificationTarget?>(null)

    /**
     * MainActivity is singleTask, so a tap while the app is already running arrives here
     * rather than through onCreate. Without this override those taps were silently
     * dropped, or cost the user their UI state when the task was cleared instead.
     */
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display mode - this handles status bar and navigation bar automatically
        enableEdgeToEdge()

        // Get application instance once
        val app = application as LibraryUpApplication

        // Initialize notification system
        checkNotificationPermission()

        // Check if we were launched from a notification
        handleNotificationIntent(intent)

        setContent {
            // Create fixed density with 1.0 scale
            val density = Density(
                density = LocalDensity.current.density,
                fontScale = 1.0f
            )

            // Apply fixed density to entire app
            CompositionLocalProvider(LocalDensity provides density) {
                // Set up the app theme with system's default dark/light mode
                LibraryUpTheme(
                    dynamicColor = false // Set to true if you want to use Material You dynamic colors
                ) {
                    // Wrap the entire app in responsive dimension providers
                    DimensionsProvider {
                        // Add accessibility scaling support
                        AccessibilityScalingProvider {
                            // Enhanced Authentication Flow with GoogleAuthScreen
                            var appState by remember { mutableStateOf(AppState.Loading) }
                            val authRepository = remember { AuthRepository(this@MainActivity) }

                            // Check authentication state on app launch
                            LaunchedEffect(Unit) {
                                // Use IO dispatcher for all heavy operations
                                withContext(Dispatchers.IO) {
                                    try {
                                        // Minimal delay for faster startup
                                        delay(500) // Reduced from 1000ms to 500ms

                                        // Check current authentication state
                                        val currentUser = authRepository.getCurrentFirebaseUser()
                                        
                                        withContext(Dispatchers.Main) {
                                            when {
                                                currentUser == null -> {
                                                    // No user signed in - show authentication screen
                                                    appState = AppState.Unauthenticated
                                                }
                                                bioAuthManager.shouldUseBiometricAuth() -> {
                                                    // User exists but needs biometric auth
                                                    appState = AppState.BiometricLock
                                                }
                                                else -> {
                                                    // User authenticated and no biometric needed
                                                    // Republish the FCM token now that a uid exists;
                                                    // a rotation while signed out lands here.
                                                    FcmTokenSyncWorker.enqueue(applicationContext)
                                                    appState = AppState.Ready
                                                }
                                            }
                                        }

                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error checking authentication state", e)
                                        // On error, show authentication screen for safety
                                        withContext(Dispatchers.Main) {
                                            appState = AppState.Unauthenticated
                                        }
                                    }
                                }
                            }

                            when (appState) {
                                AppState.Loading -> {
                                    // Show splash screen during authentication check
                                    SplashScreen(onSplashFinished = { /* Handled by LaunchedEffect */ })
                                }
                                AppState.Unauthenticated -> {
                                    // Show Google authentication screen
                                    GoogleAuthScreen(
                                        authViewModel = viewModel(
                                            factory = AuthViewModelFactory(
                                                application = app,
                                                authRepository = authRepository
                                            )
                                        ),
                                        onNavigateToMain = {
                                            // Check if biometric auth is needed after successful auth
                                            if (bioAuthManager.shouldUseBiometricAuth()) {
                                                appState = AppState.BiometricLock
                                            } else {
                                                appState = AppState.Ready
                                            }
                                        }
                                    )
                                }
                                AppState.BiometricLock -> {
                                    // Show biometric lock screen
                                    BiometricLockScreen(
                                        activity = this@MainActivity,
                                        biometricAuthManager = bioAuthManager,
                                        onAuthenticationSuccess = {
                                            appState = AppState.Ready
                                        },
                                        onSkipAuthentication = {
                                            appState = AppState.Ready
                                        }
                                    )
                                }
                                AppState.Ready -> {
                                    // Show main app
                                    LibraryUpApp(
                                        onSignOut = {
                                            // Handle sign out - reset to unauthenticated state
                                            (application as? LibraryUpApplication)
                                                ?.fcmTokenRegistrar?.onSignedOut()
                                            appState = AppState.Unauthenticated
                                        },
                                        pendingNotificationTarget = pendingNotificationTarget,
                                        onNotificationTargetConsumed = {
                                            pendingNotificationTarget.value = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // If the user re-enabled notifications in system settings while the in-app master
        // toggle was off, flip it back on, re-arm reminders, and republish the FCM opt-in
        // flag so the backend resumes addressing this device.
        runCatching {
            val app = application as? LibraryUpApplication ?: return@runCatching
            if (app.notificationHelper.reconcileOptInFromSystemSettings()) {
                app.reminderScheduler.ensureScheduled()
                FcmTokenSyncWorker.enqueue(this, force = true)
                Log.d(TAG, "Notifications re-enabled from system settings; republished opt-in")
            }
        }

        // Defer heavy operations to avoid blocking onResume
        lifecycleScope.launch {
            // Small delay to let onResume complete first
            delay(100)
            
            // Then do heavy work on IO thread
            withContext(Dispatchers.IO) {
                try {
                    if (::tenantFormSubmissionHandler.isInitialized) {
                        tenantFormSubmissionHandler.refreshTenantSubmissions()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in onResume background task", e)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        // Keep lifecycle registration on main thread but make it fast
        try {
            // Register the tenant form submission handler with the lifecycle
            if (::tenantFormSubmissionHandler.isInitialized) {
                lifecycle.addObserver(tenantFormSubmissionHandler)
                Log.d(TAG, "TenantFormSubmissionHandler registered with lifecycle")
            } else {
                Log.w(TAG, "TenantFormSubmissionHandler not initialized")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering lifecycle observer", e)
        }
    }

    override fun onStop() {
        super.onStop()

        // Keep lifecycle unregistration on main thread but make it fast
        try {
            // Unregister the tenant form submission handler from the lifecycle
            if (::tenantFormSubmissionHandler.isInitialized) {
                lifecycle.removeObserver(tenantFormSubmissionHandler)
                Log.d(TAG, "TenantFormSubmissionHandler unregistered from lifecycle")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering lifecycle observer", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        
        try {
            // Clean up lifecycle observers
            if (::tenantFormSubmissionHandler.isInitialized) {
                lifecycle.removeObserver(tenantFormSubmissionHandler)
                tenantFormSubmissionHandler.cancel() // Cancel any ongoing operations
                Log.d(TAG, "TenantFormSubmissionHandler cleaned up in onDestroy")
            }
            
            // Clean up biometric auth manager
            if (::biometricAuthManager.isInitialized) {
                // Note: BiometricAuthManager cleanup method needs to be added
                Log.d(TAG, "BiometricAuthManager cleaned up in onDestroy")
            }
            
            // Cancel any ongoing coroutines
            lifecycleScope.cancel()
            
            Log.d(TAG, "MainActivity cleanup completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy cleanup", e)
        }
    }

    /**
     * Checks and requests notification permission if needed
     */
    private fun checkNotificationPermission() {
        // Only needed for Android 13+ (Tiramisu)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            initializeNotifications()
            return
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            initializeNotifications()
            return
        }

        val prefs = (application as? LibraryUpApplication)?.notificationPreferences

        when {
            // First time ever asked: the system dialog will still be shown, so ask.
            prefs?.hasRequestedNotificationPermission() != true -> {
                prefs?.markNotificationPermissionRequested()
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            // Denied before, but "don't ask again" was not picked: the dialog can show again.
            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            // Permanently denied ("don't ask again"): a launch() here is a silent no-op. Don't
            // nag at startup; the Notifications screen offers the system-settings path instead.
            else -> {
                Log.d(TAG, "Notification permission permanently denied; skipping startup prompt")
            }
        }
    }

    /**
     * Initializes the notification system
     */
    private fun initializeNotifications() {
        val application = application as LibraryUpApplication

        // Create notification channels
        application.notificationHelper.createNotificationChannels()

        // Arm the daily reminder sweep now that we know the permission state. Scheduling
        // touches no database and sends nothing, so this is safe on the main thread.
        if (application.notificationPreferences.areRentRemindersEnabled()) {
            application.reminderScheduler.ensureScheduled()
        } else {
            application.reminderScheduler.cancel()
        }
    }
    
    /**
     * Turns a notification tap into a navigation route, plus - for rent reminders - the
     * tenant-list chip that should already be selected when the user gets there.
     *
     * The target is published to [pendingNotificationTarget] rather than applied directly,
     * because the NavController does not exist yet when this runs from onCreate. The app
     * composable consumes it once navigation is ready.
     */
    private fun handleNotificationIntent(intent: android.content.Intent?) {
        if (intent == null) return
        try {
            val notificationType = intent.getStringExtra(NotificationIntent.EXTRA_TYPE)
            val propertyId = intent.getStringExtra(NotificationIntent.EXTRA_PROPERTY_ID)
            val tenantId = intent.getStringExtra(NotificationIntent.EXTRA_TENANT_ID)
            val formId = intent.getStringExtra(NotificationIntent.EXTRA_FORM_ID)
            val filterKey = intent.getStringExtra(NotificationIntent.EXTRA_FILTER)

            if (notificationType == null) return

            // Mark onboarding notification as seen if formId is present
            if (formId != null) {
                (application as? LibraryUpApplication)
                    ?.notificationPreferences
                    ?.markOnboardingNotificationSeen(formId)
                Log.d(TAG, "Marked onboarding notification as seen for formId=$formId")
            }

            Log.d(
                TAG,
                "Notification tapped: type=$notificationType, propertyId=$propertyId, " +
                    "tenantId=$tenantId, formId=$formId, filter=$filterKey"
            )

            val route = NotificationIntent.routeFor(notificationType, propertyId)
            if (route != null) {
                pendingNotificationTarget.value = PendingNotificationTarget(route, filterKey)
                Log.d(TAG, "Queued navigation to $route (filter=$filterKey)")
            }

            // The tenant this submission created may not exist locally yet - the sync
            // worker that imports it runs independently of this notification tap - so the
            // lookup happens here, asynchronously, rather than at the time the notification
            // was built. Enriching the same target (rather than replacing it) is safe even
            // after the fast path above has already navigated: Home only reacts to a route
            // change, and simply reads tenantIdToOpen off whatever target arrives.
            if (notificationType == NotificationIntent.TYPE_TENANT_SUBMISSION && formId != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val tenant = (application as? LibraryUpApplication)
                            ?.tenantRepository
                            ?.getTenantByFormId(formId)
                        if (tenant != null) {
                            withContext(Dispatchers.Main) {
                                pendingNotificationTarget.value = PendingNotificationTarget(
                                    route = AppRoutes.HOME,
                                    tenantIdToOpen = tenant.id
                                )
                                Log.d(TAG, "Resolved formId=$formId to tenantId=${tenant.id}")
                            }
                        } else {
                            Log.d(TAG, "No local tenant yet for formId=$formId; sync may still be pending")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error resolving tenant for formId=$formId", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling notification intent", e)
        }
    }
}

/**
 * Main composable function for the RentUp application.
 * Sets up the navigation controller, navigation routes, and floating action button.
 * Now with responsive layout handling for different screen sizes and orientations.
 *
 * @param onSignOut Callback for signing out of the application
 */
@RequiresApi(Build.VERSION_CODES.N)
@Composable
fun LibraryUpApp(
    onSignOut: () -> Unit = {},
    pendingNotificationTarget: StateFlow<PendingNotificationTarget?> = MutableStateFlow(null),
    onNotificationTargetConsumed: () -> Unit = {}
) {
    // Get application and auto restore manager
    val context = LocalContext.current
    context.applicationContext as LibraryUpApplication
    val coroutineScope = rememberCoroutineScope()
    val navController = rememberNavController()
    var isAddTenantFormVisible by remember { mutableStateOf(false) }
    var isAddPropertyFormVisible by remember { mutableStateOf(false) }
    var isHomeInSelectionMode by remember { mutableStateOf(false) }

    // Tenant-list filter chip a rent reminder tap asked Home to pre-select, consumed once
    // HomeScreen applies it. Held here, above the NavHost, so it survives regardless of
    // whether Home's own composition is torn down and rebuilt in between.
    var pendingHomeFilter by remember { mutableStateOf<String?>(null) }

    // Tenant a "new tenant submission" notification tap asked Home to open details for,
    // consumed once HomeScreen shows it. See pendingHomeFilter above for why this is
    // hoisted here rather than held inside HomeScreen.
    var pendingHomeTenantId by remember { mutableStateOf<Long?>(null) }

    // Get current route for conditional FAB display
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // ponytail: reset hoisted select-mode flag off Home so FAB never stays hidden
    LaunchedEffect(currentRoute) {
        if (currentRoute != NavigationItems.Home.route) isHomeInSelectionMode = false
    }

    // Apply a target requested by a notification tap. Collected here rather than acted on
    // in the Activity because this is the first point at which the NavController exists.
    val notificationTarget by pendingNotificationTarget.collectAsState()
    LaunchedEffect(notificationTarget) {
        val target = notificationTarget ?: return@LaunchedEffect
        if (target.route == AppRoutes.HOME) {
            pendingHomeFilter = target.filterKey
            target.tenantIdToOpen?.let { pendingHomeTenantId = it }
        }
        if (target.route != currentRoute) {
            navController.navigate(target.route) {
                launchSingleTop = true
                restoreState = true
            }
        }
        onNotificationTargetConsumed()
    }

    // Get screen orientation and size class
    val configuration = LocalConfiguration.current
    val orientation = configuration.orientation
    val windowSizeClass = rememberWindowSizeClass()
    val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE

    // Determine if we should use bottom navigation or side navigation based on screen size and orientation
    val useBottomNav = !isLandscape || !windowSizeClass.isExpandedWidth



    // The guest-conversion prompts used to run from here. They advertised cloud backup
    // and multi-device sync, and after 60 days locked the app behind a mandatory
    // sign-up - all for a sync service that does nothing. Backup & Restore in Settings
    // is the real thing now, so there is nothing left for these to sell.

    Scaffold(
        bottomBar = {
            // Only show bottom bar if we're using bottom navigation and on main screens
            val shouldShowBottomBar = useBottomNav && when(currentRoute) {
                NavigationItems.Home.route,
                NavigationItems.PaymentHistory.route,
                NavigationItems.Properties.route,
                NavigationItems.Settings.route -> true
                else -> false
            }

            if (shouldShowBottomBar) {
                BottomNavigationBar(navController)
            }
        },
        floatingActionButton = {

            // Only show FAB on HomeScreen and PropertiesScreen
            when (currentRoute) {
                NavigationItems.Home.route -> {
                    // Hide FAB in select mode so it never covers the BatchDeleteBar
                    if (!isHomeInSelectionMode) {
                        // Use standard FloatingActionButton instead of ExpandableFAB
                        ExpandableFAB(
                            onAddTenant = { isAddTenantFormVisible = true },
                            onAddProperty = {
                                isAddPropertyFormVisible = true
                            }

                        )
                    }
                }
                NavigationItems.Properties.route -> {
                    // The FAB for PropertiesScreen is handled within the screen
                }
            }
        }
    ) { innerPadding ->
        // Content area with responsive padding
        NavHost(
            navController = navController,
            startDestination = NavigationItems.Home.route,
            modifier = Modifier.padding(
                bottom = innerPadding.calculateBottomPadding(),
                top = innerPadding.calculateTopPadding(),
                start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                end = innerPadding.calculateEndPadding(LayoutDirection.Ltr)
            )
        ) {
            // Navigation routes defined here
            composable(NavigationItems.Home.route) {
                HomeScreen(
                    navController = navController,
                    isAddTenantFormVisible = isAddTenantFormVisible,
                    isAddPropertyFormVisible = isAddPropertyFormVisible,
                    onAddTenantFormDismiss = { isAddTenantFormVisible = false },
                    onAddPropertyFormDismiss = { isAddPropertyFormVisible = false },
                    onShowAddPropertyFormRequest = { isAddPropertyFormVisible = true },
                    onShowAddTenantFormRequest = { isAddTenantFormVisible = true },
                    initialFilterKey = pendingHomeFilter,
                    onInitialFilterConsumed = { pendingHomeFilter = null },
                    initialTenantIdToOpen = pendingHomeTenantId,
                    onInitialTenantConsumed = { pendingHomeTenantId = null },
                    onSelectionModeChange = { isHomeInSelectionMode = it }
                )
            }

            composable(NavigationItems.PaymentHistory.route) {
                PaymentHistoryScreen(navController = navController)
            }

            composable(NavigationItems.Properties.route) {
                PropertiesScreen(
                    isAddPropertyFormVisible = isAddPropertyFormVisible,
                    onAddPropertyFormDismiss = { isAddPropertyFormVisible = false },
                    onAddPropertyClicked = { isAddPropertyFormVisible = true },
                    navController = navController
                )
            }

            composable(NavigationItems.Settings.route) {
                SettingsScreen(
                    navController = navController,
                    onLogout = {
                        // Get auth repository and sign out
                        val authRepository = AuthRepository(context)
                        coroutineScope.launch {
                            authRepository.signOut()
                            // Call the onSignOut callback to return to auth screen
                            onSignOut()
                        }
                    } // Connect the sign out button to auth
                )
            }

            // Property details screen
            composable(AppRoutes.PROPERTY_DETAILS_PATTERN) { backStackEntry ->
                val propertyId = backStackEntry.arguments?.getString("propertyId")
                if (propertyId != null) {
                    PropertyDetailsScreen(
                        propertyId = propertyId,
                        onBackPressed = { navController.popBackStack() }
                    )
                }
            }

            // Settings sub-screens
            composable(SettingsNavRoutes.ACCOUNT) {
                AccountScreen(navController = navController)
            }

            composable(SettingsNavRoutes.PROFILE_EDIT) {
                AccountScreen(navController = navController, isEditing = true)
            }

            composable(SettingsNavRoutes.SECURITY) {
                SecurityScreen(navController = navController)
            }

            composable(SettingsNavRoutes.PASSWORD_CHANGE) {
                PasswordChangeScreen(navController = navController)
            }

            composable(
                route = SettingsNavRoutes.PIN_SETUP + "?isPinSet={isPinSet}",
                arguments = listOf(
                    navArgument("isPinSet") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val isPinSet = backStackEntry.arguments?.getBoolean("isPinSet") ?: false
                PinLockScreen(
                    navController = navController,
                    isPinAlreadySet = isPinSet
                )
            }
            // Add the new App Preferences route
            composable(SettingsNavRoutes.APP_PREFERENCES) {
                AppPreferencesScreen(navController = navController)
            }

            composable(SettingsNavRoutes.NOTIFICATIONS) {
                NotificationsScreen(navController = navController)
            }

            composable(SettingsNavRoutes.LANGUAGE) {
                LanguageScreen(navController = navController)
            }

            composable(SettingsNavRoutes.HELP) {
                HelpAndSupportScreen(navController = navController)
            }

            composable(SettingsNavRoutes.PRIVACY) {
                PrivacyPolicyScreen(navController = navController)
            }

            composable(SettingsNavRoutes.BACKUP) {
                BackupRestoreScreen(navController = navController)
            }
        }


    }
}