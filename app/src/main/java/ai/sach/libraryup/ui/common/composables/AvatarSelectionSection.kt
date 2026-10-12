package ai.sach.rentup.ui.common.composables

import ai.sach.rentup.utils.ImageUtils
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import java.io.File
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import android.graphics.Bitmap
import android.util.Log

/**
 * Composable for selecting and displaying a tenant's avatar image.
 */
@Composable
fun AvatarSelectionSection(
    avatarUri: Uri?, // Uri for displaying in Coil
    onAvatarSelected: (Uri?) -> Unit, // Callback with the content Uri selected by user
    tenantName: String
) {
    val context = LocalContext.current

    // Image picker launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        // Pass the selected content Uri back immediately for processing
        onAvatarSelected(uri)
    }

    // Avatar display/selection UI
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant) // Use theme color
            .clickable { launcher.launch("image/*") }, // Launch image picker
        contentAlignment = Alignment.Center
    ) {
        if (avatarUri != null) {
            AsyncImage(
                model = avatarUri, // Load the Uri (content or file)
                contentDescription = "Selected Avatar for $tenantName",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Placeholder when no image is selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person, // Use appropriate icon
                    contentDescription = "Default Avatar Placeholder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, // Theme color
                    modifier = Modifier.size(40.dp) // Adjust size
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Add Photo",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant // Theme color
                )
            }
        }
    }
}

/**
 * Processes the selected image Uri, saves it to internal storage,
 * and returns the Uri of the saved file as a String.
 * Optimized to prevent memory leaks and GC blocking.
 */
suspend fun processSelectedImage(
    context: Context,
    contentUri: Uri?, // The Uri received from ActivityResultContracts.GetContent()
    baseFileName: String, // Base name for the file (e.g., tenant name)
    onImageProcessed: (String?) -> Unit // Callback with the *String* Uri of the saved file
) {
    contentUri?.let { uri ->
        var bitmap: Bitmap? = null
        try {
            // Load bitmap with memory optimization
            bitmap = ImageUtils.loadBitmapFromUri(context, uri)
            
            bitmap?.let { bm ->
                // Sanitize filename
                val safeFileName = baseFileName.replace(Regex("[^a-zA-Z0-9_]"), "_").take(50)
                val finalFileName = "${safeFileName}_${System.currentTimeMillis()}.jpg"

                // Create file in internal storage
                val file = File(context.filesDir, finalFileName)
                
                // Save bitmap with proper memory management
                val success = ImageUtils.saveBitmapToFile(bm, file)
                if (success) {
                    val savedFileUri = ImageUtils.getContentUri(context, file)
                    onImageProcessed(savedFileUri.toString())
                    Log.d("AvatarSelectionSection", "Image processed successfully: $savedFileUri")
                } else {
                    Log.w("AvatarSelectionSection", "Failed to save bitmap to file")
                    onImageProcessed(null)
                }
            } ?: run {
                Log.w("AvatarSelectionSection", "Failed to load bitmap from URI: $uri")
                onImageProcessed(null)
            }
        } catch (e: Exception) {
            Log.e("AvatarSelectionSection", "Error processing image: ${e.message}", e)
            onImageProcessed(null)
        } finally {
            // Always recycle bitmap to free memory
            ImageUtils.recycleBitmap(bitmap)
        }
    } ?: onImageProcessed(null)
}