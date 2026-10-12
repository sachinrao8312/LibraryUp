package ai.sach.libraryup.ui.common.document

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.FileNotFoundException
import java.io.IOException

private const val TAG = "DocumentPickerButton"
private const val MAX_FILE_SIZE_MB = 10 // Maximum file size in MB

/**
 * Document type configuration for the document picker
 * @param title Title to display in the selection dialog
 * @param mimeType MIME type for the document (e.g., "application/pdf")
 * @param fallbackExtension Extension to use if MIME type cannot be determined
 * @param maxSizeMB Maximum file size in MB (defaults to global setting)
 */
data class DocumentTypeConfig(
    val title: String,
    val mimeType: String,
    val fallbackExtension: String,
    val maxSizeMB: Int = MAX_FILE_SIZE_MB,
    val icon: @Composable () -> Unit = { Icon(Icons.AutoMirrored.Filled.InsertDriveFile,"Download Button") }
)

/**
 * A button that allows selecting documents with customizable document types
 * and proper error handling.
 *
 * @param onDocumentSelected Callback when a document is selected successfully
 * @param onError Optional callback for handling errors
 * @param selectedFileName Currently selected filename to display
 * @param documentTypes List of document types to support (defaults to PDF and images)
 * @param buttonText Text to display on the button
 */
@Composable
fun DocumentPickerButton(
    onDocumentSelected: (Uri, String, String) -> Unit,
    onError: ((String) -> Unit)? = null,
    selectedFileName: String? = null,
    documentTypes: List<DocumentTypeConfig> = listOf(
        DocumentTypeConfig(
            title = "PDF Document",
            mimeType = "application/pdf",
            fallbackExtension = "pdf",
            icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) }
        ),
        DocumentTypeConfig(
            title = "Image",
            mimeType = "image/*",
            fallbackExtension = "jpg"
        )
    ),
    buttonText: String = "Select Document"
) {
    val context = LocalContext.current
    var showDocTypeDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Create content launcher
    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) {
            val message = "Document selection cancelled or failed"
            Log.w(TAG, message)
            errorMessage = message
            onError?.invoke(message)
            return@rememberLauncherForActivityResult
        }

        try {
            // Get MIME type and validate
            val mimeType = context.contentResolver.getType(uri)
            if (mimeType == null) {
                val message = "Could not determine document type"
                Log.w(TAG, message)
                errorMessage = message
                onError?.invoke(message)
                return@rememberLauncherForActivityResult
            }

            // Check file size
            val fileSize = getFileSize(context, uri)
            if (fileSize > MAX_FILE_SIZE_MB * 1024 * 1024) {
                val message = "File size exceeds maximum allowed ($MAX_FILE_SIZE_MB MB)"
                Log.w(TAG, message)
                errorMessage = message
                onError?.invoke(message)
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }

            // Get filename
            val fileName = getFileName(context, uri)
                ?: "Document_${System.currentTimeMillis()}"

            // Success - invoke callback
            onDocumentSelected(uri, fileName, mimeType)
            errorMessage = null
        } catch (e: SecurityException) {
            val message = "Permission denied: ${e.localizedMessage}"
            Log.e(TAG, message, e)
            errorMessage = message
            onError?.invoke(message)
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        } catch (e: FileNotFoundException) {
            val message = "File not found: ${e.localizedMessage}"
            Log.e(TAG, message, e)
            errorMessage = message
            onError?.invoke(message)
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            val message = "I/O error: ${e.localizedMessage}"
            Log.e(TAG, message, e)
            errorMessage = message
            onError?.invoke(message)
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            val message = "Error processing document: ${e.localizedMessage}"
            Log.e(TAG, message, e)
            errorMessage = message
            onError?.invoke(message)
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = {
                if (documentTypes.size == 1) {
                    // If only one document type, launch directly
                    documentLauncher.launch(documentTypes[0].mimeType)
                } else {
                    showDocTypeDialog = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.AttachFile, contentDescription = "Attach")
            Spacer(modifier = Modifier.width(8.dp))
            Text(buttonText)
        }

        // Display selected filename or error message
        if (!selectedFileName.isNullOrBlank()) {
            Text(
                text = "Selected: $selectedFileName",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else if (!errorMessage.isNullOrBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
        }
    }

    // Document type selection dialog
    if (showDocTypeDialog) {
        AlertDialog(
            onDismissRequest = { showDocTypeDialog = false },
            title = { Text("Select Document Type") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    documentTypes.forEach { docType ->
                        Button(
                            onClick = {
                                documentLauncher.launch(docType.mimeType)
                                showDocTypeDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            docType.icon()
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(docType.title)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDocTypeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Helper function to get filename from URI
 * @return The filename or null if it couldn't be determined
 */
fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = it.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DocumentPicker", "Error getting filename", e)
        }
    }

    // Fallback to path
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != -1 && cut != null) {
            result = result?.substring(cut + 1)
        }
    }

    return result
}

/**
 * Helper function to get file size from URI
 * @return The file size in bytes, or 0 if it couldn't be determined
 */
fun getFileSize(context: Context, uri: Uri): Long {
    var fileSize: Long = 0
    try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1 && !it.isNull(sizeIndex)) {
                    fileSize = it.getLong(sizeIndex)
                }
            }
        }

        // If size couldn't be determined from the cursor, try opening the input stream
        if (fileSize == 0L) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                fileSize = inputStream.available().toLong()
            }
        }
    } catch (e: Exception) {
        Log.e("DocumentPicker", "Error getting file size", e)
    }

    return fileSize
}