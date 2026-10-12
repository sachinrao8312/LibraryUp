package ai.sach.libraryup.ui.screens.settingsScreen.composable

import ai.sach.libraryup.R
import ai.sach.libraryup.ui.theme.AppColors
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

/**
 * Screen for setting up or changing the PIN lock.
 *
 * @param navController Navigation controller for handling navigation
 * @param isPinAlreadySet Whether a PIN is already set up (for changing existing PIN)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinLockScreen(
    navController: NavController,
    isPinAlreadySet: Boolean = false
) {
    // PIN state
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isCurrentPinVisible by remember { mutableStateOf(false) }
    var isNewPinVisible by remember { mutableStateOf(false) }
    var isConfirmPinVisible by remember { mutableStateOf(false) }
    
    // Error state
    var currentPinError by remember { mutableStateOf<String?>(null) }
    var newPinError by remember { mutableStateOf<String?>(null) }
    var confirmPinError by remember { mutableStateOf<String?>(null) }
    
    // Context for saving preferences
    val context = LocalContext.current

    // Validation messages (looked up here; the Save button's onClick is not composable)
    val errorCurrentRequired = stringResource(R.string.pin_error_current_required)
    val errorIncorrect = stringResource(R.string.pin_error_incorrect)
    val errorTooShort = stringResource(R.string.pin_error_too_short)
    val errorMismatch = stringResource(R.string.pin_error_mismatch)
    val pinPrefs = remember { 
        context.getSharedPreferences("pin_prefs", Context.MODE_PRIVATE)
    }
    
    // Get stored PIN hash (if any)
    val storedPinHash = remember { 
        pinPrefs.getString("pin_hash", null)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (isPinAlreadySet) R.string.pin_change_title else R.string.pin_set_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // PIN icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.pin_badge),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Instruction text
            Text(
                text = if (isPinAlreadySet)
                    stringResource(R.string.pin_change_text)
                else
                    stringResource(R.string.pin_create_text),
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = stringResource(R.string.pin_rule),
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )
            
            // Current PIN field (only if changing)
            if (isPinAlreadySet) {
                OutlinedTextField(
                    value = currentPin,
                    onValueChange = { 
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            currentPin = it
                            currentPinError = null
                        }
                    },
                    label = { Text(stringResource(R.string.pin_current)) },
                    visualTransformation = if (isCurrentPinVisible) 
                        VisualTransformation.None 
                    else 
                        PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    trailingIcon = {
                        IconButton(onClick = { isCurrentPinVisible = !isCurrentPinVisible }) {
                            Icon(
                                imageVector = if (isCurrentPinVisible) 
                                    Icons.Default.VisibilityOff 
                                else 
                                    Icons.Default.Visibility,
                                contentDescription = if (isCurrentPinVisible) 
                                    stringResource(R.string.pin_hide)
                                else 
                                    stringResource(R.string.pin_show)
                            )
                        }
                    },
                    isError = currentPinError != null,
                    supportingText = currentPinError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // New PIN field
            OutlinedTextField(
                value = newPin,
                onValueChange = { 
                    if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                        newPin = it
                        newPinError = null
                    }
                },
                label = { Text(stringResource(R.string.pin_new)) },
                visualTransformation = if (isNewPinVisible) 
                    VisualTransformation.None 
                else 
                    PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                trailingIcon = {
                    IconButton(onClick = { isNewPinVisible = !isNewPinVisible }) {
                        Icon(
                            imageVector = if (isNewPinVisible) 
                                Icons.Default.VisibilityOff 
                            else 
                                Icons.Default.Visibility,
                            contentDescription = if (isNewPinVisible) 
                                stringResource(R.string.pin_hide)
                            else 
                                stringResource(R.string.pin_show)
                        )
                    }
                },
                isError = newPinError != null,
                supportingText = newPinError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Confirm PIN field
            OutlinedTextField(
                value = confirmPin,
                onValueChange = { 
                    if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                        confirmPin = it
                        confirmPinError = null
                    }
                },
                label = { Text(stringResource(R.string.pin_confirm)) },
                visualTransformation = if (isConfirmPinVisible) 
                    VisualTransformation.None 
                else 
                    PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                trailingIcon = {
                    IconButton(onClick = { isConfirmPinVisible = !isConfirmPinVisible }) {
                        Icon(
                            imageVector = if (isConfirmPinVisible) 
                                Icons.Default.VisibilityOff 
                            else 
                                Icons.Default.Visibility,
                            contentDescription = if (isConfirmPinVisible) 
                                stringResource(R.string.pin_hide)
                            else 
                                stringResource(R.string.pin_show)
                        )
                    }
                },
                isError = confirmPinError != null,
                supportingText = confirmPinError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Save button
            Button(
                onClick = {
                    // Validate input
                    var isValid = true
                    
                    // Validate current PIN if changing
                    if (isPinAlreadySet) {
                        if (currentPin.length < 4) {
                            currentPinError = errorCurrentRequired
                            isValid = false
                        } else if (storedPinHash != hashPin(currentPin)) {
                            currentPinError = errorIncorrect
                            isValid = false
                        }
                    }
                    
                    // Validate new PIN
                    if (newPin.length < 4) {
                        newPinError = errorTooShort
                        isValid = false
                    }
                    
                    // Validate confirmation
                    if (newPin != confirmPin) {
                        confirmPinError = errorMismatch
                        isValid = false
                    }
                    
                    // If valid, save PIN and navigate back
                    if (isValid) {
                        // Save hashed PIN
                        pinPrefs.edit().putString("pin_hash", hashPin(newPin)).apply()
                        
                        // Enable PIN lock
                        pinPrefs.edit().putBoolean("pin_enabled", true).apply()
                        
                        // Navigate back
                        navController.popBackStack()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(stringResource(R.string.pin_save), fontSize = 16.sp)
            }
            
            // Remove PIN button (only if changing)
            if (isPinAlreadySet) {
                TextButton(
                    onClick = {
                        // Confirm before removing
                        // Show dialog (implementation omitted for brevity)
                        
                        // Remove PIN
                        pinPrefs.edit().remove("pin_hash").apply()
                        pinPrefs.edit().putBoolean("pin_enabled", false).apply()
                        
                        // Navigate back
                        navController.popBackStack()
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.pin_remove),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Simple one-way hash function for PIN
 * Note: In a production app, use a more secure hashing algorithm
 */
private fun hashPin(pin: String): String {
    return pin.hashCode().toString()
}