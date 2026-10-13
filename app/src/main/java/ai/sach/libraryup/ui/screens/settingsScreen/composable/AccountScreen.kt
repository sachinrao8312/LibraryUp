package ai.sach.libraryup.ui.screens.settingsScreen.composable

import ai.sach.libraryup.R
import ai.sach.libraryup.ui.common.responsiveness.LocalDimensions
import ai.sach.libraryup.ui.library.LibraryTopBar
import ai.sach.libraryup.ui.screens.authentication.AuthRepository
import ai.sach.libraryup.ui.screens.authentication.AuthViewModel
import ai.sach.libraryup.ui.screens.authentication.AuthViewModelFactory
import ai.sach.libraryup.ui.screens.authentication.GuestConversionState
import ai.sach.libraryup.ui.screens.authentication.UserData
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Modern Account settings screen with enhanced UI design
 * Features smooth animations, proper visual hierarchy, and excellent UX
 */
@Composable
fun AccountScreen(
    navController: NavController,
    isEditing: Boolean = false,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val dimensions = LocalDimensions.current
    val configuration = LocalConfiguration.current
    
    // Responsive sizing
    val isSmallScreen = configuration.screenWidthDp < 360
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Get auth repository and view model
    val authRepository = remember { AuthRepository(context) }
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(
            application = (context as FragmentActivity).application,
            authRepository = authRepository
        )
    )

    // States for user profile
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    var photoUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isGuestUser by remember { mutableStateOf(false) }

    // State for guest conversion (removed intermediate dialog)

    // Animation states
    var showContent by remember { mutableStateOf(false) }
    var showGuestBanner by remember { mutableStateOf(false) }

    // Observe conversion state
    val conversionState by authViewModel.guestConversionState.collectAsState()

    // Google Sign-In launcher for conversion
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            // Check if this is our dummy intent with an error
            val isDummy = result.data?.action == "ai.sach.libraryup.DUMMY_GOOGLE_SIGN_IN"
            if (isDummy) {
                val errorType = result.data?.getStringExtra("ERROR_TYPE") ?: "UNKNOWN_ERROR"
                val errorMessage = result.data?.getStringExtra("ERROR_MESSAGE")
                
                Log.e("AccountScreen", "Received dummy Google Sign-In result: $errorType - $errorMessage")
                
                val userMessage = when (errorType) {
                    "PLAY_SERVICES_UNAVAILABLE" -> context.getString(R.string.account_error_play_services)
                    "INTENT_CREATION_ERROR" -> context.getString(R.string.account_error_connect)
                    "CRITICAL_ERROR" -> context.getString(R.string.account_error_critical)
                    else -> context.getString(R.string.account_error_google_unavailable)
                }
                
                Toast.makeText(context, "$userMessage ${errorMessage ?: ""}", Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }
            
            // Normal flow for successful Google Sign-In
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                authViewModel.convertGuestToGoogleAccount(task, context)
            } else {
                // User canceled or failed sign-in
                Log.e("AccountScreen", "Google Sign-In was canceled or failed")
                Toast.makeText(context, context.getString(R.string.account_sign_in_canceled), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("AccountScreen", "Google conversion failed", e)
            Toast.makeText(context, context.getString(R.string.account_conversion_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
        }
    }

    // Load user data
    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) {
                // Check if user is guest
                isGuestUser = authRepository.isCurrentUserGuest()

                var storedPhotoUrl: String? = null
                val currentUserId = authRepository.getCurrentUserId()
                if (currentUserId != null) {
                    authRepository.getUserData(currentUserId)?.let { userData ->
                        name = userData.name.takeIf { it.isNotBlank() }
                            ?: context.getString(if (isGuestUser) R.string.label_guest_user else R.string.label_user)
                        email = userData.email ?: ""
                        phone = userData.phoneNumber.takeIf { it.isNotBlank() } ?: ""
                        storedPhotoUrl = userData.photoUrl
                    }
                }

                // Prefer the live Firebase user's photo, but fall back to the one stored
                // in Firestore: accounts converted from guest before profile syncing was
                // fixed can have a null photoUrl on the FirebaseUser even though the real
                // Google photo was already saved there.
                val currentUser = authRepository.getCurrentFirebaseUser()
                photoUrl = currentUser?.photoUrl?.toString()?.takeIf { it.isNotBlank() } ?: storedPhotoUrl

                Log.d("AccountScreen", "Loaded photoUrl: $photoUrl, isGuest: $isGuestUser")
            }
            isLoading = false
            
            // Trigger animations
            showContent = true
            if (isGuestUser) {
                showGuestBanner = true
            }
        } catch (e: Exception) {
            Log.e("AccountScreen", "Error loading user data", e)
            isLoading = false
        }
    }



    // Handle conversion completion
    LaunchedEffect(conversionState) {
        when (conversionState) {
            is GuestConversionState.Converted -> {
                // Reload data after conversion
                withContext(Dispatchers.IO) {
                    isGuestUser = authRepository.isCurrentUserGuest()
                    val currentUserId = authRepository.getCurrentUserId()
                    if (currentUserId != null) {
                        authRepository.getUserData(currentUserId)?.let { userData ->
                            name = userData.name
                            email = userData.email ?: ""
                            phone = userData.phoneNumber
                        }
                    }
                }
                authViewModel.resetGuestConversionState()
            }
            is GuestConversionState.Error -> {
                Toast.makeText(context, (conversionState as GuestConversionState.Error).message, Toast.LENGTH_LONG).show()
                authViewModel.resetGuestConversionState()
            }
            else -> {}
        }
    }

    // Edit mode state (can be toggled from within the screen)
    var editMode by remember { mutableStateOf(isEditing) }

    // Profile image state
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }

    // Image picker launcher
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { profileImageUri = it }
    }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show welcome message for guest users
    LaunchedEffect(isGuestUser) {
        if (isGuestUser && !isLoading) {
            delay(500) // Small delay to let the screen load
            snackbarHostState.showSnackbar(
                message = context.getString(R.string.account_guest_snackbar),
                duration = SnackbarDuration.Long
            )
        }
    }

    Scaffold(
        topBar = {
            LibraryTopBar(stringResource(if (editMode) R.string.account_edit_profile else R.string.settings_account), onBack = { navController.popBackStack() })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (isLoading) {
            // Show loading indicator with modern design
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                ModernLoadingIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = paddingValues.calculateTopPadding(),
                        bottom = paddingValues.calculateBottomPadding(),
                        start = if (isSmallScreen) 12.dp else 16.dp,
                        end = if (isSmallScreen) 12.dp else 16.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (isSmallScreen) 12.dp else 16.dp)
            ) {
                // Guest Account Banner with animation
                AnimatedVisibility(
                    visible = showGuestBanner,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(800, delayMillis = 200)
                    ) + fadeIn(animationSpec = tween(800, delayMillis = 200)),
                    exit = slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(600)
                    ) + fadeOut(animationSpec = tween(600))
                ) {
                    ModernGuestBanner(
                        onUpgradeClick = {
                            // Direct Google Sign-In - no intermediate dialog
                            try {
                                val signInIntent = authViewModel.getGoogleSignInIntent()
                                if (signInIntent != null) {
                                    googleSignInLauncher.launch(signInIntent)
                                } else {
                                    Toast.makeText(context, context.getString(R.string.account_error_google_unavailable), Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Log.e("AccountScreen", "Error launching Google Sign-In", e)
                                Toast.makeText(context, context.getString(R.string.account_error_connect_detail, e.message ?: ""), Toast.LENGTH_LONG).show()
                            }
                        },
                        isSmallScreen = isSmallScreen
                    )
                }

                // Profile Picture with animation
                AnimatedVisibility(
                    visible = showContent,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(1000, delayMillis = 400)
                    ) + fadeIn(animationSpec = tween(1000, delayMillis = 400))
                ) {
                    ModernProfilePicture(
                        name = name,
                        profileImageUri = profileImageUri,
                        photoUrl = photoUrl,
                        onChangeClick = {
                            if (editMode) {
                                imagePicker.launch("image/*")
                            }
                        },
                        isEditing = editMode,
                        isSmallScreen = isSmallScreen
                    )
                }

                // Profile Information Fields with animation
                AnimatedVisibility(
                    visible = showContent,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(1000, delayMillis = 600)
                    ) + fadeIn(animationSpec = tween(1000, delayMillis = 600))
                ) {
                    ModernProfileFields(
                        name = name,
                        email = email,
                        editMode = editMode,
                        onNameChange = { name = it },
                        onEmailChange = { email = it },
                        isSmallScreen = isSmallScreen
                    )
                }

                // Action Buttons with animation
                AnimatedVisibility(
                    visible = showContent,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(1000, delayMillis = 800)
                    ) + fadeIn(animationSpec = tween(1000, delayMillis = 800))
                ) {
                    ModernActionButtons(
                        editMode = editMode,
                        onEditClick = { editMode = true },
                        onSaveClick = { showSaveDialog = true },
                        onCancelClick = { editMode = false },
                        isSmallScreen = isSmallScreen
                    )
                }
            }
        }



        // Save confirmation dialog
        if (showSaveDialog) {
            ModernSaveDialog(
                onDismiss = { showSaveDialog = false },
                onConfirm = {
                    // Save profile changes to Firestore
                    coroutineScope.launch {
                        try {
                            val userData = UserData(
                                name = name,
                                email = email,
                                phoneNumber = phone,
                                photoUrl = photoUrl
                            )
                            
                            val result = authRepository.updateUserProfile(userData)
                            if (result.isSuccess) {
                                editMode = false
                                showSaveDialog = false
                                snackbarHostState.showSnackbar(context.getString(R.string.account_profile_updated))
                            } else {
                                snackbarHostState.showSnackbar(context.getString(R.string.account_profile_update_failed, result.exceptionOrNull()?.message ?: ""))
                            }
                        } catch (e: Exception) {
                            Log.e("AccountScreen", "Error saving profile", e)
                            snackbarHostState.showSnackbar(context.getString(R.string.account_profile_update_failed, e.message ?: ""))
                        }
                    }
                },
                isSmallScreen = isSmallScreen
            )
        }
    }
}

