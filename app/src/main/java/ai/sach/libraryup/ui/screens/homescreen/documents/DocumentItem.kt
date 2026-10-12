package ai.sach.libraryup.ui.screens.homescreen.documents

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import coil.compose.AsyncImage
import java.io.File
import java.io.FileNotFoundException


@Composable
fun DocumentItem(
    name: String,
    uploadDate: String,
    documentPath: String,
    mimeType: String? = null,
    onDeleteClick: () -> Unit,
    downloadAndOpen: (String, String, String?) -> Unit // new lambda for download+open
) {
    val context = LocalContext.current
    var showImageDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        name,
                        fontWeight = FontWeight.Medium, // name is originalName
                        maxLines = 1,
                        modifier = Modifier.widthIn(max = 180.dp),
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(uploadDate, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row {
                // View document button (show image in-app if image, else download/open)
                IconButton(onClick = {
                    if (mimeType?.startsWith("image/") == true) {
                        showImageDialog = true
                    } else {
                        downloadAndOpen(documentPath, name, mimeType)
                    }
                }) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = "View",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Delete document button
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red
                    )
                }
            }
        }
    }

    // Show image dialog if requested
    if (showImageDialog) {
        AlertDialog(
            onDismissRequest = { showImageDialog = false },
            text = {
                AsyncImage(
                    model = documentPath,
                    contentDescription = name,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Row {
                    IconButton(onClick = {
                        shareImage(context, documentPath, mimeType)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share Image")
                    }
                    TextButton(onClick = { showImageDialog = false }) {
                        Text("Close")
                    }
                }
            }
        )
    }
}
/**
 * Safely opens a document using proper content URIs and permissions
 * This implementation fixes security issues with file access
 */
internal fun openDocument(context: Context, documentPath: String, mimeType: String?) {
    try {
        // Step 1: Resolve the URI and mime type
        val contentUri: Uri
        val resolvedMimeType: String

        when {
            // Handle content:// URIs directly
            documentPath.startsWith("content://") -> {
                contentUri = documentPath.toUri()
                resolvedMimeType = mimeType ?:
                        context.contentResolver.getType(contentUri) ?:
                        getMimeTypeFromExtension(documentPath.toUri().lastPathSegment ?: "")
                Log.d("DocumentOpener", "Using content URI directly: $contentUri")
            }

            // Handle file:// URIs or file paths
            else -> {
                // Extract file path
                val filePath = if (documentPath.startsWith("file://")) {
                    documentPath.removePrefix("file://")
                } else {
                    documentPath
                }

                val file = File(filePath)
                if (!file.exists()) {
                    throw FileNotFoundException("File not found: $filePath")
                }

                // Use proper authority from manifest
                val authority = "${context.packageName}.fileprovider"

                try {
                    contentUri = FileProvider.getUriForFile(context, authority, file)
                    Log.d("DocumentOpener", "Created content URI: $contentUri from file: $filePath")
                } catch (e: IllegalArgumentException) {
                    // This happens when the file is not in an allowed directory
                    Log.e("DocumentOpener", "File is outside allowed directories: $filePath", e)
                    throw SecurityException("Cannot share this file. It must be in an app-specific directory.")
                }

                resolvedMimeType = mimeType ?: getMimeTypeFromExtension(file.name)
            }
        }

        Log.d("DocumentOpener", "Opening $contentUri with mime type: $resolvedMimeType")

        // Step 2: Create secure intent with proper flags
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, resolvedMimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Step 3: Check if we have an app to handle this file type
        val packageManager = context.packageManager
        val activities = packageManager.queryIntentActivities(intent, 0)

        if (activities.isEmpty()) {
            Toast.makeText(
                context,
                "No application found to open this type of document (${resolvedMimeType})",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Step 4: Grant permissions to all apps that can handle this intent
        for (resolveInfo in activities) {
            val packageName = resolveInfo.activityInfo.packageName
            context.grantUriPermission(
                packageName,
                contentUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        // Step 5: Launch the intent
        context.startActivity(intent)

    } catch (e: FileNotFoundException) {
        Log.e("DocumentOpener", "File not found: ${e.message}")
        Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
    } catch (e: SecurityException) {
        Log.e("DocumentOpener", "Security error: ${e.message}")
        Toast.makeText(context, "Security error: Cannot access file", Toast.LENGTH_SHORT).show()
    } catch (e: ActivityNotFoundException) {
        Log.e("DocumentOpener", "No application found to open this document type", e)
        Toast.makeText(context, "No application found to open this document type", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Log.e("DocumentOpener", "Error opening document: ${e.javaClass.simpleName}: ${e.message}", e)
        Toast.makeText(context, "Error opening document", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Get MIME type from file extension with improved reliability
 */
private fun getMimeTypeFromExtension(filename: String): String {
    val extension = MimeTypeMap.getFileExtensionFromUrl(filename)
    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension?.lowercase())

    return mimeType ?: when {
        filename.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
        filename.endsWith(".jpg", ignoreCase = true) ||
                filename.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
        filename.endsWith(".png", ignoreCase = true) -> "image/png"
        filename.endsWith(".doc", ignoreCase = true) -> "application/msword"
        filename.endsWith(".docx", ignoreCase = true) ->
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        filename.endsWith(".txt", ignoreCase = true) -> "text/plain"
        else -> "application/octet-stream"  // Safe fallback
    }
}

fun shareImage(context: Context, documentPath: String, mimeType: String?) {
    try {
        val uri = when {
            documentPath.startsWith("content://") -> documentPath.toUri()
            else -> {
                val filePath = if (documentPath.startsWith("file://")) {
                    documentPath.removePrefix("file://")
                } else {
                    documentPath
                }
                val file = File(filePath)
                val authority = "${context.packageName}.fileprovider"
                FileProvider.getUriForFile(context, authority, file)
            }
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType ?: "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to share image", Toast.LENGTH_SHORT).show()
        Log.e("DocumentItem", "Error sharing image", e)
    }
}