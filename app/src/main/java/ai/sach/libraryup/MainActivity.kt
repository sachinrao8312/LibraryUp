package ai.sach.libraryup

import ai.sach.libraryup.data.repository.LibraryState
import ai.sach.libraryup.ui.common.composables.BottomNavigationBar
import ai.sach.libraryup.ui.common.responsiveness.AccessibilityScalingProvider
import ai.sach.libraryup.ui.common.responsiveness.DimensionsProvider
import ai.sach.libraryup.ui.library.AdBanner
import ai.sach.libraryup.ui.library.ProScreen
import ai.sach.libraryup.ui.library.ActivityLogScreen
import ai.sach.libraryup.ui.library.AttendanceScreen
import ai.sach.libraryup.ui.library.DashboardScreen
import ai.sach.libraryup.ui.library.DataScreen
import ai.sach.libraryup.ui.library.EnquiriesScreen
import ai.sach.libraryup.ui.library.ExpensesScreen
import ai.sach.libraryup.ui.library.LockersScreen
import ai.sach.libraryup.ui.library.LibrarySettingsScreen
import ai.sach.libraryup.ui.library.LibraryTopBar
import ai.sach.libraryup.ui.library.LibraryViewModel
import ai.sach.libraryup.ui.library.MoreScreen
import ai.sach.libraryup.ui.library.PaymentsScreen
import ai.sach.libraryup.ui.library.RemindersScreen
import ai.sach.libraryup.ui.library.SeatMapScreen
import ai.sach.libraryup.ui.library.SetupScreen
import ai.sach.libraryup.ui.library.StudentDetailScreen
import ai.sach.libraryup.ui.library.StudentFilter
import ai.sach.libraryup.ui.library.StudentFormScreen
import ai.sach.libraryup.ui.library.StudentsScreen
import ai.sach.libraryup.ui.navigation.AppRoutes
import ai.sach.libraryup.ui.navigation.NavigationItems
import ai.sach.libraryup.ui.screens.authentication.AuthRepository
import ai.sach.libraryup.ui.screens.authentication.AuthViewModelFactory
import ai.sach.libraryup.ui.screens.authentication.BiometricAuthManager
import ai.sach.libraryup.ui.screens.authentication.BiometricLockScreen
import ai.sach.libraryup.ui.screens.authentication.GoogleAuthScreen
import ai.sach.libraryup.ui.screens.authentication.SplashScreen
import ai.sach.libraryup.ui.screens.settingsScreen.SettingsScreen
import ai.sach.libraryup.ui.screens.settingsScreen.appprefernecs.AppPreferencesScreen
import ai.sach.libraryup.ui.screens.settingsScreen.composable.AccountScreen
import ai.sach.libraryup.ui.screens.settingsScreen.composable.BackupRestoreScreen
import ai.sach.libraryup.ui.screens.settingsScreen.composable.HelpAndSupportScreen
import ai.sach.libraryup.ui.screens.settingsScreen.composable.LanguageScreen
import ai.sach.libraryup.ui.screens.settingsScreen.composable.NotificationsScreen
import ai.sach.libraryup.ui.screens.settingsScreen.composable.PasswordChangeScreen
import ai.sach.libraryup.ui.screens.settingsScreen.composable.PrivacyPolicyScreen
import ai.sach.libraryup.ui.screens.settingsScreen.composable.SecurityScreen
import ai.sach.libraryup.ui.screens.settingsScreen.composable.SettingsNavRoutes
import ai.sach.libraryup.ui.theme.LibraryUpTheme
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
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
 * Entry point: sign-in, then the optional biometric/PIN lock, then the library app.
 */