/**
 * Modern loading indicator with enhanced design
 */
@Composable
private fun ModernLoadingIndicator() {
    Card(
        modifier = Modifier
            .size(120.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
        }
    }
}

/**
 * Modern guest account banner with enhanced design
 */
@Composable
private fun ModernGuestBanner(
    onUpgradeClick: () -> Unit,
    isSmallScreen: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(if (isSmallScreen) 16.dp else 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Security icon
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = stringResource(R.string.settings_security),
                modifier = Modifier.size(if (isSmallScreen) 32.dp else 40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = stringResource(R.string.account_guest_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = if (isSmallScreen) 15.sp else 17.sp
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = stringResource(R.string.account_guest_text),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = if (isSmallScreen) 12.sp else 13.sp
                ),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Button(
                onClick = onUpgradeClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                // Google logo
                Image(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = stringResource(R.string.auth_google_logo),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(R.string.account_sign_in_google),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Modern profile picture with enhanced design
 */
@Composable
private fun ModernProfilePicture(
    name: String,
    profileImageUri: Uri?,
    photoUrl: String?,
    onChangeClick: () -> Unit,
    isEditing: Boolean,
    isSmallScreen: Boolean
) {
    val logoScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1000, delayMillis = 200),
        label = "Profile Scale"
    )
    
    Box(contentAlignment = Alignment.BottomEnd) {
        // Profile image or placeholder with modern design
        Box(
            modifier = Modifier
                .scale(logoScale)
                .size(if (isSmallScreen) 140.dp else 160.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                        )
                    )
                )
                .border(
                    width = 3.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    ),
                    shape = CircleShape
                )
                .clickable(enabled = isEditing, onClick = onChangeClick),
            contentAlignment = Alignment.Center
        ) {
            when {
                // Case 1: User selected a local image
                profileImageUri != null -> {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(profileImageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = stringResource(R.string.account_profile_picture),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                // Case 2: User has a Google profile image URL
                !photoUrl.isNullOrBlank() -> {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(photoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = stringResource(R.string.account_profile_picture),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            CircularProgressIndicator(
                                modifier = Modifier.size(if (isSmallScreen) 40.dp else 50.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        error = {
                            // Show initials as fallback
                            Text(
                                text = name.take(2).uppercase(),
                                fontSize = if (isSmallScreen) 40.sp else 48.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
                // Case 3: No image available, show initials
                else -> {
                    Text(
                        text = name.take(2).uppercase(),
                        fontSize = if (isSmallScreen) 40.sp else 48.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Edit button or camera icon with modern design
        if (isEditing) {
            Surface(
                modifier = Modifier
                    .size(if (isSmallScreen) 40.dp else 48.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    ),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                IconButton(
                    onClick = onChangeClick,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = stringResource(R.string.account_change_profile_picture),
                        tint = Color.White,
                        modifier = Modifier.size(if (isSmallScreen) 20.dp else 24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Modern profile fields with enhanced design
 */
@Composable
private fun ModernProfileFields(
    name: String,
    email: String,
    editMode: Boolean,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    isSmallScreen: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(if (isSmallScreen) 16.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(if (isSmallScreen) 12.dp else 16.dp)
        ) {
            if (editMode) {
                // Editable fields with modern design
                ModernTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = stringResource(R.string.full_name),
                    icon = Icons.Default.Person,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    isSmallScreen = isSmallScreen
                )

                ModernTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = stringResource(R.string.email),
                    icon = Icons.Default.Email,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    isSmallScreen = isSmallScreen
                )
            } else {
                // Read-only display with modern design
                ModernProfileInfoItem(
                    icon = Icons.Default.Person,
                    label = stringResource(R.string.full_name),
                    value = name,
                    isSmallScreen = isSmallScreen
                )
                
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp
                )
                
                ModernProfileInfoItem(
                    icon = Icons.Default.Email,
                    label = stringResource(R.string.email),
                    value = if (email.isBlank()) stringResource(R.string.account_not_provided) else email,
                    isSmallScreen = isSmallScreen
                )
            }
        }
    }
}

/**
 * Modern text field with enhanced design
 */
@Composable
private fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardOptions: KeyboardOptions,
    isSmallScreen: Boolean
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { 
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = if (isSmallScreen) 13.sp else 15.sp
                )
            ) 
        },
        leadingIcon = { 
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(if (isSmallScreen) 20.dp else 24.dp)
            ) 
        },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = keyboardOptions,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

/**
 * Modern profile info item with enhanced design
 */
@Composable
private fun ModernProfileInfoItem(
    icon: ImageVector,
    label: String,
    value: String,
    isSmallScreen: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with background
        Surface(
            modifier = Modifier.size(if (isSmallScreen) 40.dp else 48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(if (isSmallScreen) 20.dp else 24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(if (isSmallScreen) 12.dp else 16.dp))

        // Text content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = if (isSmallScreen) 10.sp else 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = if (isSmallScreen) 13.sp else 15.sp
                ),
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Modern action buttons with enhanced design
 */
@Composable
private fun ModernActionButtons(
    editMode: Boolean,
    onEditClick: () -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    isSmallScreen: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (isSmallScreen) 12.dp else 16.dp)
    ) {
        if (editMode) {
            // Save changes button
            Button(
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 4.dp
                )
            ) {
                Text(
                    stringResource(R.string.account_save_changes),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = if (isSmallScreen) 14.sp else 16.sp
                    ),
                    fontWeight = FontWeight.Medium
                )
            }

            // Cancel button in edit mode
            OutlinedButton(
                onClick = onCancelClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    stringResource(R.string.action_cancel),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = if (isSmallScreen) 14.sp else 16.sp
                    ),
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            // Edit button when not in edit mode
            Button(
                onClick = onEditClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 4.dp
                )
            ) {
                Text(
                    stringResource(R.string.account_edit_profile),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = if (isSmallScreen) 14.sp else 16.sp
                    ),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}



/**
 * Modern save dialog with enhanced design
 */
@Composable
private fun ModernSaveDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isSmallScreen: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(if (isSmallScreen) 16.dp else 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = stringResource(R.string.account_save_changes),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = if (isSmallScreen) 18.sp else 20.sp
                ),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = stringResource(R.string.account_save_confirm),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = if (isSmallScreen) 13.sp else 15.sp
                ),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    stringResource(R.string.action_save),
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = if (isSmallScreen) 14.sp else 16.sp
                    )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    stringResource(R.string.action_cancel),
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = if (isSmallScreen) 14.sp else 16.sp
                    )
                )
            }
        }
    )
}