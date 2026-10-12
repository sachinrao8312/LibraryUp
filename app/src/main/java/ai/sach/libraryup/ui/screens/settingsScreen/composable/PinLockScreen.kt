package ai.sach.libraryup.ui.screens.settingsScreen.composable

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
                title = { Text(if (isPinAlreadySet) "Change PIN" else "Set PIN") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                    text = "PIN",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Instruction text
            Text(
                text = if (isPinAlreadySet) 
                    "Change your PIN code" 
                else 
                    "Create a PIN to protect your app",
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Your PIN must be 4-6 digits",
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
                    label = { Text("Current PIN") },
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
                                    "Hide PIN" 
                                else 
                                    "Show PIN"
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
                label = { Text("New PIN") },
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
                                "Hide PIN" 
                            else 
                                "Show PIN"
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
                label = { Text("Confirm PIN") },
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
                                "Hide PIN" 
                            else 
                                "Show PIN"
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
                            currentPinError = "Current PIN is required"
                            isValid = false
                        } else if (storedPinHash != hashPin(currentPin)) {
                            currentPinError = "Incorrect PIN"
                            isValid = false
                        }
                    }
                    
                    // Validate new PIN
                    if (newPin.length < 4) {
                        newPinError = "PIN must be at least 4 digits"
                        isValid = false
                    }
                    
                    // Validate confirmation
                    if (newPin != confirmPin) {
                        confirmPinError = "PINs don't match"
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
                Text("Save PIN", fontSize = 16.sp)
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
                        text = "Remove PIN Protection",
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