@RequiresApi(Build.VERSION_CODES.N)
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val RELOCK_AFTER_MS = 2 * 60 * 1000L
    }

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    // Falls back to manual creation if injection failed.
    private val bioAuthManager: BiometricAuthManager by lazy {
        if (::biometricAuthManager.isInitialized) biometricAuthManager else BiometricAuthManager(applicationContext)
    }

    /** True while the app lock covers the (still composed) app after a return from background. */
    private var relocked by mutableStateOf(false)
    private var stoppedAt = 0L
    private var unlockedOnce = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) scheduleDigest()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as LibraryUpApplication
        checkNotificationPermission()

        setContent {
            // Fixed font scale: layouts are tuned for it; large system fonts broke rows on small phones.
            val density = Density(density = LocalDensity.current.density, fontScale = 1.0f)
            CompositionLocalProvider(LocalDensity provides density) {
                LibraryUpTheme {
                    DimensionsProvider {
                        AccessibilityScalingProvider {
                            var appState by remember { mutableStateOf(AppState.Loading) }
                            val authRepository = remember { AuthRepository(this@MainActivity) }

                            LaunchedEffect(Unit) {
                                withContext(Dispatchers.IO) {
                                    try {
                                        delay(300)
                                        val user = authRepository.getCurrentFirebaseUser()
                                        withContext(Dispatchers.Main) {
                                            appState = when {
                                                user == null -> AppState.Unauthenticated
                                                bioAuthManager.shouldUseBiometricAuth() -> AppState.BiometricLock
                                                else -> AppState.Ready
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error checking authentication state", e)
                                        withContext(Dispatchers.Main) { appState = AppState.Unauthenticated }
                                    }
                                }
                            }

                            when (appState) {
                                AppState.Loading -> SplashScreen(onSplashFinished = {})
                                AppState.Unauthenticated -> GoogleAuthScreen(
                                    authViewModel = viewModel(factory = AuthViewModelFactory(application = app, authRepository = authRepository)),
                                    onNavigateToMain = {
                                        appState = if (bioAuthManager.shouldUseBiometricAuth()) AppState.BiometricLock else AppState.Ready
                                    }
                                )
                                AppState.BiometricLock -> BiometricLockScreen(
                                    activity = this@MainActivity,
                                    biometricAuthManager = bioAuthManager,
                                    onAuthenticationSuccess = { appState = AppState.Ready },
                                    onSkipAuthentication = { appState = AppState.Ready }
                                )
                                AppState.Ready -> {
                                    LaunchedEffect(Unit) { unlockedOnce = true }
                                    // The lock is drawn over the app rather than replacing it, so
                                    // the screen the owner was on survives a trip to WhatsApp.
                                    Box(Modifier.fillMaxSize()) {
                                        LibraryUpApp(onSignOut = { appState = AppState.Unauthenticated })
                                        if (relocked) {
                                            // Back must not reach the app hidden underneath.
                                            BackHandler { moveTaskToBack(true) }
                                            BiometricLockScreen(
                                                activity = this@MainActivity,
                                                biometricAuthManager = bioAuthManager,
                                                onAuthenticationSuccess = { relocked = false },
                                                onSkipAuthentication = { relocked = false }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        stoppedAt = SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        // Lock again after a real absence. Short trips (sending a WhatsApp reminder, picking
        // a photo) stay unlocked, or the bulk reminder queue would ask for a fingerprint
        // on every student.
        if (unlockedOnce && stoppedAt > 0 &&
            SystemClock.elapsedRealtime() - stoppedAt > RELOCK_AFTER_MS &&
            bioAuthManager.shouldUseBiometricAuth()
        ) {
            relocked = true
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            scheduleDigest()
            return
        }
        val prefs = (application as LibraryUpApplication).notificationPrefs
        if (!prefs.permissionRequested || shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            prefs.permissionRequested = true
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun scheduleDigest() {
        val app = application as LibraryUpApplication
        runCatching {
            if (app.notificationPrefs.dailyDigestEnabled) app.reminderScheduler.ensureScheduled() else app.reminderScheduler.cancel()
        }
    }
}

/**
 * The library app once signed in: first-run setup if the library has no profile yet,
 * otherwise the bottom-navigation shell around every library screen.
 */
@Composable
fun LibraryUpApp(onSignOut: () -> Unit = {}) {
    val context = LocalContext.current
    val app = context.applicationContext as LibraryUpApplication
    val vm: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory(app.libraryRepository, app.staffMode, app.proManager))
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(vm) {
        vm.messages.collect { m ->
            val text = if (m.arg != null) context.getString(m.res, m.arg) else context.getString(m.res)
            snackbar.showSnackbar(text)
        }
    }

    val current = state
    when {
        current == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        current.profile == null -> Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            Box(Modifier.padding(padding)) { SetupScreen(vm) }
        }
        else -> LibraryShell(current, vm, snackbar, onSignOut)
    }
}

@Composable
private fun LibraryShell(state: LibraryState, vm: LibraryViewModel, snackbar: SnackbarHostState, onSignOut: () -> Unit) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    LaunchedEffect(vm) { vm.paywall.collect { navController.navigate(AppRoutes.PRO) { launchSingleTop = true } } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { if (route in NavigationItems.routes) BottomNavigationBar(navController) }
    ) { padding ->
        NavHost(navController = navController, startDestination = AppRoutes.HOME, modifier = Modifier.padding(padding)) {
            composable(AppRoutes.HOME) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) {
                        DashboardScreen(
                            state = state,
                            onOpenStudents = { navController.navigate(AppRoutes.students(it.name)) { launchSingleTop = true } },
                            onAddStudent = { navController.navigate(AppRoutes.studentForm()) },
                            onOpenReminders = { navController.navigate(AppRoutes.REMINDERS) },
                            onOpenAttendance = { navController.navigate(AppRoutes.ATTENDANCE) },
                            onOpenPayments = { navController.navigate(AppRoutes.PAYMENTS) { launchSingleTop = true } }
                        )
                    }
                    if (!vm.isPro) AdBanner()
                }
            }
            composable(AppRoutes.SEATS) {
                TabScreen(stringResource(R.string.nav_seats)) {
                    SeatMapScreen(
                        state = state,
                        onOpenStudent = { navController.navigate(AppRoutes.student(it)) },
                        onAssignSeat = { seatId, shiftId -> navController.navigate(AppRoutes.studentForm(seatId = seatId, shiftId = shiftId)) },
                        withPro = vm::withPro
                    )
                }
            }
            composable(
                AppRoutes.STUDENTS,
                arguments = listOf(navArgument("filter") { type = NavType.StringType; defaultValue = "ALL" })
            ) { back ->
                val filter = runCatching { StudentFilter.valueOf(back.arguments?.getString("filter") ?: "ALL") }.getOrDefault(StudentFilter.ALL)
                TabScreen(stringResource(R.string.nav_students)) {
                    StudentsScreen(
                        state = state,
                        initialFilter = filter,
                        onOpenStudent = { navController.navigate(AppRoutes.student(it)) },
                        onAddStudent = { navController.navigate(AppRoutes.studentForm()) }
                    )
                }
            }
            composable(AppRoutes.PAYMENTS) {
                TabScreen(stringResource(R.string.nav_payments)) {
                    PaymentsScreen(state) { navController.navigate(AppRoutes.student(it)) }
                }
            }
            composable(AppRoutes.MORE) {
                TabScreen(stringResource(R.string.nav_more)) {
                    MoreScreen(vm, SettingsNavRoutes.BACKUP) { navController.navigate(it) }
                }
            }
            composable(AppRoutes.STUDENT, arguments = listOf(navArgument("studentId") { type = NavType.LongType })) { back ->
                val id = back.arguments?.getLong("studentId") ?: return@composable
                StudentDetailScreen(
                    state = state,
                    vm = vm,
                    studentId = id,
                    onEdit = { navController.navigate(AppRoutes.studentForm(studentId = id)) },
                    onAssignSeat = { navController.navigate(AppRoutes.studentForm(studentId = id)) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                AppRoutes.STUDENT_FORM,
                arguments = listOf(
                    navArgument("studentId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("seatId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("shiftId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) { back ->
                fun arg(name: String) = back.arguments?.getLong(name)?.takeIf { it > 0 }
                StudentFormScreen(
                    state = state,
                    vm = vm,
                    studentId = arg("studentId"),
                    presetSeatId = arg("seatId"),
                    presetShiftId = arg("shiftId"),
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(AppRoutes.REMINDERS) { RemindersScreen(state) { navController.popBackStack() } }
            composable(AppRoutes.ATTENDANCE) { AttendanceScreen(state, vm) { navController.popBackStack() } }
            composable(AppRoutes.EXPENSES) { ExpensesScreen(state, vm) { navController.popBackStack() } }
            composable(AppRoutes.LIBRARY_SETTINGS) { LibrarySettingsScreen(state, vm) { navController.popBackStack() } }
            composable(AppRoutes.DATA) { DataScreen(state, vm) { navController.popBackStack() } }
            composable(AppRoutes.ACTIVITY_LOG) { ActivityLogScreen(vm) { navController.popBackStack() } }
            composable(AppRoutes.PRO) { ProScreen(state, vm) { navController.popBackStack() } }
            composable(AppRoutes.LOCKERS) { LockersScreen(state, vm) { navController.popBackStack() } }
            composable(AppRoutes.ENQUIRIES) {
                EnquiriesScreen(state, vm, onOpenStudent = { navController.navigate(AppRoutes.student(it)) }) { navController.popBackStack() }
            }

            // App settings (inherited screens).
            composable(AppRoutes.SETTINGS) {
                SettingsScreen(
                    navController = navController,
                    onLogout = {
                        scope.launch {
                            AuthRepository(context).signOut()
                            onSignOut()
                        }
                    }
                )
            }
            composable(SettingsNavRoutes.ACCOUNT) { AccountScreen(navController = navController) }
            composable(SettingsNavRoutes.PROFILE_EDIT) { AccountScreen(navController = navController, isEditing = true) }
            composable(SettingsNavRoutes.SECURITY) { SecurityScreen(navController = navController) }
            composable(SettingsNavRoutes.PASSWORD_CHANGE) { PasswordChangeScreen(navController = navController) }
            composable(SettingsNavRoutes.APP_PREFERENCES) { AppPreferencesScreen(navController = navController) }
            composable(SettingsNavRoutes.NOTIFICATIONS) { NotificationsScreen(navController = navController) }
            composable(SettingsNavRoutes.LANGUAGE) { LanguageScreen(navController = navController) }
            composable(SettingsNavRoutes.HELP) { HelpAndSupportScreen(navController = navController) }
            composable(SettingsNavRoutes.PRIVACY) { PrivacyPolicyScreen(navController = navController) }
            composable(SettingsNavRoutes.BACKUP) { BackupRestoreScreen(navController = navController) }
        }
    }
}

/** A bottom-tab screen: title bar plus content. */
@Composable
private fun TabScreen(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        LibraryTopBar(title)
        content()
    }
}
