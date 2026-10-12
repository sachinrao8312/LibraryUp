package ai.sach.libraryup.ui.screens.settingsScreen.composable

import ai.sach.libraryup.R
import ai.sach.libraryup.ui.common.composables.AppTopAppBar
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

/**
 * Screen for changing account password.
 *
 * @param navController Navigation controller for handling navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordChangeScreen(navController: NavController) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    var currentPasswordError by remember { mutableStateOf<String?>(null) }
    var newPasswordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    
    var passwordChangeSuccess by remember { mutableStateOf(false) }

    // Validation messages (looked up here; the button's onClick is not composable)
    val errorCurrentRequired = stringResource(R.string.password_error_current_required)
    val errorIncorrect = stringResource(R.string.password_error_incorrect)
    val errorMinLength = stringResource(R.string.password_error_min_length)
    val errorUppercase = stringResource(R.string.password_error_uppercase)
    val errorNumber = stringResource(R.string.password_error_number)
    val errorSpecial = stringResource(R.string.password_error_special)
    val errorMismatch = stringResource(R.string.error_passwords_dont_match)
    
    Scaffold(
        topBar = {
            AppTopAppBar(
                title = stringResource(R.string.password_change_title),
                showBackButton = true,
                onBackPressed = { navController.popBackStack() },
                showDefaultMenu = true,
            )

        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Password requirements card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.password_requirements),
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                        
                        Text(
                            text = stringResource(R.string.password_requirements_list),
                            color = Color.Gray,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
            
            // Success message if password changed
            if (passwordChangeSuccess) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF388E3C)
                        )
                        
                        Text(
                            text = stringResource(R.string.password_changed),
                            color = Color(0xFF388E3C),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
            
            // Current password field
            OutlinedTextField(
                value = currentPassword,
                onValueChange = { 
                    currentPassword = it
                    currentPasswordError = null
                },
                label = { Text(stringResource(R.string.password_current)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (currentPasswordVisible) 
                    VisualTransformation.None 
                else 
                    PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                        Icon(
                            imageVector = if (currentPasswordVisible) 
                                Icons.Default.VisibilityOff 
                            else 
                                Icons.Default.Visibility,
                            contentDescription = if (currentPasswordVisible) 
                                stringResource(R.string.password_hide)
                            else 
                                stringResource(R.string.password_show)
                        )
                    }
                },
                isError = currentPasswordError != null,
                supportingText = currentPasswordError?.let { { Text(it) } }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // New password field
            OutlinedTextField(
                value = newPassword,
                onValueChange = { 
                    newPassword = it
                    newPasswordError = null
                },
                label = { Text(stringResource(R.string.password_new)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (newPasswordVisible) 
                    VisualTransformation.None 
                else 
                    PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                        Icon(
                            imageVector = if (newPasswordVisible) 
                                Icons.Default.VisibilityOff 
                            else 
                                Icons.Default.Visibility,
                            contentDescription = if (newPasswordVisible) 
                                stringResource(R.string.password_hide)
                            else 
                                stringResource(R.string.password_show)
                        )
                    }
                },
                isError = newPasswordError != null,
                supportingText = newPasswordError?.let { { Text(it) } }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Confirm password field
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { 
                    confirmPassword = it
                    confirmPasswordError = null
                },
                label = { Text(stringResource(R.string.password_confirm_new)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (confirmPasswordVisible) 
                    VisualTransformation.None 
                else 
                    PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) 
                                Icons.Default.VisibilityOff 
                            else 
                                Icons.Default.Visibility,
                            contentDescription = if (confirmPasswordVisible) 
                                stringResource(R.string.password_hide)
                            else 
                                stringResource(R.string.password_show)
                        )
                    }
                },
                isError = confirmPasswordError != null,
                supportingText = confirmPasswordError?.let { { Text(it) } }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Change password button
            Button(
                onClick = {
                    // Reset errors
                    currentPasswordError = null
                    newPasswordError = null
                    confirmPasswordError = null
                    
                    var isValid = true
                    
                    // Validate current password
                    if (currentPassword.isEmpty()) {
                        currentPasswordError = errorCurrentRequired
                        isValid = false
                    } else {
                        // In a real app, verify against stored password
                        // For demo, we'll pretend it's "password123"
                        if (currentPassword != "password123") {
                            currentPasswordError = errorIncorrect
                            isValid = false
                        }
                    }
                    
                    // Validate new password
                    if (newPassword.length < 8) {
                        newPasswordError = errorMinLength
                        isValid = false
                    } else if (!newPassword.any { it.isUpperCase() }) {
                        newPasswordError = errorUppercase
                        isValid = false
                    } else if (!newPassword.any { it.isDigit() }) {
                        newPasswordError = errorNumber
                        isValid = false
                    } else if (!newPassword.any { !it.isLetterOrDigit() }) {
                        newPasswordError = errorSpecial
                        isValid = false
                    }
                    
                    // Validate password confirmation
                    if (newPassword != confirmPassword) {
                        confirmPasswordError = errorMismatch
                        isValid = false
                    }
                    
                    // If valid, update password
                    if (isValid) {
                        // In a real app, update the password in backend/repository
                        passwordChangeSuccess = true
                        
                        // Clear fields
                        currentPassword = ""
                        newPassword = ""
                        confirmPassword = ""
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(stringResource(R.string.password_change_title), fontSize = 16.sp)
            }
        }
    }
}