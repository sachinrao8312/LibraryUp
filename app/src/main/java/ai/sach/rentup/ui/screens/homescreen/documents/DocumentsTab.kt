package ai.sach.rentup.ui.screens.homescreen.documents


import ai.sach.rentup.data.local.entity.TenantDocument
import ai.sach.rentup.data.viewmodel.TenantViewModel
import ai.sach.rentup.ui.common.dialog.AddDocumentDialog
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import android.net.Uri
import android.database.Cursor
import android.provider.OpenableColumns
import java.net.URL
import java.io.File
import android.util.Log

fun getDisplayNameFromUri(context: Context, uriString: String): String {
    val uri = Uri.parse(uriString)
    var name = uri.lastPathSegment ?: uriString
    if (uriString.startsWith("content://")) {
        try {
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst() && nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        } catch (e: SecurityException) {
            // No permission to access this URI, fallback to lastPathSegment
            // Optionally log or show a warning
        }
    }
    return name
}

suspend fun downloadFileToLocalStorage(context: Context, url: String, fileName: String): File = withContext(Dispatchers.IO) {
    val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
    val dir = File(context.filesDir, "Documents")
    if (!dir.exists()) dir.mkdirs()
    val file = File(dir, safeName)
    URL(url).openStream().use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    file
}

fun isCloudDocument(path: String) = path.startsWith("http://") || path.startsWith("https://")
fun isLocalContentUri(path: String) = path.startsWith("content://")
fun isLocalFilePath(context: Context, path: String) = path.startsWith(context.filesDir.absolutePath)

fun downloadAndOpenDocument(context: Context, viewModel: TenantViewModel, document: TenantDocument) {
    when {
        isCloudDocument(document.documentPath) -> {
            val safeName = (document.description ?: document.documentPath.substringAfterLast('/')).replace(Regex("[^A-Za-z0-9._-]"), "_")
            val localFile = File(context.filesDir, "Documents/$safeName")
            if (localFile.exists()) {
                openDocument(context, localFile.absolutePath, document.mimeType)
            } else {
                viewModel.viewModelScope.launch {
                    try {
                        val file = downloadFileToLocalStorage(context, document.documentPath, safeName)
                        openDocument(context, file.absolutePath, document.mimeType)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to download file: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        isLocalContentUri(document.documentPath) -> {
            openDocument(context, document.documentPath, document.mimeType)
        }
        isLocalFilePath(context, document.documentPath) -> {
            openDocument(context, document.documentPath, document.mimeType)
        }
        else -> {
            Toast.makeText(context, "Unknown document type", Toast.LENGTH_SHORT).show()
        }
    }
}

fun deleteDocument(context: Context, viewModel: TenantViewModel, document: TenantDocument) {
    viewModel.viewModelScope.launch {
        viewModel.deleteLocalDocument(document)
        when {
            isCloudDocument(document.documentPath) -> {
                val safeName = (document.description ?: document.documentPath.substringAfterLast('/')).replace(Regex("[^A-Za-z0-9._-]"), "_")
                val localFile = File(context.filesDir, "Documents/$safeName")
                if (localFile.exists()) localFile.delete()
            }
            isLocalContentUri(document.documentPath) -> {
                // No direct delete for content://, just remove from Room
            }
            isLocalFilePath(context, document.documentPath) -> {
                val file = File(document.documentPath)
                if (file.exists()) file.delete()
            }
        }
    }
}

@Composable
fun DocumentsTab(
    documents: List<TenantDocument>,
    tenantId: Long,
    viewModel: TenantViewModel,
    tenantName: String, // <-- Add tenantName
    tenantAvatarUri: String? // <-- Add avatarUri
) {
    var isAddDocumentDialogVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header with add button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Documents",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = { isAddDocumentDialogVisible = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Document")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // List of documents
        if (documents.isEmpty()) {
            Text(
                text = "No documents uploaded yet",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        } else {
            documents.forEach { doc ->
                Log.d("DocumentsTab", "Rendering doc: path=${doc.documentPath}, mimeType=${doc.mimeType}, desc=${doc.description}")
                val isLocal = isLocalContentUri(doc.documentPath) || isLocalFilePath(context, doc.documentPath)
                val displayName = when {
                    isLocal -> getDisplayNameFromUri(context, doc.documentPath)
                    doc.description != null -> doc.description
                    else -> doc.documentPath.substringAfterLast('/')
                }
                val effectiveMimeType = doc.mimeType ?: if (displayName.endsWith(".pdf", true)) "application/pdf" else null
                DocumentItem(
                    name = displayName,
                    uploadDate = doc.uploadDate,
                    documentPath = doc.documentPath,
                    mimeType = effectiveMimeType,
                    onDeleteClick = { deleteDocument(context, viewModel, doc) },
                    downloadAndOpen = { _, _, _ -> downloadAndOpenDocument(context, viewModel, doc) }
                )
            }
        }
    }

    // Add document dialog
    if (isAddDocumentDialogVisible) {
        AddDocumentDialog(
            tenantId = tenantId,
            onDismiss = { isAddDocumentDialogVisible = false },
            onAddDocument = { document ->
                viewModel.insertDocument(document)
                isAddDocumentDialogVisible = false
            }
        )
    }
}