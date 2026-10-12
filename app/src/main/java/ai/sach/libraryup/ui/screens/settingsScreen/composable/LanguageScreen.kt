package ai.sach.libraryup.ui.screens.settingsScreen.composable

import ai.sach.libraryup.ui.theme.AppColors
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

/**
 * Language settings screen for changing the app's language.
 * 
 * @param navController Navigation controller for handling screen navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(navController: NavController) {
    // Language options
    val languages = listOf(
        LanguageOption("English", "English", true),
        LanguageOption("Hindi", "हिन्दी", false),
        LanguageOption("Spanish", "Español", false),
        LanguageOption("French", "Français", false),
        LanguageOption("German", "Deutsch", false),
        LanguageOption("Chinese", "中文", false),
        LanguageOption("Japanese", "日本語", false),
        LanguageOption("Korean", "한국어", false),
        LanguageOption("Arabic", "العربية", false)
    )
    
    // State for the currently selected language
    var selectedLanguage by remember { mutableStateOf(languages.first { it.isSelected }.code) }
    var showLanguageChangeDialog by remember { mutableStateOf(false) }
    var pendingLanguage by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Language") },
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
                .verticalScroll(rememberScrollState())
        ) {
            // Language header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🌐",
                        fontSize = 24.sp
                    )
                }
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "App Language",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    
                    Text(
                        text = "Select your preferred language",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
            
            // Language list
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column {
                    languages.forEachIndexed { index, language ->
                        val isSelected = language.code == selectedLanguage
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!isSelected) {
                                        pendingLanguage = language.code
                                        showLanguageChangeDialog = true
                                    }
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = language.localName,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        // Add divider between items (but not after the last one)
                        if (index < languages.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
            
            // Information text
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Language Information",
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "The app language affects all menus, buttons, and text throughout the app. It does not change the content of your data.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Some system-level notifications might still appear in your device's default language.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        // Language change confirmation dialog
        if (showLanguageChangeDialog) {
            val languageName = languages.first { it.code == pendingLanguage }.name
            
            AlertDialog(
                onDismissRequest = { showLanguageChangeDialog = false },
                title = { Text("Change Language") },
                text = { 
                    Text("Are you sure you want to change the app language to $languageName? The app will restart to apply the changes.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            selectedLanguage = pendingLanguage
                            showLanguageChangeDialog = false
                            // In a real app, this would update the language setting and restart the app
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Change")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLanguageChangeDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

/**
 * Data class representing a language option.
 */
data class LanguageOption(
    val name: String,
    val localName: String,
    val isSelected: Boolean,
    val code: String = name.lowercase()
)